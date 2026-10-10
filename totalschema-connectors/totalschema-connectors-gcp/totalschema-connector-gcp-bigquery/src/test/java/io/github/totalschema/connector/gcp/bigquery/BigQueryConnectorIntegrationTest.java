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

package io.github.totalschema.connector.gcp.bigquery;

import static org.easymock.EasyMock.*;
import static org.testng.Assert.*;

import com.google.api.gax.paging.Page;
import com.google.cloud.bigquery.*;
import io.github.totalschema.config.Configuration;
import io.github.totalschema.connector.gcp.bigquery.actions.Action;
import io.github.totalschema.connector.gcp.bigquery.actions.ActionFactory;
import io.github.totalschema.connector.gcp.bigquery.actions.dataset.create.CreateDataset;
import io.github.totalschema.connector.gcp.bigquery.actions.statement.execute.ExecuteStatement;
import io.github.totalschema.engine.api.Context;
import io.github.totalschema.spi.expression.evaluator.ExpressionEvaluator;
import io.github.totalschema.spi.variables.VariableService;
import java.util.*;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Integration tests for BigQuery connector demonstrating end-to-end workflows.
 *
 * <p>Note: These are mock-based integration tests. For real integration tests against BigQuery, you
 * would need to:
 *
 * <ul>
 *   <li>Set up a test GCP project with BigQuery enabled
 *   <li>Configure authentication (service account or ADC)
 *   <li>Create and tear down test datasets/tables
 *   <li>Handle cleanup in @AfterMethod
 * </ul>
 */
public class BigQueryConnectorIntegrationTest {

    private Configuration mockConfiguration;
    private Context mockContext;
    private BigQuery mockBigQuery;
    private ExpressionEvaluator mockEvaluator;
    private VariableService mockVariableService;

    @BeforeMethod
    public void setUp() {
        mockConfiguration = createMock(Configuration.class);
        mockContext = createMock(Context.class);
        mockBigQuery = createMock(BigQuery.class);
        mockEvaluator = createMock(ExpressionEvaluator.class);
        mockVariableService = createMock(VariableService.class);

        // Default configuration
        expect(mockConfiguration.getString("defaultDataset"))
                .andReturn(Optional.empty())
                .anyTimes();
        expect(mockConfiguration.getString(anyString())).andReturn(Optional.empty()).anyTimes();
        expect(mockConfiguration.getBoolean(anyString())).andReturn(Optional.empty()).anyTimes();

        // Context mocks
        expect(mockContext.get(ExpressionEvaluator.class)).andReturn(mockEvaluator).anyTimes();
        expect(mockContext.get(VariableService.class)).andReturn(mockVariableService).anyTimes();
        expect(mockContext.get(BigQuery.class)).andReturn(mockBigQuery).anyTimes();
        expect(mockContext.get(eq(BigQuery.class), isNull(), anyObject()))
                .andReturn(mockBigQuery)
                .anyTimes();

        // Variable service
        expect(mockVariableService.getEffectiveVariables(mockContext))
                .andReturn(Collections.emptyMap())
                .anyTimes();

        // Expression evaluator (identity substitution)
        expect(mockEvaluator.evaluate(anyObject(String.class), anyObject(Map.class)))
                .andAnswer(() -> (String) getCurrentArguments()[0])
                .anyTimes();
    }

    @Test
    public void testCreateDatasetWorkflow() {
        // Given: a BigQuery connector and CreateDataset YAML
        Map<String, Object> yamlConfig = new HashMap<>();
        Map<String, Object> datasetConfig = new HashMap<>();
        datasetConfig.put("datasetId", "test_dataset");
        datasetConfig.put("description", "Integration test dataset");
        datasetConfig.put("location", "US");
        yamlConfig.put("CreateDataset", datasetConfig);

        // Mock BigQuery create
        expect(mockBigQuery.create(anyObject(DatasetInfo.class)))
                .andAnswer(
                        () -> {
                            DatasetInfo info = (DatasetInfo) getCurrentArguments()[0];
                            assertEquals(info.getDatasetId().getDataset(), "test_dataset");
                            return null;
                        });

        replay(mockConfiguration, mockContext, mockBigQuery, mockEvaluator, mockVariableService);

        // When: creating action and executing
        Action action = ActionFactory.getAction(yamlConfig, mockContext);
        action.execute(mockBigQuery, mockConfiguration);

        // Then: verify execution
        verify(mockBigQuery);
        assertTrue(action instanceof CreateDataset);
    }

