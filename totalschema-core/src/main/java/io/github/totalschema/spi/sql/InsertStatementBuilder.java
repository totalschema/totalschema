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

import java.util.LinkedList;
import java.util.List;

/**
 * Fluent builder for constructing INSERT INTO SQL statements. Instances are normally obtained via
 * {@link SqlBuilder#insertInto(String)}.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * String sql = sqlDialect.getSqlBuilder().insertInto("my_table")
 *     .column("id")
 *     .column("name")
 *     .columnWithValue("created_by", "CURRENT_USER")
 *     .build();
 * // INSERT INTO my_table (id, name, created_by) VALUES (?, ?, CURRENT_USER)
 * }</pre>
 */
public final class InsertStatementBuilder {

    private final SqlDialect sqlDialect;
    private final String tableNameExpression;
    private final List<String> columnNames = new LinkedList<>();
    private final List<String> valueExpressions = new LinkedList<>();

    static InsertStatementBuilder create(SqlDialect sqlDialect, String tableNameExpression) {
        return new InsertStatementBuilder(sqlDialect, tableNameExpression);
    }

    private InsertStatementBuilder(SqlDialect sqlDialect, String tableNameExpression) {
        this.sqlDialect = sqlDialect;
        this.tableNameExpression = tableNameExpression;
    }

    /**
     * Adds a column that will be bound with a {@code ?} placeholder parameter.
     *
     * @param columnName the column name
     * @return this builder for method chaining
     */
    public InsertStatementBuilder column(String columnName) {
        return columnWithValue(columnName, "?");
    }

    /**
     * Adds a column with an explicit, literal SQL value expression (e.g. {@code "NULL"} or {@code
     * "CURRENT_TIMESTAMP"}), instead of a bound {@code ?} placeholder.
     *
     * @param columnName the column name
     * @param valueExpression the raw SQL value expression to use for this column
     * @return this builder for method chaining
     */
    public InsertStatementBuilder columnWithValue(String columnName, String valueExpression) {
        columnNames.add(columnName);
        valueExpressions.add(valueExpression);
        return this;
    }

    /**
     * Builds the INSERT INTO SQL statement.
     *
     * @return the complete INSERT INTO SQL statement
     * @throws IllegalStateException if no columns have been added
     */
    public String build() {
        if (columnNames.isEmpty()) {
            throw new IllegalStateException("Cannot build INSERT INTO without columns");
        }

        String formattedTableName = sqlDialect.formatTableName(tableNameExpression);

        return "INSERT INTO "
                + formattedTableName
                + " ("
                + String.join(", ", columnNames)
                + ") VALUES ("
                + String.join(", ", valueExpressions)
                + ")";
    }
}
