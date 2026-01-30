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
 * Event representing the closure of a ChangeEngine.
 *
 * <p>This event is dispatched when a {@link io.github.totalschema.engine.api.ChangeEngine} instance
 * is being closed. Listeners can subscribe to this event to perform cleanup operations, release
 * resources, or perform final logging before the engine shuts down.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * eventDispatcher.subscribe(ChangeEngineCloseEvent.class, event -> {
 *     logger.info("Engine is closing, performing cleanup...");
 *     // Perform cleanup
 * });
 * }</pre>
 *
 * @see io.github.totalschema.engine.api.ChangeEngine
 * @see EventDispatcher
 */
public final class ChangeEngineCloseEvent extends Event {}
