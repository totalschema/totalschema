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

package io.github.totalschema.connector.gcp.bigquery.actions.statement.execute;

import static org.easymock.EasyMock.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.*;

import com.google.cloud.bigquery.*;
import io.github.totalschema.config.Configuration;
import io.github.totalschema.connector.gcp.bigquery.BigQueryConnectorException;
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

/** Tests for {@link ExecuteStatement} and {@link ExecuteStatementActionFactory}. */
public class ExecuteStatementTest {

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
    public void testExecuteQueryActionFromMap() {
        // Given: a YAML-like configuration map
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> executeQueryConfig = new HashMap<>();
        executeQueryConfig.put("statement", "SELECT * FROM users");
        executeQueryConfig.put("defaultDataset", "my_dataset");
        executeQueryConfig.put("useLegacySql", false);
        config.put("ExecuteStatement", executeQueryConfig);

        // When: creating an action from the config
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
    public void testExecuteQueryMinimalConfig() {
        // Given: minimal valid configuration (only query)
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> executeQueryConfig = new HashMap<>();
        executeQueryConfig.put("statement", "CREATE TABLE test (id INT64)");
        config.put("ExecuteStatement", executeQueryConfig);

        // When: creating an action
        Action action = ActionFactory.getAction(config, mockContext);

        // Then: should work with just query
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
                "CREATE TABLE users (\n"
                        + "  id INT64,\n"
                        + "  name STRING,\n"
                        + "  email STRING\n"
                        + ")";
        executeQueryConfig.put("statement", multilineQuery);
        config.put("ExecuteStatement", executeQueryConfig);

        // When: creating an action
        Action action = ActionFactory.getAction(config, mockContext);

        // Then: multiline query should be preserved
        assertNotNull(action);
        ExecuteStatement executeStatement = (ExecuteStatement) action;
        assertEquals(executeStatement.getStatement(), multilineQuery);
        assertTrue(executeStatement.getStatement().contains("\n"));
    }

    @Test
    public void testExecuteQueryWithLegacySQL() {
        // Given: configuration with legacy SQL enabled
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> executeQueryConfig = new HashMap<>();
        executeQueryConfig.put("statement", "SELECT * FROM [project:dataset.table]");
        executeQueryConfig.put("useLegacySql", true);
        config.put("ExecuteStatement", executeQueryConfig);

        // When: creating an action
        Action action = ActionFactory.getAction(config, mockContext);

        // Then: useLegacySql should be true
        assertNotNull(action);
        ExecuteStatement executeStatement = (ExecuteStatement) action;
        assertEquals(executeStatement.getUseLegacySql(), Boolean.TRUE);
    }

    @Test
    public void testExecuteQueryExecuteSuccess() throws InterruptedException {
        // Given: a valid ExecuteStatement action
        ExecuteStatement executeStatement = new ExecuteStatement();
        executeStatement.setStatement("SELECT 1");
        executeStatement.setDefaultDataset("my_dataset");
        executeStatement.setUseLegacySql(false);

        BigQuery mockBigQuery = createMock(BigQuery.class);
        BigQueryOptions mockOptions = createMock(BigQueryOptions.class);
        Job mockJob = createMock(Job.class);
        JobStatus mockStatus = createMock(JobStatus.class);

        // Setup mock expectations
        expect(mockBigQuery.getOptions()).andReturn(mockOptions).anyTimes();
        expect(mockOptions.getProjectId()).andReturn("test-project").anyTimes();
        expect(mockBigQuery.create(anyObject(JobInfo.class))).andReturn(mockJob);
        expect(mockJob.waitFor()).andReturn(mockJob);
        expect(mockJob.getStatus()).andReturn(mockStatus).anyTimes();
        expect(mockStatus.getError()).andReturn(null);

        replay(mockBigQuery, mockOptions, mockJob, mockStatus);

        // When: executing the action
        executeStatement.execute(mockBigQuery, mockConfiguration);

        // Then: verify interactions
        verify(mockBigQuery, mockOptions, mockJob, mockStatus);
    }

    @Test(expectedExceptions = BigQueryConnectorException.class)
    public void testExecuteQueryExecuteFailure() throws InterruptedException {
        // Given: an ExecuteStatement action that will fail
        ExecuteStatement executeStatement = new ExecuteStatement();
        executeStatement.setStatement("INVALID SQL");

        BigQuery mockBigQuery = createMock(BigQuery.class);
        BigQueryOptions mockOptions = createMock(BigQueryOptions.class);
        Job mockJob = createMock(Job.class);
        JobStatus mockStatus = createMock(JobStatus.class);
        // Use Mockito for BigQueryError since it's a final class
        BigQueryError mockError = mock(BigQueryError.class);
        when(mockError.toString()).thenReturn("Syntax error");

        // Setup mock expectations for failure
        expect(mockBigQuery.getOptions()).andReturn(mockOptions).anyTimes();
        expect(mockOptions.getProjectId()).andReturn("test-project").anyTimes();
        expect(mockBigQuery.create(anyObject(JobInfo.class))).andReturn(mockJob);
        expect(mockJob.waitFor()).andReturn(mockJob);
        expect(mockJob.getStatus()).andReturn(mockStatus).anyTimes();
        expect(mockStatus.getError()).andReturn(mockError).anyTimes();

        replay(mockBigQuery, mockOptions, mockJob, mockStatus);

        // When/Then: executing should throw exception
        executeStatement.execute(mockBigQuery, mockConfiguration);
    }

    @Test(expectedExceptions = BigQueryConnectorException.class)
    public void testExecuteQueryExecuteJobNotFound() throws InterruptedException {
        // Given: an ExecuteStatement action where job disappears
        ExecuteStatement executeStatement = new ExecuteStatement();
        executeStatement.setStatement("SELECT 1");

        BigQuery mockBigQuery = createMock(BigQuery.class);
        BigQueryOptions mockOptions = createMock(BigQueryOptions.class);
        Job mockJob = createMock(Job.class);

        // Setup mock expectations - job returns null after waitFor
        expect(mockBigQuery.getOptions()).andReturn(mockOptions).anyTimes();
        expect(mockOptions.getProjectId()).andReturn("test-project").anyTimes();
        expect(mockBigQuery.create(anyObject(JobInfo.class))).andReturn(mockJob);
        expect(mockJob.waitFor()).andReturn(null);

        replay(mockBigQuery, mockOptions, mockJob);

        // When/Then: executing should throw exception
        executeStatement.execute(mockBigQuery, mockConfiguration);
    }

    @Test
    public void testExecuteQueryToString() {
        // Given: an ExecuteStatement action
        ExecuteStatement executeStatement = new ExecuteStatement();
        executeStatement.setStatement("SELECT 1");

        // When: calling toString
        String result = executeStatement.toString();

        // Then: it should contain relevant information
        assertTrue(result.contains("SELECT 1"));
        assertTrue(result.contains("ExecuteStatement"));
    }

    @Test
    public void testExecuteQueryGettersSetters() {
        // Given: an ExecuteStatement action
        ExecuteStatement executeStatement = new ExecuteStatement();

        // When: setting values
        executeStatement.setStatement("SELECT * FROM users");
        executeStatement.setDefaultDataset("production");
        executeStatement.setUseLegacySql(true);

        // Then: getters should return the values
        assertEquals(executeStatement.getStatement(), "SELECT * FROM users");
        assertEquals(executeStatement.getDefaultDataset(), "production");
        assertEquals(executeStatement.getUseLegacySql(), Boolean.TRUE);
    }

    @Test
    public void testExecuteQueryActionFactoryRegistration() {
        // Given: a YAML-like configuration with ExecuteStatement
        Map<String, Object> config = new HashMap<>();
        config.put(
                "ExecuteStatement",
                new HashMap<String, Object>() {
                    {
                        put("statement", "SELECT 1");
                    }
                });

        // When: getting the action via ActionFactory
        Action action = ActionFactory.getAction(config, mockContext);

        // Then: should successfully create ExecuteStatement instance
        assertNotNull(action);
        assertTrue(action instanceof ExecuteStatement);
    }

    @Test
    public void testExecuteQueryWithComplexQuery() {
        // Given: configuration with complex SQL query
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> executeQueryConfig = new HashMap<>();
        String complexQuery =
                "CREATE OR REPLACE TABLE users (\n"
                        + "  user_id INT64 NOT NULL,\n"
                        + "  username STRING,\n"
                        + "  email STRING,\n"
                        + "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP()\n"
                        + ")\n"
                        + "PARTITION BY DATE(created_at)\n"
                        + "CLUSTER BY user_id";
        executeQueryConfig.put("statement", complexQuery);
        executeQueryConfig.put("defaultDataset", "production");
        config.put("ExecuteStatement", executeQueryConfig);

        // When: creating an action
        Action action = ActionFactory.getAction(config, mockContext);

        // Then: complex query should be preserved
        assertNotNull(action);
        ExecuteStatement executeStatement = (ExecuteStatement) action;
        assertEquals(executeStatement.getStatement(), complexQuery);
        assertTrue(executeStatement.getStatement().contains("PARTITION BY"));
        assertTrue(executeStatement.getStatement().contains("CLUSTER BY"));
    }

    @Test
    public void testExecuteQueryWithDefaultDataset() throws InterruptedException {
        // Given: action with defaultDataset
        ExecuteStatement executeStatement = new ExecuteStatement();
        executeStatement.setStatement("SELECT * FROM users");
        executeStatement.setDefaultDataset("my_dataset");

        BigQuery mockBigQuery = createMock(BigQuery.class);
        BigQueryOptions mockOptions = createMock(BigQueryOptions.class);
        Job mockJob = createMock(Job.class);
        JobStatus mockStatus = createMock(JobStatus.class);

        // Setup mock expectations
        expect(mockBigQuery.getOptions()).andReturn(mockOptions).anyTimes();
        expect(mockOptions.getProjectId()).andReturn("test-project").anyTimes();
        expect(mockBigQuery.create(anyObject(JobInfo.class)))
                .andAnswer(
                        () -> {
                            JobInfo jobInfo = (JobInfo) getCurrentArguments()[0];
                            QueryJobConfiguration queryConfig = jobInfo.getConfiguration();
                            // Verify default dataset is set
                            assertNotNull(queryConfig.getDefaultDataset());
                            assertEquals(
                                    queryConfig.getDefaultDataset().getDataset(), "my_dataset");
                            return mockJob;
                        });
        expect(mockJob.waitFor()).andReturn(mockJob);
        expect(mockJob.getStatus()).andReturn(mockStatus).anyTimes();
        expect(mockStatus.getError()).andReturn(null);

        replay(mockBigQuery, mockOptions, mockJob, mockStatus);

        // When: executing the action
        executeStatement.execute(mockBigQuery, mockConfiguration);

        // Then: verify default dataset was used
        verify(mockBigQuery, mockOptions, mockJob, mockStatus);
    }
}
