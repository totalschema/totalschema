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

package io.github.totalschema.connector.gcp.bigquery.actions.subscription.create;

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

/** Tests for {@link CreateSubscription} and {@link CreateSubscriptionActionFactory}. */
public class CreateSubscriptionTest {

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
    public void testCreateSubscriptionActionFromMap() {
        // Given: a YAML-like configuration map with all fields
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> subscriptionConfig = new HashMap<>();
        subscriptionConfig.put("project", "publisher-project");
        subscriptionConfig.put("location", "US");
        subscriptionConfig.put("dataExchangeId", "my_exchange");
        subscriptionConfig.put("listingId", "my_listing");

        Map<String, Object> destinationDataset = new HashMap<>();
        destinationDataset.put("project", "subscriber-project");
        destinationDataset.put("datasetId", "subscribed_dataset");
        destinationDataset.put("location", "EU");
        destinationDataset.put("friendlyName", "Subscribed Dataset");
        destinationDataset.put("description", "Linked dataset for subscription");
        subscriptionConfig.put("destinationDataset", destinationDataset);

        config.put("CreateSubscription", subscriptionConfig);

        // When: creating an action from the config
        Action action = ActionFactory.getAction(config, mockContext);

        // Then: it should be a CreateSubscription instance with correct values
        assertNotNull(action);
        assertTrue(action instanceof CreateSubscription);

        CreateSubscription createSubscription = (CreateSubscription) action;
        assertEquals(createSubscription.getProject(), "publisher-project");
        assertEquals(createSubscription.getLocation(), "US");
        assertEquals(createSubscription.getDataExchangeId(), "my_exchange");
        assertEquals(createSubscription.getListingId(), "my_listing");

        CreateSubscription.DestinationDatasetConfig dataset =
                createSubscription.getDestinationDataset();
        assertNotNull(dataset);
        assertEquals(dataset.getProject(), "subscriber-project");
        assertEquals(dataset.getDatasetId(), "subscribed_dataset");
        assertEquals(dataset.getLocation(), "EU");
        assertEquals(dataset.getFriendlyName(), "Subscribed Dataset");
        assertEquals(dataset.getDescription(), "Linked dataset for subscription");
    }

    @Test
    public void testCreateSubscriptionMinimalConfig() {
        // Given: minimal valid configuration (no destinationDataset)
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> subscriptionConfig = new HashMap<>();
        subscriptionConfig.put("dataExchangeId", "my_exchange");
        subscriptionConfig.put("listingId", "my_listing");
        config.put("CreateSubscription", subscriptionConfig);

        // When: creating an action
        Action action = ActionFactory.getAction(config, mockContext);

        // Then: should work with minimal config
        assertNotNull(action);
        assertTrue(action instanceof CreateSubscription);

        CreateSubscription createSubscription = (CreateSubscription) action;
        assertEquals(createSubscription.getDataExchangeId(), "my_exchange");
        assertEquals(createSubscription.getListingId(), "my_listing");
        assertNull(createSubscription.getProject());
        assertNull(createSubscription.getLocation());
        assertNull(createSubscription.getDestinationDataset());
    }

    @Test
    public void testCreateSubscriptionDestinationDatasetWithoutProject() {
        // Given: destinationDataset omitting its own project (defaults applied at execution time)
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> subscriptionConfig = new HashMap<>();
        subscriptionConfig.put("dataExchangeId", "my_exchange");
        subscriptionConfig.put("listingId", "my_listing");

        Map<String, Object> destinationDataset = new HashMap<>();
        destinationDataset.put("datasetId", "linked_dataset");
        subscriptionConfig.put("destinationDataset", destinationDataset);

        config.put("CreateSubscription", subscriptionConfig);

        // When: creating an action
        Action action = ActionFactory.getAction(config, mockContext);

        // Then: destinationDataset project should remain null in the POJO
        assertNotNull(action);
        CreateSubscription createSubscription = (CreateSubscription) action;
        CreateSubscription.DestinationDatasetConfig dataset =
                createSubscription.getDestinationDataset();
        assertNotNull(dataset);
        assertNull(dataset.getProject());
        assertEquals(dataset.getDatasetId(), "linked_dataset");
    }

