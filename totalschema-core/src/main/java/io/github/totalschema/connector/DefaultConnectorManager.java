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
import io.github.totalschema.engine.core.command.api.CommandContext;
import io.github.totalschema.engine.core.event.ChangeEngineCloseEvent;
import io.github.totalschema.engine.core.event.CloseResourceChangeEngineCloseListener;
import io.github.totalschema.engine.core.event.EventDispatcher;
import io.github.totalschema.spi.ServiceLoaderFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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

    private final Map<String, ConnectorFactory> connectorFactories;

    private final ConcurrentHashMap<String, Connector> connectorCache = new ConcurrentHashMap<>();

    public DefaultConnectorManager() {
        this.connectorFactories = loadConnectorFactories();
    }

    /**
     * Load all available connector factories via ServiceLoader API.
     *
     * @return a map of connector type to factory
     */
    private Map<String, ConnectorFactory> loadConnectorFactories() {

        List<ConnectorFactory> factories =
                ServiceLoaderFactory.getAllServices(ConnectorFactory.class);
        Map<String, ConnectorFactory> factoryMap = new HashMap<>();

        for (ConnectorFactory factory : factories) {
            String type = factory.getConnectorType();
            if (factoryMap.containsKey(type)) {
                logger.warn(
                        "Duplicate connector factory for type '{}'. Using first found: {}",
                        type,
                        factoryMap.get(type).getClass().getName());
            } else {
                factoryMap.put(type, factory);
                logger.debug(
                        "Registered connector factory for type '{}': {}",
                        type,
                        factory.getClass().getName());
            }
        }

        logger.debug(
                "Loaded {} connector factory(ies): {}", factoryMap.size(), factoryMap.keySet());
        return factoryMap;
    }

    @Override
    public final Connector getConnectorByName(String connectorName, CommandContext context) {
        return connectorCache.computeIfAbsent(
                connectorName, (name) -> createConnector(connectorName, context));
    }

    private Connector createConnector(String connectorName, CommandContext context) {

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

        Connector connector =
                instantiateConnector(connectorType, connectorName, configurationOfTheConnector);

        EventDispatcher eventDispatcher = context.get(EventDispatcher.class);
        eventDispatcher.subscribe(
                ChangeEngineCloseEvent.class,
                CloseResourceChangeEngineCloseListener.create(connector));

        logger.info("Created: {}", connector);

        return connector;
    }

    private Configuration getConfigurationOfTheConnector(
            String connectorName, CommandContext context) {

        Configuration connectorsConfig =
                getConnectorsConfiguration(context.get(Configuration.class), context);

        return connectorsConfig.getPrefixNamespace(connectorName);
    }

    private Configuration getConnectorsConfiguration(
            Configuration configuration, CommandContext context) {

        Configuration connectorsConfig = configuration.getPrefixNamespace("connectors");

        logger.debug("Global connector configuration: {}", connectorsConfig);

        if (context.has(Environment.class)) {
            String environmentName = context.get(Environment.class).getName();

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

    protected Connector instantiateConnector(
            String type, String name, Configuration connectorConfiguration) {

        ConnectorFactory factory = connectorFactories.get(type);

        if (factory == null) {
            throw new IllegalArgumentException("Unknown connector type: " + type);
        }

        logger.debug(
                "Using connector factory for type '{}': {}", type, factory.getClass().getName());
        return factory.createConnector(name, connectorConfiguration);
    }
}
