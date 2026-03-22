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

import io.github.totalschema.spi.sql.SqlDialect;
import org.testng.annotations.Test;

public class DefaultSqlDialectComponentFactoryTest {

    @Test
    public void testFactoryMetadata() {
        DefaultSqlDialectComponentFactory factory = new DefaultSqlDialectComponentFactory();

        assertFalse(factory.isLazy(), "Factory should be eager (not lazy)");
        assertEquals(factory.getConstructedClass(), SqlDialect.class);
        assertEquals(factory.getQualifier(), "default");
        assertNotNull(factory.getRequiredContextTypes());
        assertTrue(factory.getRequiredContextTypes().isEmpty());
    }

    @Test
    public void testNewComponent() {
        DefaultSqlDialectComponentFactory factory = new DefaultSqlDialectComponentFactory();

        // Context can be null since factory doesn't use it
        SqlDialect dialect = factory.newComponent(null);

        assertNotNull(dialect);
        assertTrue(dialect instanceof DefaultSqlDialect);
    }

    @Test
    public void testNewComponentCreatesNewInstances() {
        DefaultSqlDialectComponentFactory factory = new DefaultSqlDialectComponentFactory();

        SqlDialect dialect1 = factory.newComponent(null);
        SqlDialect dialect2 = factory.newComponent(null);

        assertNotNull(dialect1);
        assertNotNull(dialect2);
        // Each call should create a new instance
        assertNotSame(dialect1, dialect2);
    }
}