    @Test
    public void testCreateSubscriptionGettersSetters() {
        // Given: a CreateSubscription action
        CreateSubscription createSubscription = new CreateSubscription();

        // When: setting values
        createSubscription.setProject("test-project");
        createSubscription.setLocation("EU");
        createSubscription.setDataExchangeId("exchange_id");
        createSubscription.setListingId("listing_id");

        CreateSubscription.DestinationDatasetConfig dataset =
                new CreateSubscription.DestinationDatasetConfig();
        dataset.setProject("dest-project");
        dataset.setDatasetId("dest_dataset");
        dataset.setLocation("US");
        dataset.setFriendlyName("Friendly");
        dataset.setDescription("Desc");
        createSubscription.setDestinationDataset(dataset);

        // Then: getters should return the values
        assertEquals(createSubscription.getProject(), "test-project");
        assertEquals(createSubscription.getLocation(), "EU");
        assertEquals(createSubscription.getDataExchangeId(), "exchange_id");
        assertEquals(createSubscription.getListingId(), "listing_id");

        CreateSubscription.DestinationDatasetConfig retrieved =
                createSubscription.getDestinationDataset();
        assertNotNull(retrieved);
        assertEquals(retrieved.getProject(), "dest-project");
        assertEquals(retrieved.getDatasetId(), "dest_dataset");
        assertEquals(retrieved.getLocation(), "US");
        assertEquals(retrieved.getFriendlyName(), "Friendly");
        assertEquals(retrieved.getDescription(), "Desc");
    }

    @Test
    public void testCreateSubscriptionToString() {
        // Given: a CreateSubscription action
        CreateSubscription createSubscription = new CreateSubscription();
        createSubscription.setProject("test-project");
        createSubscription.setLocation("US");
        createSubscription.setDataExchangeId("exchange_id");
        createSubscription.setListingId("listing_id");

        // When: calling toString
        String result = createSubscription.toString();

        // Then: it should contain relevant information
        assertTrue(result.contains("CreateSubscription"));
        assertTrue(result.contains("test-project"));
        assertTrue(result.contains("US"));
        assertTrue(result.contains("exchange_id"));
        assertTrue(result.contains("listing_id"));
    }

    @Test
    public void testDestinationDatasetConfigToString() {
        // Given: a DestinationDatasetConfig
        CreateSubscription.DestinationDatasetConfig dataset =
                new CreateSubscription.DestinationDatasetConfig();
        dataset.setProject("dest-project");
        dataset.setDatasetId("dest_dataset");
        dataset.setLocation("US");

        // When: calling toString
        String result = dataset.toString();

        // Then: it should contain relevant information
        assertTrue(result.contains("DestinationDatasetConfig"));
        assertTrue(result.contains("dest-project"));
        assertTrue(result.contains("dest_dataset"));
        assertTrue(result.contains("US"));
    }

    @Test
    public void testCreateSubscriptionActionFactoryRegistration() {
        // Given: a YAML-like configuration with CreateSubscription
        Map<String, Object> config = new HashMap<>();
        config.put(
                "CreateSubscription",
                new HashMap<String, Object>() {
                    {
                        put("dataExchangeId", "exchange");
                        put("listingId", "listing");
                    }
                });

        // When: getting the action via ActionFactory
        Action action = ActionFactory.getAction(config, mockContext);

        // Then: should successfully create CreateSubscription instance
        assertNotNull(action);
        assertTrue(action instanceof CreateSubscription);
    }

    @Test
    public void testCreateSubscriptionNullValues() {
        // Given: a CreateSubscription action with only required fields
        CreateSubscription createSubscription = new CreateSubscription();
        createSubscription.setDataExchangeId("exchange_id");
        createSubscription.setListingId("listing_id");

        // Then: optional fields should be null
        assertNull(createSubscription.getProject());
        assertNull(createSubscription.getLocation());
        assertNull(createSubscription.getDestinationDataset());
    }

    @Test
    public void testDestinationDatasetConfigNullValues() {
        // Given: a DestinationDatasetConfig with no values set
        CreateSubscription.DestinationDatasetConfig dataset =
                new CreateSubscription.DestinationDatasetConfig();

        // Then: all fields should be null by default
        assertNull(dataset.getProject());
        assertNull(dataset.getDatasetId());
        assertNull(dataset.getLocation());
        assertNull(dataset.getFriendlyName());
        assertNull(dataset.getDescription());
    }

    @Test
    public void testCreateSubscriptionWithVariousLocations() {
        // Test various location formats
        String[] locations = {"US", "EU", "us-central1", "europe-west1", "asia-east1"};

        for (String location : locations) {
            Map<String, Object> config = new HashMap<>();
            Map<String, Object> subscriptionConfig = new HashMap<>();
            subscriptionConfig.put("dataExchangeId", "exchange");
            subscriptionConfig.put("listingId", "listing");
            subscriptionConfig.put("location", location);
            config.put("CreateSubscription", subscriptionConfig);

            Action action = ActionFactory.getAction(config, mockContext);
            CreateSubscription createSubscription = (CreateSubscription) action;
            assertEquals(createSubscription.getLocation(), location);
        }
    }
}
