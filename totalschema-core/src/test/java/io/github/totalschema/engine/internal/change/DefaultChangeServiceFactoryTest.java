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

package io.github.totalschema.engine.internal.change;

import static org.testng.Assert.*;

import io.github.totalschema.config.environment.Environment;
import io.github.totalschema.connector.ConnectorManager;
import io.github.totalschema.engine.core.command.api.CommandContext;
import io.github.totalschema.spi.change.ChangeService;
import org.easymock.EasyMock;
import org.testng.annotations.Test;

public class DefaultChangeServiceFactoryTest {

    @Test
    public void testGetChangeService() {
        CommandContext context = new CommandContext();
        ConnectorManager mockConnectorManager = EasyMock.createMock(ConnectorManager.class);
        Environment mockEnvironment = new Environment("TEST");

        context.setValue(ConnectorManager.class, mockConnectorManager);
        context.setValue(Environment.class, mockEnvironment);

        DefaultChangeServiceFactory factory = new DefaultChangeServiceFactory();
        ChangeService service = factory.getChangeService(context);

        assertNotNull(service);
        assertTrue(service instanceof DefaultChangeService);
    }

    @Test
    public void testGetChangeServiceCreatesNewInstance() {
        CommandContext context = new CommandContext();
        ConnectorManager mockConnectorManager = EasyMock.createMock(ConnectorManager.class);
        Environment mockEnvironment = new Environment("TEST");

        context.setValue(ConnectorManager.class, mockConnectorManager);
        context.setValue(Environment.class, mockEnvironment);

        DefaultChangeServiceFactory factory = new DefaultChangeServiceFactory();
        ChangeService service1 = factory.getChangeService(context);
        ChangeService service2 = factory.getChangeService(context);

        assertNotNull(service1);
        assertNotNull(service2);
        // Factory should create new instances each time
        assertNotSame(service1, service2);
    }
}
