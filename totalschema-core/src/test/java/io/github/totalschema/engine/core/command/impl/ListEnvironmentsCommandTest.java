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

package io.github.totalschema.engine.core.command.impl;

import static org.easymock.EasyMock.*;
import static org.testng.Assert.*;

import io.github.totalschema.config.Configuration;
import io.github.totalschema.config.environment.DefaultEnvironmentFactory;
import io.github.totalschema.config.environment.Environment;
import io.github.totalschema.config.environment.EnvironmentFactory;
import io.github.totalschema.engine.core.command.api.CommandContext;
import java.util.List;
import java.util.Optional;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ListEnvironmentsCommandTest {

    private CommandContext context;

    @BeforeMethod
    public void setUp() {
        context = new CommandContext();
        EnvironmentFactory environmentFactory = new DefaultEnvironmentFactory();
        context.setValue(EnvironmentFactory.class, environmentFactory);
    }

    @Test
    public void testExecuteWithSingleEnvironment() {
        Configuration config = createMock(Configuration.class);
        expect(config.getList("environments")).andReturn(Optional.of(List.of("DEV")));
        replay(config);

        context.setValue(Configuration.class, config);

        ListEnvironmentsCommand command = new ListEnvironmentsCommand();
        List<Environment> environments = command.execute(context);

        assertNotNull(environments);
        assertEquals(environments.size(), 1);
        assertEquals(environments.get(0).getName(), "DEV");
        verify(config);
    }

    @Test
    public void testExecuteWithMultipleEnvironments() {
        Configuration config = createMock(Configuration.class);
        expect(config.getList("environments"))
                .andReturn(Optional.of(List.of("DEV", "TEST", "PROD")));
        replay(config);

        context.setValue(Configuration.class, config);

        ListEnvironmentsCommand command = new ListEnvironmentsCommand();
        List<Environment> environments = command.execute(context);

        assertNotNull(environments);
        assertEquals(environments.size(), 3);
        assertEquals(environments.get(0).getName(), "DEV");
        assertEquals(environments.get(1).getName(), "TEST");
        assertEquals(environments.get(2).getName(), "PROD");
        verify(config);
    }

    @Test
    public void testExecuteWithEmptyEnvironmentList() {
        Configuration config = createMock(Configuration.class);
        expect(config.getList("environments")).andReturn(Optional.of(List.of()));
        replay(config);

        context.setValue(Configuration.class, config);

        ListEnvironmentsCommand command = new ListEnvironmentsCommand();
        List<Environment> environments = command.execute(context);

        assertNotNull(environments);
        assertTrue(environments.isEmpty());
        verify(config);
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testExecuteWithMissingEnvironmentsConfiguration() {
        Configuration config = createMock(Configuration.class);
        expect(config.getList("environments")).andReturn(Optional.empty());
        replay(config);

        context.setValue(Configuration.class, config);

        ListEnvironmentsCommand command = new ListEnvironmentsCommand();
        try {
            command.execute(context);
        } finally {
            verify(config);
        }
    }
}
