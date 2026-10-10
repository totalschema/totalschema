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
 * Fluent builder for constructing UPDATE SQL statements. Instances are normally obtained via {@link
 * SqlBuilder#update(String)}.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * String sql = sqlDialect.getSqlBuilder().update("my_table")
 *     .set("name", "?")
 *     .where("id = ?")
 *     .build();
 * // UPDATE my_table SET name = ? WHERE id = ?
 * }</pre>
 */
public final class UpdateStatementBuilder {

    private final SqlDialect sqlDialect;
    private final String tableNameExpression;
    private final List<String> assignments = new LinkedList<>();
    private String whereClause;

    static UpdateStatementBuilder create(SqlDialect sqlDialect, String tableNameExpression) {
        return new UpdateStatementBuilder(sqlDialect, tableNameExpression);
    }

    private UpdateStatementBuilder(SqlDialect sqlDialect, String tableNameExpression) {
        this.sqlDialect = sqlDialect;
        this.tableNameExpression = tableNameExpression;
    }

    /**
     * Adds a {@code column = valueExpression} assignment to the SET clause. Use {@code "?"} as the
     * value expression for a bound parameter, or a literal SQL expression (e.g. {@code "NULL"}) for
     * a fixed value.
     *
     * @param columnName the column name
     * @param valueExpression the raw SQL value expression (e.g. {@code "?"} or {@code "NULL"})
     * @return this builder for method chaining
     */
    public UpdateStatementBuilder set(String columnName, String valueExpression) {
        assignments.add(columnName + " = " + valueExpression);
        return this;
    }

    /**
     * Sets the WHERE clause condition, without the {@code WHERE} keyword.
     *
     * @param condition the raw SQL condition expression
     * @return this builder for method chaining
     */
    public UpdateStatementBuilder where(String condition) {
        this.whereClause = condition;
        return this;
    }

    /**
     * Builds the UPDATE SQL statement.
     *
     * @return the complete UPDATE SQL statement
     * @throws IllegalStateException if no assignments have been added
     */
    public String build() {
        if (assignments.isEmpty()) {
            throw new IllegalStateException("Cannot build UPDATE without any SET assignments");
        }

        String formattedTableName = sqlDialect.formatTableName(tableNameExpression);

        StringBuilder sql =
                new StringBuilder("UPDATE ")
                        .append(formattedTableName)
                        .append(" SET ")
                        .append(String.join(", ", assignments));

        if (whereClause != null) {
            sql.append(" WHERE ").append(whereClause);
        }

        return sql.toString();
    }
}
