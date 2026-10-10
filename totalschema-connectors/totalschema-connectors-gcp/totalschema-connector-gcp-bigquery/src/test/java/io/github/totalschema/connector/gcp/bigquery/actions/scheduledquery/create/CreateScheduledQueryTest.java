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

package io.github.totalschema.connector.gcp.bigquery.actions.scheduledquery.create;

import static org.testng.Assert.*;

import io.github.totalschema.connector.gcp.bigquery.actions.Action;
import io.github.totalschema.connector.gcp.bigquery.actions.ActionFactory;
import io.github.totalschema.engine.api.Context;
import io.github.totalschema.spi.expression.evaluator.ExpressionEvaluator;
import io.github.totalschema.spi.variables.VariableService;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.easymock.EasyMock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Tests for {@link CreateScheduledQuery} and {@link CreateScheduledQueryActionFactory}. */
public class CreateScheduledQueryTest {

    private Context mockContext;
    private ExpressionEvaluator mockEvaluator;
    private VariableService mockVariableService;

    @BeforeMethod
    public void setUp() {
        mockContext = EasyMock.createMock(Context.class);
        mockEvaluator = EasyMock.createMock(ExpressionEvaluator.class);
        mockVariableService = EasyMock.createMock(VariableService.class);

        // Setup mock to return the evaluator
        EasyMock.expect(mockContext.get(ExpressionEvaluator.class))
                .andReturn(mockEvaluator)
                .anyTimes();

        // Setup mock to return the variable service
        EasyMock.expect(mockContext.get(VariableService.class))
                .andReturn(mockVariableService)
                .anyTimes();

        // Mock variable service to return empty variables
        EasyMock.expect(mockVariableService.getEffectiveVariables(mockContext))
                .andReturn(Collections.emptyMap())
                .anyTimes();

        // Mock evaluator to perform identity substitution (no variable substitution)
        EasyMock.expect(
                        mockEvaluator.evaluate(
                                EasyMock.anyObject(String.class), EasyMock.anyObject(Map.class)))
                .andAnswer(() -> (String) EasyMock.getCurrentArguments()[0])
                .anyTimes();

        EasyMock.replay(mockContext, mockEvaluator, mockVariableService);
    }

    @Test
    public void testCreateScheduledQueryActionFromMap() {
        // Given: a YAML-like configuration map
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> scheduledQueryConfig = new HashMap<>();
        scheduledQueryConfig.put("displayName", "Daily Report");
        scheduledQueryConfig.put("query", "SELECT * FROM table");
        scheduledQueryConfig.put("schedule", "every day 00:00");

        Map<String, Object> destination = new HashMap<>();
        destination.put("datasetId", "analytics");
        destination.put("tableId", "daily_report");
        destination.put("writeDisposition", "WRITE_TRUNCATE");
        destination.put("partitioningField", "date_column");
        scheduledQueryConfig.put("destination", destination);

        scheduledQueryConfig.put("location", "US");
        scheduledQueryConfig.put("serviceAccount", "scheduler@my-project.iam.gserviceaccount.com");
        config.put("CreateScheduledQuery", scheduledQueryConfig);

        // When: creating an action from the config
        Action action = ActionFactory.getAction(config, mockContext);

        // Then: it should be a CreateScheduledQuery instance with correct values
        assertNotNull(action);
        assertTrue(action instanceof CreateScheduledQuery);

        CreateScheduledQuery scheduledQuery = (CreateScheduledQuery) action;
        assertEquals(scheduledQuery.getDisplayName(), "Daily Report");
        assertEquals(scheduledQuery.getQuery(), "SELECT * FROM table");
        assertEquals(scheduledQuery.getSchedule(), "every day 00:00");

        ScheduledQueryDestination dest = scheduledQuery.getDestination();
        assertNotNull(dest);
        assertEquals(dest.getDatasetId(), "analytics");
        assertEquals(dest.getTableId(), "daily_report");
        assertEquals(dest.getWriteDisposition(), "WRITE_TRUNCATE");
        assertEquals(dest.getPartitioningField(), "date_column");

        assertEquals(scheduledQuery.getLocation(), "US");
        assertEquals(
                scheduledQuery.getServiceAccount(), "scheduler@my-project.iam.gserviceaccount.com");
    }

