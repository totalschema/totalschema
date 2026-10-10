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

package io.github.totalschema.connector.gcp.bigquery.actions.scheduledquery.delete;

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

/** Tests for {@link DeleteScheduledQuery} and {@link DeleteScheduledQueryActionFactory}. */
public class DeleteScheduledQueryTest {

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
    public void testDeleteScheduledQueryActionFromMapWithId() {
        // Given: a YAML-like configuration map with transferConfigId
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> deleteQueryConfig = new HashMap<>();
        deleteQueryConfig.put("transferConfigId", "my-config-id");
        deleteQueryConfig.put("location", "US");
        config.put("DeleteScheduledQuery", deleteQueryConfig);

        // When: creating an action from the config
        Action action = ActionFactory.getAction(config, mockContext);

        // Then: it should be a DeleteScheduledQuery instance with correct values
        assertNotNull(action);
        assertTrue(action instanceof DeleteScheduledQuery);

        DeleteScheduledQuery deleteQuery = (DeleteScheduledQuery) action;
        assertEquals(deleteQuery.getTransferConfigId(), "my-config-id");
        assertEquals(deleteQuery.getLocation(), "US");
        assertNull(deleteQuery.getTransferConfigName());
    }

    @Test
    public void testDeleteScheduledQueryActionFromMapWithFullName() {
        // Given: a YAML-like configuration map with transferConfigName
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> deleteQueryConfig = new HashMap<>();
        String fullName =
                "projects/my-project/locations/us/transferConfigs/12345678-1234-1234-1234-123456789012";
        deleteQueryConfig.put("transferConfigName", fullName);
        config.put("DeleteScheduledQuery", deleteQueryConfig);

        // When: creating an action from the config
        Action action = ActionFactory.getAction(config, mockContext);

        // Then: it should be a DeleteScheduledQuery instance with correct values
        assertNotNull(action);
        assertTrue(action instanceof DeleteScheduledQuery);

        DeleteScheduledQuery deleteQuery = (DeleteScheduledQuery) action;
        assertEquals(deleteQuery.getTransferConfigName(), fullName);
        assertNull(deleteQuery.getTransferConfigId());
    }

    @Test
    public void testDeleteScheduledQueryMinimalConfig() {
        // Given: minimal valid configuration (only transferConfigId)
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> deleteQueryConfig = new HashMap<>();
        deleteQueryConfig.put("transferConfigId", "minimal-id");
        config.put("DeleteScheduledQuery", deleteQueryConfig);

        // When: creating an action
        Action action = ActionFactory.getAction(config, mockContext);

        // Then: should work with minimal config
        assertNotNull(action);
        assertTrue(action instanceof DeleteScheduledQuery);

        DeleteScheduledQuery deleteQuery = (DeleteScheduledQuery) action;
        assertEquals(deleteQuery.getTransferConfigId(), "minimal-id");
        assertNull(deleteQuery.getLocation());
    }

    @Test
    public void testDeleteScheduledQueryWithEULocation() {
        // Given: configuration with EU location
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> deleteQueryConfig = new HashMap<>();
        deleteQueryConfig.put("transferConfigId", "eu-config-id");
        deleteQueryConfig.put("location", "EU");
        config.put("DeleteScheduledQuery", deleteQueryConfig);

        // When: creating an action
        Action action = ActionFactory.getAction(config, mockContext);

        // Then: should have EU location set
        assertNotNull(action);
        DeleteScheduledQuery deleteQuery = (DeleteScheduledQuery) action;
        assertEquals(deleteQuery.getLocation(), "EU");
    }

    @Test
    public void testDeleteScheduledQueryGettersSetters() {
        // Given: a DeleteScheduledQuery action
        DeleteScheduledQuery deleteQuery = new DeleteScheduledQuery();

        // When: setting values
        deleteQuery.setTransferConfigId("my-id");
        deleteQuery.setLocation("US");
        deleteQuery.setTransferConfigName("full/resource/name");

        // Then: getters should return the values
        assertEquals(deleteQuery.getTransferConfigId(), "my-id");
        assertEquals(deleteQuery.getLocation(), "US");
        assertEquals(deleteQuery.getTransferConfigName(), "full/resource/name");
    }

    @Test
    public void testDeleteScheduledQueryToString() {
        // Given: a DeleteScheduledQuery action
        DeleteScheduledQuery deleteQuery = new DeleteScheduledQuery();
        deleteQuery.setTransferConfigId("test-id");
        deleteQuery.setLocation("US");

        // When: calling toString
        String result = deleteQuery.toString();

        // Then: it should contain relevant information
        assertTrue(result.contains("test-id"));
        assertTrue(result.contains("DeleteScheduledQuery"));
        assertTrue(result.contains("US"));
    }

