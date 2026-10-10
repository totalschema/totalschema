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

package io.github.totalschema.connector.gcp.bigquery.actions.listing.create;

import static org.testng.Assert.*;

import io.github.totalschema.connector.gcp.bigquery.actions.Action;
import io.github.totalschema.connector.gcp.bigquery.actions.ActionFactory;
import io.github.totalschema.engine.api.Context;
import io.github.totalschema.spi.expression.evaluator.ExpressionEvaluator;
import io.github.totalschema.spi.variables.VariableService;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.easymock.EasyMock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Tests for {@link CreateListing} and {@link CreateListingActionFactory}. */
public class CreateListingTest {

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
    public void testCreateListingActionFromMap() {
        // Given: a YAML-like configuration map
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> listingConfig = new HashMap<>();
        listingConfig.put("project", "my-project");
        listingConfig.put("location", "US");
        listingConfig.put("dataExchangeId", "my_exchange");
        listingConfig.put("listingId", "my_listing");
        listingConfig.put("displayName", "My Dataset Listing");
        listingConfig.put("description", "Shared dataset for analytics");
        listingConfig.put("primaryContact", "contact@example.com");
        listingConfig.put("documentation", "https://example.com/docs");
        listingConfig.put("categories", Arrays.asList("CATEGORY_OTHERS", "CATEGORY_RETAIL"));

        Map<String, Object> sourceDataset = new HashMap<>();
        sourceDataset.put("project", "source-project");
        sourceDataset.put("dataset", "my_dataset");
        listingConfig.put("sourceDataset", sourceDataset);

        config.put("CreateListing", listingConfig);

        // When: creating an action from the config
        Action action = ActionFactory.getAction(config, mockContext);

        // Then: it should be a CreateListing instance with correct values
        assertNotNull(action);
        assertTrue(action instanceof CreateListing);

        CreateListing createListing = (CreateListing) action;
        assertEquals(createListing.getProject(), "my-project");
        assertEquals(createListing.getLocation(), "US");
        assertEquals(createListing.getDataExchangeId(), "my_exchange");
        assertEquals(createListing.getListingId(), "my_listing");
        assertEquals(createListing.getDisplayName(), "My Dataset Listing");
        assertEquals(createListing.getDescription(), "Shared dataset for analytics");
        assertEquals(createListing.getPrimaryContact(), "contact@example.com");
        assertEquals(createListing.getDocumentation(), "https://example.com/docs");

        List<String> categories = createListing.getCategories();
        assertNotNull(categories);
        assertEquals(categories.size(), 2);
        assertTrue(categories.contains("CATEGORY_OTHERS"));
        assertTrue(categories.contains("CATEGORY_RETAIL"));

        CreateListing.SourceDataset dataset = createListing.getSourceDataset();
        assertNotNull(dataset);
        assertEquals(dataset.getProject(), "source-project");
        assertEquals(dataset.getDataset(), "my_dataset");
    }

    @Test
    public void testCreateListingMinimalConfig() {
        // Given: minimal valid configuration
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> listingConfig = new HashMap<>();
        listingConfig.put("dataExchangeId", "my_exchange");
        listingConfig.put("listingId", "my_listing");
        config.put("CreateListing", listingConfig);

        // When: creating an action
        Action action = ActionFactory.getAction(config, mockContext);

        // Then: should work with minimal config
        assertNotNull(action);
        assertTrue(action instanceof CreateListing);

        CreateListing createListing = (CreateListing) action;
        assertEquals(createListing.getDataExchangeId(), "my_exchange");
        assertEquals(createListing.getListingId(), "my_listing");
        assertNull(createListing.getProject());
        assertNull(createListing.getLocation());
        assertNull(createListing.getDisplayName());
        assertNull(createListing.getDescription());
        assertNull(createListing.getPrimaryContact());
        assertNull(createListing.getDocumentation());
        assertNull(createListing.getCategories());
        assertNull(createListing.getSourceDataset());
    }

    @Test
    public void testCreateListingSourceDatasetWithoutProject() {
        // Given: configuration where sourceDataset omits its own project
        // (it should default to the listing's project at execution time, not in the POJO)
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> listingConfig = new HashMap<>();
        listingConfig.put("dataExchangeId", "my_exchange");
        listingConfig.put("listingId", "my_listing");

        Map<String, Object> sourceDataset = new HashMap<>();
        sourceDataset.put("dataset", "shared_dataset");
        listingConfig.put("sourceDataset", sourceDataset);

        config.put("CreateListing", listingConfig);

        // When: creating an action
        Action action = ActionFactory.getAction(config, mockContext);

        // Then: sourceDataset project should remain null in the POJO
        assertNotNull(action);
        CreateListing createListing = (CreateListing) action;
        CreateListing.SourceDataset dataset = createListing.getSourceDataset();
        assertNotNull(dataset);
        assertNull(dataset.getProject());
        assertEquals(dataset.getDataset(), "shared_dataset");
    }