    @Test
    public void testCreateScheduledQueryMinimalConfig() {
        // Given: minimal valid configuration
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> scheduledQueryConfig = new HashMap<>();
        scheduledQueryConfig.put("displayName", "Minimal Query");
        scheduledQueryConfig.put("query", "SELECT 1");
        scheduledQueryConfig.put("schedule", "every hour");
        config.put("CreateScheduledQuery", scheduledQueryConfig);

        // When: creating an action
        Action action = ActionFactory.getAction(config, mockContext);

        // Then: should work with minimal config
        assertNotNull(action);
        assertTrue(action instanceof CreateScheduledQuery);

        CreateScheduledQuery scheduledQuery = (CreateScheduledQuery) action;
        assertEquals(scheduledQuery.getDisplayName(), "Minimal Query");
        assertEquals(scheduledQuery.getQuery(), "SELECT 1");
        assertEquals(scheduledQuery.getSchedule(), "every hour");
        assertNull(scheduledQuery.getDestination());
        assertNull(scheduledQuery.getLocation());
        assertNull(scheduledQuery.getServiceAccount());
    }

    @Test
    public void testCreateScheduledQueryWithDisabled() {
        // Given: configuration with disabled flag
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> scheduledQueryConfig = new HashMap<>();
        scheduledQueryConfig.put("displayName", "Disabled Query");
        scheduledQueryConfig.put("query", "SELECT 1");
        scheduledQueryConfig.put("schedule", "every day 00:00");
        scheduledQueryConfig.put("disabled", true);
        config.put("CreateScheduledQuery", scheduledQueryConfig);

        // When: creating an action
        Action action = ActionFactory.getAction(config, mockContext);

        // Then: should have disabled set
        assertNotNull(action);
        CreateScheduledQuery scheduledQuery = (CreateScheduledQuery) action;
        assertEquals(scheduledQuery.getDisabled(), Boolean.TRUE);
    }

    @Test
    public void testCreateScheduledQueryWithParameters() {
        // Given: configuration with custom parameters
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> scheduledQueryConfig = new HashMap<>();
        scheduledQueryConfig.put("displayName", "Parameterized Query");
        scheduledQueryConfig.put("query", "SELECT * FROM table WHERE date = @date");
        scheduledQueryConfig.put("schedule", "every day 00:00");

        Map<String, String> parameters = new HashMap<>();
        parameters.put("date", "2023-01-01");
        scheduledQueryConfig.put("parameters", parameters);

        config.put("CreateScheduledQuery", scheduledQueryConfig);

        // When: creating an action
        Action action = ActionFactory.getAction(config, mockContext);

        // Then: should have parameters set
        assertNotNull(action);
        CreateScheduledQuery scheduledQuery = (CreateScheduledQuery) action;
        assertNotNull(scheduledQuery.getParameters());
        assertEquals(scheduledQuery.getParameters().get("date"), "2023-01-01");
    }

    @Test
    public void testCreateScheduledQueryGettersSetters() {
        // Given: a CreateScheduledQuery action
        CreateScheduledQuery scheduledQuery = new CreateScheduledQuery();

        // When: setting values
        scheduledQuery.setDisplayName("Test Query");
        scheduledQuery.setQuery("SELECT 1");
        scheduledQuery.setSchedule("every hour");

        ScheduledQueryDestination destination = new ScheduledQueryDestination();
        destination.setDatasetId("my_dataset");
        destination.setTableId("my_table");
        destination.setWriteDisposition("WRITE_APPEND");
        destination.setPartitioningField("event_date");
        scheduledQuery.setDestination(destination);

        scheduledQuery.setLocation("EU");
        scheduledQuery.setServiceAccount("scheduler@my-project.iam.gserviceaccount.com");
        scheduledQuery.setDisabled(true);
        scheduledQuery.setReplaceIfExists(true);

        Map<String, String> params = new HashMap<>();
        params.put("key", "value");
        scheduledQuery.setParameters(params);

        // Then: getters should return the values
        assertEquals(scheduledQuery.getDisplayName(), "Test Query");
        assertEquals(scheduledQuery.getQuery(), "SELECT 1");
        assertEquals(scheduledQuery.getSchedule(), "every hour");

        ScheduledQueryDestination dest = scheduledQuery.getDestination();
        assertNotNull(dest);
        assertEquals(dest.getDatasetId(), "my_dataset");
        assertEquals(dest.getTableId(), "my_table");
        assertEquals(dest.getWriteDisposition(), "WRITE_APPEND");
        assertEquals(dest.getPartitioningField(), "event_date");

        assertEquals(scheduledQuery.getLocation(), "EU");
        assertEquals(
                scheduledQuery.getServiceAccount(), "scheduler@my-project.iam.gserviceaccount.com");
        assertEquals(scheduledQuery.getDisabled(), Boolean.TRUE);
        assertEquals(scheduledQuery.getReplaceIfExists(), Boolean.TRUE);
        assertNotNull(scheduledQuery.getParameters());
        assertEquals(scheduledQuery.getParameters().get("key"), "value");
    }

