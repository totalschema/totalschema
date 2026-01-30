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

package io.github.totalschema.config.environment;

import static org.easymock.EasyMock.*;
import static org.testng.Assert.*;

import io.github.totalschema.config.Configuration;
import java.util.List;
import java.util.Optional;
import org.testng.annotations.Test;

public class DefaultEnvironmentFactoryTest {

    @Test
    public void testGetEnvironmentsWithSingleEnvironment() {
        Configuration config = createMock(Configuration.class);
        expect(config.getList("environments")).andReturn(Optional.of(List.of("DEV")));
        replay(config);

        DefaultEnvironmentFactory factory = new DefaultEnvironmentFactory();
        List<Environment> environments = factory.getEnvironments(config);

        assertNotNull(environments);
        assertEquals(environments.size(), 1);
        assertEquals(environments.get(0).getName(), "DEV");
        verify(config);
    }

    @Test
    public void testGetEnvironmentsWithMultipleEnvironments() {
        Configuration config = createMock(Configuration.class);
        expect(config.getList("environments"))
                .andReturn(Optional.of(List.of("DEV", "TEST", "PROD")));
        replay(config);

        DefaultEnvironmentFactory factory = new DefaultEnvironmentFactory();
        List<Environment> environments = factory.getEnvironments(config);

        assertNotNull(environments);
        assertEquals(environments.size(), 3);
        assertEquals(environments.get(0).getName(), "DEV");
        assertEquals(environments.get(1).getName(), "TEST");
        assertEquals(environments.get(2).getName(), "PROD");
        verify(config);
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testGetEnvironmentsWithMissingConfiguration() {
        Configuration config = createMock(Configuration.class);
        expect(config.getList("environments")).andReturn(Optional.empty());
        replay(config);

        DefaultEnvironmentFactory factory = new DefaultEnvironmentFactory();

        try {
            factory.getEnvironments(config);
        } finally {
            verify(config);
        }
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testGetEnvironmentsThrowsExceptionWithMessage() {
        Configuration config = createMock(Configuration.class);
        expect(config.getList("environments")).andReturn(Optional.empty());
        replay(config);

        DefaultEnvironmentFactory factory = new DefaultEnvironmentFactory();

        try {
            factory.getEnvironments(config);
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("No environment is declared"));
            verify(config);
            throw e;
        }
    }

    @Test
    public void testGetEnvironmentsWithEmptyList() {
        Configuration config = createMock(Configuration.class);
        expect(config.getList("environments")).andReturn(Optional.of(List.of()));
        replay(config);

        DefaultEnvironmentFactory factory = new DefaultEnvironmentFactory();
        List<Environment> environments = factory.getEnvironments(config);

        assertNotNull(environments);
        assertEquals(environments.size(), 0);
        verify(config);
    }
}
