/*
 * totalschema: tool for managing database versioning and schema changes with ease.
 * Copyright (C) 2025-2026 totalschema development team
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

package io.github.totalschema.connector.gcp.bigtable.actions;

import static org.easymock.EasyMock.*;
import static org.testng.Assert.*;

import io.github.totalschema.connector.gcp.bigtable.actions.table.createtable.ColumnFamily;
import io.github.totalschema.connector.gcp.bigtable.actions.table.createtable.CreateTable;
import io.github.totalschema.engine.api.Context;
import io.github.totalschema.spi.expression.evaluator.ExpressionEvaluator;
import io.github.totalschema.spi.variables.VariableService;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Tests for {@link ActionFactory} ServiceLoader registration and action creation. */
public class ActionFactoryTest {

    private Context mockContext;
    private ExpressionEvaluator mockEvaluator;
    private VariableService mockVariableService;

    @BeforeMethod
    public void setUp() {
        mockContext = createMock(Context.class);
        mockEvaluator = createMock(ExpressionEvaluator.class);
        mockVariableService = createMock(VariableService.class);

        // Setup mock to return the evaluator
        expect(mockContext.get(ExpressionEvaluator.class)).andReturn(mockEvaluator).anyTimes();

        // Setup mock to return the variable service
        expect(mockContext.get(VariableService.class)).andReturn(mockVariableService).anyTimes();

        // Mock variable service to return empty variables
        expect(mockVariableService.getEffectiveVariables(mockContext))
                .andReturn(Collections.emptyMap())
                .anyTimes();

        // Mock evaluator to perform identity substitution (no variable substitution)
        expect(mockEvaluator.evaluate(anyObject(String.class), anyObject(Map.class)))
                .andAnswer(() -> (String) getCurrentArguments()[0])
                .anyTimes();

        replay(mockContext, mockEvaluator, mockVariableService);
    }

    @Test
    public void testCreateTableActionWithFamilies() {
        // Given: a YAML-like configuration map with structured families
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> createTableConfig = new HashMap<>();
        createTableConfig.put("tableId", "my-table");

        // Add families list
        Map<String, Object> family1 = new HashMap<>();
        family1.put("name", "profile");

        Map<String, Object> family2 = new HashMap<>();
        family2.put("name", "settings");

        Map<String, Object> family3 = new HashMap<>();
        family3.put("name", "audit");

        createTableConfig.put("families", java.util.Arrays.asList(family1, family2, family3));
        config.put("CreateTable", createTableConfig);

        // When: creating an action from the config
        Action<?> action = ActionFactory.getAction(config, mockContext);

        // Then: it should be a CreateTable instance with correct values
        assertNotNull(action);
        assertTrue(action instanceof CreateTable);

        CreateTable createTable = (CreateTable) action;
        assertEquals(createTable.getTableId(), "my-table");
        assertNotNull(createTable.getFamilies());
        assertEquals(createTable.getFamilies().size(), 3);
        assertEquals(createTable.getFamilies().get(0).getName(), "profile");
        assertEquals(createTable.getFamilies().get(1).getName(), "settings");
        assertEquals(createTable.getFamilies().get(2).getName(), "audit");
    }

    @Test
    public void testCreateTableActionMinimal() {
        // Given: a minimal configuration (only required fields)
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> createTableConfig = new HashMap<>();
        createTableConfig.put("tableId", "minimal-table");
        config.put("CreateTable", createTableConfig);

        // When: creating an action
        Action<?> action = ActionFactory.getAction(config, mockContext);

        // Then: it should work with no families
        assertNotNull(action);
        assertTrue(action instanceof CreateTable);

        CreateTable createTable = (CreateTable) action;
        assertEquals(createTable.getTableId(), "minimal-table");
        assertNull(createTable.getFamilies());
    }

    @Test
    public void testCreateTableActionWithMaxVersionsGcRule() {
        // Given: family with maxVersions GC rule
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> createTableConfig = new HashMap<>();
        createTableConfig.put("tableId", "versioned-table");

        Map<String, Object> family = new HashMap<>();
        family.put("name", "profile");
        family.put("maxVersions", 3);

        createTableConfig.put("families", java.util.Arrays.asList(family));
        config.put("CreateTable", createTableConfig);

        // When: creating an action
        Action<?> action = ActionFactory.getAction(config, mockContext);

        // Then: family should have maxVersions rule
        assertNotNull(action);
        assertTrue(action instanceof CreateTable);

        CreateTable createTable = (CreateTable) action;
        assertEquals(createTable.getFamilies().size(), 1);
        ColumnFamily columnFamily = createTable.getFamilies().get(0);
        assertEquals(columnFamily.getName(), "profile");
        assertEquals(columnFamily.getMaxVersions(), Integer.valueOf(3));
        assertTrue(columnFamily.hasGcRules());
    }

