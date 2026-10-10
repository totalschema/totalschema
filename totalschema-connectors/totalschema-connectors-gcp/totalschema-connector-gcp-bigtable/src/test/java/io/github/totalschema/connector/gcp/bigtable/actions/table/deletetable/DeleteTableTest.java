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

package io.github.totalschema.connector.gcp.bigtable.actions.table.deletetable;

import static org.easymock.EasyMock.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.*;

import com.google.cloud.bigtable.admin.v2.BigtableTableAdminClient;
import io.github.totalschema.connector.gcp.bigtable.actions.Action;
import io.github.totalschema.connector.gcp.bigtable.actions.ActionFactory;
import io.github.totalschema.engine.api.Context;
import io.github.totalschema.spi.expression.evaluator.ExpressionEvaluator;
import io.github.totalschema.spi.variables.VariableService;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Tests for {@link DeleteTable} and {@link DeleteTableActionFactory}. */
public class DeleteTableTest {

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
    public void testDeleteTableActionFromMap() {
        // Given: a YAML-like configuration map
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> deleteTableConfig = new HashMap<>();
        deleteTableConfig.put("tableId", "old-table");
        config.put("DeleteTable", deleteTableConfig);

        // When: creating an action from the config
        Action<?> action = ActionFactory.getAction(config, mockContext);

        // Then: it should be a DeleteTable instance with correct values
        assertNotNull(action);
        assertTrue(action instanceof DeleteTable);

        DeleteTable deleteTable = (DeleteTable) action;
        assertEquals(deleteTable.getTableId(), "old-table");
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*tableId is required.*")
    public void testDeleteTableRequiresTableId() {
        // Given: a DeleteTable action without tableId
        DeleteTable deleteTable = new DeleteTable();
        deleteTable.setTableId(null);

        // When/Then: execute should throw exception
        deleteTable.execute(null);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*tableId is required.*")
    public void testDeleteTableRequiresNonEmptyTableId() {
        // Given: a DeleteTable action with empty tableId
        DeleteTable deleteTable = new DeleteTable();
        deleteTable.setTableId("");

        // When/Then: execute should throw exception
        deleteTable.execute(null);
    }

    @Test
    public void testDeleteTableToString() {
        // Given: a DeleteTable action
        DeleteTable deleteTable = new DeleteTable();
        deleteTable.setTableId("test-table");

        // When: calling toString
        String result = deleteTable.toString();

        // Then: it should contain the tableId
        assertTrue(result.contains("test-table"));
        assertTrue(result.contains("DeleteTable"));
    }

    @Test
    public void testDeleteTableGettersSetters() {
        // Given: a DeleteTable action
        DeleteTable deleteTable = new DeleteTable();

        // When: setting tableId
        deleteTable.setTableId("my-table");

        // Then: getter should return the value
        assertEquals(deleteTable.getTableId(), "my-table");
    }

    @Test
    public void testDeleteTableActionFactoryRegistration() {
        // Given: a YAML-like configuration with DeleteTable
        Map<String, Object> config = new HashMap<>();
        config.put(
                "DeleteTable",
                new HashMap<String, Object>() {
                    {
                        put("tableId", "test");
                    }
                });

        // When: getting the action via ActionFactory
        Action<?> action = ActionFactory.getAction(config, mockContext);

        // Then: should successfully create DeleteTable instance
        assertNotNull(action);
        assertTrue(action instanceof DeleteTable);
    }

    @Test
    public void testDeleteTableMinimalConfig() {
        // Given: minimal valid configuration
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> deleteTableConfig = new HashMap<>();
        deleteTableConfig.put("tableId", "minimal-table");
        config.put("DeleteTable", deleteTableConfig);

        // When: creating an action
        Action<?> action = ActionFactory.getAction(config, mockContext);

        // Then: should work with just tableId
        assertNotNull(action);
        assertTrue(action instanceof DeleteTable);

        DeleteTable deleteTable = (DeleteTable) action;
        assertEquals(deleteTable.getTableId(), "minimal-table");
    }

    @Test
    public void testDeleteTableExecute() {
        // Given: a valid DeleteTable action
        DeleteTable deleteTable = new DeleteTable();
        deleteTable.setTableId("users");

        BigtableTableAdminClient mockClient = mock(BigtableTableAdminClient.class);

        // When: executing the action
        deleteTable.execute(mockClient);

        // Then: verify deleteTable was called with exact tableId "users"
        verify(mockClient).deleteTable(eq("users"));
    }
}
