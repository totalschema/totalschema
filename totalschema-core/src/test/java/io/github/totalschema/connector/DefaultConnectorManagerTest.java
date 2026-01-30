/*
 * totalschema: tool for managing database versioning and schema changes with ease.
 * Copyright (C) 2026 totalschema development team
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
import io.github.totalschema.config.environment.Environment;
import io.github.totalschema.engine.core.command.api.CommandContext;
import io.github.totalschema.engine.core.event.EventDispatcher;
import java.util.Map;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class DefaultConnectorManagerTest {

    private DefaultConnectorManager connectorManager;
    private CommandContext context;

    @BeforeMethod
    public void setUp() {
        connectorManager = new DefaultConnectorManager();
        context = new CommandContext();
        context.setValue(EventDispatcher.class, new EventDispatcher());
    }

    @Test
    public void testGetConnectorByNameWithValidJdbcConnector() {
        Configuration config =
                new MapConfiguration(
                        Map.of(
                                "connectors.my_db.type",
                                "jdbc",
                                "connectors.my_db.jdbc.url",
                                "jdbc:h2:mem:test",
                                "connectors.my_db.jdbc.driver.class",
                                "org.h2.Driver"));
        context.setValue(Configuration.class, config);

        Connector connector = connectorManager.getConnectorByName("my_db", context);

        assertNotNull(connector);
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testGetConnectorByNameWithMissingConfiguration() {
        Configuration config = new MapConfiguration(Map.of());
        context.setValue(Configuration.class, config);

        connectorManager.getConnectorByName("non_existent", context);
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testGetConnectorByNameWithMissingType() {
        Configuration config =
                new MapConfiguration(
                        Map.of(
                                "connectors.my_db.jdbc.url",
                                "jdbc:h2:mem:test",
                                "connectors.my_db.jdbc.driver.class",
                                "org.h2.Driver"));
        context.setValue(Configuration.class, config);

        connectorManager.getConnectorByName("my_db", context);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testGetConnectorByNameWithInvalidType() {
        Configuration config =
                new MapConfiguration(
                        Map.of("connectors.my_db.type", "invalid_type_that_does_not_exist"));
        context.setValue(Configuration.class, config);

        connectorManager.getConnectorByName("my_db", context);
    }

    @Test
    public void testGetConnectorByNameWithEnvironmentSpecificConfiguration() {
        Configuration config =
                new MapConfiguration(
                        Map.of(
                                "connectors.my_db.type",
                                "jdbc",
                                "connectors.my_db.jdbc.url",
                                "jdbc:h2:mem:default",
                                "environments.DEV.connectors.my_db.jdbc.url",
                                "jdbc:h2:mem:dev"));
        context.setValue(Configuration.class, config);
        context.setValue(Environment.class, new Environment("DEV"));

        Connector connector = connectorManager.getConnectorByName("my_db", context);

        assertNotNull(connector);
    }

    @Test
    public void testCachingBehavior() {
        Configuration config =
                new MapConfiguration(
                        Map.of(
                                "connectors.my_db.type",
                                "jdbc",
                                "connectors.my_db.jdbc.url",
                                "jdbc:h2:mem:test"));
        context.setValue(Configuration.class, config);

        Connector connector1 = connectorManager.getConnectorByName("my_db", context);
        Connector connector2 = connectorManager.getConnectorByName("my_db", context);

        assertSame(connector1, connector2, "Connector should be cached");
    }
}
