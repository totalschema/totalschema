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
 * Fluent builder for constructing DELETE SQL statements. Instances are normally obtained via {@link
 * SqlBuilder#delete(String)}.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * String sql = sqlDialect.getSqlBuilder().delete("my_table")
 *     .whereEqualsAny("id", 3)
 *     .build();
 * // DELETE FROM my_table WHERE id = ? OR id = ? OR id = ?
 * }</pre>
 */
public final class DeleteStatementBuilder {

    private final SqlDialect sqlDialect;
    private final String tableNameExpression;
    private String whereClause;

    static DeleteStatementBuilder create(SqlDialect sqlDialect, String tableNameExpression) {
        return new DeleteStatementBuilder(sqlDialect, tableNameExpression);
    }

    private DeleteStatementBuilder(SqlDialect sqlDialect, String tableNameExpression) {
        this.sqlDialect = sqlDialect;
        this.tableNameExpression = tableNameExpression;
    }

    /**
     * Sets the WHERE clause condition, without the {@code WHERE} keyword.
     *
     * @param condition the raw SQL condition expression
     * @return this builder for method chaining
     */
    public DeleteStatementBuilder where(String condition) {
        this.whereClause = condition;
        return this;
    }

    /**
     * Convenience method that sets a WHERE clause matching the given column against {@code
     * parameterCount} bound parameters, combined with {@code OR}. This is useful when deleting rows
     * by a dynamically-sized set of identifiers, generating SQL that is more portable than a SQL
     * {@code IN} expression, where drivers might have quirks around parameter binding.
     *
     * @param columnName the column name to compare
     * @param parameterCount the number of {@code ?} parameters to generate
     * @return this builder for method chaining
     * @throws IllegalArgumentException if parameterCount is less than 1
     */
    public DeleteStatementBuilder whereEqualsAny(String columnName, int parameterCount) {
        if (parameterCount < 1) {
            throw new IllegalArgumentException("parameterCount must be at least 1");
        }

        StringBuilder condition = new StringBuilder();
        for (int i = 0; i < parameterCount; i++) {
            if (i != 0) {
                condition.append(" OR ");
            }
            condition.append(columnName).append(" = ?");
        }

        return where(condition.toString());
    }

    /**
     * Builds the DELETE SQL statement.
     *
     * @return the complete DELETE SQL statement
     */
    public String build() {
        String formattedTableName = sqlDialect.formatTableName(tableNameExpression);

        StringBuilder sql = new StringBuilder("DELETE FROM ").append(formattedTableName);

        if (whereClause != null) {
            sql.append(" WHERE ").append(whereClause);
        }

        return sql.toString();
    }
}