    @Test
    public void testExecuteQueryWorkflow() throws InterruptedException {
        // Given: a BigQuery connector and ExecuteStatement YAML
        Map<String, Object> yamlConfig = new HashMap<>();
        Map<String, Object> queryConfig = new HashMap<>();
        queryConfig.put("statement", "CREATE TABLE test_dataset.users (id INT64, name STRING)");
        queryConfig.put("defaultDataset", "test_dataset");
        yamlConfig.put("ExecuteStatement", queryConfig);

        BigQueryOptions mockOptions = createMock(BigQueryOptions.class);
        Job mockJob = createMock(Job.class);
        JobStatus mockStatus = createMock(JobStatus.class);

        // Mock BigQuery query execution
        expect(mockBigQuery.getOptions()).andReturn(mockOptions).anyTimes();
        expect(mockOptions.getProjectId()).andReturn("test-project").anyTimes();
        expect(mockBigQuery.create(anyObject(JobInfo.class))).andReturn(mockJob);
        expect(mockJob.waitFor()).andReturn(mockJob);
        expect(mockJob.getStatus()).andReturn(mockStatus).anyTimes();
        expect(mockStatus.getError()).andReturn(null);

        replay(
                mockConfiguration,
                mockContext,
                mockBigQuery,
                mockOptions,
                mockJob,
                mockStatus,
                mockEvaluator,
                mockVariableService);

        // When: creating action and executing
        Action action = ActionFactory.getAction(yamlConfig, mockContext);
        action.execute(mockBigQuery, mockConfiguration);

        // Then: verify execution
        verify(mockBigQuery, mockJob);
        assertTrue(action instanceof ExecuteStatement);
    }

    @Test
    public void testSqlFileExecution() throws InterruptedException, java.sql.SQLException {
        // Given: a SQL file
        String sql = "CREATE TABLE users (\n  id INT64,\n  name STRING\n)";

        BigQueryOptions mockOptions = createMock(BigQueryOptions.class);
        Job mockJob = createMock(Job.class);
        JobStatus mockStatus = createMock(JobStatus.class);

        expect(mockBigQuery.getOptions()).andReturn(mockOptions).anyTimes();
        expect(mockOptions.getProjectId()).andReturn("test-project").anyTimes();
        expect(mockBigQuery.create(anyObject(JobInfo.class))).andReturn(mockJob);
        expect(mockJob.waitFor()).andReturn(mockJob);
        expect(mockJob.getStatus()).andReturn(mockStatus).anyTimes();
        expect(mockStatus.getError()).andReturn(null);

        replay(
                mockConfiguration,
                mockContext,
                mockBigQuery,
                mockOptions,
                mockJob,
                mockStatus,
                mockEvaluator,
                mockVariableService);

        // When: executing SQL via BigQuery SQLDatabase implementation
        BigQuerySQLDatabase sqlDatabase =
                new BigQuerySQLDatabase("test-bigquery", mockBigQuery, mockConfiguration);
        sqlDatabase.execute(sql);

        // Then: verify SQL was executed
        verify(mockBigQuery, mockJob);
    }

    @Test
    public void testCompleteDatasetCreationWorkflow() throws InterruptedException {
        // Given: complete workflow - create dataset, create table, insert data
        List<Map<String, Object>> actions = new ArrayList<>();

        // Step 1: Create dataset
        Map<String, Object> createDatasetYaml = new HashMap<>();
        Map<String, Object> datasetConfig = new HashMap<>();
        datasetConfig.put("datasetId", "ecommerce");
        datasetConfig.put("description", "E-commerce database");
        datasetConfig.put("location", "US");
        createDatasetYaml.put("CreateDataset", datasetConfig);
        actions.add(createDatasetYaml);

        // Step 2: Create users table
        Map<String, Object> createUsersTableYaml = new HashMap<>();
        Map<String, Object> usersTableConfig = new HashMap<>();
        usersTableConfig.put(
                "statement",
                "CREATE TABLE ecommerce.users (\n"
                        + "  user_id INT64,\n"
                        + "  username STRING,\n"
                        + "  email STRING\n"
                        + ")");
        usersTableConfig.put("defaultDataset", "ecommerce");
        createUsersTableYaml.put("ExecuteStatement", usersTableConfig);
        actions.add(createUsersTableYaml);

        // Step 3: Create orders table
        Map<String, Object> createOrdersTableYaml = new HashMap<>();
        Map<String, Object> ordersTableConfig = new HashMap<>();
        ordersTableConfig.put(
                "statement",
                "CREATE TABLE ecommerce.orders (\n"
                        + "  order_id INT64,\n"
                        + "  user_id INT64,\n"
                        + "  total NUMERIC\n"
                        + ")");
        ordersTableConfig.put("defaultDataset", "ecommerce");
        createOrdersTableYaml.put("ExecuteStatement", ordersTableConfig);
        actions.add(createOrdersTableYaml);

        BigQueryOptions mockOptions = createMock(BigQueryOptions.class);
        Job mockJob = createMock(Job.class);
        JobStatus mockStatus = createMock(JobStatus.class);

        // Mock dataset creation
        expect(mockBigQuery.create(anyObject(DatasetInfo.class))).andReturn(null);

        // Mock table creation (2 times)
        expect(mockBigQuery.getOptions()).andReturn(mockOptions).anyTimes();
        expect(mockOptions.getProjectId()).andReturn("test-project").anyTimes();
        expect(mockBigQuery.create(anyObject(JobInfo.class))).andReturn(mockJob).times(2);
        expect(mockJob.waitFor()).andReturn(mockJob).times(2);
        expect(mockJob.getStatus()).andReturn(mockStatus).anyTimes();
        expect(mockStatus.getError()).andReturn(null).times(2);

        replay(
                mockConfiguration,
                mockContext,
                mockBigQuery,
                mockOptions,
                mockJob,
                mockStatus,
                mockEvaluator,
                mockVariableService);

        // When: executing all actions in sequence
        for (Map<String, Object> actionYaml : actions) {
            Action action = ActionFactory.getAction(actionYaml, mockContext);
            try {
                action.execute(mockBigQuery, mockConfiguration);
            } catch (Exception e) {
                // ExecuteStatement may throw checked exceptions
                throw new RuntimeException(e);
            }
        }

        // Then: all actions should have been executed
        verify(mockBigQuery);
    }

