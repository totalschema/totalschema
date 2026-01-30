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

package io.github.totalschema.config;

import io.github.totalschema.config.environment.Environment;
import io.github.totalschema.spi.ServiceLoaderFactory;
import io.github.totalschema.spi.config.ConfigurationSupplier;
import io.github.totalschema.spi.expression.evaluator.ExpressionEvaluator;
import io.github.totalschema.spi.variables.VariableService;
import io.github.totalschema.spi.variables.VariableServiceFactory;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class ConfigurationFactory {

    private static final String ENVIRONMENT_KEY = "environment";

    public static ConfigurationFactory getInstance() {
        return ServiceLoaderFactory.getSingleService(ConfigurationFactory.class)
                .orElseGet(DefaultConfigurationFactory::new);
    }

    public Configuration getEvaluatedConfiguration(
            ConfigurationSupplier configurationSupplier,
            ExpressionEvaluator expressionEvaluator,
            Environment environment) {

        Configuration rawConfiguration = getRawConfiguration(configurationSupplier, environment);

        VariableService variableService =
                VariableServiceFactory.getInstance()
                        .getVariableService(rawConfiguration, expressionEvaluator);

        Configuration applicableConfiguration;

        Map<String, String> variables;
        if (environment != null) {
            variables = variableService.getVariablesInEnvironment(environment);

            applicableConfiguration =
                    new MapConfiguration(Map.of(ENVIRONMENT_KEY, environment.getName()))
                            .addAll(rawConfiguration);

        } else {
            variables = variableService.getVariables();
            applicableConfiguration = rawConfiguration;
        }

        Map<String, String> map =
                applicableConfiguration.getKeys().stream()
                        .collect(
                                Collectors.toMap(
                                        Function.identity(),
                                        key ->
                                                evaluateExpression(
                                                        expressionEvaluator,
                                                        key,
                                                        applicableConfiguration,
                                                        variables)));

        return new MapConfiguration(map);
    }

    private static String evaluateExpression(
            ExpressionEvaluator expressionEvaluator,
            String key,
            Configuration rawConfiguration,
            Map<String, String> variables) {
        try {
            String expression =
                    rawConfiguration
                            .getString(key)
                            .orElseThrow(() -> new IllegalStateException("Key not found: " + key));

            return expressionEvaluator.evaluate(expression, variables);

        } catch (RuntimeException ex) {
            throw new RuntimeException(
                    String.format("Failure evaluating configuration key '%s'", key), ex);
        }
    }

    public abstract Configuration getRawConfiguration(
            ConfigurationSupplier configurationSupplier, Environment environment);
}
