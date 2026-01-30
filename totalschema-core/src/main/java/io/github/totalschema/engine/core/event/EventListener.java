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
 * Listener interface for receiving events of a specific type.
 *
 * <p>Implementations of this interface can be registered with an {@link EventDispatcher} to receive
 * notifications when events of type {@code E} are dispatched. The type parameter ensures type
 * safety at compile time.
 *
 * <p>Listeners are invoked synchronously on the same thread that dispatches the event. If a
 * listener throws an exception, the dispatcher will still notify all other listeners before
 * propagating the exception.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * EventListener<ChangeEngineCloseEvent> listener = event -> {
 *     System.out.println("Engine closed at: " + event.getTimestamp());
 * };
 *
 * eventDispatcher.subscribe(ChangeEngineCloseEvent.class, listener);
 * }</pre>
 *
 * @param <E> the type of event this listener handles, must extend {@link Event}
 * @see EventDispatcher
 * @see Event
 * @see Subscription
 */
@FunctionalInterface
public interface EventListener<E extends Event> {

    /**
     * Called when an event of the subscribed type is dispatched.
     *
     * <p>Implementations should be designed to execute quickly to avoid blocking the event
     * dispatcher. For long-running operations, consider offloading work to a separate thread.
     *
     * @param event the event that was dispatched, never {@code null}
     */
    void onEvent(E event);
}
