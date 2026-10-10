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

package io.github.totalschema.connector.gcp.bigquery.actions.dataset.create;

import static org.easymock.EasyMock.*;
import static org.testng.Assert.*;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.DatasetInfo;
import io.github.totalschema.config.Configuration;
import io.github.totalschema.connector.gcp.bigquery.actions.Action;
import io.github.totalschema.connector.gcp.bigquery.actions.ActionFactory;
import io.github.totalschema.engine.api.Context;
import io.github.totalschema.spi.expression.evaluator.ExpressionEvaluator;
import io.github.totalschema.spi.variables.VariableService;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Tests for {@link CreateDataset} and {@link CreateDatasetActionFactory}. */
public class CreateDatasetTest {

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

        // Setup mock configuration to return empty Optional for any getString() call
        expect(mockConfiguration.getString(anyString())).andReturn(Optional.empty()).anyTimes();

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
    public void testCreateDatasetActionFromMap() {
        // Given: a YAML-like configuration map
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> createDatasetConfig = new HashMap<>();
        createDatasetConfig.put("datasetId", "my_dataset");
        createDatasetConfig.put("description", "Test dataset");
        createDatasetConfig.put("location", "US");
        config.put("CreateDataset", createDatasetConfig);

        // When: creating an action from the config
        Action action = ActionFactory.getAction(config, mockContext);

        // Then: it should be a CreateDataset instance with correct values
        assertNotNull(action);
        assertTrue(action instanceof CreateDataset);

        CreateDataset createDataset = (CreateDataset) action;
        assertEquals(createDataset.getDatasetId(), "my_dataset");
        assertEquals(createDataset.getDescription(), "Test dataset");
        assertEquals(createDataset.getLocation(), "US");
    }

    @Test
    public void testCreateDatasetMinimalConfig() {
        // Given: minimal valid configuration (only datasetId)
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> createDatasetConfig = new HashMap<>();
        createDatasetConfig.put("datasetId", "minimal_dataset");
        config.put("CreateDataset", createDatasetConfig);

        // When: creating an action
        Action action = ActionFactory.getAction(config, mockContext);

        // Then: should work with just datasetId
        assertNotNull(action);
        assertTrue(action instanceof CreateDataset);

        CreateDataset createDataset = (CreateDataset) action;
        assertEquals(createDataset.getDatasetId(), "minimal_dataset");
        assertNull(createDataset.getDescription());
        assertNull(createDataset.getLocation());
    }

    @Test
    public void testCreateDatasetWithDescription() {
        // Given: configuration with description
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> createDatasetConfig = new HashMap<>();
        createDatasetConfig.put("datasetId", "described_dataset");
        createDatasetConfig.put("description", "This is a test dataset");
        config.put("CreateDataset", createDatasetConfig);

        // When: creating an action
        Action action = ActionFactory.getAction(config, mockContext);

        // Then: should have description
        assertNotNull(action);
        CreateDataset createDataset = (CreateDataset) action;
        assertEquals(createDataset.getDescription(), "This is a test dataset");
    }

    @Test
    public void testCreateDatasetWithLocation() {
        // Given: configuration with location
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> createDatasetConfig = new HashMap<>();
        createDatasetConfig.put("datasetId", "eu_dataset");
        createDatasetConfig.put("location", "EU");
        config.put("CreateDataset", createDatasetConfig);

        // When: creating an action
        Action action = ActionFactory.getAction(config, mockContext);

        // Then: should have location
        assertNotNull(action);
        CreateDataset createDataset = (CreateDataset) action;
        assertEquals(createDataset.getLocation(), "EU");
    }

    @Test
    public void testCreateDatasetExecute() {
        // Given: a valid CreateDataset action
        CreateDataset createDataset = new CreateDataset();
        createDataset.setDatasetId("test_dataset");
        createDataset.setDescription("Test dataset description");
        createDataset.setLocation("US");

        BigQuery mockBigQuery = createMock(BigQuery.class);

        // Expect the create method to be called with a DatasetInfo
        expect(mockBigQuery.create(anyObject(DatasetInfo.class)))
                .andAnswer(
                        () -> {
                            DatasetInfo datasetInfo = (DatasetInfo) getCurrentArguments()[0];
                            // Verify the dataset info has correct values
                            assertEquals(datasetInfo.getDatasetId().getDataset(), "test_dataset");
                            assertEquals(datasetInfo.getDescription(), "Test dataset description");
                            assertEquals(datasetInfo.getLocation(), "US");
                            return null; // Return doesn't matter for this test
                        });

        replay(mockBigQuery);

        // When: executing the action
        createDataset.execute(mockBigQuery, mockConfiguration);

        // Then: verify create was called
        verify(mockBigQuery);
    }

