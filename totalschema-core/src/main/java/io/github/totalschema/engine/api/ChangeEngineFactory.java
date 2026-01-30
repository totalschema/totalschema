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

package io.github.totalschema.engine.api;

import io.github.totalschema.engine.core.DefaultChangeEngineFactory;
import io.github.totalschema.spi.ServiceLoaderFactory;
import io.github.totalschema.spi.config.ConfigurationSupplier;
import io.github.totalschema.spi.secrets.SecretsManager;

/**
 * Factory interface for creating ChangeEngine instances. Implementations can be provided via
 * ServiceLoader or the default factory is used.
 */
public interface ChangeEngineFactory {

    /**
     * Returns a ChangeEngineFactory instance, either from ServiceLoader or the default
     * implementation.
     *
     * @return ChangeEngineFactory instance
     */
    static ChangeEngineFactory getInstance() {
        return ServiceLoaderFactory.getSingleService(ChangeEngineFactory.class)
                .orElseGet(DefaultChangeEngineFactory::new);
    }

    /**
     * Creates a ChangeEngine instance without a specific environment. This can be used for
     * environment-agnostic operations like listing environments.
     *
     * @param configurationSupplier provides configuration properties
     * @param secretsManager manages encrypted secrets
     * @return ChangeEngine instance
     */
    @SuppressWarnings("unused") // Possible API usage
    ChangeEngine getChangeEngine(
            ConfigurationSupplier configurationSupplier, SecretsManager secretsManager);

    /**
     * Creates a ChangeEngine instance for a specific environment.
     *
     * @param configurationSupplier provides configuration properties
     * @param secretsManager manages encrypted secrets
     * @param environmentName the target environment (e.g., DEV, QA, PROD)
     * @return ChangeEngine instance
     */
    ChangeEngine getChangeEngine(
            ConfigurationSupplier configurationSupplier,
            SecretsManager secretsManager,
            String environmentName);
}