    @Test
    public void testConnectorCheckConnection() {
        // Given: a BigQuery connector
        Page<Dataset> mockPage = createMock(Page.class);

        expect(mockBigQuery.listDatasets()).andReturn(mockPage);
        expect(mockPage.iterateAll()).andReturn(Collections.emptyList());

        replay(
                mockConfiguration,
                mockContext,
                mockBigQuery,
                mockPage,
                mockEvaluator,
                mockVariableService);

        BigQueryConnector connector = new BigQueryConnector("mybigquery", mockConfiguration);

        // When: checking connection
        connector.checkConnection(mockContext, Collections.emptyList());

        // Then: should succeed without exception
        verify(mockBigQuery, mockPage);
    }

    @Test
    public void testVariableSubstitutionWorkflow() {
        // Given: configuration with variables
        Map<String, String> variables = new HashMap<>();
        variables.put("DATASET_NAME", "production_data");
        variables.put("LOCATION", "EU");

        Context contextWithVars = createMock(Context.class);
        ExpressionEvaluator evaluatorWithVars = createMock(ExpressionEvaluator.class);
        VariableService variableServiceWithVars = createMock(VariableService.class);

        expect(contextWithVars.get(ExpressionEvaluator.class))
                .andReturn(evaluatorWithVars)
                .anyTimes();
        expect(contextWithVars.get(VariableService.class))
                .andReturn(variableServiceWithVars)
                .anyTimes();
        expect(variableServiceWithVars.getEffectiveVariables(contextWithVars))
                .andReturn(variables)
                .anyTimes();

        // Variable substitution
        expect(evaluatorWithVars.evaluate(eq("${DATASET_NAME}"), eq(variables)))
                .andReturn("production_data")
                .anyTimes();
        expect(evaluatorWithVars.evaluate(eq("${LOCATION}"), eq(variables)))
                .andReturn("EU")
                .anyTimes();
        expect(evaluatorWithVars.evaluate(eq("Production dataset"), eq(variables)))
                .andReturn("Production dataset")
                .anyTimes();

        // Mock dataset creation
        expect(mockBigQuery.create(anyObject(DatasetInfo.class)))
                .andAnswer(
                        () -> {
                            DatasetInfo info = (DatasetInfo) getCurrentArguments()[0];
                            assertEquals(info.getDatasetId().getDataset(), "production_data");
                            assertEquals(info.getLocation(), "EU");
                            return null;
                        });

        replay(contextWithVars, evaluatorWithVars, variableServiceWithVars, mockBigQuery);

        // YAML with variables
        Map<String, Object> yamlConfig = new HashMap<>();
        Map<String, Object> datasetConfig = new HashMap<>();
        datasetConfig.put("datasetId", "${DATASET_NAME}");
        datasetConfig.put("description", "Production dataset");
        datasetConfig.put("location", "${LOCATION}");
        yamlConfig.put("CreateDataset", datasetConfig);

        // When: creating and executing action with variable substitution
        Action action = ActionFactory.getAction(yamlConfig, contextWithVars);
        action.execute(mockBigQuery, mockConfiguration);

        // Then: variables should be substituted correctly
        verify(mockBigQuery);
        assertTrue(action instanceof CreateDataset);
        CreateDataset createDataset = (CreateDataset) action;
        assertEquals(createDataset.getDatasetId(), "production_data");
        assertEquals(createDataset.getLocation(), "EU");
    }
}
