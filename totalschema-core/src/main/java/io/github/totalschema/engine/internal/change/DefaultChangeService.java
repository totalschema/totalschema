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

package io.github.totalschema.engine.internal.change;

import io.github.totalschema.config.environment.Environment;
import io.github.totalschema.connector.Connector;
import io.github.totalschema.connector.ConnectorManager;
import io.github.totalschema.engine.core.command.api.CommandContext;
import io.github.totalschema.model.ChangeFile;
import io.github.totalschema.spi.change.ChangeExecutionException;
import io.github.totalschema.spi.change.ChangeService;

/**
 * Default implementation of ChangeService that routes changes to appropriate connectors. Validates
 * that the change file matches the current environment before applying.
 */
final class DefaultChangeService implements ChangeService {

    private final ConnectorManager connectorManager;
    private final Environment environment;

    DefaultChangeService(ConnectorManager connectorManager, Environment environment) {
        this.connectorManager = connectorManager;
        this.environment = environment;
    }

    @Override
    public void execute(ChangeFile changeFile, CommandContext context)
            throws ChangeExecutionException, InterruptedException {

        String thisEnvironmentName = environment.getName();

        requireMatchingEnvironment(changeFile, thisEnvironmentName);

        try {

            String changeFileConnector = changeFile.getConnector();

            Connector connector = connectorManager.getConnectorByName(changeFileConnector, context);

            connector.execute(changeFile, context);

        } catch (RuntimeException ex) {
            throw new ChangeExecutionException(
                    changeFile,
                    String.format(
                            "Change execution failed for '%s': %s",
                            changeFile.getRelativePath(), ex.getMessage()),
                    ex);
        }
    }

    private static void requireMatchingEnvironment(
            ChangeFile changeFile, String thisEnvironmentName) {

        changeFile
                .getEnvironment()
                .ifPresent(
                        environmentOfTheChangeFile -> {
                            // a change file might not have a restriction on environment,
                            // but if it has one, we check that it matches the current environment
                            if (!thisEnvironmentName.equalsIgnoreCase(environmentOfTheChangeFile)) {
                                throw new IllegalArgumentException(
                                        String.format(
                                                "Change file %s should not be executed in environment '%s'",
                                                changeFile, thisEnvironmentName));
                            }
                        });
    }
}