    @Test
    public void testDeleteScheduledQueryActionFactoryRegistration() {
        // Given: a YAML-like configuration with DeleteScheduledQuery
        Map<String, Object> config = new HashMap<>();
        config.put(
                "DeleteScheduledQuery",
                new HashMap<String, Object>() {
                    {
                        put("transferConfigId", "test");
                    }
                });

        // When: getting the action via ActionFactory
        Action action = ActionFactory.getAction(config, mockContext);

        // Then: should successfully create DeleteScheduledQuery instance
        assertNotNull(action);
        assertTrue(action instanceof DeleteScheduledQuery);
    }

    @Test
    public void testDeleteScheduledQueryWithGuidFormat() {
        // Given: configuration with GUID format ID
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> deleteQueryConfig = new HashMap<>();
        deleteQueryConfig.put("transferConfigId", "12345678-1234-1234-1234-123456789012");
        deleteQueryConfig.put("location", "US");
        config.put("DeleteScheduledQuery", deleteQueryConfig);

        // When: creating an action
        Action action = ActionFactory.getAction(config, mockContext);

        // Then: should preserve GUID format
        assertNotNull(action);
        DeleteScheduledQuery deleteQuery = (DeleteScheduledQuery) action;
        assertEquals(deleteQuery.getTransferConfigId(), "12345678-1234-1234-1234-123456789012");
    }

    @Test
    public void testDeleteScheduledQueryNullValues() {
        // Given: a DeleteScheduledQuery action with null optional fields
        DeleteScheduledQuery deleteQuery = new DeleteScheduledQuery();
        deleteQuery.setTransferConfigId("test-id");

        // Then: optional fields should be null
        assertNull(deleteQuery.getLocation());
        assertNull(deleteQuery.getTransferConfigName());
    }

    @Test
    public void testDeleteScheduledQueryBothIdAndNameSet() {
        // Given: configuration with both ID and full name (name should take precedence)
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> deleteQueryConfig = new HashMap<>();
        deleteQueryConfig.put("transferConfigId", "my-id");
        deleteQueryConfig.put(
                "transferConfigName",
                "projects/proj/locations/us/transferConfigs/12345678-1234-1234-1234-123456789012");
        config.put("DeleteScheduledQuery", deleteQueryConfig);

        // When: creating an action
        Action action = ActionFactory.getAction(config, mockContext);

        // Then: both should be set (execute method will prefer transferConfigName)
        assertNotNull(action);
        DeleteScheduledQuery deleteQuery = (DeleteScheduledQuery) action;
        assertEquals(deleteQuery.getTransferConfigId(), "my-id");
        assertNotNull(deleteQuery.getTransferConfigName());
    }

    @Test
    public void testDeleteScheduledQueryEmptyValues() {
        // Given: a DeleteScheduledQuery action with empty strings
        DeleteScheduledQuery deleteQuery = new DeleteScheduledQuery();
        deleteQuery.setTransferConfigId("");
        deleteQuery.setLocation("");
        deleteQuery.setTransferConfigName("");

        // Then: empty strings should be preserved
        assertEquals(deleteQuery.getTransferConfigId(), "");
        assertEquals(deleteQuery.getLocation(), "");
        assertEquals(deleteQuery.getTransferConfigName(), "");
    }

    @Test
    public void testDeleteScheduledQueryWithMultiRegionLocations() {
        // Test various location formats
        String[] locations = {"US", "EU", "us-central1", "europe-west1", "asia-east1"};

        for (String location : locations) {
            Map<String, Object> config = new HashMap<>();
            Map<String, Object> deleteQueryConfig = new HashMap<>();
            deleteQueryConfig.put("transferConfigId", "test-id");
            deleteQueryConfig.put("location", location);
            config.put("DeleteScheduledQuery", deleteQueryConfig);

            Action action = ActionFactory.getAction(config, mockContext);
            DeleteScheduledQuery deleteQuery = (DeleteScheduledQuery) action;
            assertEquals(deleteQuery.getLocation(), location);
        }
    }

    @Test
    public void testDeleteScheduledQueryWithVariousResourceNames() {
        // Test various full resource name formats
        String[] resourceNames = {
            "projects/my-project/locations/us/transferConfigs/abc123",
            "projects/test-project/locations/eu/transferConfigs/12345678-1234-1234-1234-123456789012",
            "projects/prod-project/locations/us-central1/transferConfigs/config-name"
        };

        for (String resourceName : resourceNames) {
            Map<String, Object> config = new HashMap<>();
            Map<String, Object> deleteQueryConfig = new HashMap<>();
            deleteQueryConfig.put("transferConfigName", resourceName);
            config.put("DeleteScheduledQuery", deleteQueryConfig);

            Action action = ActionFactory.getAction(config, mockContext);
            DeleteScheduledQuery deleteQuery = (DeleteScheduledQuery) action;
            assertEquals(deleteQuery.getTransferConfigName(), resourceName);
        }
    }
}
