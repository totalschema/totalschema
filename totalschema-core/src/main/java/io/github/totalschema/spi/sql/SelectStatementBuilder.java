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
 * Fluent builder for constructing SELECT SQL statements. Instances are normally obtained via {@link
 * SqlBuilder#select(String)}.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * String sql = sqlDialect.getSqlBuilder().select("my_table")
 *     .column("id")
 *     .column("name")
 *     .where("id = ?")
 *     .build();
 * // SELECT id, name FROM my_table WHERE id = ?
 * }</pre>
 */
public final class SelectStatementBuilder {

    private final SqlDialect sqlDialect;
    private final String tableNameExpression;
    private final List<String> columnNames = new LinkedList<>();
    private String whereClause;

    static SelectStatementBuilder create(SqlDialect sqlDialect, String tableNameExpression) {
        return new SelectStatementBuilder(sqlDialect, tableNameExpression);
    }

    private SelectStatementBuilder(SqlDialect sqlDialect, String tableNameExpression) {
        this.sqlDialect = sqlDialect;
        this.tableNameExpression = tableNameExpression;
    }

    /**
     * Adds a column to the SELECT clause.
     *
     * @param columnName the column name
     * @return this builder for method chaining
     */
    public SelectStatementBuilder column(String columnName) {
        columnNames.add(columnName);
        return this;
    }

    /**
     * Sets the WHERE clause condition, without the {@code WHERE} keyword.
     *
     * @param condition the raw SQL condition expression
     * @return this builder for method chaining
     */
    public SelectStatementBuilder where(String condition) {
        this.whereClause = condition;
        return this;
    }

    /**
     * Builds the SELECT SQL statement.
     *
     * @return the complete SELECT SQL statement
     * @throws IllegalStateException if no columns have been added
     */
    public String build() {
        if (columnNames.isEmpty()) {
            throw new IllegalStateException("Cannot build SELECT without columns");
        }

        String formattedTableName = sqlDialect.formatTableName(tableNameExpression);

        StringBuilder sql =
                new StringBuilder("SELECT ")
                        .append(String.join(", ", columnNames))
                        .append(" FROM ")
                        .append(formattedTableName);

        if (whereClause != null) {
            sql.append(" WHERE ").append(whereClause);
        }

        return sql.toString();
    }
}
