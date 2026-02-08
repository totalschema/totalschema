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

package io.github.totalschema.engine.core;

import static java.util.Objects.requireNonNull;

import io.github.totalschema.config.Configuration;
import io.github.totalschema.config.ConfigurationFactory;
import io.github.totalschema.config.environment.Environment;
import io.github.totalschema.spi.config.ConfigurationSupplier;
import io.github.totalschema.spi.expression.evaluator.ExpressionEvaluator;
import io.github.totalschema.spi.expression.evaluator.ExpressionEvaluatorFactory;
import io.github.totalschema.spi.secrets.SecretsManager;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

final class EngineContext {

    private final Environment environment;
    private final SecretsManager secretsManager;
    private final ExpressionEvaluator expressionEvaluator;
    private final Configuration configuration;

    static EngineContext create(
            ConfigurationSupplier configurationSupplier,
            Environment environment,
            SecretsManager secretsManager) {
        return new EngineContext(configurationSupplier, environment, secretsManager);
    }

    private EngineContext(
            ConfigurationSupplier configurationSupplier,
            Environment environment,
            SecretsManager secretsManager) {

        requireNonNull(configurationSupplier, "configurationSupplier must not be null");
        requireNonNull(secretsManager, "secretsManager must not be null");

        this.environment = environment;
        this.secretsManager = secretsManager;
        this.expressionEvaluator =
                ExpressionEvaluatorFactory.getInstance().getExpressionEvaluator(secretsManager);
        this.configuration =
                ConfigurationFactory.getInstance()
                        .getEvaluatedConfiguration(
                                configurationSupplier, expressionEvaluator, environment);
    }

    Map<Class<?>, Object> asMap() {
        Map<Class<?>, Object> context = new HashMap<>();

        if (environment != null) {
            context.put(Environment.class, environment);
        }

        context.put(SecretsManager.class, secretsManager);
        context.put(ExpressionEvaluator.class, expressionEvaluator);
        context.put(Configuration.class, configuration);

        return Collections.unmodifiableMap(context);
    }
}
