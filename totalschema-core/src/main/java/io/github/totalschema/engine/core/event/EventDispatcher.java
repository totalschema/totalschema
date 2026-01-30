/*
 * totalschema: tool for managing database versioning and schema changes with ease.
 * Copyright (C) 2025 totalschema development team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.github.totalschema.engine.core.event;

import io.github.totalschema.concurrent.LockTemplate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generic event dispatcher that supports type-safe subscription and notification of application
 * events. Supports multiple event types with independent listener lists.
 *
 * <p>This implementation is thread-safe and collects all exceptions from listeners, throwing them
 * after all listeners have been notified.
 *
 * <p>The event dispatcher provides a publish-subscribe pattern where listeners can register
 * interest in specific event types and will be notified when events of those types are dispatched.
 * The system is fully type-safe, with compile-time checking of event types and listener
 * compatibility.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * EventDispatcher eventDispatcher = new EventDispatcher();
 *
 * // Subscribe to an event with a lambda
 * eventDispatcher.subscribe(ChangeEngineCloseEvent.class, event -> {
 *     System.out.println("Engine closed at: " + event.getTimestamp());
 * });
 *
 * // Or with a method reference
 * eventDispatcher.subscribe(ChangeEngineCloseEvent.class, this::handleClose);
 *
 * // Dispatch the event
 * eventDispatcher.dispatch(new ChangeEngineCloseEvent());
 * }</pre>
 *
 * <p><strong>Thread Safety:</strong> All methods are thread-safe. Multiple threads can concurrently
 * subscribe, unsubscribe, and dispatch events without external synchronization.
 *
 * <p><strong>Exception Handling:</strong> If a listener throws an exception during event
 * notification, the exception is caught and collected. After all listeners have been notified,
 * collected exceptions are thrown. This ensures that one failing listener doesn't prevent other
 * listeners from being notified.
 *
 * @see Event
 * @see EventListener
 * @see Subscription
 */
public final class EventDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(EventDispatcher.class);

    private final LockTemplate lockTemplate =
            new LockTemplate(30, TimeUnit.SECONDS, new ReentrantLock());

    // Map of event type to set of listeners for that event type
    private final Map<Class<?>, Set<EventListener<?>>> eventListeners = new ConcurrentHashMap<>();

    /**
     * Subscribes a listener to events of the specified type.
     *
     * @param <E> the event type
     * @param eventType the class of the event to listen for
     * @param listener the listener to be notified when events of this type are fired
     * @return a subscription handle that can be used to unsubscribe
     */
    public <E extends Event> Subscription<E> subscribe(
            Class<E> eventType, EventListener<E> listener) {
        Objects.requireNonNull(eventType, "Event type cannot be null");
        Objects.requireNonNull(listener, "Listener cannot be null");

        lockTemplate.withTryLock(
                () -> {
                    eventListeners
                            .computeIfAbsent(eventType, k -> new LinkedHashSet<>())
                            .add(listener);
                    logger.debug("Subscribed listener for event type: {}", eventType.getName());
                    return null;
                });

        return new Subscription<>(this, eventType, listener);
    }

    /**
     * Unsubscribes a listener from events of the specified type.
     *
     * @param <E> the event type
     * @param eventType the class of the event
     * @param listener the listener to unsubscribe
     * @return true if the listener was subscribed and has been removed, false otherwise
     */
    public <E extends Event> boolean unsubscribe(Class<E> eventType, EventListener<E> listener) {
        Objects.requireNonNull(eventType, "Event type cannot be null");
        Objects.requireNonNull(listener, "Listener cannot be null");

        return lockTemplate.withTryLock(
                () -> {
                    Set<EventListener<?>> listeners = eventListeners.get(eventType);
                    if (listeners != null) {
                        boolean removed = listeners.remove(listener);
                        if (removed) {
                            logger.debug(
                                    "Unsubscribed listener for event type: {}",
                                    eventType.getName());
                            if (listeners.isEmpty()) {
                                eventListeners.remove(eventType);
                            }
                        }
                        return removed;
                    }
                    return false;
                });
    }

    /**
     * Dispatches an event, notifying all subscribed listeners. Collects exceptions from listeners
     * and throws them after all listeners have been notified.
     *
     * @param <E> the event type
     * @param event the event to fire
     * @throws RuntimeException if one or more listeners throw exceptions
     */
    public <E extends Event> void dispatch(E event) {
        Objects.requireNonNull(event, "Event cannot be null");

        Class<?> eventType = event.getClass();

        List<EventListener<E>> listenerSnapshot =
                lockTemplate.withTryLock(
                        () -> {
                            Set<EventListener<?>> listeners = eventListeners.get(eventType);
                            if (listeners == null || listeners.isEmpty()) {
                                return Collections.emptyList();
                            }
                            @SuppressWarnings("unchecked")
                            List<EventListener<E>> snapshot =
                                    listeners.stream()
                                            .map(l -> (EventListener<E>) l)
                                            .collect(Collectors.toList());
                            return snapshot;
                        });

        if (listenerSnapshot.isEmpty()) {
            logger.trace("No listeners for event type: {}", eventType.getName());
            return;
        }

        List<Exception> caughtExceptions = new ArrayList<>();

        for (EventListener<E> listener : listenerSnapshot) {
            try {
                logger.trace("Notifying listener about event: {}", eventType.getName());
                listener.onEvent(event);
            } catch (RuntimeException exception) {
                logger.warn(
                        "Listener threw exception for event type: {}",
                        eventType.getName(),
                        exception);
                caughtExceptions.add(exception);
            }
        }

        if (caughtExceptions.size() > 1) {
            RuntimeException runtimeException =
                    new RuntimeException(
                            caughtExceptions.size()
                                    + " listener notifications failed for event: "
                                    + eventType.getName());
            for (Exception ex : caughtExceptions) {
                runtimeException.addSuppressed(ex);
            }
            throw runtimeException;
        } else if (caughtExceptions.size() == 1) {
            throw new RuntimeException(
                    "Listener notification failed for event: " + eventType.getName(),
                    caughtExceptions.get(0));
        }
    }

    /**
     * Returns the number of listeners subscribed to events of the specified type.
     *
     * @param eventType the event type to query
     * @return the number of subscribed listeners
     */
    public int getListenerCount(Class<?> eventType) {
        return lockTemplate.withTryLock(
                () -> {
                    Set<EventListener<?>> listeners = eventListeners.get(eventType);
                    return listeners != null ? listeners.size() : 0;
                });
    }

    /** Removes all listeners for all event types. */
    public void clear() {
        lockTemplate.withTryLock(
                () -> {
                    eventListeners.clear();
                    logger.debug("Cleared all event listeners");
                    return null;
                });
    }
}
