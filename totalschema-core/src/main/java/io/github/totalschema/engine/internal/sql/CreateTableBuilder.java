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

package io.github.totalschema.engine.internal.sql;

import io.github.totalschema.config.Configuration;
import io.github.totalschema.spi.sql.SqlDialect;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

/**
 * Fluent builder for constructing CREATE TABLE SQL statements with support for configuration
 * overrides. This builder integrates with {@link SqlDialect} for database-specific type expressions
 * and supports the project's configuration system for column type and SQL overrides.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * String sql = CreateTableBuilder.create(sqlDialect, "my_table", configuration)
 *     .column("id", d -> d.varchar(255))
 *     .column("timestamp", SqlDialect::timestamp)
 *     .primaryKey("id")
 *     .build();
 * }</pre>
 *
 * <p>Configuration overrides:
 *
 * <ul>
 *   <li>{@code table.columns.<columnName>.type} - Override specific column type
 *   <li>{@code table.primaryKeyClause.omit} - Set to true to omit primary key
 *   <li>{@code table.primaryKeyClause.definition} - Override primary key clause
 * </ul>
 */
public final class CreateTableBuilder {
    private final SqlDialect sqlDialect;
    private final String tableNameExpression;
    private final Configuration configuration;
    private final List<TableColumn> columns;
    private String primaryKeyClause;
    private final boolean omitPrimaryKey;

    /**
     * Creates a new CREATE TABLE builder using the specified SQL dialect and configuration.
     *
     * @param sqlDialect the SQL dialect for database-specific expressions
     * @param tableNameExpression the fully qualified table name (e.g., "schema.table")
     * @param configuration the configuration for overrides
     * @return a new CREATE TABLE builder instance
     */
    public static CreateTableBuilder create(
            SqlDialect sqlDialect, String tableNameExpression, Configuration configuration) {
        return new CreateTableBuilder(sqlDialect, tableNameExpression, configuration);
    }

    /**
     * Private constructor - use {@link #create(SqlDialect, String, Configuration)} instead.
     *
     * @param sqlDialect the SQL dialect for database-specific expressions
     * @param tableNameExpression the fully qualified table name (e.g., "schema.table")
     * @param configuration the configuration for overrides
     */
    private CreateTableBuilder(
            SqlDialect sqlDialect, String tableNameExpression, Configuration configuration) {
        this.sqlDialect = sqlDialect;
        this.tableNameExpression = tableNameExpression;
        this.configuration = configuration;
        this.columns = new LinkedList<>();
        this.omitPrimaryKey = false;
    }

    /**
     * Adds a column to the table. The type expression is provided by a function that receives the
     * SqlDialect, allowing database-specific type generation. Configuration can override the type
     * via {@code table.columns.<columnName>.type}.
     *
     * @param columnName the column name
     * @param typeExpressionProvider function that generates the SQL type expression using the
     *     dialect
     * @return this builder for method chaining
     */
    public CreateTableBuilder column(
            String columnName, Function<SqlDialect, String> typeExpressionProvider) {
        String defaultType = typeExpressionProvider.apply(sqlDialect);
        String actualType = getColumnType(columnName, defaultType);
        columns.add(new TableColumn(columnName, actualType));
        return this;
    }

    /**
     * Adds a column with a pre-computed type expression. Useful when the type doesn't come from
     * SqlDialect. Configuration can still override via {@code table.columns.<columnName>.type}.
     *
     * @param columnName the column name
     * @param typeExpression the SQL type expression (e.g., "VARCHAR(255)")
     * @return this builder for method chaining
     */
    public CreateTableBuilder column(String columnName, String typeExpression) {
        String actualType = getColumnType(columnName, typeExpression);
        columns.add(new TableColumn(columnName, actualType));
        return this;
    }

    /**
     * Sets the primary key constraint. Configuration can override via {@code
     * table.primaryKeyClause.definition} or omit via {@code table.primaryKeyClause.omit=true}.
     *
     * @param columnNames the column names forming the primary key
     * @return this builder for method chaining
     */
    public CreateTableBuilder primaryKey(String... columnNames) {
        if (columnNames == null || columnNames.length == 0) {
            throw new IllegalArgumentException("Primary key must have at least one column");
        }
        this.primaryKeyClause = formatPrimaryKeyClause(columnNames);
        return this;
    }

    /**
     * Sets a custom primary key clause directly.
     *
     * @param clause the custom primary key clause (e.g., "CONSTRAINT pk_name PRIMARY KEY (id)")
     * @return this builder for method chaining
     */
    @SuppressWarnings("unused") // Public API for extensibility
    public CreateTableBuilder primaryKeyClause(String clause) {
        this.primaryKeyClause = clause;
        return this;
    }

    /**
     * Builds the CREATE TABLE SQL statement. Respects configuration overrides for column types and
     * primary key clauses.
     *
     * @return the complete CREATE TABLE SQL statement
     * @throws IllegalStateException if no columns have been added
     */
    public String build() {
        if (columns.isEmpty()) {
            throw new IllegalStateException("Cannot build CREATE TABLE without columns");
        }

        StringBuilder sql = new StringBuilder("CREATE TABLE ");
        sql.append(tableNameExpression).append(" (");

        // Add columns
        final int numberOfColumns = columns.size();
        for (int i = 0; i < numberOfColumns; i++) {
            if (i > 0) {
                sql.append(", ");
            }
            TableColumn column = columns.get(i);
            sql.append(column.getName()).append(" ").append(column.getTypeExpression());
        }

        // Add primary key if configured
        if (shouldIncludePrimaryKey()) {
            String effectivePrimaryKeyClause = getEffectivePrimaryKeyClause();
            sql.append(", ").append(effectivePrimaryKeyClause);
        }

        sql.append(")");

        return sql.toString();
    }

    /**
     * Gets the column type, checking configuration for overrides.
     *
     * @param columnName the column name
     * @param defaultType the default type expression
     * @return the effective type expression (overridden or default)
     */
    private String getColumnType(String columnName, String defaultType) {
        return configuration.getString("table.columns." + columnName + ".type").orElse(defaultType);
    }

    /**
     * Formats the primary key clause from column names.
     *
     * @param columnNames the column names
     * @return formatted PRIMARY KEY clause
     */
    private String formatPrimaryKeyClause(String... columnNames) {
        return "PRIMARY KEY(" + String.join(", ", columnNames) + ")";
    }

    /**
     * Checks if the primary key should be included based on configuration.
     *
     * @return true if primary key should be included
     */
    private boolean shouldIncludePrimaryKey() {
        if (primaryKeyClause == null) {
            return false;
        }
        boolean omitFromConfig =
                configuration.getBoolean("table.primaryKeyClause.omit").orElse(false);
        return !omitFromConfig && !omitPrimaryKey;
    }

    /**
     * Gets the effective primary key clause, checking configuration for overrides.
     *
     * @return the effective primary key clause
     */
    private String getEffectivePrimaryKeyClause() {
        return configuration
                .getString("table.primaryKeyClause.definition")
                .orElse(primaryKeyClause);
    }
}
