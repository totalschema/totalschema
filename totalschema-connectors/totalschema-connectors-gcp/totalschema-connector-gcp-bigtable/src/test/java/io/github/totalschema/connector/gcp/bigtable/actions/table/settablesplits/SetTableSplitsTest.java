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

package io.github.totalschema.connector.gcp.bigtable.actions.table.settablesplits;

import static org.easymock.EasyMock.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.*;

import com.google.cloud.bigtable.admin.v2.BigtableTableAdminClient;
import com.google.cloud.bigtable.admin.v2.models.CreateTableRequest;
import io.github.totalschema.connector.gcp.bigtable.actions.Action;
import io.github.totalschema.connector.gcp.bigtable.actions.ActionFactory;
import io.github.totalschema.engine.api.Context;
import io.github.totalschema.spi.expression.evaluator.ExpressionEvaluator;
import io.github.totalschema.spi.variables.VariableService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Tests for {@link SetTableSplits} and {@link SetTableSplitsActionFactory}. */
public class SetTableSplitsTest {

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
    public void testSetTableSplitsActionFromMap() {
        // Given: a YAML-like configuration map
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> actionConfig = new HashMap<>();
        actionConfig.put("tableId", "users");
        actionConfig.put("splitKeys", Arrays.asList("user#1000", "user#2000", "user#3000"));
        config.put("SetTableSplits", actionConfig);

        // When: creating an action from the config
        Action<?> action = ActionFactory.getAction(config, mockContext);

        // Then: it should be a SetTableSplits instance with correct values
        assertNotNull(action);
        assertTrue(action instanceof SetTableSplits);

        SetTableSplits setTableSplits = (SetTableSplits) action;
        assertEquals(setTableSplits.getTableId(), "users");
        assertNotNull(setTableSplits.getSplitKeys());
        assertEquals(setTableSplits.getSplitKeys().size(), 3);
        assertEquals(setTableSplits.getSplitKeys().get(0), "user#1000");
        assertEquals(setTableSplits.getSplitKeys().get(1), "user#2000");
        assertEquals(setTableSplits.getSplitKeys().get(2), "user#3000");
    }

