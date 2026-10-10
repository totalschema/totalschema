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

package io.github.totalschema.connector.gcp.bigtable.actions;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.totalschema.engine.api.Context;
import io.github.totalschema.spi.expression.evaluator.ExpressionEvaluator;
import io.github.totalschema.spi.variables.VariableService;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import org.yaml.snakeyaml.Yaml;

/**
 * Abstract factory for creating {@link Action} instances from YAML configuration.
 *
 * <p>Concrete subclasses register themselves via {@link ServiceLoader} and are automatically
 * discovered at startup. The action type name is derived from the Action class simple name.
 *
 * <p>Example: {@code CreateTableActionFactory} with {@code CreateTable.class} handles action type
 * "CreateTable".
 *
 * @param <T> the concrete Action type this factory creates
 */
public abstract class ActionFactory<T extends Action<?>> {

    private static final Map<String, ActionFactory<?>> REGISTRY;
    private static final ObjectMapper OBJECT_MAPPER =
            new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);

    static {
        Map<String, ActionFactory<?>> registry = new HashMap<>();
        @SuppressWarnings("rawtypes")
        ServiceLoader<ActionFactory> loader = ServiceLoader.load(ActionFactory.class);
        for (ActionFactory<?> factory : loader) {
            String actionType = factory.actionClass.getSimpleName();
            registry.put(actionType, factory);
        }
        REGISTRY = Collections.unmodifiableMap(registry);
    }

    private static final class Evaluator {
        private final Map<String, String> variables;
        private final ExpressionEvaluator expressionEvaluator;

        private Evaluator(Map<String, String> variables, ExpressionEvaluator expressionEvaluator) {
            this.variables = variables;
            this.expressionEvaluator = expressionEvaluator;
        }

        private String evaluate(String expression) {
            return expressionEvaluator.evaluate(expression, variables);
        }
    }

    private final Class<T> actionClass;

    /**
     * Constructor for concrete factories.
     *
     * @param actionClass the Action class this factory creates
     */
    protected ActionFactory(Class<T> actionClass) {
        this.actionClass = Objects.requireNonNull(actionClass, "actionClass must not be null");
    }

    /**
     * Creates an Action from a YAML file.
     *
     * @param path path to the YAML file
     * @param context the context for variable substitution
     * @return the deserialized Action
     */
    public static Action<?> getAction(Path path, Context context) {
        try (InputStream inputStream = Files.newInputStream(path)) {
            return getAction(inputStream, context);
        } catch (IOException e) {
            throw new RuntimeException("Failure reading: " + path, e);
        }
    }

    /**
     * Creates an Action from a YAML input stream.
     *
     * @param inputStream YAML input stream
     * @param context the context for variable substitution
     * @return the deserialized Action
     */
    public static Action<?> getAction(InputStream inputStream, Context context) {
        Yaml yaml = new Yaml();
        Map<String, Object> config = yaml.loadAs(inputStream, Map.class);
        return getAction(config, context);
    }

    /**
     * Creates an Action from a configuration map.
     *
     * @param config configuration map with a single top-level key
     * @param context the context for variable substitution
     * @return the deserialized Action
     * @throws IllegalArgumentException if the configuration doesn't have exactly one top-level key
     *     or the action type is unknown
     */
    public static Action<?> getAction(Map<String, Object> config, Context context) {
        Set<String> keys = config.keySet();
        if (keys.size() != 1) {
            throw new IllegalArgumentException(
                    "Configuration must contain exactly one top-level key, found: " + keys.size());
        }

        String actionType = keys.iterator().next();
        ActionFactory<?> factory = REGISTRY.get(actionType);

        if (factory == null) {
            throw new IllegalArgumentException("Unknown action type: " + actionType);
        }

        Object actionConfig = config.get(actionType);

        // Substitute variables in the configuration
        ExpressionEvaluator expressionEvaluator = context.get(ExpressionEvaluator.class);
        VariableService variableService = context.get(VariableService.class);
        Map<String, String> variables = variableService.getEffectiveVariables(context);

        Evaluator evaluator = new Evaluator(variables, expressionEvaluator);

        Object processedConfig = substituteVariables(actionConfig, evaluator);

        return factory.deserialize(processedConfig);
    }

    /**
     * Recursively substitutes variables in configuration values using ExpressionEvaluator.
     *
     * @param value the configuration value (can be Map, List, String, or other types)
     * @param evaluator the expression evaluator
     * @return the processed value with substituted variables
     */
    @SuppressWarnings("unchecked")
    private static Object substituteVariables(Object value, Evaluator evaluator) {
        if (value == null) {
            return null;
        }

        if (value instanceof String) {
            return evaluator.evaluate((String) value);
        }

        if (value instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) value;
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                result.put(entry.getKey(), substituteVariables(entry.getValue(), evaluator));
            }
            return result;
        }

        if (value instanceof Set) {
            Set<?> set = (Set<?>) value;
            Set<Object> result = new LinkedHashSet<>();
            for (Object item : set) {
                result.add(substituteVariables(item, evaluator));
            }
            return result;
        }

        if (value instanceof Iterable) {
            Iterable<?> iterable = (Iterable<?>) value;
            List<Object> result = new LinkedList<>();
            for (Object item : iterable) {
                result.add(substituteVariables(item, evaluator));
            }
            return result;
        }

        // For other types (numbers, booleans, etc.), return as-is
        return value;
    }

    /**
     * Deserializes the action configuration into an Action instance using Jackson.
     *
     * @param config the action-specific configuration
     * @return the deserialized Action
     */
    protected T deserialize(Object config) {
        return OBJECT_MAPPER.convertValue(config, actionClass);
    }
}
