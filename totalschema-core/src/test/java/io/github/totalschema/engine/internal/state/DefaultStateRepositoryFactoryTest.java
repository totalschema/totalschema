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

package io.github.totalschema.engine.internal.state;

import static org.testng.Assert.*;

import io.github.totalschema.config.Configuration;
import io.github.totalschema.config.MapConfiguration;
import io.github.totalschema.engine.core.command.api.CommandContext;
import java.util.Map;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class DefaultStateRepositoryFactoryTest {

    private DefaultStateRepositoryFactory factory;
    private CommandContext context;

    @BeforeMethod
    public void setUp() {
        factory = new DefaultStateRepositoryFactory();
        context = new CommandContext();
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testGetStateRecordRepositoryWithMissingType() {
        Configuration config = new MapConfiguration(Map.of());
        context.setValue(Configuration.class, config);

        factory.getStateRecordRepository(context);
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testGetStateRecordRepositoryWithInvalidType() {
        Configuration config = new MapConfiguration(Map.of("stateRepository.type", "invalid-type"));
        context.setValue(Configuration.class, config);

        factory.getStateRecordRepository(context);
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testGetStateRecordRepositoryWithCsvTypeMissingDirectory() {
        Configuration config = new MapConfiguration(Map.of("stateRepository.type", "csv"));
        context.setValue(Configuration.class, config);

        factory.getStateRecordRepository(context);
    }
}
