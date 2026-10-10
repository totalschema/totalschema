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

package io.github.totalschema.connector.gcp.bigtable.actions.table.cleartablesplits;

import static org.easymock.EasyMock.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.*;

import com.google.cloud.bigtable.admin.v2.BigtableTableAdminClient;
import com.google.cloud.bigtable.admin.v2.models.ColumnFamily;
import com.google.cloud.bigtable.admin.v2.models.CreateTableRequest;
import com.google.cloud.bigtable.admin.v2.models.GCRules;
import com.google.cloud.bigtable.admin.v2.models.Table;
import io.github.totalschema.connector.gcp.bigtable.actions.Action;
import io.github.totalschema.connector.gcp.bigtable.actions.ActionFactory;
import io.github.totalschema.engine.api.Context;
import io.github.totalschema.spi.expression.evaluator.ExpressionEvaluator;
import io.github.totalschema.spi.variables.VariableService;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Tests for {@link ClearTableSplits} and {@link ClearTableSplitsActionFactory}. */
public class ClearTableSplitsTest {

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
    public void testClearTableSplitsActionFromMap() {
        // Given: a YAML-like configuration map
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> actionConfig = new HashMap<>();
        actionConfig.put("tableId", "users");
        actionConfig.put("confirmDeletion", true);
        config.put("ClearTableSplits", actionConfig);

        // When: creating an action from the config
        Action<?> action = ActionFactory.getAction(config, mockContext);

        // Then: it should be a ClearTableSplits instance with correct values
        assertNotNull(action);
        assertTrue(action instanceof ClearTableSplits);

        ClearTableSplits clearTableSplits = (ClearTableSplits) action;
        assertEquals(clearTableSplits.getTableId(), "users");
        assertEquals(clearTableSplits.getConfirmDeletion(), Boolean.TRUE);
    }

