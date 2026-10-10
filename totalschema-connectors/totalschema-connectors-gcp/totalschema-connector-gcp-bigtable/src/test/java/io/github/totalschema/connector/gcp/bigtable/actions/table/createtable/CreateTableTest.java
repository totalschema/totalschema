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

package io.github.totalschema.connector.gcp.bigtable.actions.table.createtable;

import static org.easymock.EasyMock.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.*;

import com.google.cloud.bigtable.admin.v2.BigtableTableAdminClient;
import com.google.cloud.bigtable.admin.v2.models.CreateTableRequest;
import io.github.totalschema.connector.gcp.bigtable.actions.Action;
import io.github.totalschema.connector.gcp.bigtable.actions.ActionFactory;
import io.github.totalschema.engine.api.Context;
import io.github.totalschema.spi.expression.evaluator.ExpressionEvaluator;
import io.github.totalschema.spi.variables.VariableService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Tests for {@link CreateTable} and {@link CreateTableActionFactory}. */
public class CreateTableTest {

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
    public void testCreateTableActionFromMap() {
        // Given: a YAML-like configuration map
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> createTableConfig = new HashMap<>();
        createTableConfig.put("tableId", "users");
        config.put("CreateTable", createTableConfig);

        // When: creating an action from the config
        Action<?> action = ActionFactory.getAction(config, mockContext);

        // Then: it should be a CreateTable instance with correct values
        assertNotNull(action);
        assertTrue(action instanceof CreateTable);

        CreateTable createTable = (CreateTable) action;
        assertEquals(createTable.getTableId(), "users");
        assertNull(createTable.getFamilies());
    }

    @Test
    public void testCreateTableWithFamilies() {
        // Given: configuration with column families
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> createTableConfig = new HashMap<>();
        createTableConfig.put("tableId", "users");

        List<Map<String, Object>> familiesConfig = new ArrayList<>();
        Map<String, Object> family1 = new HashMap<>();
        family1.put("name", "profile");
        familiesConfig.add(family1);

        Map<String, Object> family2 = new HashMap<>();
        family2.put("name", "settings");
        familiesConfig.add(family2);

        createTableConfig.put("families", familiesConfig);
        config.put("CreateTable", createTableConfig);

        // When: creating an action
        Action<?> action = ActionFactory.getAction(config, mockContext);

        // Then: should have families
        assertNotNull(action);
        assertTrue(action instanceof CreateTable);

        CreateTable createTable = (CreateTable) action;
        assertEquals(createTable.getTableId(), "users");
        assertNotNull(createTable.getFamilies());
        assertEquals(createTable.getFamilies().size(), 2);
        assertEquals(createTable.getFamilies().get(0).getName(), "profile");
        assertEquals(createTable.getFamilies().get(1).getName(), "settings");
    }

