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

package io.github.totalschema.spi.change;

import io.github.totalschema.model.ChangeFile;

/** Exception thrown when the execution of a {@link ChangeFile} fails. */
public class ChangeExecutionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final ChangeFile changeFile;

    /**
     * Constructs a new change execution exception with the failed change file and a detail message.
     *
     * @param changeFile the change file whose execution failed
     * @param message the detail message describing the failure
     */
    public ChangeExecutionException(ChangeFile changeFile, String message) {
        super(message);
        this.changeFile = changeFile;
    }

    /**
     * Constructs a new change execution exception with the failed change file, a detail message,
     * and the underlying cause.
     *
     * @param changeFile the change file whose execution failed
     * @param message the detail message describing the failure
     * @param cause the cause of the failure
     */
    public ChangeExecutionException(ChangeFile changeFile, String message, Throwable cause) {
        super(message, cause);
        this.changeFile = changeFile;
    }

    /**
     * Returns the change file whose execution failed.
     *
     * @return the failed change file
     */
    public ChangeFile getChangeFile() {
        return changeFile;
    }
}
