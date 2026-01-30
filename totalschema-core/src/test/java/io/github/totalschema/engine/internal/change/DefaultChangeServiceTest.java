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

import static org.easymock.EasyMock.*;
import static org.testng.Assert.*;

import io.github.totalschema.config.environment.Environment;
import io.github.totalschema.connector.Connector;
import io.github.totalschema.connector.ConnectorManager;
import io.github.totalschema.engine.core.command.api.CommandContext;
import io.github.totalschema.model.ApplyFile;
import io.github.totalschema.model.ChangeFile;
import io.github.totalschema.model.ChangeType;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class DefaultChangeServiceTest {

    private ConnectorManager mockConnectorManager;
    private Environment environment;
    private CommandContext context;
    private Connector mockConnector;
    private DefaultChangeService changeService;

    @BeforeMethod
    public void setUp() {
        mockConnectorManager = createMock(ConnectorManager.class);
        environment = new Environment("DEV");
        context = new CommandContext();
        mockConnector = createMock(Connector.class);
    }

    @Test
    public void testExecuteWithMatchingEnvironment() throws Exception {
        Path changesDir = Paths.get("/changes");
        Path file = Paths.get("/changes/1.X/001.test.DEV.apply.jdbc.sql");
        ChangeFile.Id id =
                new ChangeFile.Id("1.X", "001", "test", "DEV", ChangeType.APPLY, "jdbc", "sql");
        ApplyFile changeFile = new ApplyFile(changesDir, file, id);

        expect(mockConnectorManager.getConnectorByName("jdbc", context)).andReturn(mockConnector);
        mockConnector.execute(changeFile, context);
        expectLastCall().once();

        replay(mockConnectorManager, mockConnector);

        changeService = new DefaultChangeService(mockConnectorManager, environment);
        changeService.execute(changeFile, context);

        verify(mockConnectorManager, mockConnector);
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
        changeService = new DefaultChangeService(mockConnectorManager, prodEnvironment);
        changeService.execute(changeFile, context);
    }

    @Test
    public void testExecuteWithNoEnvironmentRestriction() throws Exception {
        Path changesDir = Paths.get("/changes");
        Path file = Paths.get("/changes/1.X/001.test.apply.jdbc.sql");
        ChangeFile.Id id =
                new ChangeFile.Id("1.X", "001", "test", null, ChangeType.APPLY, "jdbc", "sql");
        ApplyFile changeFile = new ApplyFile(changesDir, file, id);

        expect(mockConnectorManager.getConnectorByName("jdbc", context)).andReturn(mockConnector);
        mockConnector.execute(changeFile, context);
        expectLastCall().once();

        replay(mockConnectorManager, mockConnector);

        changeService = new DefaultChangeService(mockConnectorManager, environment);
        changeService.execute(changeFile, context);

        verify(mockConnectorManager, mockConnector);
    }

    @Test
    public void testExecuteWithDifferentConnectorTypes() throws Exception {
        Path changesDir = Paths.get("/changes");
        Path file = Paths.get("/changes/1.X/001.test.apply.shell.sh");
        ChangeFile.Id id =
                new ChangeFile.Id("1.X", "001", "test", null, ChangeType.APPLY, "shell", "sh");
        ApplyFile changeFile = new ApplyFile(changesDir, file, id);

        expect(mockConnectorManager.getConnectorByName("shell", context)).andReturn(mockConnector);
        mockConnector.execute(changeFile, context);
        expectLastCall().once();

        replay(mockConnectorManager, mockConnector);

        changeService = new DefaultChangeService(mockConnectorManager, environment);
        changeService.execute(changeFile, context);

        verify(mockConnectorManager, mockConnector);
    }

    @Test
    public void testConstructorWithCommandContext() {
        context.setValue(ConnectorManager.class, mockConnectorManager);
        context.setValue(Environment.class, environment);

        DefaultChangeService service = new DefaultChangeService(context);
        assertNotNull(service);
    }

    @Test
    public void testDefaultChangeServiceFactory() {
        DefaultChangeServiceFactory factory = new DefaultChangeServiceFactory();

        context.setValue(ConnectorManager.class, mockConnectorManager);
        context.setValue(Environment.class, environment);

        DefaultChangeService service = (DefaultChangeService) factory.getChangeService(context);
        assertNotNull(service);
    }
}