    @Test
    public void testCreateTableWithGcRules() {
        // Given: configuration with GC rules
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> createTableConfig = new HashMap<>();
        createTableConfig.put("tableId", "users");

        List<Map<String, Object>> familiesConfig = new ArrayList<>();
        Map<String, Object> family1 = new HashMap<>();
        family1.put("name", "profile");
        family1.put("maxVersions", 3);
        familiesConfig.add(family1);

        Map<String, Object> family2 = new HashMap<>();
        family2.put("name", "audit");
        family2.put("maxAge", "90d");
        familiesConfig.add(family2);

        createTableConfig.put("families", familiesConfig);
        config.put("CreateTable", createTableConfig);

        // When: creating an action
        Action<?> action = ActionFactory.getAction(config, mockContext);

        // Then: should have families with GC rules
        assertNotNull(action);
        assertTrue(action instanceof CreateTable);

        CreateTable createTable = (CreateTable) action;
        assertEquals(createTable.getFamilies().size(), 2);
        assertEquals(createTable.getFamilies().get(0).getMaxVersions(), Integer.valueOf(3));
        assertEquals(createTable.getFamilies().get(1).getMaxAge(), "90d");
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*tableId is required.*")
    public void testCreateTableRequiresTableId() {
        // Given: a CreateTable action without tableId
        CreateTable createTable = new CreateTable();
        createTable.setTableId(null);

        // When/Then: execute should throw exception
        createTable.execute(null);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*tableId is required.*")
    public void testCreateTableRequiresNonEmptyTableId() {
        // Given: a CreateTable action with empty tableId
        CreateTable createTable = new CreateTable();
        createTable.setTableId("");

        // When/Then: execute should throw exception
        createTable.execute(null);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*Column family name is required.*")
    public void testCreateTableRequiresFamilyName() {
        // Given: a CreateTable action with a family without name
        CreateTable createTable = new CreateTable();
        createTable.setTableId("users");

        ColumnFamily family = new ColumnFamily();
        family.setName(null);
        createTable.setFamilies(Collections.singletonList(family));

        // When/Then: execute should throw exception
        createTable.execute(null);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*Column family name is required.*")
    public void testCreateTableRequiresNonEmptyFamilyName() {
        // Given: a CreateTable action with a family with empty name
        CreateTable createTable = new CreateTable();
        createTable.setTableId("users");

        ColumnFamily family = new ColumnFamily();
        family.setName("");
        createTable.setFamilies(Collections.singletonList(family));

        // When/Then: execute should throw exception
        createTable.execute(null);
    }

    @Test
    public void testCreateTableExecuteWithoutFamilies() {
        // Given: a valid action without families
        CreateTable createTable = new CreateTable();
        createTable.setTableId("users");

        BigtableTableAdminClient mockClient = mock(BigtableTableAdminClient.class);

        // When: executing the action
        createTable.execute(mockClient);

        // Then: verify createTable was called with correct tableId
        ArgumentCaptor<CreateTableRequest> requestCaptor =
                ArgumentCaptor.forClass(CreateTableRequest.class);
        verify(mockClient).createTable(requestCaptor.capture());

        CreateTableRequest capturedRequest = requestCaptor.getValue();
        // Verify the request was built with the correct tableId
        assertEquals(
                capturedRequest.toProto("foo", "bar").getTableId(),
                "users",
                "Expected tableId to be 'users'");
    }

    @Test
    public void testCreateTableExecuteWithFamiliesNoGcRules() {
        // Given: a valid action with families but no GC rules
        CreateTable createTable = new CreateTable();
        createTable.setTableId("users");

        ColumnFamily family1 = new ColumnFamily();
        family1.setName("profile");

        ColumnFamily family2 = new ColumnFamily();
        family2.setName("settings");

        createTable.setFamilies(List.of(family1, family2));

        BigtableTableAdminClient mockClient = mock(BigtableTableAdminClient.class);

        // When: executing the action
        createTable.execute(mockClient);

        // Then: verify createTable was called with correct tableId and families
        ArgumentCaptor<CreateTableRequest> requestCaptor =
                ArgumentCaptor.forClass(CreateTableRequest.class);
        verify(mockClient).createTable(requestCaptor.capture());

        CreateTableRequest capturedRequest = requestCaptor.getValue();
        // Verify tableId
        assertEquals(capturedRequest.toProto("foo", "bar").getTableId(), "users");
        // Verify families are present in the request
        assertTrue(
                capturedRequest
                        .toProto("foo", "bar")
                        .getTable()
                        .getColumnFamiliesMap()
                        .containsKey("profile"),
                "Expected 'profile' family in request");
        assertTrue(
                capturedRequest
                        .toProto("foo", "bar")
                        .getTable()
                        .getColumnFamiliesMap()
                        .containsKey("settings"),
                "Expected 'settings' family in request");
        assertEquals(
                capturedRequest.toProto("foo", "bar").getTable().getColumnFamiliesCount(),
                2,
                "Expected 2 families");
    }

    @Test
    public void testCreateTableExecuteWithFamiliesAndGcRules() {
        // Given: a valid action with families and GC rules
        CreateTable createTable = new CreateTable();
        createTable.setTableId("users");

        ColumnFamily family1 = new ColumnFamily();
        family1.setName("profile");
        family1.setMaxVersions(3);

        ColumnFamily family2 = new ColumnFamily();
        family2.setName("audit");
        family2.setMaxAge("90d");

        ColumnFamily family3 = new ColumnFamily();
        family3.setName("cache");
        family3.setMaxVersions(10);
        family3.setMaxAge("30d");

        createTable.setFamilies(List.of(family1, family2, family3));

        BigtableTableAdminClient mockClient = mock(BigtableTableAdminClient.class);

        // When: executing the action
        createTable.execute(mockClient);

        // Then: verify createTable was called with correct content
        ArgumentCaptor<CreateTableRequest> requestCaptor =
                ArgumentCaptor.forClass(CreateTableRequest.class);
        verify(mockClient).createTable(requestCaptor.capture());

        CreateTableRequest capturedRequest = requestCaptor.getValue();
        // Verify tableId
        assertEquals(capturedRequest.toProto("foo", "bar").getTableId(), "users");

        // Verify families with GC rules are present
        Map<String, com.google.bigtable.admin.v2.ColumnFamily> familiesMap =
                capturedRequest.toProto("foo", "bar").getTable().getColumnFamiliesMap();

        assertEquals(familiesMap.size(), 3, "Expected 3 families");

        // Verify profile family has GC rule (maxVersions=3)
        assertTrue(familiesMap.containsKey("profile"), "Expected 'profile' family");
        assertTrue(
                familiesMap.get("profile").hasGcRule(),
                "Expected 'profile' family to have GC rule");

        // Verify audit family has GC rule (maxAge)
        assertTrue(familiesMap.containsKey("audit"), "Expected 'audit' family");
        assertTrue(familiesMap.get("audit").hasGcRule(), "Expected 'audit' family to have GC rule");

        // Verify cache family has GC rule (intersection of maxVersions and maxAge)
        assertTrue(familiesMap.containsKey("cache"), "Expected 'cache' family");
        assertTrue(familiesMap.get("cache").hasGcRule(), "Expected 'cache' family to have GC rule");
    }

    @Test
    public void testCreateTableExecuteWithNullFamilies() {
        // Given: a valid action with null families (equivalent to no families)
        CreateTable createTable = new CreateTable();
        createTable.setTableId("users");
        createTable.setFamilies(null);

        BigtableTableAdminClient mockClient = mock(BigtableTableAdminClient.class);

        // When: executing the action
        createTable.execute(mockClient);

        // Then: verify createTable was called with correct tableId and no families
        ArgumentCaptor<CreateTableRequest> requestCaptor =
                ArgumentCaptor.forClass(CreateTableRequest.class);
        verify(mockClient).createTable(requestCaptor.capture());

        CreateTableRequest capturedRequest = requestCaptor.getValue();
        // Verify tableId
        assertEquals(capturedRequest.toProto("foo", "bar").getTableId(), "users");
        // Verify no families
        assertEquals(
                capturedRequest.toProto("foo", "bar").getTable().getColumnFamiliesCount(),
                0,
                "Expected 0 families");
    }

    @Test
    public void testCreateTableToString() {
        // Given: a CreateTable action
        CreateTable createTable = new CreateTable();
        createTable.setTableId("test-table");

        ColumnFamily family = new ColumnFamily();
        family.setName("profile");
        createTable.setFamilies(Collections.singletonList(family));

        // When: calling toString
        String result = createTable.toString();

        // Then: it should contain relevant information
        assertTrue(result.contains("test-table"));
        assertTrue(result.contains("CreateTable"));
    }

    @Test
    public void testCreateTableGettersSetters() {
        // Given: a CreateTable action
        CreateTable createTable = new CreateTable();

        // When: setting values
        createTable.setTableId("my-table");

        ColumnFamily family = new ColumnFamily();
        family.setName("profile");
        List<ColumnFamily> families = Collections.singletonList(family);
        createTable.setFamilies(families);

        // Then: getters should return the values
        assertEquals(createTable.getTableId(), "my-table");
        assertEquals(createTable.getFamilies(), families);
        assertEquals(createTable.getFamilies().size(), 1);
        assertEquals(createTable.getFamilies().get(0).getName(), "profile");
    }

    @Test
    public void testCreateTableActionFactoryRegistration() {
        // Given: a YAML-like configuration with CreateTable
        Map<String, Object> config = new HashMap<>();
        config.put(
                "CreateTable",
                new HashMap<String, Object>() {
                    {
                        put("tableId", "test");
                    }
                });

        // When: getting the action via ActionFactory
        Action<?> action = ActionFactory.getAction(config, mockContext);

        // Then: should successfully create CreateTable instance
        assertNotNull(action);
        assertTrue(action instanceof CreateTable);
    }

    @Test
    public void testCreateTableMinimalConfig() {
        // Given: minimal valid configuration
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> createTableConfig = new HashMap<>();
        createTableConfig.put("tableId", "minimal-table");
        config.put("CreateTable", createTableConfig);

        // When: creating an action
        Action<?> action = ActionFactory.getAction(config, mockContext);

        // Then: should work with just tableId
        assertNotNull(action);
        assertTrue(action instanceof CreateTable);

        CreateTable createTable = (CreateTable) action;
        assertEquals(createTable.getTableId(), "minimal-table");
        assertNull(createTable.getFamilies());
    }

    @Test
    public void testCreateTableWithComplexGcRules() {
        // Given: configuration with complex GC rules (union/intersection)
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> createTableConfig = new HashMap<>();
        createTableConfig.put("tableId", "users");

        List<Map<String, Object>> familiesConfig = new ArrayList<>();

        // Family with union rule
        Map<String, Object> family1 = new HashMap<>();
        family1.put("name", "cache");
        List<Map<String, Object>> unionRules = new ArrayList<>();
        Map<String, Object> unionRule1 = new HashMap<>();
        unionRule1.put("maxVersions", 3);
        unionRules.add(unionRule1);
        Map<String, Object> unionRule2 = new HashMap<>();
        unionRule2.put("maxAge", "24h");
        unionRules.add(unionRule2);
        family1.put("union", unionRules);
        familiesConfig.add(family1);

        // Family with intersection rule
        Map<String, Object> family2 = new HashMap<>();
        family2.put("name", "important");
        List<Map<String, Object>> intersectionRules = new ArrayList<>();
        Map<String, Object> intersectionRule1 = new HashMap<>();
        intersectionRule1.put("maxVersions", 100);
        intersectionRules.add(intersectionRule1);
        Map<String, Object> intersectionRule2 = new HashMap<>();
        intersectionRule2.put("maxAge", "365d");
        intersectionRules.add(intersectionRule2);
        family2.put("intersection", intersectionRules);
        familiesConfig.add(family2);

        createTableConfig.put("families", familiesConfig);
        config.put("CreateTable", createTableConfig);

        // When: creating an action
        Action<?> action = ActionFactory.getAction(config, mockContext);

        // Then: should have families with complex GC rules
        assertNotNull(action);
        assertTrue(action instanceof CreateTable);

        CreateTable createTable = (CreateTable) action;
        assertEquals(createTable.getFamilies().size(), 2);

        // Verify union family
        ColumnFamily cacheFamily = createTable.getFamilies().get(0);
        assertEquals(cacheFamily.getName(), "cache");
        assertNotNull(cacheFamily.getUnion());
        assertEquals(cacheFamily.getUnion().size(), 2);

        // Verify intersection family
        ColumnFamily importantFamily = createTable.getFamilies().get(1);
        assertEquals(importantFamily.getName(), "important");
        assertNotNull(importantFamily.getIntersection());
        assertEquals(importantFamily.getIntersection().size(), 2);
    }
}
