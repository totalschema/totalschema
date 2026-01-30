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

import java.io.Closeable;
import java.io.IOException;
import java.util.Objects;

/**
 * An event listener that closes a {@link Closeable} resource when the ChangeEngine closes.
 *
 * <p>This listener wraps a {@link Closeable} resource and automatically closes it when a {@link
 * ChangeEngineCloseEvent} is dispatched. This is useful for ensuring that resources like database
 * connections, file handles, or network connections are properly released when the engine shuts
 * down.
 *
 * <p>If the resource's {@link Closeable#close()} method throws an {@link IOException}, it will be
 * wrapped in a {@link RuntimeException} and propagated.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * Connection dbConnection = dataSource.getConnection();
 * var closeListener = CloseResourceChangeEngineCloseListener.create(dbConnection);
 * eventDispatcher.subscribe(ChangeEngineCloseEvent.class, closeListener);
 * }</pre>
 *
 * @see ChangeEngineCloseEvent
 * @see EventDispatcher
 * @see Closeable
 */
public final class CloseResourceChangeEngineCloseListener
        implements EventListener<ChangeEngineCloseEvent> {

    private final Closeable closeableResource;

    /**
     * Factory method to create a listener that closes the given resource.
     *
     * @param closeableResource the resource to close when the event is received, must not be {@code
     *     null}
     * @return a new listener instance
     * @throws NullPointerException if {@code closeableResource} is {@code null}
     */
    public static CloseResourceChangeEngineCloseListener create(Closeable closeableResource) {
        return new CloseResourceChangeEngineCloseListener(closeableResource);
    }

    /**
     * Creates a listener that closes the given resource.
     *
     * @param closeableResource the resource to close when the event is received, must not be {@code
     *     null}
     * @throws NullPointerException if {@code closeableResource} is {@code null}
     */
    public CloseResourceChangeEngineCloseListener(Closeable closeableResource) {
        this.closeableResource =
                Objects.requireNonNull(closeableResource, "closeableResource cannot be null");
    }

    /**
     * Closes the wrapped resource when the ChangeEngine close event is received.
     *
     * <p>If closing the resource throws an {@link IOException}, it will be wrapped in a {@link
     * RuntimeException}.
     *
     * @param event the close event (not used by this implementation)
     * @throws RuntimeException if closing the resource fails
     */
    @Override
    public void onEvent(ChangeEngineCloseEvent event) {
        try {
            closeableResource.close();

        } catch (IOException e) {
            throw new RuntimeException("Failure closing: " + closeableResource, e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        CloseResourceChangeEngineCloseListener that = (CloseResourceChangeEngineCloseListener) o;
        return Objects.equals(closeableResource, that.closeableResource);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(closeableResource);
    }

    @Override
    public String toString() {
        return "CloseResourceChangeEngineCloseListener{"
                + "closeableResource="
                + closeableResource
                + '}';
    }
}
