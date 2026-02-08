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

import io.github.totalschema.config.environment.Environment;
import io.github.totalschema.engine.api.ChangeEngine;
import io.github.totalschema.engine.api.ChangeEngineFactory;
import io.github.totalschema.engine.core.command.api.CommandExecutor;
import io.github.totalschema.engine.core.command.api.CommandInvoker;
import io.github.totalschema.engine.core.command.interceptor.LockInterceptor;
import io.github.totalschema.engine.core.command.interceptor.ServiceInitializerInterceptor;
import io.github.totalschema.spi.config.ConfigurationSupplier;
import io.github.totalschema.spi.secrets.SecretManagerFactory;
import io.github.totalschema.spi.secrets.SecretsManager;

/**
 * Default implementation of ChangeEngineFactory. Creates ChangeEngine instances with a chain of
 * interceptors for executing commands inside the engine.
 */
public class DefaultChangeEngineFactory implements ChangeEngineFactory {

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

        CommandExecutor commandExecutor = new CommandInvoker();

        commandExecutor = new LockInterceptor(commandExecutor);

        commandExecutor = new ServiceInitializerInterceptor(commandExecutor);

        Environment environment = environmentName != null ? new Environment(environmentName) : null;

        if (secretsManager == null) {
            SecretManagerFactory secretManagerFactory = SecretManagerFactory.getInstance();
            secretsManager = secretManagerFactory.getSecretsManager(null, null);
        }

        return new DefaultChangeEngine(
                commandExecutor, configurationSupplier, environment, secretsManager);
    }
}