    @Test
    public void testSetTableSplitsGettersSetters() {
        // Given: a SetTableSplits action
        SetTableSplits action = new SetTableSplits();

        // When: setting properties
        action.setTableId("test-table");
        List<String> splitKeys = Arrays.asList("key1", "key2", "key3");
        action.setSplitKeys(splitKeys);

        // Then: getters should return the values
        assertEquals(action.getTableId(), "test-table");
        assertEquals(action.getSplitKeys(), splitKeys);
        assertEquals(action.getSplitKeys().size(), 3);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*tableId is required.*")
    public void testSetTableSplitsMissingTableId() {
        // Given: a SetTableSplits action without tableId
        SetTableSplits action = new SetTableSplits();
        action.setSplitKeys(Arrays.asList("key1", "key2"));

        // When/Then: should throw IllegalArgumentException
        action.execute(null);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*tableId is required.*")
    public void testSetTableSplitsEmptyTableId() {
        // Given: a SetTableSplits action with empty tableId
        SetTableSplits action = new SetTableSplits();
        action.setTableId("");
        action.setSplitKeys(Arrays.asList("key1", "key2"));

        // When/Then: should throw IllegalArgumentException
        action.execute(null);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*splitKeys is required.*")
    public void testSetTableSplitsMissingSplitKeys() {
        // Given: a SetTableSplits action without splitKeys
        SetTableSplits action = new SetTableSplits();
        action.setTableId("users");
        action.setSplitKeys(null);

        // When/Then: should throw IllegalArgumentException
        action.execute(null);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*splitKeys is required.*")
    public void testSetTableSplitsEmptySplitKeys() {
        // Given: a SetTableSplits action with empty splitKeys
        SetTableSplits action = new SetTableSplits();
        action.setTableId("users");
        action.setSplitKeys(new ArrayList<>());

        // When/Then: should throw IllegalArgumentException
        action.execute(null);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*must be in ascending lexicographical order.*")
    public void testSetTableSplitsUnorderedKeys() {
        // Given: a SetTableSplits action with unordered split keys
        SetTableSplits action = new SetTableSplits();
        action.setTableId("users");
        action.setSplitKeys(Arrays.asList("user#3000", "user#1000", "user#2000"));

        BigtableTableAdminClient mockClient = mock(BigtableTableAdminClient.class);
        when(mockClient.exists("users")).thenReturn(false);

        // When/Then: should throw IllegalArgumentException
        action.execute(mockClient);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*must be in ascending lexicographical order.*")
    public void testSetTableSplitsDuplicateKeys() {
        // Given: a SetTableSplits action with duplicate split keys
        SetTableSplits action = new SetTableSplits();
        action.setTableId("users");
        action.setSplitKeys(Arrays.asList("user#1000", "user#2000", "user#2000"));

        BigtableTableAdminClient mockClient = mock(BigtableTableAdminClient.class);
        when(mockClient.exists("users")).thenReturn(false);

        // When/Then: should throw IllegalArgumentException
        action.execute(mockClient);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*Table .* already exists.*")
    public void testSetTableSplitsTableAlreadyExists() {
        // Given: a SetTableSplits action for an existing table
        SetTableSplits action = new SetTableSplits();
        action.setTableId("users");
        action.setSplitKeys(Arrays.asList("user#1000", "user#2000", "user#3000"));

        BigtableTableAdminClient mockClient = mock(BigtableTableAdminClient.class);
        when(mockClient.exists("users")).thenReturn(true);

        // When/Then: should throw IllegalArgumentException
        action.execute(mockClient);
    }

    @Test
    public void testSetTableSplitsExecuteSuccess() {
        // Given: a valid SetTableSplits action
        SetTableSplits action = new SetTableSplits();
        action.setTableId("users");
        action.setSplitKeys(Arrays.asList("user#1000", "user#2000", "user#3000"));

        BigtableTableAdminClient mockClient = mock(BigtableTableAdminClient.class);

        // Mock table doesn't exist
        when(mockClient.exists("users")).thenReturn(false);

        // When: executing the action
        action.execute(mockClient);

        // Then: verify createTable was called with split keys
        ArgumentCaptor<CreateTableRequest> requestCaptor =
                ArgumentCaptor.forClass(CreateTableRequest.class);
        verify(mockClient).createTable(requestCaptor.capture());

        CreateTableRequest capturedRequest = requestCaptor.getValue();
        assertNotNull(capturedRequest);
        assertEquals(capturedRequest.toProto("foo", "bar").getTableId(), "users");

        // Verify split keys are present in the request
        assertEquals(
                capturedRequest.toProto("foo", "bar").getInitialSplitsCount(),
                3,
                "Expected 3 split keys");
    }

    @Test
    public void testSetTableSplitsExecuteWithSingleSplit() {
        // Given: a valid SetTableSplits action with a single split
        SetTableSplits action = new SetTableSplits();
        action.setTableId("events");
        action.setSplitKeys(Collections.singletonList("2026-06#"));

        BigtableTableAdminClient mockClient = mock(BigtableTableAdminClient.class);

        // Mock table doesn't exist
        when(mockClient.exists("events")).thenReturn(false);

        // When: executing the action
        action.execute(mockClient);

        // Then: verify createTable was called with one split key
        ArgumentCaptor<CreateTableRequest> requestCaptor =
                ArgumentCaptor.forClass(CreateTableRequest.class);
        verify(mockClient).createTable(requestCaptor.capture());

        CreateTableRequest capturedRequest = requestCaptor.getValue();
        assertNotNull(capturedRequest);
        assertEquals(
                capturedRequest.toProto("foo", "bar").getInitialSplitsCount(),
                1,
                "Expected 1 split key");
    }

    @Test
    public void testSetTableSplitsExecuteWithManySplits() {
        // Given: a SetTableSplits action with many splits
        SetTableSplits action = new SetTableSplits();
        action.setTableId("large-table");

        List<String> manySplits = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            manySplits.add(String.format("key#%04d", i * 100));
        }
        action.setSplitKeys(manySplits);

        BigtableTableAdminClient mockClient = mock(BigtableTableAdminClient.class);

        // Mock table doesn't exist
        when(mockClient.exists("large-table")).thenReturn(false);

        // When: executing the action
        action.execute(mockClient);

        // Then: verify createTable was called with all splits
        ArgumentCaptor<CreateTableRequest> requestCaptor =
                ArgumentCaptor.forClass(CreateTableRequest.class);
        verify(mockClient).createTable(requestCaptor.capture());

        CreateTableRequest capturedRequest = requestCaptor.getValue();
        assertNotNull(capturedRequest);
        assertEquals(
                capturedRequest.toProto("foo", "bar").getInitialSplitsCount(),
                20,
                "Expected 20 split keys");
    }

    @Test
    public void testSetTableSplitsToString() {
        // Given: a SetTableSplits action
        SetTableSplits action = new SetTableSplits();
        action.setTableId("test-table");
        action.setSplitKeys(Arrays.asList("key1", "key2", "key3"));

        // When/Then: toString should work and contain relevant info
        String str = action.toString();
        assertTrue(str.contains("SetTableSplits"));
        assertTrue(str.contains("test-table"));
        assertTrue(str.contains("key1"));
    }

    @Test
    public void testSetTableSplitsActionFactoryRegistration() {
        // Given: a YAML-like configuration with SetTableSplits
        Map<String, Object> config = new HashMap<>();
        config.put(
                "SetTableSplits",
                new HashMap<String, Object>() {
                    {
                        put("tableId", "test-table");
                        put("splitKeys", Arrays.asList("split1", "split2", "split3"));
                    }
                });

        // When: getting the action via ActionFactory
        Action<?> action = ActionFactory.getAction(config, mockContext);

        // Then: should successfully create SetTableSplits instance
        assertNotNull(action);
        assertTrue(action instanceof SetTableSplits);
    }

    @Test
    public void testSetTableSplitsWithTimestampBasedKeys() {
        // Given: split keys based on timestamp prefixes (common pattern)
        SetTableSplits action = new SetTableSplits();
        action.setTableId("events");
        action.setSplitKeys(
                Arrays.asList(
                        "2026-01#", "2026-02#", "2026-03#", "2026-04#", "2026-05#", "2026-06#"));

        BigtableTableAdminClient mockClient = mock(BigtableTableAdminClient.class);
        when(mockClient.exists("events")).thenReturn(false);

        // When: executing the action
        action.execute(mockClient);

        // Then: verify createTable was called
        ArgumentCaptor<CreateTableRequest> requestCaptor =
                ArgumentCaptor.forClass(CreateTableRequest.class);
        verify(mockClient).createTable(requestCaptor.capture());

        CreateTableRequest capturedRequest = requestCaptor.getValue();
        assertEquals(capturedRequest.toProto("foo", "bar").getInitialSplitsCount(), 6);
    }

    @Test
    public void testSetTableSplitsWithTenantIdBasedKeys() {
        // Given: split keys based on tenant IDs (multi-tenant pattern)
        SetTableSplits action = new SetTableSplits();
        action.setTableId("tenant_data");
        action.setSplitKeys(
                Arrays.asList(
                        "tenant_100#", "tenant_200#", "tenant_300#", "tenant_400#", "tenant_500#"));

        BigtableTableAdminClient mockClient = mock(BigtableTableAdminClient.class);
        when(mockClient.exists("tenant_data")).thenReturn(false);

        // When: executing the action
        action.execute(mockClient);

        // Then: verify createTable was called
        ArgumentCaptor<CreateTableRequest> requestCaptor =
                ArgumentCaptor.forClass(CreateTableRequest.class);
        verify(mockClient).createTable(requestCaptor.capture());

        CreateTableRequest capturedRequest = requestCaptor.getValue();
        assertEquals(capturedRequest.toProto("foo", "bar").getInitialSplitsCount(), 5);
    }

    @Test
    public void testSetTableSplitsMinimalConfig() {
        // Given: minimal valid configuration (1 split key)
        SetTableSplits action = new SetTableSplits();
        action.setTableId("minimal-table");
        action.setSplitKeys(Collections.singletonList("split1"));

        // When/Then: should have all values set correctly
        assertEquals(action.getTableId(), "minimal-table");
        assertEquals(action.getSplitKeys().size(), 1);
        assertEquals(action.getSplitKeys().get(0), "split1");
    }
}
