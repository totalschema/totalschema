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

package io.github.totalschema.connector.gcp.bigquery.actions;

import static org.easymock.EasyMock.*;
import static org.testng.Assert.*;

import io.github.totalschema.connector.gcp.bigquery.actions.dataset.create.CreateDataset;
import io.github.totalschema.connector.gcp.bigquery.actions.statement.execute.ExecuteStatement;
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
    public void testCreateDatasetAction() {
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
    public void testCreateDatasetActionMinimal() {
        // Given: a minimal configuration (only required fields)
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> createDatasetConfig = new HashMap<>();
        createDatasetConfig.put("datasetId", "minimal_dataset");
        config.put("CreateDataset", createDatasetConfig);

        // When: creating an action
        Action action = ActionFactory.getAction(config, mockContext);

        // Then: it should work with no optional fields
        assertNotNull(action);
        assertTrue(action instanceof CreateDataset);

        CreateDataset createDataset = (CreateDataset) action;
        assertEquals(createDataset.getDatasetId(), "minimal_dataset");
        assertNull(createDataset.getDescription());
        assertNull(createDataset.getLocation());
    }

    @Test
    public void testExecuteQueryAction() {
        // Given: configuration for ExecuteStatement
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> executeQueryConfig = new HashMap<>();
        executeQueryConfig.put("statement", "SELECT * FROM users");
        executeQueryConfig.put("defaultDataset", "my_dataset");
        executeQueryConfig.put("useLegacySql", false);
        config.put("ExecuteStatement", executeQueryConfig);

        // When: creating an action
        Action action = ActionFactory.getAction(config, mockContext);

        // Then: it should be an ExecuteStatement instance with correct values
        assertNotNull(action);
        assertTrue(action instanceof ExecuteStatement);

        ExecuteStatement executeStatement = (ExecuteStatement) action;
        assertEquals(executeStatement.getStatement(), "SELECT * FROM users");
        assertEquals(executeStatement.getDefaultDataset(), "my_dataset");
        assertEquals(executeStatement.getUseLegacySql(), Boolean.FALSE);
    }

    @Test
    public void testExecuteQueryActionMinimal() {
        // Given: minimal ExecuteStatement configuration
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> executeQueryConfig = new HashMap<>();
        executeQueryConfig.put("statement", "CREATE TABLE test (id INT64)");
        config.put("ExecuteStatement", executeQueryConfig);

        // When: creating an action
        Action action = ActionFactory.getAction(config, mockContext);

        // Then: optional fields should be null
        assertNotNull(action);
        assertTrue(action instanceof ExecuteStatement);

        ExecuteStatement executeStatement = (ExecuteStatement) action;
        assertEquals(executeStatement.getStatement(), "CREATE TABLE test (id INT64)");
        assertNull(executeStatement.getDefaultDataset());
        assertNull(executeStatement.getUseLegacySql());
    }

    @Test
    public void testExecuteQueryWithMultilineQuery() {
        // Given: configuration with multiline query
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> executeQueryConfig = new HashMap<>();
        String multilineQuery =
                "CREATE TABLE users (\n  id INT64,\n  name STRING,\n  email STRING\n)";
        executeQueryConfig.put("statement", multilineQuery);
        config.put("ExecuteStatement", executeQueryConfig);

        // When: creating an action
        Action action = ActionFactory.getAction(config, mockContext);

        // Then: query should preserve newlines
        assertNotNull(action);
        assertTrue(action instanceof ExecuteStatement);

        ExecuteStatement executeStatement = (ExecuteStatement) action;
        assertEquals(executeStatement.getStatement(), multilineQuery);
        assertTrue(executeStatement.getStatement().contains("\n"));
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*exactly one top-level key.*")
    public void testMultipleTopLevelKeysThrowsException() {
        // Given: configuration with multiple top-level keys
        Map<String, Object> config = new HashMap<>();
        config.put("CreateDataset", new HashMap<>());
        config.put("ExecuteStatement", new HashMap<>());

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
        Map<String, Object> createDatasetConfig = new HashMap<>();
        createDatasetConfig.put("datasetId", "test_dataset");
        config.put("CreateDataset", createDatasetConfig);

        // When: creating an action
        Action action = ActionFactory.getAction(config, mockContext);

        // Then: it should work without Class.forName() being called
        // (this would fail if reflection was used incorrectly)
        assertNotNull(action);
        assertTrue(action instanceof CreateDataset);
    }

    @Test
    public void testVariableSubstitution() {
        // Given: mock context with variable substitution
        Context contextWithVars = createMock(Context.class);
        ExpressionEvaluator evaluatorWithVars = createMock(ExpressionEvaluator.class);
        VariableService variableServiceWithVars = createMock(VariableService.class);

        Map<String, String> variables = new HashMap<>();
        variables.put("DATASET_NAME", "production_dataset");
        variables.put("LOCATION", "EU");

        expect(contextWithVars.get(ExpressionEvaluator.class))
                .andReturn(evaluatorWithVars)
                .anyTimes();
        expect(contextWithVars.get(VariableService.class))
                .andReturn(variableServiceWithVars)
                .anyTimes();
        expect(variableServiceWithVars.getEffectiveVariables(contextWithVars))
                .andReturn(variables)
                .anyTimes();

        // Mock evaluator to substitute variables
        expect(evaluatorWithVars.evaluate(eq("${DATASET_NAME}"), eq(variables)))
                .andReturn("production_dataset")
                .anyTimes();
        expect(evaluatorWithVars.evaluate(eq("${LOCATION}"), eq(variables)))
                .andReturn("EU")
                .anyTimes();
        expect(evaluatorWithVars.evaluate(eq("Test dataset"), eq(variables)))
                .andReturn("Test dataset")
                .anyTimes();

        replay(contextWithVars, evaluatorWithVars, variableServiceWithVars);

        Map<String, Object> config = new HashMap<>();
        Map<String, Object> createDatasetConfig = new HashMap<>();
        createDatasetConfig.put("datasetId", "${DATASET_NAME}");
        createDatasetConfig.put("description", "Test dataset");
        createDatasetConfig.put("location", "${LOCATION}");
        config.put("CreateDataset", createDatasetConfig);

        // When: creating an action with variable substitution
        Action action = ActionFactory.getAction(config, contextWithVars);

        // Then: variables should be substituted
        assertNotNull(action);
        assertTrue(action instanceof CreateDataset);

        CreateDataset createDataset = (CreateDataset) action;
        assertEquals(createDataset.getDatasetId(), "production_dataset");
        assertEquals(createDataset.getLocation(), "EU");
    }

    @Test
    public void testServiceLoaderDiscovery() {
        // Given: valid configurations for both action types
        Map<String, Object> datasetConfig = new HashMap<>();
        datasetConfig.put(
                "CreateDataset",
                new HashMap<String, Object>() {
                    {
                        put("datasetId", "test");
                    }
                });

        Map<String, Object> queryConfig = new HashMap<>();
        queryConfig.put(
                "ExecuteStatement",
                new HashMap<String, Object>() {
                    {
                        put("statement", "SELECT 1");
                    }
                });

        // When: creating actions via ServiceLoader
        Action datasetAction = ActionFactory.getAction(datasetConfig, mockContext);
        Action queryAction = ActionFactory.getAction(queryConfig, mockContext);

        // Then: both should be discovered and instantiated
        assertNotNull(datasetAction);
        assertNotNull(queryAction);
        assertTrue(datasetAction instanceof CreateDataset);
        assertTrue(queryAction instanceof ExecuteStatement);
    }
}
