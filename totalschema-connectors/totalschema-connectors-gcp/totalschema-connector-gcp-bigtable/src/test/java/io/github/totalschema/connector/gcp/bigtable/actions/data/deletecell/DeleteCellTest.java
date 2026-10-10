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

package io.github.totalschema.connector.gcp.bigtable.actions.data.deletecell;

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

/** Tests for {@link DeleteCell} and {@link DeleteCellActionFactory}. */
public class DeleteCellTest {

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
    public void testDeleteCellActionFromMap() {
        // Given: a YAML-like configuration map
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> deleteCellConfig = new HashMap<>();
        deleteCellConfig.put("tableId", "system_config");
        deleteCellConfig.put("rowKey", "config#deprecated");
        deleteCellConfig.put("familyName", "metadata");
        deleteCellConfig.put("columnQualifier", "old_value");
        config.put("DeleteCell", deleteCellConfig);

        // When: creating an action from the config
        Action<?> action = ActionFactory.getAction(config, mockContext);

        // Then: it should be a DeleteCell instance with correct values
        assertNotNull(action);
        assertTrue(action instanceof DeleteCell);

        DeleteCell deleteCell = (DeleteCell) action;
        assertEquals(deleteCell.getTableId(), "system_config");
        assertEquals(deleteCell.getRowKey(), "config#deprecated");
        assertEquals(deleteCell.getFamilyName(), "metadata");
        assertEquals(deleteCell.getColumnQualifier(), "old_value");
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*tableId is required.*")
    public void testDeleteCellRequiresTableId() {
        // Given: a DeleteCell action without tableId
        DeleteCell deleteCell = new DeleteCell();
        deleteCell.setTableId(null);
        deleteCell.setRowKey("key");
        deleteCell.setFamilyName("family");
        deleteCell.setColumnQualifier("qualifier");

        // When/Then: execute should throw exception
        deleteCell.execute(null);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*rowKey is required.*")
    public void testDeleteCellRequiresRowKey() {
        // Given: a DeleteCell action without rowKey
        DeleteCell deleteCell = new DeleteCell();
        deleteCell.setTableId("table");
        deleteCell.setRowKey(null);
        deleteCell.setFamilyName("family");
        deleteCell.setColumnQualifier("qualifier");

        // When/Then: execute should throw exception
        deleteCell.execute(null);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*familyName is required.*")
    public void testDeleteCellRequiresFamilyName() {
        // Given: a DeleteCell action without familyName
        DeleteCell deleteCell = new DeleteCell();
        deleteCell.setTableId("table");
        deleteCell.setRowKey("key");
        deleteCell.setFamilyName(null);
        deleteCell.setColumnQualifier("qualifier");

        // When/Then: execute should throw exception
        deleteCell.execute(null);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*columnQualifier is required.*")
    public void testDeleteCellRequiresColumnQualifier() {
        // Given: a DeleteCell action without columnQualifier
        DeleteCell deleteCell = new DeleteCell();
        deleteCell.setTableId("table");
        deleteCell.setRowKey("key");
        deleteCell.setFamilyName("family");
        deleteCell.setColumnQualifier(null);

        // When/Then: execute should throw exception
        deleteCell.execute(null);
    }

    @Test
    public void testDeleteCellExecute() {
        // Given: a valid action to delete all versions
        DeleteCell deleteCell = new DeleteCell();
        deleteCell.setTableId("feature_flags");
        deleteCell.setRowKey("feature#old_ui");
        deleteCell.setFamilyName("flags");
        deleteCell.setColumnQualifier("enabled");

        BigtableDataClient mockClient = mock(BigtableDataClient.class);

        // When: executing the action
        deleteCell.execute(mockClient);

        // Then: verify mutateRow was called
        ArgumentCaptor<RowMutation> mutationCaptor = ArgumentCaptor.forClass(RowMutation.class);
        verify(mockClient).mutateRow(mutationCaptor.capture());

        RowMutation capturedMutation = mutationCaptor.getValue();
        assertNotNull(capturedMutation);
    }

    @Test
    public void testDeleteCellToString() {
        // Given: a DeleteCell action
        DeleteCell deleteCell = new DeleteCell();
        deleteCell.setTableId("test-table");
        deleteCell.setRowKey("test-key");
        deleteCell.setFamilyName("test-family");
        deleteCell.setColumnQualifier("test-qualifier");

        // When: calling toString
        String result = deleteCell.toString();

        // Then: it should contain relevant information
        assertTrue(result.contains("test-table"));
        assertTrue(result.contains("test-key"));
        assertTrue(result.contains("test-family"));
        assertTrue(result.contains("test-qualifier"));
        assertTrue(result.contains("DeleteCell"));
    }

    @Test
    public void testDeleteCellGettersSetters() {
        // Given: a DeleteCell action
        DeleteCell deleteCell = new DeleteCell();

        // When: setting values
        deleteCell.setTableId("my-table");
        deleteCell.setRowKey("my-key");
        deleteCell.setFamilyName("my-family");
        deleteCell.setColumnQualifier("my-qualifier");

        // Then: getters should return the values
        assertEquals(deleteCell.getTableId(), "my-table");
        assertEquals(deleteCell.getRowKey(), "my-key");
        assertEquals(deleteCell.getFamilyName(), "my-family");
        assertEquals(deleteCell.getColumnQualifier(), "my-qualifier");
    }

    @Test
    public void testDeleteCellActionFactoryRegistration() {
        // Given: a YAML-like configuration with DeleteCell
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> deleteCellConfig = new HashMap<>();
        deleteCellConfig.put("tableId", "test");
        deleteCellConfig.put("rowKey", "key");
        deleteCellConfig.put("familyName", "family");
        deleteCellConfig.put("columnQualifier", "qualifier");
        config.put("DeleteCell", deleteCellConfig);

        // When: getting the action via ActionFactory
        Action<?> action = ActionFactory.getAction(config, mockContext);

        // Then: should successfully create DeleteCell instance
        assertNotNull(action);
        assertTrue(action instanceof DeleteCell);
    }

    @Test
    public void testDeleteCellMinimalConfig() {
        // Given: minimal valid configuration
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> deleteCellConfig = new HashMap<>();
        deleteCellConfig.put("tableId", "minimal-table");
        deleteCellConfig.put("rowKey", "minimal-key");
        deleteCellConfig.put("familyName", "minimal-family");
        deleteCellConfig.put("columnQualifier", "minimal-qualifier");
        config.put("DeleteCell", deleteCellConfig);

        // When: creating an action
        Action<?> action = ActionFactory.getAction(config, mockContext);

        // Then: should work with minimal config
        assertNotNull(action);
        assertTrue(action instanceof DeleteCell);

        DeleteCell deleteCell = (DeleteCell) action;
        assertEquals(deleteCell.getTableId(), "minimal-table");
        assertEquals(deleteCell.getRowKey(), "minimal-key");
        assertEquals(deleteCell.getFamilyName(), "minimal-family");
        assertEquals(deleteCell.getColumnQualifier(), "minimal-qualifier");
    }
}
