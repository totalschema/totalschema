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

package io.github.totalschema.connector.gcp.bigquery.actions.dataset.drop;

import static org.easymock.EasyMock.*;
import static org.testng.Assert.*;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQuery.DatasetDeleteOption;
import io.github.totalschema.config.Configuration;
import io.github.totalschema.connector.gcp.bigquery.actions.Action;
import io.github.totalschema.connector.gcp.bigquery.actions.ActionFactory;
import io.github.totalschema.engine.api.Context;
import io.github.totalschema.spi.expression.evaluator.ExpressionEvaluator;
import io.github.totalschema.spi.variables.VariableService;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Tests for {@link DropDataset} and {@link DropDatasetActionFactory}. */
public class DropDatasetTest {

    private Context mockContext;
    private ExpressionEvaluator mockEvaluator;
    private VariableService mockVariableService;
    private Configuration mockConfiguration;

    @BeforeMethod
    public void setUp() {
        mockContext = createMock(Context.class);
        mockEvaluator = createMock(ExpressionEvaluator.class);
        mockVariableService = createMock(VariableService.class);
        mockConfiguration = createMock(Configuration.class);
        expect(mockConfiguration.getString(anyString()))
                .andReturn(java.util.Optional.empty())
                .anyTimes();

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

        replay(mockContext, mockEvaluator, mockVariableService, mockConfiguration);
    }

    @Test
    public void testDropDatasetActionFromMap() {
        // Given: a YAML-like configuration map
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> dropDatasetConfig = new HashMap<>();
        dropDatasetConfig.put("datasetId", "my_dataset");
        dropDatasetConfig.put("deleteContents", true);
        config.put("DropDataset", dropDatasetConfig);

        // When: creating an action from the config
        Action action = ActionFactory.getAction(config, mockContext);

        // Then: it should be a DropDataset instance with correct values
        assertNotNull(action);
        assertTrue(action instanceof DropDataset);

        DropDataset dropDataset = (DropDataset) action;
        assertEquals(dropDataset.getDatasetId(), "my_dataset");
        assertEquals(dropDataset.getDeleteContents(), Boolean.TRUE);
    }

    @Test
    public void testDropDatasetMinimalConfig() {
        // Given: minimal valid configuration (only datasetId)
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> dropDatasetConfig = new HashMap<>();
        dropDatasetConfig.put("datasetId", "minimal_dataset");
        config.put("DropDataset", dropDatasetConfig);

        // When: creating an action
        Action action = ActionFactory.getAction(config, mockContext);

        // Then: should work with just datasetId
        assertNotNull(action);
        assertTrue(action instanceof DropDataset);

        DropDataset dropDataset = (DropDataset) action;
        assertEquals(dropDataset.getDatasetId(), "minimal_dataset");
        assertNull(dropDataset.getDeleteContents());
    }

    @Test
    public void testDropDatasetWithDeleteContentsTrue() {
        // Given: configuration with deleteContents true
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> dropDatasetConfig = new HashMap<>();
        dropDatasetConfig.put("datasetId", "dataset_with_contents");
        dropDatasetConfig.put("deleteContents", true);
        config.put("DropDataset", dropDatasetConfig);

        // When: creating an action
        Action action = ActionFactory.getAction(config, mockContext);

        // Then: should have deleteContents set to true
        assertNotNull(action);
        DropDataset dropDataset = (DropDataset) action;
        assertEquals(dropDataset.getDeleteContents(), Boolean.TRUE);
    }

    @Test
    public void testDropDatasetWithDeleteContentsFalse() {
        // Given: configuration with deleteContents false
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> dropDatasetConfig = new HashMap<>();
        dropDatasetConfig.put("datasetId", "dataset_no_delete_contents");
        dropDatasetConfig.put("deleteContents", false);
        config.put("DropDataset", dropDatasetConfig);

        // When: creating an action
        Action action = ActionFactory.getAction(config, mockContext);

        // Then: should have deleteContents set to false
        assertNotNull(action);
        DropDataset dropDataset = (DropDataset) action;
        assertEquals(dropDataset.getDeleteContents(), Boolean.FALSE);
    }

    @Test
    public void testDropDatasetExecuteWithoutDeleteContents() {
        // Given: a DropDataset action without deleteContents
        DropDataset dropDataset = new DropDataset();
        dropDataset.setDatasetId("test_dataset");

        BigQuery mockBigQuery = createMock(BigQuery.class);

        // Expect delete to be called without options
        expect(mockBigQuery.delete("test_dataset")).andReturn(true);

        replay(mockBigQuery);

        // When: executing the action
        dropDataset.execute(mockBigQuery, mockConfiguration);

        // Then: verify delete was called
        verify(mockBigQuery);
    }

