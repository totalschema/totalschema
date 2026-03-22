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

package io.github.totalschema.concurrent;

/**
 * Exception thrown when a lock cannot be acquired within the specified timeout period.
 *
 * <p>This exception is thrown by {@link LockTemplate} when it fails to acquire a lock. It provides
 * type safety and allows callers to distinguish lock acquisition failures from other runtime
 * exceptions.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * try {
 *     lockTemplate.withTryLock(() -> {
 *         // critical section
 *     });
 * } catch (LockAcquisitionException e) {
 *     // Handle lock timeout specifically
 *     log.warn("Could not acquire lock: {}", e.getMessage());
 * }
 * }</pre>
 *
 * @see LockTemplate
 */
public class LockAcquisitionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new lock acquisition exception with the specified detail message.
     *
     * @param message the detail message explaining why the lock could not be acquired
     */
    public LockAcquisitionException(String message) {
        super(message);
    }

    /**
     * Constructs a new lock acquisition exception with the specified detail message and cause.
     *
     * @param message the detail message explaining why the lock could not be acquired
     * @param cause the cause of the failure (e.g., {@link InterruptedException})
     */
    public LockAcquisitionException(String message, Throwable cause) {
        super(message, cause);
    }
}
