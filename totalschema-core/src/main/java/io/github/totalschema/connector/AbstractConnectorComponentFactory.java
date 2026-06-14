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
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for Connector ComponentFactories.
 *
 * <p><strong>ALL custom connector factories MUST extend this class.</strong> This is the public API
 * base class that handles configuration resolution, environment-specific overrides, connector
 * lifecycle management, and argument validation. Do not reimplement this logic in custom connector
 * factories, as the implementation may evolve over time.
 *
 * <p>Concrete implementations only need to:
 *
 * <ol>
 *   <li>Call the superclass constructor with the connector type string (e.g., {@code "jdbc"},
 *       {@code "ssh-script"}, {@code "python"}) that matches the {@code type} field in {@code
 *       totalschema.yml}
 *   <li>Implement {@link #createConnector(String, Configuration)} to construct their specific
 *       connector instance
 * </ol>
 *
 * <p><strong>Example:</strong>
 *
 * <pre>{@code
 * public class MyCustomConnectorFactory extends AbstractConnectorComponentFactory {
 *     public MyCustomConnectorFactory() {
 *         super("my-custom-type");
 *     }
 *
 *     @Override
 *     protected Connector createConnector(String connectorName, Configuration configuration) {
 *         return new MyCustomConnector(connectorName, configuration);
 *     }
 * }
 * }</pre>
 *
 * <p><strong>Arguments:</strong> This factory requires exactly two arguments when instantiating a
 * connector:
 *
 * <ul>
 *   <li><strong>name</strong> (String): The connector name from the {@code connectors} section in
 *       {@code totalschema.yml}
 *   <li><strong>configuration</strong> (Configuration): The fully resolved connector configuration
 *       (with environment-specific overrides already merged)
 * </ul>
 *
 * <p>The factory automatically validates that the connector's {@code type} field matches this
 * factory's qualifier, ensuring type safety during connector instantiation.
 *
 * @see Connector
 * @see io.github.totalschema.spi.factory.ComponentFactory
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

    private final String connectorType;

    /**
     * Constructs a new connector factory with the specified connector type qualifier.
     *
     * <p>The connector type string identifies this factory and must match the {@code type} field in
     * the connector configuration within {@code totalschema.yml}.
     *
     * <p>Built-in connector types include: {@code "jdbc"}, {@code "ssh-script"}, {@code
     * "ssh-commands"}, {@code "shell"}, {@code "python"}.
     *
     * @param connectorType the connector type string (must not be null or blank)
     * @throws IllegalArgumentException if connectorType is null or blank
     */
    protected AbstractConnectorComponentFactory(String connectorType) {
        if (connectorType == null || connectorType.isBlank()) {
            throw new IllegalArgumentException("Connector type must not be null or blank");
        }
        this.connectorType = connectorType;
    }

    /**
     * Returns the connector type qualifier.
     *
     * <p>This method returns the connector type string that was provided to the constructor. The
     * type is used to:
     *
     * <ul>
     *   <li>Match connector configurations in {@code totalschema.yml}
     *   <li>Validate that the correct factory is being used for a given connector
     *   <li>Support multiple connector types within the same container
     * </ul>
     *
     * @return An {@code Optional} containing the connector type string (always present)
     */
    @Override
    public final Optional<String> getQualifier() {
        return Optional.of(connectorType);
    }

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
