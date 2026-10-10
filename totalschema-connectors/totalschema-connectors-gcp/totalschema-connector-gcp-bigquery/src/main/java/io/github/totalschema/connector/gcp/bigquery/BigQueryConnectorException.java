/*
 * totalschema: tool for managing database versioning and schema changes with ease.
 * Copyright (C) 2025-2026 totalschema development team
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

package io.github.totalschema.connector.gcp.bigquery;

/** Exception thrown when BigQuery connector operations fail. */
public class BigQueryConnectorException extends RuntimeException {

    /**
     * Constructs a new BigQueryConnectorException with the specified detail message.
     *
     * @param message the detail message
     */
    public BigQueryConnectorException(String message) {
        super(message);
    }

    /**
     * Constructs a new BigQueryConnectorException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause
     */
    public BigQueryConnectorException(String message, Throwable cause) {
        super(message, cause);
    }
}
