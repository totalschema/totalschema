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

package io.github.totalschema.connector.gcp.bigquery.actions.exchange.create;

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

/** Tests for {@link CreateExchange} and {@link CreateExchangeActionFactory}. */
public class CreateExchangeTest {

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
    public void testCreateExchangeActionFromMap() {
        // Given: a YAML-like configuration map
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> exchangeConfig = new HashMap<>();
        exchangeConfig.put("project", "my-project");
        exchangeConfig.put("location", "US");
        exchangeConfig.put("dataExchangeId", "my_exchange");
        exchangeConfig.put("displayName", "My Data Exchange");
        exchangeConfig.put("description", "Exchange for sharing analytics datasets");
        exchangeConfig.put("primaryContact", "contact@example.com");
        exchangeConfig.put("documentation", "https://example.com/docs");
        config.put("CreateExchange", exchangeConfig);

        // When: creating an action from the config
        Action action = ActionFactory.getAction(config, mockContext);

        // Then: it should be a CreateExchange instance with correct values
        assertNotNull(action);
        assertTrue(action instanceof CreateExchange);

        CreateExchange createExchange = (CreateExchange) action;
        assertEquals(createExchange.getProject(), "my-project");
        assertEquals(createExchange.getLocation(), "US");
        assertEquals(createExchange.getDataExchangeId(), "my_exchange");
        assertEquals(createExchange.getDisplayName(), "My Data Exchange");
        assertEquals(createExchange.getDescription(), "Exchange for sharing analytics datasets");
        assertEquals(createExchange.getPrimaryContact(), "contact@example.com");
        assertEquals(createExchange.getDocumentation(), "https://example.com/docs");
    }

    @Test
    public void testCreateExchangeMinimalConfig() {
        // Given: minimal valid configuration
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> exchangeConfig = new HashMap<>();
        exchangeConfig.put("dataExchangeId", "my_exchange");
        config.put("CreateExchange", exchangeConfig);

        // When: creating an action
        Action action = ActionFactory.getAction(config, mockContext);

        // Then: should work with minimal config
        assertNotNull(action);
        assertTrue(action instanceof CreateExchange);

        CreateExchange createExchange = (CreateExchange) action;
        assertEquals(createExchange.getDataExchangeId(), "my_exchange");
        assertNull(createExchange.getProject());
        assertNull(createExchange.getLocation());
        assertNull(createExchange.getDisplayName());
        assertNull(createExchange.getDescription());
        assertNull(createExchange.getPrimaryContact());
        assertNull(createExchange.getDocumentation());
    }

    @Test
    public void testCreateExchangeGettersSetters() {
        // Given: a CreateExchange action
        CreateExchange createExchange = new CreateExchange();

        // When: setting values
        createExchange.setProject("test-project");
        createExchange.setLocation("EU");
        createExchange.setDataExchangeId("exchange_id");
        createExchange.setDisplayName("Test Exchange");
        createExchange.setDescription("Test description");
        createExchange.setPrimaryContact("owner@example.com");
        createExchange.setDocumentation("https://docs.example.com");

        // Then: getters should return the values
        assertEquals(createExchange.getProject(), "test-project");
        assertEquals(createExchange.getLocation(), "EU");
        assertEquals(createExchange.getDataExchangeId(), "exchange_id");
        assertEquals(createExchange.getDisplayName(), "Test Exchange");
        assertEquals(createExchange.getDescription(), "Test description");
        assertEquals(createExchange.getPrimaryContact(), "owner@example.com");
        assertEquals(createExchange.getDocumentation(), "https://docs.example.com");
    }

    @Test
    public void testCreateExchangeToString() {
        // Given: a CreateExchange action
        CreateExchange createExchange = new CreateExchange();
        createExchange.setProject("test-project");
        createExchange.setLocation("US");
        createExchange.setDataExchangeId("exchange_id");
        createExchange.setDisplayName("Test Exchange");
        createExchange.setDescription("Test description");

        // When: calling toString
        String result = createExchange.toString();

        // Then: it should contain relevant information
        assertTrue(result.contains("CreateExchange"));
        assertTrue(result.contains("test-project"));
        assertTrue(result.contains("US"));
        assertTrue(result.contains("exchange_id"));
        assertTrue(result.contains("Test Exchange"));
        assertTrue(result.contains("Test description"));
    }

    @Test
    public void testCreateExchangeActionFactoryRegistration() {
        // Given: a YAML-like configuration with CreateExchange
        Map<String, Object> config = new HashMap<>();
        config.put(
                "CreateExchange",
                new HashMap<String, Object>() {
                    {
                        put("dataExchangeId", "exchange");
                    }
                });

        // When: getting the action via ActionFactory
        Action action = ActionFactory.getAction(config, mockContext);

        // Then: should successfully create CreateExchange instance
        assertNotNull(action);
        assertTrue(action instanceof CreateExchange);
    }

    @Test
    public void testCreateExchangeNullValues() {
        // Given: a CreateExchange action with only required field
        CreateExchange createExchange = new CreateExchange();
        createExchange.setDataExchangeId("exchange_id");

        // Then: optional fields should be null
        assertNull(createExchange.getProject());
        assertNull(createExchange.getLocation());
        assertNull(createExchange.getDisplayName());
        assertNull(createExchange.getDescription());
        assertNull(createExchange.getPrimaryContact());
        assertNull(createExchange.getDocumentation());
    }

    @Test
    public void testCreateExchangeWithVariousLocations() {
        // Test various location formats
        String[] locations = {"US", "EU", "us-central1", "europe-west1", "asia-east1"};

        for (String location : locations) {
            Map<String, Object> config = new HashMap<>();
            Map<String, Object> exchangeConfig = new HashMap<>();
            exchangeConfig.put("dataExchangeId", "exchange");
            exchangeConfig.put("location", location);
            config.put("CreateExchange", exchangeConfig);

            Action action = ActionFactory.getAction(config, mockContext);
            CreateExchange createExchange = (CreateExchange) action;
            assertEquals(createExchange.getLocation(), location);
        }
    }

    @Test
    public void testCreateExchangeWithMultilineDescription() {
        // Given: configuration with a multi-line description
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> exchangeConfig = new HashMap<>();
        exchangeConfig.put("dataExchangeId", "exchange");
        String description = "Line one.\nLine two.\nLine three.";
        exchangeConfig.put("description", description);
        config.put("CreateExchange", exchangeConfig);

        // When: creating an action
        Action action = ActionFactory.getAction(config, mockContext);

        // Then: should preserve the multi-line description
        assertNotNull(action);
        CreateExchange createExchange = (CreateExchange) action;
        assertEquals(createExchange.getDescription(), description);
    }
}
