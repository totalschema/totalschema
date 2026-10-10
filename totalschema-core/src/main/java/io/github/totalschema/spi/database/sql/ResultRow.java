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

package io.github.totalschema.spi.database.sql;

import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * Represents a single row of query results.
 *
 * <p>Implementations provide access to column values by name. Column names are normalized to
 * uppercase for case-insensitive access.
 */
public interface ResultRow {

    /**
     * Retrieves the value of the designated column as a String.
     *
     * @param columnName the name of the column
     * @return the column value as a String, or null if the value is NULL
     * @throws SQLException if the column name is not found or a database error occurs
     */
    String getString(String columnName) throws SQLException;

    /**
     * Retrieves the value of the designated column as a Timestamp.
     *
     * @param columnName the name of the column
     * @return the column value as a Timestamp, or null if the value is NULL
     * @throws SQLException if the column name is not found or a database error occurs
     */
    Timestamp getTimestamp(String columnName) throws SQLException;
}
