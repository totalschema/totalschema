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

package io.github.totalschema.engine.services.sql;

import static org.testng.Assert.*;

import io.github.totalschema.engine.core.command.api.CommandContext;
import io.github.totalschema.spi.sql.SqlDialect;
import org.testng.annotations.Test;

public class DefaultSqlDialectFactoryTest {

    @Test
    public void testGetSqlDialect() {
        CommandContext context = new CommandContext();
        DefaultSqlDialectFactory factory = new DefaultSqlDialectFactory();

        SqlDialect dialect = factory.getSqlDialect(context);

        assertNotNull(dialect);
        assertTrue(dialect instanceof DefaultSqlDialect);
    }

    @Test
    public void testGetSqlDialectCreatesNewInstances() {
        CommandContext context = new CommandContext();
        DefaultSqlDialectFactory factory = new DefaultSqlDialectFactory();

        SqlDialect dialect1 = factory.getSqlDialect(context);
        SqlDialect dialect2 = factory.getSqlDialect(context);

        assertNotNull(dialect1);
        assertNotNull(dialect2);
        // Each call should create a new instance
        assertNotSame(dialect1, dialect2);
    }

    @Test
    public void testGetSqlDialectWithNullContext() {
        DefaultSqlDialectFactory factory = new DefaultSqlDialectFactory();

        // Should work even with null context since DefaultSqlDialect doesn't use it
        SqlDialect dialect = factory.getSqlDialect(null);

        assertNotNull(dialect);
    }
}
