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

package io.github.totalschema.connector.gcp.bigquery.actions.dataset.create;

import static org.testng.Assert.*;

import com.google.cloud.bigquery.Dataset;
import com.google.cloud.bigquery.DatasetId;
import io.github.totalschema.connector.gcp.bigquery.AbstractBigQueryIntegrationTest;
import org.testng.annotations.Test;

/**
 * Integration tests for {@link CreateDataset} action using the BigQuery emulator.
 *
 * <p>These tests verify that the CreateDataset action correctly creates datasets in BigQuery using
 * the real BigQuery API against the emulator.
 */
public class CreateDatasetIntegrationTest extends AbstractBigQueryIntegrationTest {

    @Test
    public void testCreateDatasetMinimal() {
        // Given: a CreateDataset action with minimal configuration
        CreateDataset action = new CreateDataset();
        action.setDatasetId("test_dataset_minimal");

        // When: executing the action
        action.execute(bigQuery, mockConfiguration);

        // Then: dataset should be created
        Dataset dataset =
                bigQuery.getDataset(DatasetId.of(TEST_PROJECT_ID, "test_dataset_minimal"));
        assertNotNull(dataset, "Dataset should be created");
        assertEquals(dataset.getDatasetId().getDataset(), "test_dataset_minimal");
    }

    @Test
    public void testCreateDatasetWithDescription() {
        // Given: a CreateDataset action with description
        CreateDataset action = new CreateDataset();
        action.setDatasetId("test_dataset_with_desc");
        action.setDescription("Test dataset description");

        // When: executing the action
        action.execute(bigQuery, mockConfiguration);

        // Then: dataset should be created with description
        Dataset dataset =
                bigQuery.getDataset(DatasetId.of(TEST_PROJECT_ID, "test_dataset_with_desc"));
        assertNotNull(dataset, "Dataset should be created");
        assertEquals(dataset.getDescription(), "Test dataset description");
    }

    @Test
    public void testCreateDatasetWithLocation() {
        // Given: a CreateDataset action with location
        CreateDataset action = new CreateDataset();
        action.setDatasetId("test_dataset_with_location");
        action.setLocation("US");

        // When: executing the action
        action.execute(bigQuery, mockConfiguration);

        // Then: dataset should be created with location
        Dataset dataset =
                bigQuery.getDataset(DatasetId.of(TEST_PROJECT_ID, "test_dataset_with_location"));
        assertNotNull(dataset, "Dataset should be created");
        assertEquals(dataset.getLocation(), "US");
    }

    @Test
    public void testCreateDatasetComplete() {
        // Given: a CreateDataset action with all fields
        CreateDataset action = new CreateDataset();
        action.setDatasetId("test_dataset_complete");
        action.setDescription("Complete test dataset");
        action.setLocation("US");

        // When: executing the action
        action.execute(bigQuery, mockConfiguration);

        // Then: dataset should be created with all properties
        Dataset dataset =
                bigQuery.getDataset(DatasetId.of(TEST_PROJECT_ID, "test_dataset_complete"));
        assertNotNull(dataset, "Dataset should be created");
        assertEquals(dataset.getDatasetId().getDataset(), "test_dataset_complete");
        assertEquals(dataset.getDescription(), "Complete test dataset");
        assertEquals(dataset.getLocation(), "US");
    }

    @Test
    public void testCreateMultipleDatasets() {
        // Given: multiple CreateDataset actions
        CreateDataset action1 = new CreateDataset();
        action1.setDatasetId("dataset_one");
        action1.setDescription("First dataset");

        CreateDataset action2 = new CreateDataset();
        action2.setDatasetId("dataset_two");
        action2.setDescription("Second dataset");

        CreateDataset action3 = new CreateDataset();
        action3.setDatasetId("dataset_three");
        action3.setDescription("Third dataset");

        // When: executing all actions
        action1.execute(bigQuery, mockConfiguration);
        action2.execute(bigQuery, mockConfiguration);
        action3.execute(bigQuery, mockConfiguration);

        // Then: all datasets should be created
        assertNotNull(bigQuery.getDataset("dataset_one"));
        assertNotNull(bigQuery.getDataset("dataset_two"));
        assertNotNull(bigQuery.getDataset("dataset_three"));

        // Verify descriptions
        assertEquals(bigQuery.getDataset("dataset_one").getDescription(), "First dataset");
        assertEquals(bigQuery.getDataset("dataset_two").getDescription(), "Second dataset");
        assertEquals(bigQuery.getDataset("dataset_three").getDescription(), "Third dataset");
    }

    @Test
    public void testCreateDatasetWithSpecialCharacters() {
        // Given: a dataset with underscores and numbers
        CreateDataset action = new CreateDataset();
        action.setDatasetId("test_dataset_123_special");
        action.setDescription("Dataset with special chars: 'quotes' and numbers 123");

        // When: executing the action
        action.execute(bigQuery, mockConfiguration);

        // Then: dataset should be created
        Dataset dataset =
                bigQuery.getDataset(DatasetId.of(TEST_PROJECT_ID, "test_dataset_123_special"));
        assertNotNull(dataset, "Dataset should be created");
        assertTrue(dataset.getDescription().contains("special chars"));
    }

    @Test
    public void testDatasetPersistence() {
        // Given: a created dataset
        CreateDataset action = new CreateDataset();
        action.setDatasetId("persistent_dataset");
        action.setDescription("Should persist");
        action.execute(bigQuery, mockConfiguration);

        // When: retrieving the dataset multiple times
        Dataset dataset1 = bigQuery.getDataset("persistent_dataset");
        Dataset dataset2 = bigQuery.getDataset("persistent_dataset");

        // Then: dataset should be retrievable consistently
        assertNotNull(dataset1);
        assertNotNull(dataset2);
        assertEquals(dataset1.getDatasetId(), dataset2.getDatasetId());
        assertEquals(dataset1.getDescription(), dataset2.getDescription());
    }
}
