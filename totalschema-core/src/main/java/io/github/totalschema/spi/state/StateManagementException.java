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

package io.github.totalschema.spi.state;

/**
 * Exception thrown when state management operations fail.
 *
 * <p>This exception is thrown by {@link StateService} and {@link StateRepository} implementations
 * when they encounter issues during state tracking, persistence, or retrieval operations. It
 * provides type safety and allows callers to distinguish state management failures from other
 * runtime exceptions.
 *
 * <p>Common scenarios:
 *
 * <ul>
 *   <li>Failure to persist state records to storage
 *   <li>Failure to retrieve state records from storage
 *   <li>State table initialization errors
 *   <li>File I/O errors during state operations
 *   <li>Concurrent state access violations
 * </ul>
 *
 * @see StateService
 * @see StateRepository
 */
public class StateManagementException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new state management exception with the specified detail message.
     *
     * @param message the detail message explaining why the state operation failed
     */
    public StateManagementException(String message) {
        super(message);
    }

    /**
     * Constructs a new state management exception with the specified detail message and cause.
     *
     * @param message the detail message explaining why the state operation failed
     * @param cause the underlying cause of the failure (e.g., {@link java.io.IOException}, {@link
     *     java.sql.SQLException})
     */
    public StateManagementException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new state management exception with the specified cause.
     *
     * @param cause the underlying cause of the failure
     */
    public StateManagementException(Throwable cause) {
        super(cause);
    }
}
