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

import static org.testng.Assert.*;

import io.github.totalschema.config.Configuration;
import io.github.totalschema.config.MapConfiguration;
import io.github.totalschema.connector.jdbc.JdbcConnectorFactory;
import io.github.totalschema.engine.core.container.ComponentContainer;
import io.github.totalschema.spi.ServiceLoaderFactory;
import io.github.totalschema.spi.factory.ComponentFactory;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.testng.annotations.Test;

/** Tests for Connector ComponentFactory functionality. */
public class ConnectorComponentFactoryTest {

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void testBuiltInConnectorComponentFactoriesAreLoaded() {
        // Verify that all built-in connector ComponentFactories are discoverable via ServiceLoader
        List<ComponentFactory> allFactories =
                (List<ComponentFactory>)
                        (List) ServiceLoaderFactory.getAllServices(ComponentFactory.class);

        // Filter for Connector factories
        List<ComponentFactory> connectorFactories =
                allFactories.stream()
                        .filter(f -> Connector.class.equals(f.getComponentType()))
                        .collect(Collectors.toList());

        assertFalse(
                connectorFactories.isEmpty(),
                "Should find at least one connector ComponentFactory");

        // Verify we have all 4 built-in connector factories
        assertEquals(
                connectorFactories.size(),
                1,
                "Should find at the single built-in connector ComponentFactories, found: "
                        + connectorFactories.size());

        // Verify that all built-in qualifiers are present
        List<String> qualifiers =
                connectorFactories.stream()
                        .map(f -> (Optional<String>) f.getQualifier())
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(Collectors.toList());

        assertTrue(qualifiers.contains("jdbc"), "Should have jdbc ComponentFactory");
    }

    @Test
    public void testJdbcConnectorComponentFactoryCreatesConnector() {
        // Global configuration
        Configuration globalConfig =
                new MapConfiguration(
                        Map.of(
                                "connectors.test-jdbc.type", "jdbc",
                                "connectors.test-jdbc.url", "jdbc:h2:mem:test",
                                "connectors.test-jdbc.username", "sa",
                                "connectors.test-jdbc.password", ""));

        try (ComponentContainer container =
                ComponentContainer.builder()
                        .withComponent(Configuration.class, globalConfig)
                        .withFactory(new JdbcConnectorFactory())
                        .build()) {

            Configuration connectorConfig = globalConfig.getPrefixNamespace("connectors.test-jdbc");

            // Get JDBC connector from container
            // Pass the connector-specific config, not the global config
            Connector connector =
                    container.get(Connector.class, "jdbc", "test-jdbc", connectorConfig);

            assertNotNull(connector, "ComponentFactory should create a connector");
            assertTrue(
                    connector.toString().contains("test-jdbc"),
                    "Connector should include the name");
        }
    }

    @Test
    public void testAllConnectorFactoriesAreLazy() {
        // All connector factories should be lazy since they require arguments
        @SuppressWarnings("rawtypes")
        List<ComponentFactory> allFactories =
                (List<ComponentFactory>)
                        ServiceLoaderFactory.getAllServices(ComponentFactory.class);

        @SuppressWarnings("rawtypes")
        List<ComponentFactory> connectorFactories =
                allFactories.stream()
                        .filter(f -> Connector.class.equals(f.getComponentType()))
                        .collect(Collectors.toList());

        for (ComponentFactory<?> factory : connectorFactories) {
            assertTrue(
                    factory.isLazy(),
                    "Connector factory "
                            + factory.getClass().getName()
                            + " should be lazy (created on-demand with arguments)");
        }
    }

    @Test
    public void testConnectorFactoriesHaveArguments() {
        // All connector factories should require name and configuration arguments
        @SuppressWarnings("rawtypes")
        List<ComponentFactory> allFactories =
                (List<ComponentFactory>)
                        (List<?>) ServiceLoaderFactory.getAllServices(ComponentFactory.class);

        @SuppressWarnings("rawtypes")
        List<ComponentFactory> connectorFactories =
                allFactories.stream()
                        .filter(f -> Connector.class.equals(f.getComponentType()))
                        .collect(Collectors.toList());

        for (ComponentFactory<?> factory : connectorFactories) {
            assertNotNull(
                    factory.getArgumentSpecifications(),
                    "Factory " + factory.getClass().getName() + " should have argument specs");
            assertEquals(
                    factory.getArgumentSpecifications().size(),
                    2,
                    "Factory "
                            + factory.getClass().getName()
                            + " should require 2 arguments: name and configuration");
        }
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void testConnectorQualifiersAreUnique() {
        // Verify that each connector factory has a unique qualifier
        List<ComponentFactory> allFactories =
                (List<ComponentFactory>)
                        (List) ServiceLoaderFactory.getAllServices(ComponentFactory.class);

        List<String> qualifiers =
                allFactories.stream()
                        .filter(f -> Connector.class.equals(f.getComponentType()))
                        .map(ComponentFactory::getQualifier)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .map(String.class::cast)
                        .collect(Collectors.toList());

        long uniqueQualifiers = qualifiers.stream().distinct().count();

        assertEquals(
                uniqueQualifiers,
                qualifiers.size(),
                "All connector factory qualifiers should be unique");
    }
}