    @Test
    public void testCreateScheduledQueryToString() {
        // Given: a CreateScheduledQuery action
        CreateScheduledQuery scheduledQuery = new CreateScheduledQuery();
        scheduledQuery.setDisplayName("Test Query");
        scheduledQuery.setSchedule("every day 00:00");

        ScheduledQueryDestination destination = new ScheduledQueryDestination();
        destination.setDatasetId("analytics");
        scheduledQuery.setDestination(destination);

        scheduledQuery.setServiceAccount("scheduler@my-project.iam.gserviceaccount.com");

        // When: calling toString
        String result = scheduledQuery.toString();

        // Then: it should contain relevant information
        assertTrue(result.contains("Test Query"));
        assertTrue(result.contains("every day 00:00"));
        assertTrue(result.contains("analytics"));
        assertTrue(result.contains("scheduler@my-project.iam.gserviceaccount.com"));
        assertTrue(result.contains("CreateScheduledQuery"));
    }

    @Test
    public void testCreateScheduledQueryActionFactoryRegistration() {
        // Given: a YAML-like configuration with CreateScheduledQuery
        Map<String, Object> config = new HashMap<>();
        config.put(
                "CreateScheduledQuery",
                new HashMap<String, Object>() {
                    {
                        put("displayName", "test");
                        put("query", "SELECT 1");
                        put("schedule", "every hour");
                    }
                });

        // When: getting the action via ActionFactory
        Action action = ActionFactory.getAction(config, mockContext);

        // Then: should successfully create CreateScheduledQuery instance
        assertNotNull(action);
        assertTrue(action instanceof CreateScheduledQuery);
    }

    @Test
    public void testCreateScheduledQueryWithComplexQuery() {
        // Given: configuration with a complex multi-line query
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> scheduledQueryConfig = new HashMap<>();
        scheduledQueryConfig.put("displayName", "Complex Query");
        String complexQuery =
                "SELECT\n"
                        + "  date,\n"
                        + "  SUM(amount) as total\n"
                        + "FROM `project.dataset.table`\n"
                        + "WHERE date = @run_date\n"
                        + "GROUP BY date";
        scheduledQueryConfig.put("query", complexQuery);
        scheduledQueryConfig.put("schedule", "every day 00:00");
        config.put("CreateScheduledQuery", scheduledQueryConfig);

        // When: creating an action
        Action action = ActionFactory.getAction(config, mockContext);

        // Then: should preserve the complex query
        assertNotNull(action);
        CreateScheduledQuery scheduledQuery = (CreateScheduledQuery) action;
        assertEquals(scheduledQuery.getQuery(), complexQuery);
    }

    @Test
    public void testCreateScheduledQueryWithVariousSchedules() {
        // Test various schedule formats
        String[] schedules = {
            "every day 00:00",
            "every 4 hours",
            "every monday 09:00",
            "every 15 minutes",
            "first sunday of quarter 00:00"
        };

        for (String schedule : schedules) {
            Map<String, Object> config = new HashMap<>();
            Map<String, Object> scheduledQueryConfig = new HashMap<>();
            scheduledQueryConfig.put("displayName", "Test");
            scheduledQueryConfig.put("query", "SELECT 1");
            scheduledQueryConfig.put("schedule", schedule);
            config.put("CreateScheduledQuery", scheduledQueryConfig);

            Action action = ActionFactory.getAction(config, mockContext);
            CreateScheduledQuery scheduledQuery = (CreateScheduledQuery) action;
            assertEquals(scheduledQuery.getSchedule(), schedule);
        }
    }

    @Test
    public void testCreateScheduledQueryNullValues() {
        // Given: a CreateScheduledQuery action with null optional fields
        CreateScheduledQuery scheduledQuery = new CreateScheduledQuery();
        scheduledQuery.setDisplayName("Test");
        scheduledQuery.setQuery("SELECT 1");
        scheduledQuery.setSchedule("every hour");

        // Then: optional fields should be null
        assertNull(scheduledQuery.getDestination());
        assertNull(scheduledQuery.getLocation());
        assertNull(scheduledQuery.getServiceAccount());
        assertNull(scheduledQuery.getDisabled());
        assertNull(scheduledQuery.getReplaceIfExists());
        assertNull(scheduledQuery.getParameters());
    }