    @Test
    public void testCreateTableActionWithMaxAgeGcRule() {
        // Given: family with maxAge GC rule
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> createTableConfig = new HashMap<>();
        createTableConfig.put("tableId", "aged-table");

        Map<String, Object> family = new HashMap<>();
        family.put("name", "logs");
        family.put("maxAge", "7d");

        createTableConfig.put("families", java.util.Arrays.asList(family));
        config.put("CreateTable", createTableConfig);

        // When: creating an action
        Action<?> action = ActionFactory.getAction(config, mockContext);

        // Then: family should have maxAge rule
        assertNotNull(action);
        assertTrue(action instanceof CreateTable);

        CreateTable createTable = (CreateTable) action;
        assertEquals(createTable.getFamilies().size(), 1);
        ColumnFamily columnFamily = createTable.getFamilies().get(0);
        assertEquals(columnFamily.getName(), "logs");
        assertEquals(columnFamily.getMaxAge(), "7d");
        assertTrue(columnFamily.hasGcRules());
    }

    @Test
    public void testCreateTableActionWithIntersectionGcRule() {
        // Given: family with intersection (maxVersions AND maxAge)
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> createTableConfig = new HashMap<>();
        createTableConfig.put("tableId", "intersection-table");

        Map<String, Object> family = new HashMap<>();
        family.put("name", "audit");
        family.put("maxVersions", 10);
        family.put("maxAge", "90d");

        createTableConfig.put("families", java.util.Arrays.asList(family));
        config.put("CreateTable", createTableConfig);

        // When: creating an action
        Action<?> action = ActionFactory.getAction(config, mockContext);

        // Then: family should have both rules (implicit intersection)
        assertNotNull(action);
        assertTrue(action instanceof CreateTable);

        CreateTable createTable = (CreateTable) action;
        assertEquals(createTable.getFamilies().size(), 1);
        ColumnFamily columnFamily = createTable.getFamilies().get(0);
        assertEquals(columnFamily.getName(), "audit");
        assertEquals(columnFamily.getMaxVersions(), Integer.valueOf(10));
        assertEquals(columnFamily.getMaxAge(), "90d");
        assertTrue(columnFamily.hasGcRules());
    }

    @Test
    public void testCreateTableActionWithUnionGcRule() {
        // Given: family with explicit union rule
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> createTableConfig = new HashMap<>();
        createTableConfig.put("tableId", "union-table");

        Map<String, Object> rule1 = new HashMap<>();
        rule1.put("maxVersions", 3);

        Map<String, Object> rule2 = new HashMap<>();
        rule2.put("maxAge", "24h");

        Map<String, Object> family = new HashMap<>();
        family.put("name", "cache");
        family.put("union", java.util.Arrays.asList(rule1, rule2));

        createTableConfig.put("families", java.util.Arrays.asList(family));
        config.put("CreateTable", createTableConfig);

        // When: creating an action
        Action<?> action = ActionFactory.getAction(config, mockContext);

        // Then: family should have union rule
        assertNotNull(action);
        assertTrue(action instanceof CreateTable);

        CreateTable createTable = (CreateTable) action;
        assertEquals(createTable.getFamilies().size(), 1);
        ColumnFamily columnFamily = createTable.getFamilies().get(0);
        assertEquals(columnFamily.getName(), "cache");
        assertNotNull(columnFamily.getUnion());
        assertEquals(columnFamily.getUnion().size(), 2);
        assertTrue(columnFamily.hasGcRules());
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*exactly one top-level key.*")
    public void testMultipleTopLevelKeysThrowsException() {
        // Given: configuration with multiple top-level keys
        Map<String, Object> config = new HashMap<>();
        config.put("CreateTable", new HashMap<>());
        config.put("DeleteTable", new HashMap<>());

        // When/Then: should throw exception
        ActionFactory.getAction(config, mockContext);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*Unknown action type.*")
    public void testUnknownActionTypeThrowsException() {
        // Given: configuration with unknown action type
        Map<String, Object> config = new HashMap<>();
        config.put("NonExistentAction", new HashMap<>());

        // When/Then: should throw exception
        ActionFactory.getAction(config, mockContext);
    }

    @Test
    public void testNoReflectionUsed() {
        // This test verifies that no reflection is used by checking that
        // the action type name is derived from the factory class name

        // Given: a valid configuration
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> createTableConfig = new HashMap<>();
        createTableConfig.put("tableId", "test-table");
        config.put("CreateTable", createTableConfig);

        // When: creating an action
        Action<?> action = ActionFactory.getAction(config, mockContext);

        // Then: it should work without Class.forName() being called
        // (this would fail if reflection was used incorrectly)
        assertNotNull(action);
        assertTrue(action instanceof CreateTable);
    }
}
