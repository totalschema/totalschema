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

package io.github.totalschema.connector.jdbc;

import io.github.totalschema.config.Configuration;
import io.github.totalschema.config.environment.Environment;
import io.github.totalschema.connector.Connector;
import io.github.totalschema.engine.core.command.api.CommandContext;
import io.github.totalschema.model.ChangeFile;
import io.github.totalschema.spi.expression.evaluator.ExpressionEvaluator;
import io.github.totalschema.spi.script.ScriptExecutor;
import io.github.totalschema.spi.variables.VariableService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JDBC connector for executing SQL and other database scripts.
 *
 * <p>Supports any JDBC-compliant database including PostgreSQL, MySQL, Oracle, H2, SQL Server,
 * BigQuery, and others.
 */
public class JdbcConnector extends Connector {

    public static final String CONNECTOR_TYPE = "jdbc";

    private static final Logger logger = LoggerFactory.getLogger(JdbcConnector.class);

    private final String name;
    private final Configuration connectorConfiguration;

    public JdbcConnector(String name, Configuration connectorConfiguration) {
        this.name = name;
        this.connectorConfiguration = connectorConfiguration;
    }

    @Override
    public String toString() {
        return "JDBC Connector named '"
                + name
                + "'{"
                + " configuration='"
                + connectorConfiguration
                + '\''
                + '}';
    }

    @Override
    public void execute(ChangeFile changeFile, CommandContext context) throws InterruptedException {
        Path file = changeFile.getFile();

        try {
            String fileContent = Files.readString(file);

            String extension = changeFile.getId().getExtension();

            if (isVariableSubstitutionEnabled(extension)) {
                logger.debug(
                        "Variable substitution is enabled for file '{}' with extension: {}",
                        file,
                        extension);

                fileContent = substituteVariables(fileContent, context);
            } else {
                logger.debug(
                        "Variable substitution is disabled for file '{}' with extension: {}",
                        file,
                        extension);
            }

            // Get ScriptExecutor directly from IoC container with qualifier and arguments
            // e.g., context.get(ScriptExecutor.class, "sql", name, configuration)
            ScriptExecutor scriptExecutor =
                    context.get(ScriptExecutor.class, extension, name, connectorConfiguration);

            scriptExecutor.execute(fileContent, context);

        } catch (IOException e) {
            throw new RuntimeException("Failure reading: " + file);
        }
    }

    /**
     * Returns {@code true} if variable substitution is configured for the given file extension.
     *
     * <p>Substitution is opt-in: it must be explicitly enabled in the connector configuration via
     * {@code variableSubstitution.extensions}. When the key is absent, this method always returns
     * {@code false} and no substitution is attempted.
     *
     * @param extension the file extension (without the leading dot)
     * @return {@code true} if the extension appears in {@code variableSubstitution.extensions}
     */
    private boolean isVariableSubstitutionEnabled(String extension) {

        List<String> enabledExtensions =
                connectorConfiguration
                        .getList("variableSubstitution.extensions")
                        .orElse(Collections.emptyList());

        boolean isEnabled = enabledExtensions.contains(extension);

        logger.debug(
                "variableSubstitution.extensions for connector '{}': {}. Is '{}' enabled: {}",
                name,
                enabledExtensions,
                extension,
                isEnabled);

        return isEnabled;
    }

    /**
     * Replaces {@code ${varName}} placeholders in {@code content} with the resolved variable values
     * for the current environment.
     *
     * <p>This method should only be called after confirming that substitution is enabled for the
     * file's extension via {@link #isVariableSubstitutionEnabled(String)}.
     *
     * <p>Variables are resolved from the connector's {@link Configuration} and, when an {@link
     * Environment} is present in the context, environment-specific overrides are applied.
     *
     * @param content raw file content
     * @param context the current command context
     * @return content with all known variable references replaced
     */
    private String substituteVariables(String content, CommandContext context) {

        ExpressionEvaluator expressionEvaluator = context.get(ExpressionEvaluator.class);
        VariableService variableService = context.get(VariableService.class);

        Map<String, String> variables =
                context.getOptional(Environment.class)
                        .map(variableService::getVariablesInEnvironment)
                        .orElseGet(variableService::getVariables);

        return expressionEvaluator.evaluate(content, variables);
    }
}
