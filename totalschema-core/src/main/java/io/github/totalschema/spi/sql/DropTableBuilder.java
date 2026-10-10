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
 * Fluent builder for constructing DROP TABLE SQL statements. Instances are normally obtained via
 * {@link SqlBuilder#dropTable(String)}.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * String sql = sqlDialect.getSqlBuilder().dropTable("my_table").build();
 * // DROP TABLE my_table
 * }</pre>
 */
public final class DropTableBuilder {

    private final SqlDialect sqlDialect;
    private final String tableNameExpression;

    static DropTableBuilder create(SqlDialect sqlDialect, String tableNameExpression) {
        return new DropTableBuilder(sqlDialect, tableNameExpression);
    }

    private DropTableBuilder(SqlDialect sqlDialect, String tableNameExpression) {
        this.sqlDialect = sqlDialect;
        this.tableNameExpression = tableNameExpression;
    }

    /**
     * Builds the DROP TABLE SQL statement.
     *
     * @return the complete DROP TABLE SQL statement
     */
    public String build() {
        String formattedTableName = sqlDialect.formatTableName(tableNameExpression);
        return "DROP TABLE " + formattedTableName;
    }
}
