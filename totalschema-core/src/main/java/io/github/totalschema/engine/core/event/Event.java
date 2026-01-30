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
 * Base class for all events in the TotalSchema event system.
 *
 * <p>Events represent occurrences or state changes within the application that other components may
 * be interested in observing. Each event type extends this base class and carries specific
 * information about what happened.
 *
 * <p>Events are dispatched through the {@link EventDispatcher} to registered {@link EventListener}
 * instances. The event system is type-safe, allowing listeners to subscribe only to specific event
 * types they are interested in.
 *
 * <p>Example of creating a custom event:
 *
 * <pre>{@code
 * public class DatabaseMigrationEvent extends Event {
 *     private final String database;
 *     private final int version;
 *
 *     public DatabaseMigrationEvent(String database, int version) {
 *         this.database = database;
 *         this.version = version;
 *     }
 *
 *     public String getDatabase() { return database; }
 *     public int getVersion() { return version; }
 * }
 * }</pre>
 *
 * @see EventDispatcher
 * @see EventListener
 */
public abstract class Event {}
