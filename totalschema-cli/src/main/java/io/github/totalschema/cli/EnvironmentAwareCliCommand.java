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

import io.github.totalschema.engine.api.ChangeEngine;
import io.github.totalschema.engine.api.ChangeEngineFactory;
import io.github.totalschema.spi.config.ConfigurationSupplier;
import io.github.totalschema.spi.secrets.SecretsManager;
import picocli.CommandLine;

public abstract class EnvironmentAwareCliCommand extends CommonCliCommandBase {

    @CommandLine.Option(
            names = {"-e", "--environment"},
            required = true,
            description = "The name of the environment in which actions are to be performed")
    protected String environment;

    @Override
    protected final ChangeEngine getChangeEngine(
            ConfigurationSupplier configurationSupplier,
            ChangeEngineFactory changeEngineFactory,
            SecretsManager secretsManager) {

        return changeEngineFactory.getChangeEngine(
                configurationSupplier, secretsManager, environment);
    }
}
