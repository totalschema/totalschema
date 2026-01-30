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

package io.github.totalschema.cli;

import io.github.totalschema.config.FileSystemYamlFileConfiguration;
import io.github.totalschema.engine.api.ChangeEngine;
import io.github.totalschema.engine.api.ChangeEngineFactory;
import io.github.totalschema.spi.config.ConfigurationSupplier;
import io.github.totalschema.spi.secrets.SecretsManager;

public abstract class CommonCliCommandBase extends SecretManagerServiceCliCommand {

    @Override
    protected final void run(SecretsManager secretsManager) {

        ConfigurationSupplier configurationSupplier = FileSystemYamlFileConfiguration::create;

        ChangeEngineFactory changeEngineFactory = ChangeEngineFactory.getInstance();

        try (ChangeEngine changeEngine =
                getChangeEngine(configurationSupplier, changeEngineFactory, secretsManager)) {

            run(changeEngine);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected ChangeEngine getChangeEngine(
            ConfigurationSupplier configurationSupplier,
            ChangeEngineFactory changeEngineFactory,
            SecretsManager secretsManager) {

        return changeEngineFactory.getChangeEngine(configurationSupplier, secretsManager);
    }

    protected abstract void run(ChangeEngine changeEngine);
}
