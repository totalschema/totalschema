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

package io.github.totalschema.connector.gcp.bigtable.actions.data.deletecellversions;

import static org.easymock.EasyMock.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.*;

import com.google.cloud.bigtable.data.v2.BigtableDataClient;
import com.google.cloud.bigtable.data.v2.models.RowMutation;
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

/** Tests for {@link DeleteCellVersions} and {@link DeleteCellVersionsActionFactory}. */
public class DeleteCellVersionsTest {

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
    public void testDeleteCellVersionsActionFromMapWithBothTimestamps() {
        // Given: a YAML-like configuration map with both timestamps
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> actionConfig = new HashMap<>();
        actionConfig.put("tableId", "audit_log");
        actionConfig.put("rowKey", "log#12345");
        actionConfig.put("familyName", "events");
        actionConfig.put("columnQualifier", "details");
        actionConfig.put("startTimestampMicros", 1692201600000000L);
        actionConfig.put("endTimestampMicros", 1692288000000000L);
        config.put("DeleteCellVersions", actionConfig);

        // When: creating an action from the config
        Action<?> action = ActionFactory.getAction(config, mockContext);

        // Then: it should be a DeleteCellVersions instance with correct values
        assertNotNull(action);
        assertTrue(action instanceof DeleteCellVersions);

        DeleteCellVersions deleteCellVersions = (DeleteCellVersions) action;
        assertEquals(deleteCellVersions.getTableId(), "audit_log");
        assertEquals(deleteCellVersions.getRowKey(), "log#12345");
        assertEquals(deleteCellVersions.getFamilyName(), "events");
        assertEquals(deleteCellVersions.getColumnQualifier(), "details");
        assertEquals(deleteCellVersions.getStartTimestampMicros(), Long.valueOf(1692201600000000L));
        assertEquals(deleteCellVersions.getEndTimestampMicros(), Long.valueOf(1692288000000000L));
    }

    @Test
    public void testDeleteCellVersionsActionFromMapWithStartTimestampOnly() {
        // Given: a YAML-like configuration map with only start timestamp
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> actionConfig = new HashMap<>();
        actionConfig.put("tableId", "user_sessions");
        actionConfig.put("rowKey", "session#user-123");
        actionConfig.put("familyName", "data");
        actionConfig.put("columnQualifier", "session_data");
        actionConfig.put("startTimestampMicros", 1692288000000000L);
        config.put("DeleteCellVersions", actionConfig);

        // When: creating an action from the config
        Action<?> action = ActionFactory.getAction(config, mockContext);

        // Then: it should have start timestamp but no end timestamp
        assertNotNull(action);
        assertTrue(action instanceof DeleteCellVersions);

        DeleteCellVersions deleteCellVersions = (DeleteCellVersions) action;
        assertEquals(deleteCellVersions.getStartTimestampMicros(), Long.valueOf(1692288000000000L));
        assertNull(deleteCellVersions.getEndTimestampMicros());
    }

