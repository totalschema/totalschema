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

package io.github.totalschema.spi.sql;

import io.github.totalschema.engine.internal.sql.CreateTableBuilder;

/**
 * SQL dialect interface for database-specific SQL syntax. Provides methods for generating
 * database-specific column type definitions.
 *
 * <p>This interface is focused solely on SQL type mappings and does not handle SQL statement
 * construction. For building CREATE TABLE statements, use {@link CreateTableBuilder}.
 */
public interface SqlDialect {
    /**
     * Returns the VARCHAR column type definition for this dialect.
     *
     * @param length the maximum length of the varchar column
     * @return the VARCHAR type definition (e.g., "VARCHAR(255)")
     */
    String varchar(int length);

    /**
     * Returns the TIMESTAMP column type definition for this dialect.
     *
     * @return the TIMESTAMP type definition (e.g., "TIMESTAMP")
     */
    String timestamp();
}
