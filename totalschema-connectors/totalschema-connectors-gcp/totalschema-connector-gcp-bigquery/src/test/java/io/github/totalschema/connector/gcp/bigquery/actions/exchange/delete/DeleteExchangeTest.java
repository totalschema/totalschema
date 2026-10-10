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

package io.github.totalschema.connector.gcp.bigquery.actions.exchange.delete;

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

/** Tests for {@link DeleteExchange} and {@link DeleteExchangeActionFactory}. */
public class DeleteExchangeTest {

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
    public void testDeleteExchangeActionFromMap() {
        // Given: a YAML-like configuration map with all fields
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> deleteExchangeConfig = new HashMap<>();
        deleteExchangeConfig.put("project", "my-project");
        deleteExchangeConfig.put("location", "US");
        deleteExchangeConfig.put("dataExchangeId", "my_exchange");
        config.put("DeleteExchange", deleteExchangeConfig);

        // When: creating an action from the config
        Action action = ActionFactory.getAction(config, mockContext);

        // Then: it should be a DeleteExchange instance with correct values
        assertNotNull(action);
        assertTrue(action instanceof DeleteExchange);

        DeleteExchange deleteExchange = (DeleteExchange) action;
        assertEquals(deleteExchange.getProject(), "my-project");
        assertEquals(deleteExchange.getLocation(), "US");
        assertEquals(deleteExchange.getDataExchangeId(), "my_exchange");
    }

    @Test
    public void testDeleteExchangeMinimalConfig() {
        // Given: minimal valid configuration (only dataExchangeId)
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> deleteExchangeConfig = new HashMap<>();
        deleteExchangeConfig.put("dataExchangeId", "minimal_exchange");
        config.put("DeleteExchange", deleteExchangeConfig);

        // When: creating an action
        Action action = ActionFactory.getAction(config, mockContext);

        // Then: should work with minimal config
        assertNotNull(action);
        assertTrue(action instanceof DeleteExchange);

        DeleteExchange deleteExchange = (DeleteExchange) action;
        assertEquals(deleteExchange.getDataExchangeId(), "minimal_exchange");
        assertNull(deleteExchange.getProject());
        assertNull(deleteExchange.getLocation());
    }

    @Test
    public void testDeleteExchangeWithEULocation() {
        // Given: configuration with EU location
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> deleteExchangeConfig = new HashMap<>();
        deleteExchangeConfig.put("dataExchangeId", "eu_exchange");
        deleteExchangeConfig.put("location", "EU");
        config.put("DeleteExchange", deleteExchangeConfig);

        // When: creating an action
        Action action = ActionFactory.getAction(config, mockContext);

        // Then: should have EU location set
        assertNotNull(action);
        DeleteExchange deleteExchange = (DeleteExchange) action;
        assertEquals(deleteExchange.getLocation(), "EU");
    }

    @Test
    public void testDeleteExchangeGettersSetters() {
        // Given: a DeleteExchange action
        DeleteExchange deleteExchange = new DeleteExchange();

        // When: setting values
        deleteExchange.setProject("test-project");
        deleteExchange.setLocation("US");
        deleteExchange.setDataExchangeId("exchange_id");

        // Then: getters should return the values
        assertEquals(deleteExchange.getProject(), "test-project");
        assertEquals(deleteExchange.getLocation(), "US");
        assertEquals(deleteExchange.getDataExchangeId(), "exchange_id");
    }

    @Test
    public void testDeleteExchangeToString() {
        // Given: a DeleteExchange action
        DeleteExchange deleteExchange = new DeleteExchange();
        deleteExchange.setProject("test-project");
        deleteExchange.setLocation("US");
        deleteExchange.setDataExchangeId("exchange_id");

        // When: calling toString
        String result = deleteExchange.toString();

        // Then: it should contain relevant information
        assertTrue(result.contains("DeleteExchange"));
        assertTrue(result.contains("test-project"));
        assertTrue(result.contains("US"));
        assertTrue(result.contains("exchange_id"));
    }

    @Test
    public void testDeleteExchangeActionFactoryRegistration() {
        // Given: a YAML-like configuration with DeleteExchange
        Map<String, Object> config = new HashMap<>();
        config.put(
                "DeleteExchange",
                new HashMap<String, Object>() {
                    {
                        put("dataExchangeId", "exchange");
                    }
                });

        // When: getting the action via ActionFactory
        Action action = ActionFactory.getAction(config, mockContext);

        // Then: should successfully create DeleteExchange instance
        assertNotNull(action);
        assertTrue(action instanceof DeleteExchange);
    }

    @Test
    public void testDeleteExchangeNullValues() {
        // Given: a DeleteExchange action with only required field
        DeleteExchange deleteExchange = new DeleteExchange();
        deleteExchange.setDataExchangeId("exchange_id");

        // Then: optional fields should be null
        assertNull(deleteExchange.getProject());
        assertNull(deleteExchange.getLocation());
    }

    @Test
    public void testDeleteExchangeEmptyValues() {
        // Given: a DeleteExchange action with empty strings
        DeleteExchange deleteExchange = new DeleteExchange();
        deleteExchange.setProject("");
        deleteExchange.setLocation("");
        deleteExchange.setDataExchangeId("");

        // Then: empty strings should be preserved
        assertEquals(deleteExchange.getProject(), "");
        assertEquals(deleteExchange.getLocation(), "");
        assertEquals(deleteExchange.getDataExchangeId(), "");
    }

    @Test
    public void testDeleteExchangeWithMultiRegionLocations() {
        // Test various location formats
        String[] locations = {"US", "EU", "us-central1", "europe-west1", "asia-east1"};

        for (String location : locations) {
            Map<String, Object> config = new HashMap<>();
            Map<String, Object> deleteExchangeConfig = new HashMap<>();
            deleteExchangeConfig.put("dataExchangeId", "exchange");
            deleteExchangeConfig.put("location", location);
            config.put("DeleteExchange", deleteExchangeConfig);

            Action action = ActionFactory.getAction(config, mockContext);
            DeleteExchange deleteExchange = (DeleteExchange) action;
            assertEquals(deleteExchange.getLocation(), location);
        }
    }

    @Test
    public void testDeleteExchangeWithVariousDataExchangeIds() {
        // Test various ID formats
        String[] dataExchangeIds = {
            "exchange1", "my-data-exchange", "12345678-1234-1234-1234-123456789012"
        };

        for (String dataExchangeId : dataExchangeIds) {
            Map<String, Object> config = new HashMap<>();
            Map<String, Object> deleteExchangeConfig = new HashMap<>();
            deleteExchangeConfig.put("dataExchangeId", dataExchangeId);
            config.put("DeleteExchange", deleteExchangeConfig);

            Action action = ActionFactory.getAction(config, mockContext);
            DeleteExchange deleteExchange = (DeleteExchange) action;
            assertEquals(deleteExchange.getDataExchangeId(), dataExchangeId);
        }
    }
}
