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

package io.github.totalschema.engine.internal.script;

import io.github.totalschema.config.Configuration;
import io.github.totalschema.engine.core.command.api.CommandContext;
import io.github.totalschema.jdbc.JdbcDatabase;
import io.github.totalschema.jdbc.JdbcDatabaseFactory;
import io.github.totalschema.spi.script.ScriptExecutor;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class SqlScriptExecutor implements ScriptExecutor {

    private final String statementSeparator;
    private final JdbcDatabase jdbcDatabase;

    private static final class DefaultValues {

        private static final String STATEMENT_SEPARATOR = ";";
    }

    public SqlScriptExecutor(String name, Configuration connectorConfiguration) {

        if (connectorConfiguration.getBoolean("no.statementSeparator").orElse(false)) {
            this.statementSeparator = null;
        } else {
            this.statementSeparator =
                    connectorConfiguration
                            .getString("statementSeparator")
                            .orElse(DefaultValues.STATEMENT_SEPARATOR);
        }

        JdbcDatabaseFactory jdbcDatabaseFactory = JdbcDatabaseFactory.getInstance();
        jdbcDatabase = jdbcDatabaseFactory.getJdbcDatabase(name, connectorConfiguration);
    }

    @Override
    public void execute(String script, CommandContext context) throws InterruptedException {

        List<String> statements = getStatements(script);

        for (String statement : statements) {

            try {
                jdbcDatabase.executeUpdate(statement);

            } catch (SQLException e) {
                throw new RuntimeException("Statement failed: " + statement, e);
            }
        }
    }

    private List<String> getStatements(String fileContent) {

        List<String> statements;

        if (statementSeparator != null) {

            statements = List.of(fileContent.split(statementSeparator));

        } else {

            statements = List.of(fileContent);
        }

        return statements.stream()
                .map(String::trim)
                .filter(Predicate.not(String::isBlank))
                .collect(Collectors.toList());
    }

    @Override
    public void close() throws IOException {
        jdbcDatabase.close();
    }

    @Override
    public String toString() {
        return "SqlScriptExecutor{"
                + " jdbcDatabase="
                + jdbcDatabase
                + " ,statementSeparator='"
                + statementSeparator
                + '\''
                + '}';
    }
}
