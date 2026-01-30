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

package io.github.totalschema.engine.internal.lock;

import static org.testng.Assert.*;

import io.github.totalschema.config.Configuration;
import io.github.totalschema.config.MapConfiguration;
import io.github.totalschema.engine.core.command.api.CommandContext;
import io.github.totalschema.spi.lock.LockService;
import java.util.Map;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class DefaultLockServiceFactoryTest {

    private DefaultLockServiceFactory factory;
    private CommandContext context;

    @BeforeMethod
    public void setUp() {
        factory = new DefaultLockServiceFactory();
        context = new CommandContext();
    }

    @Test
    public void testGetLockServiceWithNoneType() {
        Configuration config = new MapConfiguration(Map.of("lock.type", "none"));
        context.setValue(Configuration.class, config);

        LockService lockService = factory.getLockService(context);

        assertNull(lockService, "Lock service should be null when type is 'none'");
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testGetLockServiceWithMissingType() {
        Configuration config = new MapConfiguration(Map.of());
        context.setValue(Configuration.class, config);

        factory.getLockService(context);
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testGetLockServiceWithInvalidType() {
        Configuration config = new MapConfiguration(Map.of("lock.type", "invalid-lock-type"));
        context.setValue(Configuration.class, config);

        factory.getLockService(context);
    }
}
