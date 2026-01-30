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

package io.github.totalschema.engine.internal.hash;

import static org.testng.Assert.*;

import io.github.totalschema.config.Configuration;
import io.github.totalschema.config.MapConfiguration;
import io.github.totalschema.engine.core.command.api.CommandContext;
import io.github.totalschema.spi.hash.HashService;
import java.util.Map;
import org.testng.annotations.Test;

public class DefaultHashServiceFactoryTest {

    @Test
    public void testGetHashService() {
        CommandContext context = new CommandContext();
        Configuration config = new MapConfiguration(Map.of());
        context.setValue(Configuration.class, config);

        DefaultHashServiceFactory factory = new DefaultHashServiceFactory();
        HashService service = factory.getHashService(context);

        assertNotNull(service);
        assertTrue(service instanceof DefaultHashService);
    }

    @Test
    public void testGetHashServiceWithInvalidConfiguration() {
        CommandContext context = new CommandContext();
        Configuration config = new MapConfiguration(Map.of("hash.algorithm", "INVALID"));
        context.setValue(Configuration.class, config);

        DefaultHashServiceFactory factory = new DefaultHashServiceFactory();

        RuntimeException thrown =
                expectThrows(RuntimeException.class, () -> factory.getHashService(context));

        assertTrue(thrown.getMessage().contains("Failure initializing HashService"));
        assertNotNull(thrown.getCause());
    }

    @Test
    public void testGetHashServiceCreatesNewInstances() {
        CommandContext context = new CommandContext();
        Configuration config = new MapConfiguration(Map.of());
        context.setValue(Configuration.class, config);

        DefaultHashServiceFactory factory = new DefaultHashServiceFactory();
        HashService service1 = factory.getHashService(context);
        HashService service2 = factory.getHashService(context);

        assertNotNull(service1);
        assertNotNull(service2);
        assertNotSame(service1, service2);
    }
}
