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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DefaultVariableService implements VariableService {

    private final Configuration configuration;

    private final ExpressionEvaluator expressionEvaluator;

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

        Configuration globalVariablesConfiguration = configuration.getPrefixNamespace(VARIABLES);

        return getVariables(globalVariablesConfiguration);
    }

    @Override
    public Map<String, String> getVariablesInEnvironment(Environment environment) {

        String environmentName = environment.getName();

        Configuration globalVariablesConfiguration = configuration.getPrefixNamespace(VARIABLES);

        Configuration environmentSpecificVariablesConfiguration =
                configuration.getPrefixNamespace(ENVIRONMENTS, environmentName, VARIABLES);

        // environment specific variables override global variables, hence come first in the chain
        Configuration variablesConfiguration =
                environmentSpecificVariablesConfiguration.addAll(globalVariablesConfiguration);

        return getVariables(variablesConfiguration.withEntry("environment", environmentName));
    }

    private LinkedHashMap<String, String> getVariables(Configuration variablesConfiguration) {
        Stream<String> stream = variablesConfiguration.getKeys().stream();

        return stream.collect(
                Collectors.toMap(
                        Function.identity(),
                        variableName -> evaluateVariable(variableName, variablesConfiguration),
                        (firstKey, secondKey) -> firstKey,
                        LinkedHashMap::new));
    }

    private String evaluateVariable(String variableName, Configuration variablesConfiguration) {

        String variableExpression =
                variablesConfiguration
                        .getString(variableName)
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "No value expression is found for variable: "
                                                        + variableName));

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