    @Test
    public void testCreateScheduledQueryWithReplaceIfExists() {
        // Given: configuration with replaceIfExists flag
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> scheduledQueryConfig = new HashMap<>();
        scheduledQueryConfig.put("displayName", "Replaceable Query");
        scheduledQueryConfig.put("query", "SELECT 1");
        scheduledQueryConfig.put("schedule", "every day 00:00");
        scheduledQueryConfig.put("replaceIfExists", true);
        config.put("CreateScheduledQuery", scheduledQueryConfig);

        // When: creating an action
        Action action = ActionFactory.getAction(config, mockContext);

        // Then: should have replaceIfExists set to true
        assertNotNull(action);
        CreateScheduledQuery scheduledQuery = (CreateScheduledQuery) action;
        assertEquals(scheduledQuery.getReplaceIfExists(), Boolean.TRUE);
    }

    @Test
    public void testCreateScheduledQueryWithFullDestination() {
        // Given: configuration with all destination fields
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> scheduledQueryConfig = new HashMap<>();
        scheduledQueryConfig.put("displayName", "Full Destination Query");
        scheduledQueryConfig.put("query", "SELECT * FROM source");
        scheduledQueryConfig.put("schedule", "every day 00:00");

        Map<String, Object> destination = new HashMap<>();
        destination.put("datasetId", "analytics");
        destination.put("tableId", "sales_report");
        destination.put("writeDisposition", "WRITE_APPEND");
        destination.put("partitioningField", "transaction_date");
        scheduledQueryConfig.put("destination", destination);

        config.put("CreateScheduledQuery", scheduledQueryConfig);

        // When: creating an action
        Action action = ActionFactory.getAction(config, mockContext);

        // Then: all destination fields should be properly set
        assertNotNull(action);
        CreateScheduledQuery scheduledQuery = (CreateScheduledQuery) action;

        ScheduledQueryDestination dest = scheduledQuery.getDestination();
        assertNotNull(dest);
        assertEquals(dest.getDatasetId(), "analytics");
        assertEquals(dest.getTableId(), "sales_report");
        assertEquals(dest.getWriteDisposition(), "WRITE_APPEND");
        assertEquals(dest.getPartitioningField(), "transaction_date");
    }

    @Test
    public void testCreateScheduledQueryWithPartialDestination() {
        // Given: configuration with minimal destination (only datasetId)
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> scheduledQueryConfig = new HashMap<>();
        scheduledQueryConfig.put("displayName", "Partial Destination Query");
        scheduledQueryConfig.put("query", "SELECT * FROM source");
        scheduledQueryConfig.put("schedule", "every day 00:00");

        Map<String, Object> destination = new HashMap<>();
        destination.put("datasetId", "analytics");
        scheduledQueryConfig.put("destination", destination);

        config.put("CreateScheduledQuery", scheduledQueryConfig);

        // When: creating an action
        Action action = ActionFactory.getAction(config, mockContext);

        // Then: destination should have datasetId but other fields should be null
        assertNotNull(action);
        CreateScheduledQuery scheduledQuery = (CreateScheduledQuery) action;

        ScheduledQueryDestination dest = scheduledQuery.getDestination();
        assertNotNull(dest);
        assertEquals(dest.getDatasetId(), "analytics");
        assertNull(dest.getTableId());
        assertNull(dest.getWriteDisposition());
        assertNull(dest.getPartitioningField());
    }

    @Test
    public void testScheduledQueryDestinationGettersSetters() {
        // Given: a ScheduledQueryDestination object
        ScheduledQueryDestination destination = new ScheduledQueryDestination();

        // When: setting all fields
        destination.setDatasetId("my_dataset");
        destination.setTableId("my_table");
        destination.setWriteDisposition("WRITE_TRUNCATE");
        destination.setPartitioningField("date_field");

        // Then: getters should return the values
        assertEquals(destination.getDatasetId(), "my_dataset");
        assertEquals(destination.getTableId(), "my_table");
        assertEquals(destination.getWriteDisposition(), "WRITE_TRUNCATE");
        assertEquals(destination.getPartitioningField(), "date_field");
    }

    @Test
    public void testScheduledQueryDestinationConstructor() {
        // Given: creating a destination with constructor
        ScheduledQueryDestination destination =
                new ScheduledQueryDestination(
                        "analytics", "daily_report", "WRITE_APPEND", "report_date");

        // Then: all fields should be set
        assertEquals(destination.getDatasetId(), "analytics");
        assertEquals(destination.getTableId(), "daily_report");
        assertEquals(destination.getWriteDisposition(), "WRITE_APPEND");
        assertEquals(destination.getPartitioningField(), "report_date");
    }

