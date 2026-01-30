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

package io.github.totalschema.spi.script;

import static org.testng.Assert.*;

import org.testng.annotations.Test;

public class ScriptExecutorManagerTest {

    @Test
    public void testGetInstanceReturnsSingleton() {
        ScriptExecutorManager instance1 = ScriptExecutorManager.getInstance();
        ScriptExecutorManager instance2 = ScriptExecutorManager.getInstance();

        assertNotNull(instance1);
        assertSame(instance1, instance2);
    }

    @Test
    public void testGetScriptExecutorFactoryByExtensionForSql() {
        ScriptExecutorManager manager = ScriptExecutorManager.getInstance();

        ScriptExecutorFactory factory = manager.getScriptExecutorFactoryByExtension("sql");

        assertNotNull(factory);
    }

    @Test
    public void testGetScriptExecutorFactoryByExtensionCaseInsensitive() {
        ScriptExecutorManager manager = ScriptExecutorManager.getInstance();

        ScriptExecutorFactory factoryLower = manager.getScriptExecutorFactoryByExtension("sql");
        ScriptExecutorFactory factoryUpper = manager.getScriptExecutorFactoryByExtension("SQL");
        ScriptExecutorFactory factoryMixed = manager.getScriptExecutorFactoryByExtension("Sql");

        assertNotNull(factoryLower);
        assertNotNull(factoryUpper);
        assertNotNull(factoryMixed);
        assertSame(factoryLower, factoryUpper);
        assertSame(factoryLower, factoryMixed);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testGetScriptExecutorFactoryByExtensionThrowsForUnknownExtension() {
        ScriptExecutorManager manager = ScriptExecutorManager.getInstance();

        manager.getScriptExecutorFactoryByExtension("unknown");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testGetScriptExecutorFactoryByExtensionThrowsWithMessage() {
        ScriptExecutorManager manager = ScriptExecutorManager.getInstance();

        try {
            manager.getScriptExecutorFactoryByExtension("xyz");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("No ScriptExecutorFactory found for"));
            assertTrue(e.getMessage().contains("xyz"));
            throw e;
        }
    }

    @Test
    public void testGetScriptExecutorFactoryIsCachedAcrossCalls() {
        ScriptExecutorManager manager = ScriptExecutorManager.getInstance();

        // First call initializes the cache
        ScriptExecutorFactory factory1 = manager.getScriptExecutorFactoryByExtension("sql");
        // Second call should use cached value
        ScriptExecutorFactory factory2 = manager.getScriptExecutorFactoryByExtension("sql");

        assertSame(factory1, factory2);
    }
}
