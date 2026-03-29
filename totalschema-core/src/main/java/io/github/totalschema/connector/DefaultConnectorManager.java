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

package io.github.totalschema.connector;

import io.github.totalschema.config.Configuration;
import io.github.totalschema.config.environment.Environment;
import io.github.totalschema.engine.api.Context;
import io.github.totalschema.engine.core.container.FactoryNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of ConnectorManager.
 *
 * <p>Creates and caches connector instances based on configuration. Discovers connector factories
 * via Java ServiceLoader API, supporting both built-in connector types and user-defined connector
 * types.
 */
public class DefaultConnectorManager implements ConnectorManager {

    private static final Logger logger = LoggerFactory.getLogger(DefaultConnectorManager.class);

    @Override
    public final Connector getConnectorByName(String connectorName, Context context) {

        Configuration configurationOfTheConnector =
                getConfigurationOfTheConnector(connectorName, context);

        if (configurationOfTheConnector.isEmpty()) {
            throw new RuntimeException(
                    "Configuration for the connector is not found: " + connectorName);
        }

        String connectorType =
                configurationOfTheConnector
                        .getString("type")
                        .orElseThrow(
                                () ->
                                        new RuntimeException(
                                                "No type is specified for connector: "
                                                        + connectorName));

        try {

            Connector connector =
                    context.get(
                            Connector.class,
                            connectorType,
                            connectorName,
                            configurationOfTheConnector);

            logger.info("Created: {}", connector);

            return connector;

        } catch (FactoryNotFoundException ex) {
            throw new IllegalArgumentException(
                    String.format("No such connector: '%s'", connectorName), ex);
        }
    }

    private Configuration getConfigurationOfTheConnector(String connectorName, Context context) {

        Configuration connectorsConfig =
                getConnectorsConfiguration(
                        context.get(Configuration.class),
                        context.getOptional(Environment.class).orElse(null));

        return connectorsConfig.getPrefixNamespace(connectorName);
    }

    private Configuration getConnectorsConfiguration(
            Configuration configuration, Environment environment) {

        Configuration connectorsConfig = configuration.getPrefixNamespace("connectors");

        logger.debug("Global connector configuration: {}", connectorsConfig);

        if (environment != null) {
            String environmentName = environment.getName();

            Configuration environmentSpecificConnectorConfig =
                    configuration.getPrefixNamespace("environments", environmentName, "connectors");

            logger.debug(
                    "{} environment specific connector configuration: {}",
                    environmentName,
                    connectorsConfig);

            connectorsConfig = connectorsConfig.addAll(environmentSpecificConnectorConfig);
        }

        logger.debug("Applicable connector configuration: {}", connectorsConfig);

        return connectorsConfig;
    }
}