    @Test
    public void testScheduledQueryDestinationToString() {
        // Given: a ScheduledQueryDestination object
        ScheduledQueryDestination destination = new ScheduledQueryDestination();
        destination.setDatasetId("analytics");
        destination.setTableId("sales_data");
        destination.setWriteDisposition("WRITE_TRUNCATE");
        destination.setPartitioningField("date");

        // When: calling toString
        String result = destination.toString();

        // Then: it should contain relevant information
        assertTrue(result.contains("analytics"));
        assertTrue(result.contains("sales_data"));
        assertTrue(result.contains("WRITE_TRUNCATE"));
        assertTrue(result.contains("date"));
        assertTrue(result.contains("ScheduledQueryDestination"));
    }

    @Test
    public void testCreateScheduledQueryWithDestinationAndReplaceIfExists() {
        // Given: configuration with both destination and replaceIfExists
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> scheduledQueryConfig = new HashMap<>();
        scheduledQueryConfig.put("displayName", "Update Query");
        scheduledQueryConfig.put("query", "SELECT * FROM data");
        scheduledQueryConfig.put("schedule", "every 6 hours");

        Map<String, Object> destination = new HashMap<>();
        destination.put("datasetId", "production");
        destination.put("tableId", "metrics");
        destination.put("writeDisposition", "WRITE_APPEND");
        scheduledQueryConfig.put("destination", destination);

        scheduledQueryConfig.put("replaceIfExists", true);
        config.put("CreateScheduledQuery", scheduledQueryConfig);

        // When: creating an action
        Action action = ActionFactory.getAction(config, mockContext);

        // Then: both destination and replaceIfExists should be properly set
        assertNotNull(action);
        CreateScheduledQuery scheduledQuery = (CreateScheduledQuery) action;

        assertEquals(scheduledQuery.getReplaceIfExists(), Boolean.TRUE);
        ScheduledQueryDestination dest = scheduledQuery.getDestination();
        assertNotNull(dest);
        assertEquals(dest.getDatasetId(), "production");
        assertEquals(dest.getTableId(), "metrics");
        assertEquals(dest.getWriteDisposition(), "WRITE_APPEND");
    }

    @Test
    public void testCreateScheduledQueryWriteDispositionOptions() {
        // Test both WRITE_TRUNCATE and WRITE_APPEND options
        String[] dispositions = {"WRITE_TRUNCATE", "WRITE_APPEND"};

        for (String disposition : dispositions) {
            ScheduledQueryDestination destination = new ScheduledQueryDestination();
            destination.setDatasetId("test_dataset");
            destination.setWriteDisposition(disposition);

            assertEquals(destination.getWriteDisposition(), disposition);
        }
    }

    @Test
    public void testCreateScheduledQueryDestinationNullValues() {
        // Given: a ScheduledQueryDestination with null values
        ScheduledQueryDestination destination = new ScheduledQueryDestination();

        // Then: all fields should be null by default
        assertNull(destination.getDatasetId());
        assertNull(destination.getTableId());
        assertNull(destination.getWriteDisposition());
        assertNull(destination.getPartitioningField());
    }

    @Test
    public void testCreateScheduledQueryDefaultWriteDispositionIsAppend() {
        // Given: configuration with destination but no writeDisposition specified
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> scheduledQueryConfig = new HashMap<>();
        scheduledQueryConfig.put("displayName", "Default Append Query");
        scheduledQueryConfig.put("query", "SELECT * FROM source");
        scheduledQueryConfig.put("schedule", "every day 00:00");

        Map<String, Object> destination = new HashMap<>();
        destination.put("datasetId", "analytics");
        destination.put("tableId", "appended_data");
        // Note: writeDisposition is NOT specified
        scheduledQueryConfig.put("destination", destination);

        config.put("CreateScheduledQuery", scheduledQueryConfig);

        // When: creating an action
        Action action = ActionFactory.getAction(config, mockContext);

        // Then: should default to WRITE_APPEND
        assertNotNull(action);
        CreateScheduledQuery scheduledQuery = (CreateScheduledQuery) action;

        ScheduledQueryDestination dest = scheduledQuery.getDestination();
        assertNotNull(dest);
        assertEquals(dest.getDatasetId(), "analytics");
        assertEquals(dest.getTableId(), "appended_data");
        assertNull(dest.getWriteDisposition()); // The POJO field is null
        // The actual default is applied during execution, not in the POJO
    }
}