    @Test
    public void testDropDatasetExecuteWithDeleteContentsFalse() {
        // Given: a DropDataset action with deleteContents explicitly false
        DropDataset dropDataset = new DropDataset();
        dropDataset.setDatasetId("test_dataset");
        dropDataset.setDeleteContents(false);

        BigQuery mockBigQuery = createMock(BigQuery.class);

        // Expect delete to be called without options
        expect(mockBigQuery.delete("test_dataset")).andReturn(true);

        replay(mockBigQuery);

        // When: executing the action
        dropDataset.execute(mockBigQuery, mockConfiguration);

        // Then: verify delete was called without deleteContents option
        verify(mockBigQuery);
    }

    @Test
    public void testDropDatasetExecuteWithDeleteContentsTrue() {
        // Given: a DropDataset action with deleteContents true
        DropDataset dropDataset = new DropDataset();
        dropDataset.setDatasetId("test_dataset_with_tables");
        dropDataset.setDeleteContents(true);

        BigQuery mockBigQuery = createMock(BigQuery.class);

        // Expect delete to be called with deleteContents option
        expect(
                        mockBigQuery.delete(
                                "test_dataset_with_tables", DatasetDeleteOption.deleteContents()))
                .andReturn(true);

        replay(mockBigQuery);

        // When: executing the action
        dropDataset.execute(mockBigQuery, mockConfiguration);

        // Then: verify delete was called with deleteContents option
        verify(mockBigQuery);
    }

    @Test
    public void testDropDatasetToString() {
        // Given: a DropDataset action
        DropDataset dropDataset = new DropDataset();
        dropDataset.setDatasetId("test_dataset");
        dropDataset.setDeleteContents(true);

        // When: calling toString
        String result = dropDataset.toString();

        // Then: it should contain relevant information
        assertTrue(result.contains("test_dataset"));
        assertTrue(result.contains("DropDataset"));
        assertTrue(result.contains("true"));
    }

    @Test
    public void testDropDatasetGettersSetters() {
        // Given: a DropDataset action
        DropDataset dropDataset = new DropDataset();

        // When: setting values
        dropDataset.setDatasetId("my_dataset");
        dropDataset.setDeleteContents(true);

        // Then: getters should return the values
        assertEquals(dropDataset.getDatasetId(), "my_dataset");
        assertEquals(dropDataset.getDeleteContents(), Boolean.TRUE);
    }

    @Test
    public void testDropDatasetActionFactoryRegistration() {
        // Given: a YAML-like configuration with DropDataset
        Map<String, Object> config = new HashMap<>();
        config.put(
                "DropDataset",
                new HashMap<String, Object>() {
                    {
                        put("datasetId", "test");
                    }
                });

        // When: getting the action via ActionFactory
        Action action = ActionFactory.getAction(config, mockContext);

        // Then: should successfully create DropDataset instance
        assertNotNull(action);
        assertTrue(action instanceof DropDataset);
    }

    @Test
    public void testDropDatasetWithSpecialCharactersInId() {
        // Given: configuration with special characters in dataset ID
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> dropDatasetConfig = new HashMap<>();
        dropDatasetConfig.put("datasetId", "special_dataset_123");
        config.put("DropDataset", dropDatasetConfig);

        // When: creating an action
        Action action = ActionFactory.getAction(config, mockContext);

        // Then: special characters should be preserved
        assertNotNull(action);
        DropDataset dropDataset = (DropDataset) action;
        assertEquals(dropDataset.getDatasetId(), "special_dataset_123");
    }

    @Test
    public void testDropDatasetExecuteReturnsSuccess() {
        // Given: a DropDataset action
        DropDataset dropDataset = new DropDataset();
        dropDataset.setDatasetId("success_dataset");

        BigQuery mockBigQuery = createMock(BigQuery.class);

        // Expect delete to return true (successful deletion)
        expect(mockBigQuery.delete("success_dataset")).andReturn(true);

        replay(mockBigQuery);

        // When: executing the action
        dropDataset.execute(mockBigQuery, mockConfiguration);

        // Then: should complete without exception
        verify(mockBigQuery);
    }

    @Test
    public void testDropDatasetExecuteReturnsFailure() {
        // Given: a DropDataset action
        DropDataset dropDataset = new DropDataset();
        dropDataset.setDatasetId("nonexistent_dataset");

        BigQuery mockBigQuery = createMock(BigQuery.class);

        // Expect delete to return false (dataset not found or already deleted)
        expect(mockBigQuery.delete("nonexistent_dataset")).andReturn(false);

        replay(mockBigQuery);

        // When: executing the action
        dropDataset.execute(mockBigQuery, mockConfiguration);

        // Then: should complete without exception
        verify(mockBigQuery);
    }

    @Test
    public void testDropDatasetBooleanHandling() {
        // Given: a DropDataset action
        DropDataset dropDataset = new DropDataset();

        // When: setting deleteContents to null, false, and true
        dropDataset.setDeleteContents(null);
        assertNull(dropDataset.getDeleteContents());

        dropDataset.setDeleteContents(false);
        assertEquals(dropDataset.getDeleteContents(), Boolean.FALSE);

        dropDataset.setDeleteContents(true);
        assertEquals(dropDataset.getDeleteContents(), Boolean.TRUE);
    }
}