    @Test
    public void testDeleteCellVersionsActionFromMapWithEndTimestampOnly() {
        // Given: a YAML-like configuration map with only end timestamp
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> actionConfig = new HashMap<>();
        actionConfig.put("tableId", "logs");
        actionConfig.put("rowKey", "log#456");
        actionConfig.put("familyName", "events");
        actionConfig.put("columnQualifier", "data");
        actionConfig.put("endTimestampMicros", 1692288000000000L);
        config.put("DeleteCellVersions", actionConfig);

        // When: creating an action from the config
        Action<?> action = ActionFactory.getAction(config, mockContext);

        // Then: it should have end timestamp but no start timestamp
        assertNotNull(action);
        assertTrue(action instanceof DeleteCellVersions);

        DeleteCellVersions deleteCellVersions = (DeleteCellVersions) action;
        assertNull(deleteCellVersions.getStartTimestampMicros());
        assertEquals(deleteCellVersions.getEndTimestampMicros(), Long.valueOf(1692288000000000L));
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*tableId is required.*")
    public void testDeleteCellVersionsRequiresTableId() {
        // Given: a DeleteCellVersions action without tableId
        DeleteCellVersions action = new DeleteCellVersions();
        action.setTableId(null);
        action.setRowKey("key");
        action.setFamilyName("family");
        action.setColumnQualifier("qualifier");
        action.setStartTimestampMicros(1000000L);

        // When/Then: execute should throw exception
        action.execute(null);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*tableId is required.*")
    public void testDeleteCellVersionsRequiresNonEmptyTableId() {
        // Given: a DeleteCellVersions action with empty tableId
        DeleteCellVersions action = new DeleteCellVersions();
        action.setTableId("");
        action.setRowKey("key");
        action.setFamilyName("family");
        action.setColumnQualifier("qualifier");
        action.setStartTimestampMicros(1000000L);

        // When/Then: execute should throw exception
        action.execute(null);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*rowKey is required.*")
    public void testDeleteCellVersionsRequiresRowKey() {
        // Given: a DeleteCellVersions action without rowKey
        DeleteCellVersions action = new DeleteCellVersions();
        action.setTableId("table");
        action.setRowKey(null);
        action.setFamilyName("family");
        action.setColumnQualifier("qualifier");
        action.setStartTimestampMicros(1000000L);

        // When/Then: execute should throw exception
        action.execute(null);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*rowKey is required.*")
    public void testDeleteCellVersionsRequiresNonEmptyRowKey() {
        // Given: a DeleteCellVersions action with empty rowKey
        DeleteCellVersions action = new DeleteCellVersions();
        action.setTableId("table");
        action.setRowKey("");
        action.setFamilyName("family");
        action.setColumnQualifier("qualifier");
        action.setStartTimestampMicros(1000000L);

        // When/Then: execute should throw exception
        action.execute(null);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*familyName is required.*")
    public void testDeleteCellVersionsRequiresFamilyName() {
        // Given: a DeleteCellVersions action without familyName
        DeleteCellVersions action = new DeleteCellVersions();
        action.setTableId("table");
        action.setRowKey("key");
        action.setFamilyName(null);
        action.setColumnQualifier("qualifier");
        action.setStartTimestampMicros(1000000L);

        // When/Then: execute should throw exception
        action.execute(null);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*familyName is required.*")
    public void testDeleteCellVersionsRequiresNonEmptyFamilyName() {
        // Given: a DeleteCellVersions action with empty familyName
        DeleteCellVersions action = new DeleteCellVersions();
        action.setTableId("table");
        action.setRowKey("key");
        action.setFamilyName("");
        action.setColumnQualifier("qualifier");
        action.setStartTimestampMicros(1000000L);

        // When/Then: execute should throw exception
        action.execute(null);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*columnQualifier is required.*")
    public void testDeleteCellVersionsRequiresColumnQualifier() {
        // Given: a DeleteCellVersions action without columnQualifier
        DeleteCellVersions action = new DeleteCellVersions();
        action.setTableId("table");
        action.setRowKey("key");
        action.setFamilyName("family");
        action.setColumnQualifier(null);
        action.setStartTimestampMicros(1000000L);

        // When/Then: execute should throw exception
        action.execute(null);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*columnQualifier is required.*")
    public void testDeleteCellVersionsRequiresNonEmptyColumnQualifier() {
        // Given: a DeleteCellVersions action with empty columnQualifier
        DeleteCellVersions action = new DeleteCellVersions();
        action.setTableId("table");
        action.setRowKey("key");
        action.setFamilyName("family");
        action.setColumnQualifier("");
        action.setStartTimestampMicros(1000000L);

        // When/Then: execute should throw exception
        action.execute(null);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp =
                    ".*At least one of 'startTimestampMicros' or 'endTimestampMicros' must be"
                            + " specified.*")
    public void testDeleteCellVersionsRequiresAtLeastOneTimestamp() {
        // Given: a DeleteCellVersions action without any timestamps
        DeleteCellVersions action = new DeleteCellVersions();
        action.setTableId("table");
        action.setRowKey("key");
        action.setFamilyName("family");
        action.setColumnQualifier("qualifier");

        // When/Then: execute should throw exception
        action.execute(null);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp =
                    ".*startTimestampMicros must be less than endTimestampMicros.*")
    public void testDeleteCellVersionsValidatesTimestampOrder() {
        // Given: a DeleteCellVersions action with start >= end
        DeleteCellVersions action = new DeleteCellVersions();
        action.setTableId("table");
        action.setRowKey("key");
        action.setFamilyName("family");
        action.setColumnQualifier("qualifier");
        action.setStartTimestampMicros(2000000L);
        action.setEndTimestampMicros(1000000L);

        // When/Then: execute should throw exception
        action.execute(null);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp =
                    ".*startTimestampMicros must be less than endTimestampMicros.*")
    public void testDeleteCellVersionsValidatesTimestampOrderEqual() {
        // Given: a DeleteCellVersions action with start == end
        DeleteCellVersions action = new DeleteCellVersions();
        action.setTableId("table");
        action.setRowKey("key");
        action.setFamilyName("family");
        action.setColumnQualifier("qualifier");
        action.setStartTimestampMicros(1000000L);
        action.setEndTimestampMicros(1000000L);

        // When/Then: execute should throw exception
        action.execute(null);
    }

    @Test
    public void testDeleteCellVersionsExecuteWithBothTimestamps() {
        // Given: a valid action with both timestamps
        DeleteCellVersions action = new DeleteCellVersions();
        action.setTableId("audit_log");
        action.setRowKey("log#12345");
        action.setFamilyName("events");
        action.setColumnQualifier("details");
        action.setStartTimestampMicros(1692201600000000L);
        action.setEndTimestampMicros(1692288000000000L);

        BigtableDataClient mockClient = mock(BigtableDataClient.class);

        // When: executing the action
        action.execute(mockClient);

        // Then: verify mutateRow was called
        ArgumentCaptor<RowMutation> mutationCaptor = ArgumentCaptor.forClass(RowMutation.class);
        verify(mockClient).mutateRow(mutationCaptor.capture());

        RowMutation capturedMutation = mutationCaptor.getValue();
        assertNotNull(capturedMutation);
    }

    @Test
    public void testDeleteCellVersionsExecuteWithStartTimestampOnly() {
        // Given: a valid action with only start timestamp
        DeleteCellVersions action = new DeleteCellVersions();
        action.setTableId("user_sessions");
        action.setRowKey("session#user-123");
        action.setFamilyName("data");
        action.setColumnQualifier("session_data");
        action.setStartTimestampMicros(1692288000000000L);

        BigtableDataClient mockClient = mock(BigtableDataClient.class);

        // When: executing the action
        action.execute(mockClient);

        // Then: verify mutateRow was called
        ArgumentCaptor<RowMutation> mutationCaptor = ArgumentCaptor.forClass(RowMutation.class);
        verify(mockClient).mutateRow(mutationCaptor.capture());

        RowMutation capturedMutation = mutationCaptor.getValue();
        assertNotNull(capturedMutation);
    }

    @Test
    public void testDeleteCellVersionsExecuteWithEndTimestampOnly() {
        // Given: a valid action with only end timestamp
        DeleteCellVersions action = new DeleteCellVersions();
        action.setTableId("logs");
        action.setRowKey("log#456");
        action.setFamilyName("events");
        action.setColumnQualifier("data");
        action.setEndTimestampMicros(1692288000000000L);

        BigtableDataClient mockClient = mock(BigtableDataClient.class);

        // When: executing the action
        action.execute(mockClient);

        // Then: verify mutateRow was called
        ArgumentCaptor<RowMutation> mutationCaptor = ArgumentCaptor.forClass(RowMutation.class);
        verify(mockClient).mutateRow(mutationCaptor.capture());

        RowMutation capturedMutation = mutationCaptor.getValue();
        assertNotNull(capturedMutation);
    }

    @Test
    public void testDeleteCellVersionsToString() {
        // Given: a DeleteCellVersions action
        DeleteCellVersions action = new DeleteCellVersions();
        action.setTableId("test-table");
        action.setRowKey("test-key");
        action.setFamilyName("test-family");
        action.setColumnQualifier("test-qualifier");
        action.setStartTimestampMicros(1000000L);
        action.setEndTimestampMicros(2000000L);

        // When: calling toString
        String result = action.toString();

        // Then: it should contain relevant information
        assertTrue(result.contains("test-table"));
        assertTrue(result.contains("test-key"));
        assertTrue(result.contains("test-family"));
        assertTrue(result.contains("test-qualifier"));
        assertTrue(result.contains("1000000"));
        assertTrue(result.contains("2000000"));
        assertTrue(result.contains("DeleteCellVersions"));
    }

    @Test
    public void testDeleteCellVersionsGettersSetters() {
        // Given: a DeleteCellVersions action
        DeleteCellVersions action = new DeleteCellVersions();

        // When: setting values
        action.setTableId("my-table");
        action.setRowKey("my-key");
        action.setFamilyName("my-family");
        action.setColumnQualifier("my-qualifier");
        action.setStartTimestampMicros(1000000L);
        action.setEndTimestampMicros(2000000L);

        // Then: getters should return the values
        assertEquals(action.getTableId(), "my-table");
        assertEquals(action.getRowKey(), "my-key");
        assertEquals(action.getFamilyName(), "my-family");
        assertEquals(action.getColumnQualifier(), "my-qualifier");
        assertEquals(action.getStartTimestampMicros(), Long.valueOf(1000000L));
        assertEquals(action.getEndTimestampMicros(), Long.valueOf(2000000L));
    }

    @Test
    public void testDeleteCellVersionsActionFactoryRegistration() {
        // Given: a YAML-like configuration with DeleteCellVersions
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> actionConfig = new HashMap<>();
        actionConfig.put("tableId", "test");
        actionConfig.put("rowKey", "key");
        actionConfig.put("familyName", "family");
        actionConfig.put("columnQualifier", "qualifier");
        actionConfig.put("startTimestampMicros", 1000000L);
        config.put("DeleteCellVersions", actionConfig);

        // When: getting the action via ActionFactory
        Action<?> action = ActionFactory.getAction(config, mockContext);

        // Then: should successfully create DeleteCellVersions instance
        assertNotNull(action);
        assertTrue(action instanceof DeleteCellVersions);
    }

    @Test
    public void testDeleteCellVersionsMinimalConfigWithStartTimestamp() {
        // Given: minimal valid configuration with start timestamp
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> actionConfig = new HashMap<>();
        actionConfig.put("tableId", "minimal-table");
        actionConfig.put("rowKey", "minimal-key");
        actionConfig.put("familyName", "minimal-family");
        actionConfig.put("columnQualifier", "minimal-qualifier");
        actionConfig.put("startTimestampMicros", 1692288000000000L);
        config.put("DeleteCellVersions", actionConfig);

        // When: creating an action
        Action<?> action = ActionFactory.getAction(config, mockContext);

        // Then: should work with minimal config
        assertNotNull(action);
        assertTrue(action instanceof DeleteCellVersions);

        DeleteCellVersions deleteCellVersions = (DeleteCellVersions) action;
        assertEquals(deleteCellVersions.getTableId(), "minimal-table");
        assertEquals(deleteCellVersions.getRowKey(), "minimal-key");
        assertEquals(deleteCellVersions.getFamilyName(), "minimal-family");
        assertEquals(deleteCellVersions.getColumnQualifier(), "minimal-qualifier");
        assertEquals(deleteCellVersions.getStartTimestampMicros(), Long.valueOf(1692288000000000L));
        assertNull(deleteCellVersions.getEndTimestampMicros());
    }

    @Test
    public void testDeleteCellVersionsMinimalConfigWithEndTimestamp() {
        // Given: minimal valid configuration with end timestamp
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> actionConfig = new HashMap<>();
        actionConfig.put("tableId", "minimal-table");
        actionConfig.put("rowKey", "minimal-key");
        actionConfig.put("familyName", "minimal-family");
        actionConfig.put("columnQualifier", "minimal-qualifier");
        actionConfig.put("endTimestampMicros", 1692288000000000L);
        config.put("DeleteCellVersions", actionConfig);

        // When: creating an action
        Action<?> action = ActionFactory.getAction(config, mockContext);

        // Then: should work with minimal config
        assertNotNull(action);
        assertTrue(action instanceof DeleteCellVersions);

        DeleteCellVersions deleteCellVersions = (DeleteCellVersions) action;
        assertEquals(deleteCellVersions.getTableId(), "minimal-table");
        assertEquals(deleteCellVersions.getRowKey(), "minimal-key");
        assertEquals(deleteCellVersions.getFamilyName(), "minimal-family");
        assertEquals(deleteCellVersions.getColumnQualifier(), "minimal-qualifier");
        assertNull(deleteCellVersions.getStartTimestampMicros());
        assertEquals(deleteCellVersions.getEndTimestampMicros(), Long.valueOf(1692288000000000L));
    }
}
