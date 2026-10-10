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

package io.github.totalschema.spi.sql;

/**
 * SQL dialect interface for database-specific SQL syntax. Provides methods for generating
 * database-specific column type definitions, as well as a {@link SqlBuilder} factory for
 * constructing full SQL statements (CREATE TABLE, INSERT, SELECT, UPDATE, DELETE, DROP TABLE) in a
 * database-specific, portable way.
 *
 * @see SqlBuilder
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

    /**
     * Formats a table name expression according to the SQL dialect conventions.
     *
     * <p>This method applies dialect-specific formatting rules to table name expressions, such as
     * quoting identifiers or applying schema prefixes. The default implementation returns the table
     * name expression unmodified. Implementations should override this method if the dialect
     * requires custom formatting for table identifiers.
     *
     * @param tableNameExpression the table name expression to format (e.g., "schema.table",
     *     "table", or quoted identifiers)
     * @return the formatted table name expression suitable for use in SQL statements
     */
    default String formatTableName(String tableNameExpression) {
        return tableNameExpression;
    }

    /**
     * Returns a {@link SqlBuilder} bound to this dialect, providing factory methods for
     * constructing database-specific SQL statements (CREATE TABLE, INSERT, SELECT, UPDATE, DELETE,
     * DROP TABLE).
     *
     * <p>The default implementation returns a builder that delegates to this dialect's {@link
     * #varchar(int)}, {@link #timestamp()} and {@link #formatTableName(String)} methods.
     * Implementations may override this method to return a custom {@link SqlBuilder} if the dialect
     * requires statement-level customizations beyond type mappings and table name formatting.
     *
     * @return a {@link SqlBuilder} bound to this dialect
     */
    default SqlBuilder getSqlBuilder() {
        return new DefaultSqlBuilder(this);
    }
}