    @Test
    public void testClearTableSplitsGettersSetters() {
        // Given: a ClearTableSplits action
        ClearTableSplits action = new ClearTableSplits();

        // When: setting properties
        action.setTableId("test-table");
        action.setConfirmDeletion(true);

        // Then: getters should return the values
        assertEquals(action.getTableId(), "test-table");
        assertEquals(action.getConfirmDeletion(), Boolean.TRUE);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*tableId is required.*")
    public void testClearTableSplitsMissingTableId() {
        // Given: a ClearTableSplits action without tableId
        ClearTableSplits action = new ClearTableSplits();
        action.setConfirmDeletion(true);

        // When/Then: should throw IllegalArgumentException
        action.execute(null);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*tableId is required.*")
    public void testClearTableSplitsEmptyTableId() {
        // Given: a ClearTableSplits action with empty tableId
        ClearTableSplits action = new ClearTableSplits();
        action.setTableId("");
        action.setConfirmDeletion(true);

        // When/Then: should throw IllegalArgumentException
        action.execute(null);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*confirmDeletion must be set to true.*")
    public void testClearTableSplitsMissingConfirmDeletion() {
        // Given: a ClearTableSplits action without confirmDeletion
        ClearTableSplits action = new ClearTableSplits();
        action.setTableId("users");

        // When/Then: should throw IllegalArgumentException
        action.execute(null);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*confirmDeletion must be set to true.*")
    public void testClearTableSplitsConfirmDeletionFalse() {
        // Given: a ClearTableSplits action with confirmDeletion = false
        ClearTableSplits action = new ClearTableSplits();
        action.setTableId("users");
        action.setConfirmDeletion(false);

        // When/Then: should throw IllegalArgumentException
        action.execute(null);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*Table .* does not exist.*")
    public void testClearTableSplitsTableDoesNotExist() {
        // Given: a ClearTableSplits action for non-existent table
        ClearTableSplits action = new ClearTableSplits();
        action.setTableId("non-existent-table");
        action.setConfirmDeletion(true);

        BigtableTableAdminClient mockClient = mock(BigtableTableAdminClient.class);

        // Mock table doesn't exist
        when(mockClient.exists("non-existent-table")).thenReturn(false);

        // When/Then: should throw IllegalArgumentException
        action.execute(mockClient);
    }

    @Test
    public void testClearTableSplitsExecuteSuccess() {
        // Given: a valid ClearTableSplits action
        ClearTableSplits action = new ClearTableSplits();
        action.setTableId("users");
        action.setConfirmDeletion(true);

        BigtableTableAdminClient mockClient = mock(BigtableTableAdminClient.class);
        Table mockTable = mock(Table.class);
        ColumnFamily mockFamily = mock(ColumnFamily.class);

        java.util.List<ColumnFamily> families = java.util.Collections.singletonList(mockFamily);

        // Mock table exists and has schema
        when(mockClient.exists("users")).thenReturn(true);
        when(mockClient.getTable("users")).thenReturn(mockTable);
        when(mockTable.getColumnFamilies()).thenReturn(families);
        when(mockFamily.getId()).thenReturn("profile");
        when(mockFamily.hasGCRule()).thenReturn(false);

        // When: executing the action
        action.execute(mockClient);

        // Then: verify deleteTable and createTable were called
        verify(mockClient, times(1)).deleteTable("users");

        ArgumentCaptor<CreateTableRequest> requestCaptor =
                ArgumentCaptor.forClass(CreateTableRequest.class);
        verify(mockClient, times(1)).createTable(requestCaptor.capture());

        CreateTableRequest capturedRequest = requestCaptor.getValue();
        assertNotNull(capturedRequest);
        assertEquals(capturedRequest.toProto("foo", "bar").getTableId(), "users");
    }

    @Test
    public void testClearTableSplitsExecuteWithGcRules() {
        // Given: a ClearTableSplits action for a table with GC rules
        ClearTableSplits action = new ClearTableSplits();
        action.setTableId("events");
        action.setConfirmDeletion(true);

        BigtableTableAdminClient mockClient = mock(BigtableTableAdminClient.class);
        Table mockTable = mock(Table.class);
        ColumnFamily mockFamily = mock(ColumnFamily.class);
        GCRules.GCRule mockGcRule = GCRules.GCRULES.maxVersions(3);

        java.util.List<ColumnFamily> families = java.util.Collections.singletonList(mockFamily);

        // Mock table exists with GC rules
        when(mockClient.exists("events")).thenReturn(true);
        when(mockClient.getTable("events")).thenReturn(mockTable);
        when(mockTable.getColumnFamilies()).thenReturn(families);
        when(mockFamily.getId()).thenReturn("data");
        when(mockFamily.hasGCRule()).thenReturn(true);
        when(mockFamily.getGCRule()).thenReturn(mockGcRule);

        // When: executing the action
        action.execute(mockClient);

        // Then: verify deleteTable and createTable were called
        verify(mockClient, times(1)).deleteTable("events");

        ArgumentCaptor<CreateTableRequest> requestCaptor =
                ArgumentCaptor.forClass(CreateTableRequest.class);
        verify(mockClient, times(1)).createTable(requestCaptor.capture());

        CreateTableRequest capturedRequest = requestCaptor.getValue();
        assertNotNull(capturedRequest);
        assertEquals(capturedRequest.toProto("foo", "bar").getTableId(), "events");
    }

    @Test
    public void testClearTableSplitsExecuteWithMultipleFamilies() {
        // Given: a ClearTableSplits action for a table with multiple families
        ClearTableSplits action = new ClearTableSplits();
        action.setTableId("multi-family-table");
        action.setConfirmDeletion(true);

        BigtableTableAdminClient mockClient = mock(BigtableTableAdminClient.class);
        Table mockTable = mock(Table.class);
        ColumnFamily mockFamily1 = mock(ColumnFamily.class);
        ColumnFamily mockFamily2 = mock(ColumnFamily.class);
        ColumnFamily mockFamily3 = mock(ColumnFamily.class);

        java.util.List<ColumnFamily> families =
                java.util.Arrays.asList(mockFamily1, mockFamily2, mockFamily3);

        // Mock table exists with multiple families
        when(mockClient.exists("multi-family-table")).thenReturn(true);
        when(mockClient.getTable("multi-family-table")).thenReturn(mockTable);
        when(mockTable.getColumnFamilies()).thenReturn(families);
        when(mockFamily1.getId()).thenReturn("profile");
        when(mockFamily1.hasGCRule()).thenReturn(false);
        when(mockFamily2.getId()).thenReturn("settings");
        when(mockFamily2.hasGCRule()).thenReturn(false);
        when(mockFamily3.getId()).thenReturn("audit");
        when(mockFamily3.hasGCRule()).thenReturn(false);

        // When: executing the action
        action.execute(mockClient);

        // Then: verify deleteTable and createTable were called
        verify(mockClient, times(1)).deleteTable("multi-family-table");

        ArgumentCaptor<CreateTableRequest> requestCaptor =
                ArgumentCaptor.forClass(CreateTableRequest.class);
        verify(mockClient, times(1)).createTable(requestCaptor.capture());

        CreateTableRequest capturedRequest = requestCaptor.getValue();
        assertNotNull(capturedRequest);

        // Verify all families are recreated
        Map<String, com.google.bigtable.admin.v2.ColumnFamily> recreatedFamilies =
                capturedRequest.toProto("foo", "bar").getTable().getColumnFamiliesMap();
        assertEquals(recreatedFamilies.size(), 3, "Expected 3 families to be recreated");
        assertTrue(recreatedFamilies.containsKey("profile"));
        assertTrue(recreatedFamilies.containsKey("settings"));
        assertTrue(recreatedFamilies.containsKey("audit"));
    }

    @Test
    public void testClearTableSplitsToString() {
        // Given: a ClearTableSplits action
        ClearTableSplits action = new ClearTableSplits();
        action.setTableId("test-table");
        action.setConfirmDeletion(true);

        // When/Then: toString should work and contain relevant info
        String str = action.toString();
        assertTrue(str.contains("ClearTableSplits"));
        assertTrue(str.contains("test-table"));
        assertTrue(str.contains("true"));
    }

    @Test
    public void testClearTableSplitsActionFactoryRegistration() {
        // Given: a YAML-like configuration with ClearTableSplits
        Map<String, Object> config = new HashMap<>();
        config.put(
                "ClearTableSplits",
                new HashMap<String, Object>() {
                    {
                        put("tableId", "test-table");
                        put("confirmDeletion", true);
                    }
                });

        // When: getting the action via ActionFactory
        Action<?> action = ActionFactory.getAction(config, mockContext);

        // Then: should successfully create ClearTableSplits instance
        assertNotNull(action);
        assertTrue(action instanceof ClearTableSplits);
    }

    @Test
    public void testClearTableSplitsMinimalConfig() {
        // Given: minimal valid configuration
        ClearTableSplits action = new ClearTableSplits();
        action.setTableId("minimal-table");
        action.setConfirmDeletion(true);

        // When/Then: should have all values set correctly
        assertEquals(action.getTableId(), "minimal-table");
        assertEquals(action.getConfirmDeletion(), Boolean.TRUE);
    }

    @Test
    public void testClearTableSplitsPreservesGcRulesCorrectly() {
        // Given: a table with complex GC rules
        ClearTableSplits action = new ClearTableSplits();
        action.setTableId("complex-table");
        action.setConfirmDeletion(true);

        BigtableTableAdminClient mockClient = mock(BigtableTableAdminClient.class);
        Table mockTable = mock(Table.class);
        ColumnFamily mockFamily1 = mock(ColumnFamily.class);
        ColumnFamily mockFamily2 = mock(ColumnFamily.class);

        GCRules.GCRule gcRule1 = GCRules.GCRULES.maxVersions(5);
        GCRules.GCRule gcRule2 =
                GCRules.GCRULES
                        .intersection()
                        .rule(GCRules.GCRULES.maxVersions(10))
                        .rule(GCRules.GCRULES.maxAge(org.threeten.bp.Duration.ofDays(90)));

        java.util.List<ColumnFamily> families = java.util.Arrays.asList(mockFamily1, mockFamily2);

        // Mock table with GC rules
        when(mockClient.exists("complex-table")).thenReturn(true);
        when(mockClient.getTable("complex-table")).thenReturn(mockTable);
        when(mockTable.getColumnFamilies()).thenReturn(families);
        when(mockFamily1.getId()).thenReturn("cache");
        when(mockFamily1.hasGCRule()).thenReturn(true);
        when(mockFamily1.getGCRule()).thenReturn(gcRule1);
        when(mockFamily2.getId()).thenReturn("important");
        when(mockFamily2.hasGCRule()).thenReturn(true);
        when(mockFamily2.getGCRule()).thenReturn(gcRule2);

        // When: executing the action
        action.execute(mockClient);

        // Then: verify table was deleted and recreated
        verify(mockClient, times(1)).deleteTable("complex-table");
        verify(mockClient, times(1)).createTable(org.mockito.ArgumentMatchers.any());
    }
}