    @Test
    public void testCreateDatasetExecuteMinimal() {
        // Given: a minimal CreateDataset action (no optional fields)
        CreateDataset createDataset = new CreateDataset();
        createDataset.setDatasetId("minimal_dataset");

        BigQuery mockBigQuery = createMock(BigQuery.class);

        // Expect the create method to be called
        expect(mockBigQuery.create(anyObject(DatasetInfo.class)))
                .andAnswer(
                        () -> {
                            DatasetInfo datasetInfo = (DatasetInfo) getCurrentArguments()[0];
                            assertEquals(
                                    datasetInfo.getDatasetId().getDataset(), "minimal_dataset");
                            assertNull(datasetInfo.getDescription());
                            // Location may be null or not set
                            return null;
                        });

        replay(mockBigQuery);

        // When: executing the action
        createDataset.execute(mockBigQuery, mockConfiguration);

        // Then: verify create was called
        verify(mockBigQuery);
    }

    @Test
    public void testCreateDatasetExecuteWithoutLocation() {
        // Given: action without location
        CreateDataset createDataset = new CreateDataset();
        createDataset.setDatasetId("no_location_dataset");
        createDataset.setDescription("Dataset without location");

        BigQuery mockBigQuery = createMock(BigQuery.class);

        expect(mockBigQuery.create(anyObject(DatasetInfo.class)))
                .andAnswer(
                        () -> {
                            DatasetInfo datasetInfo = (DatasetInfo) getCurrentArguments()[0];
                            assertEquals(
                                    datasetInfo.getDatasetId().getDataset(), "no_location_dataset");
                            assertEquals(datasetInfo.getDescription(), "Dataset without location");
                            return null;
                        });

        replay(mockBigQuery);

        // When: executing the action
        createDataset.execute(mockBigQuery, mockConfiguration);

        // Then: verify create was called without location
        verify(mockBigQuery);
    }

    @Test
    public void testCreateDatasetToString() {
        // Given: a CreateDataset action
        CreateDataset createDataset = new CreateDataset();
        createDataset.setDatasetId("test_dataset");
        createDataset.setDescription("Test");
        createDataset.setLocation("US");

        // When: calling toString
        String result = createDataset.toString();

        // Then: it should contain relevant information
        assertTrue(result.contains("test_dataset"));
        assertTrue(result.contains("CreateDataset"));
    }

    @Test
    public void testCreateDatasetGettersSetters() {
        // Given: a CreateDataset action
        CreateDataset createDataset = new CreateDataset();

        // When: setting values
        createDataset.setDatasetId("my_dataset");
        createDataset.setDescription("My description");
        createDataset.setLocation("EU");

        // Then: getters should return the values
        assertEquals(createDataset.getDatasetId(), "my_dataset");
        assertEquals(createDataset.getDescription(), "My description");
        assertEquals(createDataset.getLocation(), "EU");
    }

    @Test
    public void testCreateDatasetActionFactoryRegistration() {
        // Given: a YAML-like configuration with CreateDataset
        Map<String, Object> config = new HashMap<>();
        config.put(
                "CreateDataset",
                new HashMap<String, Object>() {
                    {
                        put("datasetId", "test");
                    }
                });

        // When: getting the action via ActionFactory
        Action action = ActionFactory.getAction(config, mockContext);

        // Then: should successfully create CreateDataset instance
        assertNotNull(action);
        assertTrue(action instanceof CreateDataset);
    }

    @Test
    public void testCreateDatasetWithEmptyOptionalFields() {
        // Given: configuration with empty strings for optional fields
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> createDatasetConfig = new HashMap<>();
        createDatasetConfig.put("datasetId", "test_dataset");
        createDatasetConfig.put("description", "");
        createDatasetConfig.put("location", "");
        config.put("CreateDataset", createDatasetConfig);

        // When: creating an action
        Action action = ActionFactory.getAction(config, mockContext);

        // Then: empty strings should be preserved
        assertNotNull(action);
        CreateDataset createDataset = (CreateDataset) action;
        assertEquals(createDataset.getDescription(), "");
        assertEquals(createDataset.getLocation(), "");
    }

    @Test
    public void testCreateDatasetWithSpecialCharacters() {
        // Given: configuration with special characters in description
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> createDatasetConfig = new HashMap<>();
        createDatasetConfig.put("datasetId", "special_dataset");
        createDatasetConfig.put("description", "Dataset with 'quotes' and \"double quotes\"");
        config.put("CreateDataset", createDatasetConfig);

        // When: creating an action
        Action action = ActionFactory.getAction(config, mockContext);

        // Then: special characters should be preserved
        assertNotNull(action);
        CreateDataset createDataset = (CreateDataset) action;
        assertTrue(createDataset.getDescription().contains("'quotes'"));
        assertTrue(createDataset.getDescription().contains("\"double quotes\""));
    }
}
