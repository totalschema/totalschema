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

/**
 * A handle for an event subscription that can be used to unsubscribe from events.
 *
 * <p>Subscriptions are returned by {@link EventDispatcher#subscribe(Class, EventListener)} and
 * provide a convenient way to manage the lifecycle of event listeners. Once unsubscribed, the
 * listener will no longer receive notifications for the event type.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * Subscription<ChangeEngineCloseEvent> subscription =
 *     eventDispatcher.subscribe(ChangeEngineCloseEvent.class, event -> {
 *         // Handle event
 *     });
 *
 * // Later, when no longer needed
 * subscription.unsubscribe();
 * }</pre>
 *
 * <p>This class is thread-safe. Multiple threads can safely call {@link #unsubscribe()} and {@link
 * #isActive()} concurrently.
 *
 * @param <E> the type of event this subscription is for
 * @see EventDispatcher#subscribe(Class, EventListener)
 * @see EventListener
 */
public final class Subscription<E extends Event> {
    private final EventDispatcher eventDispatcher;
    private final Class<E> eventType;
    private final EventListener<E> listener;
    private volatile boolean active = true;

    /**
     * Package-private constructor. Subscriptions should only be created by {@link EventDispatcher}.
     *
     * @param eventDispatcher the dispatcher that manages this subscription
     * @param eventType the type of event being subscribed to
     * @param listener the listener that will be notified of events
     */
    Subscription(EventDispatcher eventDispatcher, Class<E> eventType, EventListener<E> listener) {
        this.eventDispatcher = eventDispatcher;
        this.eventType = eventType;
        this.listener = listener;
    }

    /**
     * Unsubscribes this subscription, removing the listener from the event dispatcher.
     *
     * <p>After calling this method, the listener will no longer receive notifications for events of
     * the subscribed type. Calling this method multiple times has no additional effect.
     *
     * @return {@code true} if the subscription was active and has been successfully cancelled,
     *     {@code false} if it was already inactive
     */
    public boolean unsubscribe() {
        if (!active) {
            return false;
        }
        active = false;
        return eventDispatcher.unsubscribe(eventType, listener);
    }

    /**
     * Returns whether this subscription is still active.
     *
     * <p>A subscription is considered active if it has not been unsubscribed. Active subscriptions
     * will receive event notifications when events of the subscribed type are dispatched.
     *
     * @return {@code true} if the subscription is still active, {@code false} if it has been
     *     unsubscribed
     */
    public boolean isActive() {
        return active;
    }
}
