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

package io.github.totalschema.engine.internal.expression.evaluator;

import io.github.totalschema.config.Configuration;
import io.github.totalschema.spi.expression.evaluator.ExpressionEvaluator;
import io.github.totalschema.spi.lookup.ExpressionLookup;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.text.StringSubstitutor;
import org.apache.commons.text.lookup.StringLookup;
import org.apache.commons.text.lookup.StringLookupFactory;

final class DefaultExpressionEvaluator implements ExpressionEvaluator {

    private static final class StringLookupAdapter implements StringLookup {

        private final ExpressionLookup expressionLookup;

        private StringLookupAdapter(ExpressionLookup expressionLookup) {
            this.expressionLookup = expressionLookup;
        }

        @Override
        public String lookup(String key) {
            return expressionLookup.apply(key);
        }
    }

    private final Map<String, StringLookup> stringLookups;

    DefaultExpressionEvaluator(List<ExpressionLookup> lookups) {
        stringLookups =
                lookups.stream()
                        .collect(
                                Collectors.toMap(
                                        ExpressionLookup::getKey, StringLookupAdapter::new));
    }

    @Override
    public String evaluate(String expressionString, Configuration variablesConfiguration) {

        Map<String, String> valuesMap = new HashMap<>();

        for (String key : variablesConfiguration.getKeys()) {

            String value =
                    variablesConfiguration
                            .getString(key)
                            .orElseThrow(() -> new IllegalStateException("Could not find: " + key));

            valuesMap.put(key, value);
        }

        return evaluate(expressionString, valuesMap);
    }

    @Override
    public String evaluate(String expressionString, Map<String, String> values) {

        try {
            StringLookupFactory stringLookupFactory = StringLookupFactory.INSTANCE;

            StringLookup variableResolver =
                    stringLookupFactory.interpolatorStringLookup(
                            stringLookups, stringLookupFactory.mapStringLookup(values), true);

            StringSubstitutor stringSubstitutor =
                    new StringSubstitutor(variableResolver, "${", "}", '$')
                            .setEnableSubstitutionInVariables(true);

            return stringSubstitutor.replace(expressionString);
        } catch (RuntimeException e) {
            throw new RuntimeException(
                    String.format(
                            "Failed to evaluate expression: '%s' with values: %s",
                            expressionString, values),
                    e);
        }
    }
}
