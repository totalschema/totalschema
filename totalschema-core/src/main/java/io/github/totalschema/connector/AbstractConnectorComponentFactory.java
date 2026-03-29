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

import static io.github.totalschema.spi.factory.ArgumentSpecification.*;

import io.github.totalschema.config.Configuration;
import io.github.totalschema.engine.api.Context;
import io.github.totalschema.spi.factory.ArgumentHandler;
import io.github.totalschema.spi.factory.ArgumentSpecification;
import io.github.totalschema.spi.factory.ComponentFactory;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for Connector ComponentFactories.
 *
 * <p>This class handles all the complexity of configuration resolution (including
 * environment-specific overrides) and connector lifecycle management. Concrete implementations only
 * need to implement {@link #createConnector(String, Configuration)} to construct their specific
 * connector type.
 *
 * <p>All connector factories require one argument: the connector name from configuration. This
 * allows the factory to look up the correct configuration for the connector. The factory merges
 * global and environment specific configuration for the connector being instantiated.
 */
public abstract class AbstractConnectorComponentFactory extends ComponentFactory<Connector> {

    private static final Logger logger =
            LoggerFactory.getLogger(AbstractConnectorComponentFactory.class);

    private static final ArgumentSpecification<String> NAME =
            string("name").withConstraint(notBlank());

    private static final ArgumentSpecification<Configuration> CONFIGURATION =
            configuration("configuration");

    private static final ArgumentHandler ARGUMENTS =
            ArgumentHandler.getInstance(
                    AbstractConnectorComponentFactory.class, NAME, CONFIGURATION);

    @Override
    public final boolean isLazy() {
        return true; // Connectors are always created on-demand with arguments
    }

    @Override
    public final Class<Connector> getComponentType() {
        return Connector.class;
    }

    @Override
    public final List<Class<?>> getDependencies() {
        return List.of();
    }

    @Override
    public final List<ArgumentSpecification<?>> getArgumentSpecifications() {
        return ARGUMENTS.getSpecifications();
    }

    @Override
    public final Connector createComponent(Context context, List<Object> arguments) {
        ARGUMENTS.validateStructure(arguments);

        String connectorName = ARGUMENTS.getArgument(NAME, arguments);
        Configuration configuration = ARGUMENTS.getArgument(CONFIGURATION, arguments);

        // Validate connector type matches this factory's qualifier
        String configuredType =
                configuration
                        .getString("type")
                        .orElseThrow(
                                () ->
                                        new RuntimeException(
                                                "No type is specified for connector: "
                                                        + connectorName));

        String expectedType =
                getQualifier()
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "Connector factory must have a qualifier"));

        if (!expectedType.equals(configuredType)) {
            throw new IllegalArgumentException(
                    String.format(
                            "Connector '%s' has type '%s' but factory expects '%s'",
                            connectorName, configuredType, expectedType));
        }

        logger.debug(
                "Creating connector '{}' of type '{}' using factory {} with configuration {}",
                connectorName,
                configuredType,
                getClass().getSimpleName(),
                configuration);

        // Create the connector using the subclass implementation
        Connector connector = createConnector(connectorName, configuration);

        logger.info("Created: {}", connector);

        return connector;
    }

    /**
     * Create the specific connector instance.
     *
     * <p>Subclasses implement this method to construct their specific connector type with the
     * resolved configuration.
     *
     * @param connectorName the connector name from configuration
     * @param configuration the fully resolved connector configuration (with environment overrides)
     * @return the connector instance
     */
    protected abstract Connector createConnector(String connectorName, Configuration configuration);
}
