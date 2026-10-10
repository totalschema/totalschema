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

package io.github.totalschema.connector.gcp.bigquery.actions.listing.delete;

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

/** Tests for {@link DeleteListing} and {@link DeleteListingActionFactory}. */
public class DeleteListingTest {

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
    public void testDeleteListingActionFromMap() {
        // Given: a YAML-like configuration map with all fields
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> deleteListingConfig = new HashMap<>();
        deleteListingConfig.put("project", "my-project");
        deleteListingConfig.put("location", "US");
        deleteListingConfig.put("dataExchangeId", "my_exchange");
        deleteListingConfig.put("listingId", "my_listing");
        config.put("DeleteListing", deleteListingConfig);

        // When: creating an action from the config
        Action action = ActionFactory.getAction(config, mockContext);

        // Then: it should be a DeleteListing instance with correct values
        assertNotNull(action);
        assertTrue(action instanceof DeleteListing);

        DeleteListing deleteListing = (DeleteListing) action;
        assertEquals(deleteListing.getProject(), "my-project");
        assertEquals(deleteListing.getLocation(), "US");
        assertEquals(deleteListing.getDataExchangeId(), "my_exchange");
        assertEquals(deleteListing.getListingId(), "my_listing");
    }

    @Test
    public void testDeleteListingMinimalConfig() {
        // Given: minimal valid configuration (only dataExchangeId and listingId)
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> deleteListingConfig = new HashMap<>();
        deleteListingConfig.put("dataExchangeId", "minimal_exchange");
        deleteListingConfig.put("listingId", "minimal_listing");
        config.put("DeleteListing", deleteListingConfig);

        // When: creating an action
        Action action = ActionFactory.getAction(config, mockContext);

        // Then: should work with minimal config
        assertNotNull(action);
        assertTrue(action instanceof DeleteListing);

        DeleteListing deleteListing = (DeleteListing) action;
        assertEquals(deleteListing.getDataExchangeId(), "minimal_exchange");
        assertEquals(deleteListing.getListingId(), "minimal_listing");
        assertNull(deleteListing.getProject());
        assertNull(deleteListing.getLocation());
    }

    @Test
    public void testDeleteListingWithEULocation() {
        // Given: configuration with EU location
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> deleteListingConfig = new HashMap<>();
        deleteListingConfig.put("dataExchangeId", "eu_exchange");
        deleteListingConfig.put("listingId", "eu_listing");
        deleteListingConfig.put("location", "EU");
        config.put("DeleteListing", deleteListingConfig);

        // When: creating an action
        Action action = ActionFactory.getAction(config, mockContext);

        // Then: should have EU location set
        assertNotNull(action);
        DeleteListing deleteListing = (DeleteListing) action;
        assertEquals(deleteListing.getLocation(), "EU");
    }

    @Test
    public void testDeleteListingGettersSetters() {
        // Given: a DeleteListing action
        DeleteListing deleteListing = new DeleteListing();

        // When: setting values
        deleteListing.setProject("test-project");
        deleteListing.setLocation("US");
        deleteListing.setDataExchangeId("exchange_id");
        deleteListing.setListingId("listing_id");

        // Then: getters should return the values
        assertEquals(deleteListing.getProject(), "test-project");
        assertEquals(deleteListing.getLocation(), "US");
        assertEquals(deleteListing.getDataExchangeId(), "exchange_id");
        assertEquals(deleteListing.getListingId(), "listing_id");
    }

    @Test
    public void testDeleteListingToString() {
        // Given: a DeleteListing action
        DeleteListing deleteListing = new DeleteListing();
        deleteListing.setProject("test-project");
        deleteListing.setLocation("US");
        deleteListing.setDataExchangeId("exchange_id");
        deleteListing.setListingId("listing_id");

        // When: calling toString
        String result = deleteListing.toString();

        // Then: it should contain relevant information
        assertTrue(result.contains("DeleteListing"));
        assertTrue(result.contains("test-project"));
        assertTrue(result.contains("US"));
        assertTrue(result.contains("exchange_id"));
        assertTrue(result.contains("listing_id"));
    }

    @Test
    public void testDeleteListingActionFactoryRegistration() {
        // Given: a YAML-like configuration with DeleteListing
        Map<String, Object> config = new HashMap<>();
        config.put(
                "DeleteListing",
                new HashMap<String, Object>() {
                    {
                        put("dataExchangeId", "exchange");
                        put("listingId", "listing");
                    }
                });

        // When: getting the action via ActionFactory
        Action action = ActionFactory.getAction(config, mockContext);

        // Then: should successfully create DeleteListing instance
        assertNotNull(action);
        assertTrue(action instanceof DeleteListing);
    }

    @Test
    public void testDeleteListingNullValues() {
        // Given: a DeleteListing action with only required fields
        DeleteListing deleteListing = new DeleteListing();
        deleteListing.setDataExchangeId("exchange_id");
        deleteListing.setListingId("listing_id");

        // Then: optional fields should be null
        assertNull(deleteListing.getProject());
        assertNull(deleteListing.getLocation());
    }

    @Test
    public void testDeleteListingEmptyValues() {
        // Given: a DeleteListing action with empty strings
        DeleteListing deleteListing = new DeleteListing();
        deleteListing.setProject("");
        deleteListing.setLocation("");
        deleteListing.setDataExchangeId("");
        deleteListing.setListingId("");

        // Then: empty strings should be preserved
        assertEquals(deleteListing.getProject(), "");
        assertEquals(deleteListing.getLocation(), "");
        assertEquals(deleteListing.getDataExchangeId(), "");
        assertEquals(deleteListing.getListingId(), "");
    }

    @Test
    public void testDeleteListingWithMultiRegionLocations() {
        // Test various location formats
        String[] locations = {"US", "EU", "us-central1", "europe-west1", "asia-east1"};

        for (String location : locations) {
            Map<String, Object> config = new HashMap<>();
            Map<String, Object> deleteListingConfig = new HashMap<>();
            deleteListingConfig.put("dataExchangeId", "exchange");
            deleteListingConfig.put("listingId", "listing");
            deleteListingConfig.put("location", location);
            config.put("DeleteListing", deleteListingConfig);

            Action action = ActionFactory.getAction(config, mockContext);
            DeleteListing deleteListing = (DeleteListing) action;
            assertEquals(deleteListing.getLocation(), location);
        }
    }

    @Test
    public void testDeleteListingWithVariousDataExchangeAndListingIds() {
        // Test various ID formats
        String[][] idPairs = {
            {"exchange1", "listing1"},
            {"my-data-exchange", "my-listing-id"},
            {"12345678-1234-1234-1234-123456789012", "abcdefgh"}
        };

        for (String[] idPair : idPairs) {
            Map<String, Object> config = new HashMap<>();
            Map<String, Object> deleteListingConfig = new HashMap<>();
            deleteListingConfig.put("dataExchangeId", idPair[0]);
            deleteListingConfig.put("listingId", idPair[1]);
            config.put("DeleteListing", deleteListingConfig);

            Action action = ActionFactory.getAction(config, mockContext);
            DeleteListing deleteListing = (DeleteListing) action;
            assertEquals(deleteListing.getDataExchangeId(), idPair[0]);
            assertEquals(deleteListing.getListingId(), idPair[1]);
        }
    }
}
