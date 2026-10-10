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

package io.github.totalschema.connector.gcp.bigquery.actions.subscription.delete;

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

/** Tests for {@link DeleteSubscription} and {@link DeleteSubscriptionActionFactory}. */
public class DeleteSubscriptionTest {

    private Context mockContext;
    private ExpressionEvaluator mockEvaluator;
    private VariableService mockVariableService;

    @BeforeMethod
    public void setUp() {
        mockContext = EasyMock.createMock(Context.class);
        mockEvaluator = EasyMock.createMock(ExpressionEvaluator.class);
        mockVariableService = EasyMock.createMock(VariableService.class);

        EasyMock.expect(mockContext.get(ExpressionEvaluator.class))
                .andReturn(mockEvaluator)
                .anyTimes();

        EasyMock.expect(mockContext.get(VariableService.class))
                .andReturn(mockVariableService)
                .anyTimes();

        EasyMock.expect(mockVariableService.getEffectiveVariables(mockContext))
                .andReturn(Collections.emptyMap())
                .anyTimes();

        EasyMock.expect(
                        mockEvaluator.evaluate(
                                EasyMock.anyObject(String.class), EasyMock.anyObject(Map.class)))
                .andAnswer(() -> (String) EasyMock.getCurrentArguments()[0])
                .anyTimes();

        EasyMock.replay(mockContext, mockEvaluator, mockVariableService);
    }

    @Test
    public void testDeleteSubscriptionActionFromMap() {
        // Given: a YAML-like configuration map with all fields
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> deleteSubscriptionConfig = new HashMap<>();
        deleteSubscriptionConfig.put("project", "subscriber-project");
        deleteSubscriptionConfig.put("datasetId", "subscribed_dataset");
        deleteSubscriptionConfig.put("deleteContents", true);
        config.put("DeleteSubscription", deleteSubscriptionConfig);

        // When: creating an action from the config
        Action action = ActionFactory.getAction(config, mockContext);

        // Then: it should be a DeleteSubscription instance with correct values
        assertNotNull(action);
        assertTrue(action instanceof DeleteSubscription);

        DeleteSubscription deleteSubscription = (DeleteSubscription) action;
        assertEquals(deleteSubscription.getProject(), "subscriber-project");
        assertEquals(deleteSubscription.getDatasetId(), "subscribed_dataset");
        assertEquals(deleteSubscription.getDeleteContents(), Boolean.TRUE);
    }

    @Test
    public void testDeleteSubscriptionMinimalConfig() {
        // Given: minimal valid configuration (only datasetId)
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> deleteSubscriptionConfig = new HashMap<>();
        deleteSubscriptionConfig.put("datasetId", "minimal_dataset");
        config.put("DeleteSubscription", deleteSubscriptionConfig);

        // When: creating an action
        Action action = ActionFactory.getAction(config, mockContext);

        // Then: should work with minimal config
        assertNotNull(action);
        assertTrue(action instanceof DeleteSubscription);

        DeleteSubscription deleteSubscription = (DeleteSubscription) action;
        assertEquals(deleteSubscription.getDatasetId(), "minimal_dataset");
        assertNull(deleteSubscription.getProject());
        assertNull(deleteSubscription.getDeleteContents());
    }

    @Test
    public void testDeleteSubscriptionGettersSetters() {
        // Given: a DeleteSubscription action
        DeleteSubscription deleteSubscription = new DeleteSubscription();

        // When: setting values
        deleteSubscription.setProject("test-project");
        deleteSubscription.setDatasetId("dataset_id");
        deleteSubscription.setDeleteContents(true);

        // Then: getters should return the values
        assertEquals(deleteSubscription.getProject(), "test-project");
        assertEquals(deleteSubscription.getDatasetId(), "dataset_id");
        assertEquals(deleteSubscription.getDeleteContents(), Boolean.TRUE);
    }

    @Test
    public void testDeleteSubscriptionToString() {
        // Given: a DeleteSubscription action
        DeleteSubscription deleteSubscription = new DeleteSubscription();
        deleteSubscription.setProject("test-project");
        deleteSubscription.setDatasetId("dataset_id");
        deleteSubscription.setDeleteContents(false);

        // When: calling toString
        String result = deleteSubscription.toString();

        // Then: it should contain relevant information
        assertTrue(result.contains("DeleteSubscription"));
        assertTrue(result.contains("test-project"));
        assertTrue(result.contains("dataset_id"));
        assertTrue(result.contains("false"));
    }

    @Test
    public void testDeleteSubscriptionActionFactoryRegistration() {
        // Given: a YAML-like configuration with DeleteSubscription
        Map<String, Object> config = new HashMap<>();
        config.put(
                "DeleteSubscription",
                new HashMap<String, Object>() {
                    {
                        put("datasetId", "dataset");
                    }
                });

        // When: getting the action via ActionFactory
        Action action = ActionFactory.getAction(config, mockContext);

        // Then: should successfully create DeleteSubscription instance
        assertNotNull(action);
        assertTrue(action instanceof DeleteSubscription);
    }

    @Test
    public void testDeleteSubscriptionNullValues() {
        // Given: a DeleteSubscription action with only required field
        DeleteSubscription deleteSubscription = new DeleteSubscription();
        deleteSubscription.setDatasetId("dataset_id");

        // Then: optional fields should be null
        assertNull(deleteSubscription.getProject());
        assertNull(deleteSubscription.getDeleteContents());
    }

    @Test
    public void testDeleteSubscriptionEmptyValues() {
        // Given: a DeleteSubscription action with empty strings
        DeleteSubscription deleteSubscription = new DeleteSubscription();
        deleteSubscription.setProject("");
        deleteSubscription.setDatasetId("");

        // Then: empty strings should be preserved
        assertEquals(deleteSubscription.getProject(), "");
        assertEquals(deleteSubscription.getDatasetId(), "");
    }

    @Test
    public void testDeleteSubscriptionWithDeleteContentsFalse() {
        // Given: configuration with explicit deleteContents=false
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> deleteSubscriptionConfig = new HashMap<>();
        deleteSubscriptionConfig.put("datasetId", "dataset_id");
        deleteSubscriptionConfig.put("deleteContents", false);
        config.put("DeleteSubscription", deleteSubscriptionConfig);

        // When: creating an action
        Action action = ActionFactory.getAction(config, mockContext);

        // Then: deleteContents should be false
        assertNotNull(action);
        DeleteSubscription deleteSubscription = (DeleteSubscription) action;
        assertEquals(deleteSubscription.getDeleteContents(), Boolean.FALSE);
    }

    @Test
    public void testDeleteSubscriptionWithVariousDatasetIds() {
        // Test various dataset ID formats
        String[] datasetIds = {"dataset1", "my_linked_dataset", "subscribed_data_2026"};

        for (String datasetId : datasetIds) {
            Map<String, Object> config = new HashMap<>();
            Map<String, Object> deleteSubscriptionConfig = new HashMap<>();
            deleteSubscriptionConfig.put("datasetId", datasetId);
            config.put("DeleteSubscription", deleteSubscriptionConfig);

            Action action = ActionFactory.getAction(config, mockContext);
            DeleteSubscription deleteSubscription = (DeleteSubscription) action;
            assertEquals(deleteSubscription.getDatasetId(), datasetId);
        }
    }
}
