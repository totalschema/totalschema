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

package io.github.totalschema.engine.internal.change;

import static org.testng.Assert.*;

import io.github.totalschema.config.Configuration;
import io.github.totalschema.config.MapConfiguration;
import io.github.totalschema.config.environment.Environment;
import io.github.totalschema.connector.ConnectorManager;
import io.github.totalschema.connector.DefaultConnectorManager;
import io.github.totalschema.engine.core.command.api.CommandContext;
import io.github.totalschema.engine.core.event.EventDispatcher;
import io.github.totalschema.model.ApplyFile;
import io.github.totalschema.model.ChangeFile;
import io.github.totalschema.model.ChangeType;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class DefaultChangeServiceTest {

    private Environment environment;
    private CommandContext context;
    private DefaultChangeService changeService;
    private Configuration config;

    @BeforeMethod
    public void setUp() {
        environment = new Environment("DEV");
        context = new CommandContext();

        // Set up basic configuration
        config =
                new MapConfiguration(
                        Map.of(
                                "connectors.jdbc.type", "jdbc",
                                "connectors.shell.type", "shell"));

        context.setValue(Configuration.class, config);
        context.setValue(Environment.class, environment);
        context.setValue(EventDispatcher.class, new EventDispatcher());
        context.setValue(ConnectorManager.class, new DefaultConnectorManager());

        changeService = new DefaultChangeService(new DefaultConnectorManager(), environment);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testExecuteWithMismatchedEnvironment() throws Exception {
        Path changesDir = Paths.get("/changes");
        Path file = Paths.get("/changes/1.X/001.test.DEV.apply.jdbc.sql");
        ChangeFile.Id id =
                new ChangeFile.Id("1.X", "001", "test", "DEV", ChangeType.APPLY, "jdbc", "sql");
        ApplyFile changeFile = new ApplyFile(changesDir, file, id);

        // Create environment with different name to cause mismatch
        Environment prodEnvironment = new Environment("PROD");
        DefaultChangeService prodChangeService =
                new DefaultChangeService(new DefaultConnectorManager(), prodEnvironment);
        prodChangeService.execute(changeFile, context);
    }

    @Test
    public void testConstructorWithEnvironment() {
        DefaultChangeService service =
                new DefaultChangeService(new DefaultConnectorManager(), environment);
        assertNotNull(service);
    }

    @Test
    public void testDefaultChangeServiceFactory() {
        DefaultChangeServiceFactory factory = new DefaultChangeServiceFactory();

        DefaultChangeService service = (DefaultChangeService) factory.createComponent(context);
        assertNotNull(service);
    }
}
