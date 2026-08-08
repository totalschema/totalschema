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

package io.github.totalschema.engine.internal.script;

import io.github.totalschema.config.Configuration;
import io.github.totalschema.engine.api.Context;
import io.github.totalschema.jdbc.JdbcDatabase;
import io.github.totalschema.spi.change.executor.SimpleChangeExecutor;
import io.github.totalschema.spi.expression.evaluator.ExpressionEvaluator;
import io.github.totalschema.spi.variables.VariableService;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Executes SQL scripts against a JDBC database.
 *
 * <p>Responsibilities:
 *
 * <ul>
 *   <li>Optionally substitutes {@code ${varName}} placeholders when {@code
 *       scriptExecutors.sql.variableSubstitution} is set to {@code true} in the connector
 *       configuration.
 *   <li>Splits the (possibly substituted) script on the configured statement separator (default:
 *       {@code ";"}) and executes each non-blank statement via JDBC.
 * </ul>
 *
 * <p>The {@link io.github.totalschema.jdbc.JdbcDatabase} is retrieved from the context at execution
 * time.
 */
final class SqlChangeExecutor extends SimpleChangeExecutor {

    private final boolean variableSubstitutionEnabled;
    private final String statementSeparator;

    private static final class DefaultValues {

        private static final String STATEMENT_SEPARATOR = ";";
    }

    /**
     * @param configuration Configuration for the script executor
     */
    SqlChangeExecutor(Configuration configuration, Context context) {
        super(context);

        this.variableSubstitutionEnabled =
                configuration.getBoolean("scriptExecutors.sql.variableSubstitution").orElse(false);

        if (configuration.getBoolean("disableStatementSeparator").orElse(false)) {
            this.statementSeparator = null;
        } else {
            this.statementSeparator =
                    configuration
                            .getString("statementSeparator")
                            .orElse(SqlChangeExecutor.DefaultValues.STATEMENT_SEPARATOR);
        }
    }

    @Override
    public void execute(InputStream inputStream, Context context) throws InterruptedException {

        String script = readAsString(inputStream);

        if (variableSubstitutionEnabled) {
            script = substituteVariables(script);
        }

        JdbcDatabase jdbcDatabase = context.get(JdbcDatabase.class);

        List<String> statements = getStatements(script);

        for (String statement : statements) {

            try {
                jdbcDatabase.execute(statement);

            } catch (SQLException e) {
                throw new RuntimeException("Statement failed: " + statement, e);
            }
        }
    }

    /**
     * Replaces {@code ${varName}} placeholders in {@code content} with the resolved variable values
     * for the current environment.
     *
     * @param content raw SQL content
     * @return content with all known variable references replaced
     */
    private String substituteVariables(String content) {

        ExpressionEvaluator expressionEvaluator = context.get(ExpressionEvaluator.class);
        VariableService variableService = context.get(VariableService.class);

        Map<String, String> variables = variableService.getEffectiveVariables(context);

        return expressionEvaluator.evaluate(content, variables);
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
    public String toString() {
        return "SqlScriptExecutor{"
                + "variableSubstitutionEnabled="
                + variableSubstitutionEnabled
                + ", statementSeparator='"
                + statementSeparator
                + '\''
                + '}';
    }
}
