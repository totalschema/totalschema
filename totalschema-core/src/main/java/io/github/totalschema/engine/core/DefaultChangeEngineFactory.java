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
import io.github.totalschema.config.environment.EnvironmentFactory;
import io.github.totalschema.connector.ConnectorManager;
import io.github.totalschema.engine.api.ChangeEngine;
import io.github.totalschema.engine.api.ChangeEngineFactory;
import io.github.totalschema.engine.core.command.api.CommandExecutor;
import io.github.totalschema.engine.core.command.api.CommandInvoker;
import io.github.totalschema.engine.core.command.interceptor.LockInterceptor;
import io.github.totalschema.engine.core.container.ComponentContainer;
import io.github.totalschema.engine.core.container.ComponentContainerBuilder;
import io.github.totalschema.engine.core.event.EventDispatcher;
import io.github.totalschema.engine.internal.changefile.ChangeFileFactory;
import io.github.totalschema.spi.ComponentFactory;
import io.github.totalschema.spi.ServiceLoaderFactory;
import io.github.totalschema.spi.config.ConfigurationSupplier;
import io.github.totalschema.spi.expression.evaluator.ExpressionEvaluator;
import io.github.totalschema.spi.expression.evaluator.ExpressionEvaluatorFactory;
import io.github.totalschema.spi.hash.HashService;
import io.github.totalschema.spi.hash.HashServiceFactory;
import io.github.totalschema.spi.lock.LockService;
import io.github.totalschema.spi.secrets.SecretManagerFactory;
import io.github.totalschema.spi.secrets.SecretsManager;
import io.github.totalschema.spi.sql.SqlDialect;
import io.github.totalschema.spi.sql.SqlDialectFactory;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of ChangeEngineFactory. Creates ChangeEngine instances with a chain of
 * interceptors for executing commands inside the engine.
 */
public class DefaultChangeEngineFactory implements ChangeEngineFactory {

    private final Logger logger = LoggerFactory.getLogger(DefaultChangeEngineFactory.class);

    @Override
    @SuppressWarnings("unused") // Possible API usage
    public ChangeEngine getChangeEngine(
            ConfigurationSupplier configurationSupplier, SecretsManager secretsManager) {

        return getChangeEngine(configurationSupplier, secretsManager, null);
    }

    @Override
    public ChangeEngine getChangeEngine(
            ConfigurationSupplier configurationSupplier,
            SecretsManager secretsManager,
            String environmentName) {

        Environment environment = environmentName != null ? new Environment(environmentName) : null;

        if (secretsManager == null) {
            SecretManagerFactory secretManagerFactory = SecretManagerFactory.getInstance();

            logger.debug(
                    "Creating SecretsManager using SecretManagerFactory: {}",
                    secretManagerFactory.getClass().getName());

            secretsManager = secretManagerFactory.getSecretsManager(null, null);
        }

        EventDispatcher eventDispatcher = new EventDispatcher();

        ComponentContainer componentContainer =
                createComponentContainer(
                        environment, configurationSupplier, secretsManager, eventDispatcher);

        CommandExecutor commandExecutor = new CommandInvoker();

        if (componentContainer.has(LockService.class)) {
            logger.debug(
                    "Adding LockInterceptor to command execution chain, as LockService is available");

            commandExecutor = new LockInterceptor(commandExecutor);
        }

        return new DefaultChangeEngine(commandExecutor, componentContainer, eventDispatcher);
    }

    static ComponentContainer createComponentContainer(
            Environment environment,
            ConfigurationSupplier configurationSupplier,
            SecretsManager secretsManager,
            EventDispatcher eventDispatcher) {

        requireNonNull(configurationSupplier, "configurationSupplier must not be null");
        requireNonNull(secretsManager, "secretsManager must not be null");

        ComponentContainerBuilder builder = ComponentContainer.builder();

        builder.withComponent(EventDispatcher.class, eventDispatcher);

        if (environment != null) {
            builder.withComponent(Environment.class, environment);
        }

        builder.withComponent(SecretsManager.class, secretsManager);

        ExpressionEvaluator expressionEvaluator =
                ExpressionEvaluatorFactory.getInstance().getExpressionEvaluator(secretsManager);

        builder.withComponent(ExpressionEvaluator.class, expressionEvaluator);

        Configuration configuration =
                ConfigurationFactory.getInstance()
                        .getEvaluatedConfiguration(
                                configurationSupplier, expressionEvaluator, environment);

        builder.withComponent(Configuration.class, configuration);

        builder.withComponent(EnvironmentFactory.class, EnvironmentFactory.getInstance());
        builder.withComponent(ConnectorManager.class, ConnectorManager.getInstance());

        builder.withComponent(SqlDialect.class, SqlDialectFactory.getInstance().getSqlDialect());

        getHashService(configuration)
                .ifPresent(hashService -> builder.withComponent(HashService.class, hashService));

        builder.withComponent(ChangeFileFactory.class, new ChangeFileFactory(configuration));

        // Register all ComponentFactory implementations discovered via ServiceLoader
        // This includes SqlScriptExecutorComponentFactory, GroovyScriptExecutorFactory, etc.
        ServiceLoaderFactory.getAllServices(ComponentFactory.class).forEach(builder::withFactory);

        return builder.allowUnqualifiedAccessToSingleComponents(true).build();
    }

    private static Optional<HashService> getHashService(Configuration configuration) {

        // Initialize hashService if validation.type is set to contentHash
        final HashService hashService;
        if ("contentHash"
                .equalsIgnoreCase(configuration.getString("validation.type").orElse(null))) {

            hashService = HashServiceFactory.getInstance().getHashService(configuration);
        } else {
            hashService = null;
        }

        return Optional.ofNullable(hashService);
    }
}