    @Test
    public void testCreateListingGettersSetters() {
        // Given: a CreateListing action
        CreateListing createListing = new CreateListing();

        // When: setting values
        createListing.setProject("test-project");
        createListing.setLocation("EU");
        createListing.setDataExchangeId("exchange_id");
        createListing.setListingId("listing_id");
        createListing.setDisplayName("Test Listing");
        createListing.setDescription("Test description");
        createListing.setPrimaryContact("owner@example.com");
        createListing.setDocumentation("https://docs.example.com");
        createListing.setCategories(Collections.singletonList("CATEGORY_FINANCIAL"));

        CreateListing.SourceDataset sourceDataset = new CreateListing.SourceDataset();
        sourceDataset.setProject("data-project");
        sourceDataset.setDataset("data_set");
        createListing.setSourceDataset(sourceDataset);

        // Then: getters should return the values
        assertEquals(createListing.getProject(), "test-project");
        assertEquals(createListing.getLocation(), "EU");
        assertEquals(createListing.getDataExchangeId(), "exchange_id");
        assertEquals(createListing.getListingId(), "listing_id");
        assertEquals(createListing.getDisplayName(), "Test Listing");
        assertEquals(createListing.getDescription(), "Test description");
        assertEquals(createListing.getPrimaryContact(), "owner@example.com");
        assertEquals(createListing.getDocumentation(), "https://docs.example.com");
        assertEquals(
                createListing.getCategories(), Collections.singletonList("CATEGORY_FINANCIAL"));

        CreateListing.SourceDataset dataset = createListing.getSourceDataset();
        assertNotNull(dataset);
        assertEquals(dataset.getProject(), "data-project");
        assertEquals(dataset.getDataset(), "data_set");
    }

    @Test
    public void testCreateListingToString() {
        // Given: a CreateListing action
        CreateListing createListing = new CreateListing();
        createListing.setProject("test-project");
        createListing.setLocation("US");
        createListing.setDataExchangeId("exchange_id");
        createListing.setListingId("listing_id");
        createListing.setDisplayName("Test Listing");
        createListing.setDescription("Test description");

        // When: calling toString
        String result = createListing.toString();

        // Then: it should contain relevant information
        assertTrue(result.contains("CreateListing"));
        assertTrue(result.contains("test-project"));
        assertTrue(result.contains("US"));
        assertTrue(result.contains("exchange_id"));
        assertTrue(result.contains("listing_id"));
        assertTrue(result.contains("Test Listing"));
        assertTrue(result.contains("Test description"));
    }

    @Test
    public void testCreateListingActionFactoryRegistration() {
        // Given: a YAML-like configuration with CreateListing
        Map<String, Object> config = new HashMap<>();
        config.put(
                "CreateListing",
                new HashMap<String, Object>() {
                    {
                        put("dataExchangeId", "exchange");
                        put("listingId", "listing");
                    }
                });

        // When: getting the action via ActionFactory
        Action action = ActionFactory.getAction(config, mockContext);

        // Then: should successfully create CreateListing instance
        assertNotNull(action);
        assertTrue(action instanceof CreateListing);
    }

    @Test
    public void testCreateListingWithMultipleCategories() {
        // Given: configuration with many categories
        List<String> allCategories =
                Arrays.asList(
                        "CATEGORY_OTHERS",
                        "CATEGORY_ADVERTISING_AND_MARKETING",
                        "CATEGORY_COMMERCE",
                        "CATEGORY_GEOSPATIAL");

        Map<String, Object> config = new HashMap<>();
        Map<String, Object> listingConfig = new HashMap<>();
        listingConfig.put("dataExchangeId", "exchange");
        listingConfig.put("listingId", "listing");
        listingConfig.put("categories", allCategories);
        config.put("CreateListing", listingConfig);

        // When: creating an action
        Action action = ActionFactory.getAction(config, mockContext);

        // Then: all categories should be preserved
        assertNotNull(action);
        CreateListing createListing = (CreateListing) action;
        assertEquals(createListing.getCategories(), allCategories);
    }

    @Test
    public void testCreateListingNullValues() {
        // Given: a CreateListing action with only required fields
        CreateListing createListing = new CreateListing();
        createListing.setDataExchangeId("exchange_id");
        createListing.setListingId("listing_id");

        // Then: optional fields should be null
        assertNull(createListing.getProject());
        assertNull(createListing.getLocation());
        assertNull(createListing.getDisplayName());
        assertNull(createListing.getDescription());
        assertNull(createListing.getPrimaryContact());
        assertNull(createListing.getDocumentation());
        assertNull(createListing.getCategories());
        assertNull(createListing.getSourceDataset());
    }

    @Test
    public void testSourceDatasetGettersSetters() {
        // Given: a SourceDataset object
        CreateListing.SourceDataset sourceDataset = new CreateListing.SourceDataset();

        // When: setting values
        sourceDataset.setProject("my-source-project");
        sourceDataset.setDataset("my_source_dataset");

        // Then: getters should return the values
        assertEquals(sourceDataset.getProject(), "my-source-project");
        assertEquals(sourceDataset.getDataset(), "my_source_dataset");
    }

    @Test
    public void testCreateListingWithVariousLocations() {
        // Test various location formats
        String[] locations = {"US", "EU", "us-central1", "europe-west1", "asia-east1"};

        for (String location : locations) {
            Map<String, Object> config = new HashMap<>();
            Map<String, Object> listingConfig = new HashMap<>();
            listingConfig.put("dataExchangeId", "exchange");
            listingConfig.put("listingId", "listing");
            listingConfig.put("location", location);
            config.put("CreateListing", listingConfig);

            Action action = ActionFactory.getAction(config, mockContext);
            CreateListing createListing = (CreateListing) action;
            assertEquals(createListing.getLocation(), location);
        }
    }
}
