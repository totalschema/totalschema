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

import io.github.totalschema.config.Configuration;

/**
 * Factory for database-specific SQL statement builders.
 *
 * <p>An instance is obtained from a {@link SqlDialect} via {@link SqlDialect#getSqlBuilder()}. Each
 * factory method returns a fluent builder for a specific kind of SQL statement, allowing
 * repositories to generate portable, database-specific SQL without hand-writing and concatenating
 * SQL strings.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * SqlBuilder sqlBuilder = sqlDialect.getSqlBuilder();
 *
 * String createTableSql = sqlBuilder.createTable("my_table", configuration)
 *     .column("id", d -> d.varchar(255))
 *     .primaryKey("id")
 *     .build();
 *
 * String insertSql = sqlBuilder.insertInto("my_table")
 *     .column("id")
 *     .build();
 * }</pre>
 *
 * @see SqlDialect
 * @see CreateTableBuilder
 * @see InsertStatementBuilder
 * @see SelectStatementBuilder
 * @see UpdateStatementBuilder
 * @see DeleteStatementBuilder
 * @see DropTableBuilder
 */
public interface SqlBuilder {

    /**
     * Creates a builder for a CREATE TABLE SQL statement.
     *
     * @param tableNameExpression the fully qualified table name (e.g., "schema.table")
     * @param configuration the configuration for column type and primary key overrides
     * @return a new {@link CreateTableBuilder} instance
     */
    CreateTableBuilder createTable(String tableNameExpression, Configuration configuration);

    /**
     * Creates a builder for an INSERT INTO SQL statement.
     *
     * @param tableNameExpression the fully qualified table name (e.g., "schema.table")
     * @return a new {@link InsertStatementBuilder} instance
     */
    InsertStatementBuilder insertInto(String tableNameExpression);

    /**
     * Creates a builder for a SELECT SQL statement.
     *
     * @param tableNameExpression the fully qualified table name (e.g., "schema.table")
     * @return a new {@link SelectStatementBuilder} instance
     */
    SelectStatementBuilder select(String tableNameExpression);

    /**
     * Creates a builder for an UPDATE SQL statement.
     *
     * @param tableNameExpression the fully qualified table name (e.g., "schema.table")
     * @return a new {@link UpdateStatementBuilder} instance
     */
    UpdateStatementBuilder update(String tableNameExpression);

    /**
     * Creates a builder for a DELETE SQL statement.
     *
     * @param tableNameExpression the fully qualified table name (e.g., "schema.table")
     * @return a new {@link DeleteStatementBuilder} instance
     */
    DeleteStatementBuilder delete(String tableNameExpression);

    /**
     * Creates a builder for a DROP TABLE SQL statement.
     *
     * @param tableNameExpression the fully qualified table name (e.g., "schema.table")
     * @return a new {@link DropTableBuilder} instance
     */
    DropTableBuilder dropTable(String tableNameExpression);
}
