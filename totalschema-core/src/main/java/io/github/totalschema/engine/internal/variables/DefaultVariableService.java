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

package io.github.totalschema.engine.internal.variables;

import static io.github.totalschema.ProjectConventions.ConfigurationPropertyNames.*;

import io.github.totalschema.config.Configuration;
import io.github.totalschema.config.environment.Environment;
import io.github.totalschema.spi.expression.evaluator.ExpressionEvaluator;
import io.github.totalschema.spi.variables.VariableService;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class DefaultVariableService implements VariableService {

    private static final VariableCacheKey GLOBAL_VARIABLES_CACHE_KEY = new VariableCacheKey(null);

    private final Configuration configuration;

    private final ExpressionEvaluator expressionEvaluator;

    private final ConcurrentHashMap<VariableCacheKey, Map<String, String>> cachedVariables =
            new ConcurrentHashMap<>();

    public DefaultVariableService(
            Configuration configuration, ExpressionEvaluator expressionEvaluator) {
        this.configuration =
                Objects.requireNonNull(configuration, "argument configuration cannot be null");

        this.expressionEvaluator =
                Objects.requireNonNull(
                        expressionEvaluator, "argument expressionEvaluator cannot be null");
    }

    @Override
    public Map<String, String> getVariables() {

        return cachedVariables.computeIfAbsent(
                GLOBAL_VARIABLES_CACHE_KEY, k -> computeGlobalVariables());
    }

    @Override
    public Map<String, String> getVariablesInEnvironment(Environment environment) {

        VariableCacheKey variableCacheKey = new VariableCacheKey(environment);

        return cachedVariables.computeIfAbsent(
                variableCacheKey, k -> computeEnvironmentVariables(environment));
    }

    private Map<String, String> computeGlobalVariables() {

        Map<String, String> variablesConfiguration =
                configuration.getPrefixNamespace(VARIABLES).asMap().orElse(Collections.emptyMap());

        return evaluateVariables(variablesConfiguration);
    }

    private Map<String, String> computeEnvironmentVariables(Environment environment) {

        String environmentName = environment.getName();

        Configuration globalVariablesConfiguration = configuration.getPrefixNamespace(VARIABLES);

        Configuration environmentSpecificVariablesConfiguration =
                configuration.getPrefixNamespace(ENVIRONMENTS, environmentName, VARIABLES);

        // environment specific variables override global variables, hence come first in the chain
        Configuration variablesConfiguration =
                environmentSpecificVariablesConfiguration.addAll(globalVariablesConfiguration);

        Configuration configurationWithEnvironment =
                variablesConfiguration.withEntry("environment", environmentName);

        Map<String, String> variableConfig =
                configurationWithEnvironment.asMap().orElse(Collections.emptyMap());

        return evaluateVariables(variableConfig);
    }

    private Map<String, String> evaluateVariables(Map<String, String> variablesConfiguration) {
        LinkedHashMap<String, String> result =
                variablesConfiguration.keySet().stream()
                        .collect(
                                Collectors.toMap(
                                        Function.identity(),
                                        variableName ->
                                                evaluateVariable(
                                                        variableName, variablesConfiguration),
                                        (firstKey, secondKey) -> firstKey,
                                        LinkedHashMap::new));

        return Collections.unmodifiableMap(result);
    }

    private String evaluateVariable(
            String variableName, Map<String, String> variablesConfiguration) {

        String variableExpression = variablesConfiguration.get(variableName);
        if (variableExpression == null) {
            // should not occur
            throw new IllegalStateException(
                    "No value expression is found for variable: " + variableName);
        }

        try {
            return expressionEvaluator.evaluate(variableExpression, variablesConfiguration);

        } catch (RuntimeException re) {
            throw new RuntimeException(
                    String.format(
                            "Failed to evaluate variable '%s' expression: '%s'",
                            variableName, variableExpression),
                    re);
        }
    }
}
