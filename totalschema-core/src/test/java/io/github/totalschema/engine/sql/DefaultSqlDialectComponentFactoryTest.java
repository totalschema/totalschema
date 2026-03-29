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

package io.github.totalschema.engine.sql;

import static org.testng.Assert.*;

import io.github.totalschema.engine.internal.sql.DefaultSqlDialect;
import io.github.totalschema.engine.internal.sql.DefaultSqlDialectComponentFactory;
import io.github.totalschema.spi.sql.SqlDialect;
import java.util.List;
import java.util.Optional;
import org.testng.annotations.Test;

public class DefaultSqlDialectComponentFactoryTest {

    @Test
    public void testFactoryMetadata() {
        DefaultSqlDialectComponentFactory factory = new DefaultSqlDialectComponentFactory();

        assertFalse(factory.isLazy(), "Factory should be eager (not lazy)");
        assertEquals(factory.getComponentType(), SqlDialect.class);
        assertEquals(factory.getQualifier(), Optional.of("default"));
        assertNotNull(factory.getDependencies());
        assertTrue(factory.getDependencies().isEmpty());
    }

    @Test
    public void testCreateComponent() {
        DefaultSqlDialectComponentFactory factory = new DefaultSqlDialectComponentFactory();

        // Context can be null since factory doesn't use it
        SqlDialect dialect = factory.createComponent(null, List.of());

        assertNotNull(dialect);
        assertTrue(dialect instanceof DefaultSqlDialect);
    }

    @Test
    public void testNewComponentCreatesCreateInstances() {
        DefaultSqlDialectComponentFactory factory = new DefaultSqlDialectComponentFactory();

        SqlDialect dialect1 = factory.createComponent(null, List.of());
        SqlDialect dialect2 = factory.createComponent(null, List.of());

        assertNotNull(dialect1);
        assertNotNull(dialect2);
        // Each call should create a new instance
        assertNotSame(dialect1, dialect2);
    }
}
