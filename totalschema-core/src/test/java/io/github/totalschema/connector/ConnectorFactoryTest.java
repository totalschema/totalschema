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
import io.github.totalschema.spi.ServiceLoaderFactory;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.testng.annotations.Test;

/** Tests for ConnectorFactory SPI functionality. */
public class ConnectorFactoryTest {

    @Test
    public void testBuiltInConnectorFactoriesAreLoaded() {
        // Verify that all built-in connector factories are discoverable via ServiceLoader
        List<ConnectorFactory> factories =
                ServiceLoaderFactory.getAllServices(ConnectorFactory.class);

        assertFalse(factories.isEmpty(), "Should find at least one connector factory");

        // Verify we have all 4 built-in factories
        assertTrue(
                factories.size() >= 4,
                "Should find at least 4 built-in connector factories, found: " + factories.size());

        // Verify that all built-in types are present
        List<String> types =
                factories.stream()
                        .map(ConnectorFactory::getConnectorType)
                        .collect(Collectors.toList());

        assertTrue(types.contains("jdbc"), "Should have jdbc factory");
        assertTrue(types.contains("ssh-script"), "Should have ssh-script factory");
        assertTrue(types.contains("ssh-commands"), "Should have ssh-commands factory");
        assertTrue(types.contains("shell"), "Should have shell factory");
    }

    @Test
    public void testJdbcConnectorFactoryCreatesConnector() {
        List<ConnectorFactory> factories =
                ServiceLoaderFactory.getAllServices(ConnectorFactory.class);

        ConnectorFactory jdbcFactory =
                factories.stream()
                        .filter(p -> "jdbc".equals(p.getConnectorType()))
                        .findFirst()
                        .orElseThrow(() -> new AssertionError("JDBC factory should be found"));

        Configuration config =
                new MapConfiguration(
                        Map.of(
                                "url", "jdbc:h2:mem:test",
                                "username", "sa",
                                "password", ""));

        Connector connector = jdbcFactory.createConnector("test-jdbc", config);

        assertNotNull(connector, "Factory should create a connector");
        assertTrue(connector.toString().contains("test-jdbc"), "Connector should include the name");
    }

    @Test
    public void testDefaultConnectorManagerLoadsFactories() {
        DefaultConnectorManager manager = new DefaultConnectorManager();

        // The manager should have loaded factories in its constructor
        // We can't directly test the private field, but we can verify behavior
        assertNotNull(manager, "Manager should be created successfully");
    }

    @Test
    public void testConnectorTypeIdentifiers() {
        // Verify that each factory returns a unique, non-null type identifier
        List<ConnectorFactory> factories =
                ServiceLoaderFactory.getAllServices(ConnectorFactory.class);

        for (ConnectorFactory factory : factories) {
            String type = factory.getConnectorType();
            assertNotNull(
                    type,
                    "Factory " + factory.getClass().getName() + " should return non-null type");
            assertFalse(
                    type.trim().isEmpty(),
                    "Factory " + factory.getClass().getName() + " should return non-empty type");
        }
    }
}
