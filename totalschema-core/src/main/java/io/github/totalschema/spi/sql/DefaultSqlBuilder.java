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
 * Default {@link SqlBuilder} implementation returned by {@link SqlDialect#getSqlBuilder()}. Simply
 * delegates to the static factory methods of each statement builder, bound to the owning {@link
 * SqlDialect}.
 */
public class DefaultSqlBuilder implements SqlBuilder {

    private final SqlDialect sqlDialect;

    protected DefaultSqlBuilder(SqlDialect sqlDialect) {
        this.sqlDialect = sqlDialect;
    }

    @Override
    public CreateTableBuilder createTable(String tableNameExpression, Configuration configuration) {
        return CreateTableBuilder.create(sqlDialect, tableNameExpression, configuration);
    }

    @Override
    public InsertStatementBuilder insertInto(String tableNameExpression) {
        return InsertStatementBuilder.create(sqlDialect, tableNameExpression);
    }

    @Override
    public SelectStatementBuilder select(String tableNameExpression) {
        return SelectStatementBuilder.create(sqlDialect, tableNameExpression);
    }

    @Override
    public UpdateStatementBuilder update(String tableNameExpression) {
        return UpdateStatementBuilder.create(sqlDialect, tableNameExpression);
    }

    @Override
    public DeleteStatementBuilder delete(String tableNameExpression) {
        return DeleteStatementBuilder.create(sqlDialect, tableNameExpression);
    }

    @Override
    public DropTableBuilder dropTable(String tableNameExpression) {
        return DropTableBuilder.create(sqlDialect, tableNameExpression);
    }
}
