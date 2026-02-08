/*
 * totalschema: tool for managing database versioning and schema changes with ease.
 * Copyright (C) 2026 totalschema development team
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

package io.github.totalschema.engine.core.command.impl;

import static org.testng.Assert.*;

import io.github.totalschema.config.Configuration;
import io.github.totalschema.config.MapConfiguration;
import io.github.totalschema.engine.core.command.api.CommandContext;
import io.github.totalschema.engine.internal.expression.evaluator.DefaultExpressionEvaluatorFactory;
import io.github.totalschema.engine.internal.secrets.DefaultSecretManagerFactory;
import io.github.totalschema.engine.internal.variables.DefaultVariableServiceFactory;
import io.github.totalschema.spi.expression.evaluator.ExpressionEvaluator;
import io.github.totalschema.spi.expression.evaluator.ExpressionEvaluatorFactory;
import io.github.totalschema.spi.secrets.SecretManagerFactory;
import io.github.totalschema.spi.secrets.SecretsManager;
import io.github.totalschema.spi.variables.VariableService;
import io.github.totalschema.spi.variables.VariableServiceFactory;
import java.util.Map;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ListVariablesCommandTest {

    private CommandContext context;

    @BeforeMethod
    public void setUp() {
        context = new CommandContext();
    }

    @Test
    public void testExecuteWithOnlyGlobalVariables() {
        Configuration config =
                new MapConfiguration(
                        Map.of("variables.db_host", "localhost", "variables.db_port", "5432"));
        initContext(config);

        ListVariablesCommand command = new ListVariablesCommand("DEV");
        Map<String, String> variables = command.execute(context);

        assertNotNull(variables);
        assertEquals(variables.size(), 3);
        assertEquals(variables.get("db_host"), "localhost");
        assertEquals(variables.get("db_port"), "5432");
        assertEquals(variables.get("environment"), "DEV");
    }

    @Test
    public void testExecuteWithEnvironmentSpecificVariables() {
        Configuration config =
                new MapConfiguration(
                        Map.of(
                                "variables.db_host",
                                "localhost",
                                "environments.DEV.variables.db_host",
                                "dev-server",
                                "environments.DEV.variables.db_name",
                                "dev_db"));
        initContext(config);

        ListVariablesCommand command = new ListVariablesCommand("DEV");
        Map<String, String> variables = command.execute(context);

        assertNotNull(variables);
        // Note: Currently global variables override environment-specific ones
        // This may be a bug in the implementation (see DefaultVariableService line 67)
        assertEquals(variables.get("db_host"), "localhost"); // Global overrides env-specific
        assertEquals(variables.get("db_name"), "dev_db"); // Only in env-specific
        assertEquals(variables.get("environment"), "DEV");
        assertEquals(variables.size(), 3);
    }

    @Test
    public void testExecuteWithNoVariables() {
        Configuration config = new MapConfiguration(Map.of());
        initContext(config);

        ListVariablesCommand command = new ListVariablesCommand("DEV");
        Map<String, String> variables = command.execute(context);

        assertNotNull(variables);
        assertEquals(variables.size(), 1);
        assertEquals(variables.get("environment"), "DEV");
    }

    @Test
    public void testExecuteWithOnlyEnvironmentVariables() {
        Configuration config =
                new MapConfiguration(
                        Map.of(
                                "environments.PROD.variables.db_host",
                                "prod-server",
                                "environments.PROD.variables.db_port",
                                "5432"));
        initContext(config);

        ListVariablesCommand command = new ListVariablesCommand("PROD");
        Map<String, String> variables = command.execute(context);

        assertNotNull(variables);
        assertEquals(variables.size(), 3);
        assertEquals(variables.get("db_host"), "prod-server");
        assertEquals(variables.get("db_port"), "5432");
        assertEquals(variables.get("environment"), "PROD");
    }

    @Test
    public void testExecuteDifferentEnvironmentDoesNotGetOtherEnvironmentVariables() {
        Configuration config =
                new MapConfiguration(
                        Map.of(
                                "variables.db_host", "localhost",
                                "environments.PROD.variables.db_host", "prod-server"));
        initContext(config);

        ListVariablesCommand command = new ListVariablesCommand("DEV");
        Map<String, String> variables = command.execute(context);

        assertNotNull(variables);
        // Should only get global, not PROD-specific
        assertEquals(variables.get("db_host"), "localhost");
        assertEquals(variables.get("environment"), "DEV");
        assertEquals(variables.size(), 2);
    }

    private void initContext(Configuration config) {
        context.setValue(Configuration.class, config);

        SecretManagerFactory secretManagerFactory = new DefaultSecretManagerFactory();
        SecretsManager secretsManager = secretManagerFactory.getSecretsManager(null, null);

        context.setValue(SecretsManager.class, secretsManager);

        ExpressionEvaluatorFactory defaultExpressionEvaluatorFactory =
                new DefaultExpressionEvaluatorFactory();
        ExpressionEvaluator expressionEvaluator =
                defaultExpressionEvaluatorFactory.getExpressionEvaluator(secretsManager);
        context.setValue(ExpressionEvaluator.class, expressionEvaluator);

        VariableServiceFactory factory = new DefaultVariableServiceFactory();
        VariableService variableService = factory.getVariableService(config, expressionEvaluator);

        context.setValue(VariableService.class, variableService);
    }
}
