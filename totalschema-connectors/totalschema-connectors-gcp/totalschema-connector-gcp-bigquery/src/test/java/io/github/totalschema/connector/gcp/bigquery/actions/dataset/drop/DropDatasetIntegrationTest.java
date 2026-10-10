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

package io.github.totalschema.connector.gcp.bigquery.actions.dataset.drop;

import static org.testng.Assert.*;

import com.google.cloud.bigquery.*;
import io.github.totalschema.connector.gcp.bigquery.AbstractBigQueryIntegrationTest;
import io.github.totalschema.connector.gcp.bigquery.actions.dataset.create.CreateDataset;
import org.testng.annotations.Test;

/**
 * Integration tests for {@link DropDataset} action using the BigQuery emulator.
 *
 * <p>These tests verify that the DropDataset action correctly drops datasets in BigQuery using the
 * real BigQuery API against the emulator.
 */
public class DropDatasetIntegrationTest extends AbstractBigQueryIntegrationTest {

    @Test
    public void testDropDatasetMinimal() {
        // Given: an existing dataset
        CreateDataset createAction = new CreateDataset();
        createAction.setDatasetId("test_drop_minimal");
        createAction.execute(bigQuery, mockConfiguration);

        // Verify dataset exists
        Dataset dataset = bigQuery.getDataset(DatasetId.of(TEST_PROJECT_ID, "test_drop_minimal"));
        assertNotNull(dataset, "Dataset should exist before drop");

        // When: dropping the dataset
        DropDataset dropAction = new DropDataset();
        dropAction.setDatasetId("test_drop_minimal");
        dropAction.execute(bigQuery, mockConfiguration);

        // Then: dataset should no longer exist
        Dataset droppedDataset = bigQuery.getDataset("test_drop_minimal");
        assertNull(droppedDataset, "Dataset should not exist after drop");
    }

    @Test
    public void testDropDatasetWithDeleteContentsFalse() {
        // Given: an empty dataset
        CreateDataset createAction = new CreateDataset();
        createAction.setDatasetId("test_drop_empty");
        createAction.execute(bigQuery, mockConfiguration);

        // When: dropping with deleteContents false
        DropDataset dropAction = new DropDataset();
        dropAction.setDatasetId("test_drop_empty");
        dropAction.setDeleteContents(false);
        dropAction.execute(bigQuery, mockConfiguration);

        // Then: dataset should be dropped
        Dataset droppedDataset = bigQuery.getDataset("test_drop_empty");
        assertNull(droppedDataset, "Empty dataset should be dropped");
    }

    @Test
    public void testDropDatasetWithDeleteContentsTrue() {
        // Given: a dataset with a table
        CreateDataset createAction = new CreateDataset();
        createAction.setDatasetId("test_drop_with_tables");
        createAction.execute(bigQuery, mockConfiguration);

        // Create a table in the dataset
        TableId tableId = TableId.of(TEST_PROJECT_ID, "test_drop_with_tables", "test_table");
        Schema schema =
                Schema.of(
                        Field.of("name", StandardSQLTypeName.STRING),
                        Field.of("age", StandardSQLTypeName.INT64));
        TableDefinition tableDefinition = StandardTableDefinition.of(schema);
        TableInfo tableInfo = TableInfo.newBuilder(tableId, tableDefinition).build();
        bigQuery.create(tableInfo);

        // Verify table exists
        Table table = bigQuery.getTable(tableId);
        assertNotNull(table, "Table should exist");

        // When: dropping with deleteContents true
        DropDataset dropAction = new DropDataset();
        dropAction.setDatasetId("test_drop_with_tables");
        dropAction.setDeleteContents(true);
        dropAction.execute(bigQuery, mockConfiguration);

        // Then: dataset and table should be dropped
        Dataset droppedDataset = bigQuery.getDataset("test_drop_with_tables");
        assertNull(droppedDataset, "Dataset should be dropped");

        Table droppedTable = bigQuery.getTable(tableId);
        assertNull(droppedTable, "Table should be dropped");
    }

    @Test(expectedExceptions = BigQueryException.class)
    public void testDropDatasetWithTablesFailsWithoutDeleteContents() {
        // Given: a dataset with a table
        CreateDataset createAction = new CreateDataset();
        createAction.setDatasetId("test_drop_fail");
        createAction.execute(bigQuery, mockConfiguration);

        // Create a table in the dataset
        TableId tableId = TableId.of(TEST_PROJECT_ID, "test_drop_fail", "test_table");
        Schema schema = Schema.of(Field.of("name", StandardSQLTypeName.STRING));
        TableDefinition tableDefinition = StandardTableDefinition.of(schema);
        TableInfo tableInfo = TableInfo.newBuilder(tableId, tableDefinition).build();
        bigQuery.create(tableInfo);

        // When: dropping without deleteContents (should fail)
        DropDataset dropAction = new DropDataset();
        dropAction.setDatasetId("test_drop_fail");
        dropAction.setDeleteContents(false);

        // Then: should throw BigQueryException
        dropAction.execute(bigQuery, mockConfiguration);
    }

    @Test
    public void testDropMultipleDatasets() {
        // Given: multiple datasets
        CreateDataset createAction1 = new CreateDataset();
        createAction1.setDatasetId("drop_dataset_one");
        createAction1.execute(bigQuery, mockConfiguration);

        CreateDataset createAction2 = new CreateDataset();
        createAction2.setDatasetId("drop_dataset_two");
        createAction2.execute(bigQuery, mockConfiguration);

        CreateDataset createAction3 = new CreateDataset();
        createAction3.setDatasetId("drop_dataset_three");
        createAction3.execute(bigQuery, mockConfiguration);

        // Verify all exist
        assertNotNull(bigQuery.getDataset("drop_dataset_one"));
        assertNotNull(bigQuery.getDataset("drop_dataset_two"));
        assertNotNull(bigQuery.getDataset("drop_dataset_three"));

        // When: dropping all datasets
        DropDataset dropAction1 = new DropDataset();
        dropAction1.setDatasetId("drop_dataset_one");
        dropAction1.execute(bigQuery, mockConfiguration);

        DropDataset dropAction2 = new DropDataset();
        dropAction2.setDatasetId("drop_dataset_two");
        dropAction2.execute(bigQuery, mockConfiguration);

        DropDataset dropAction3 = new DropDataset();
        dropAction3.setDatasetId("drop_dataset_three");
        dropAction3.execute(bigQuery, mockConfiguration);

        // Then: all should be dropped
        assertNull(bigQuery.getDataset("drop_dataset_one"));
        assertNull(bigQuery.getDataset("drop_dataset_two"));
        assertNull(bigQuery.getDataset("drop_dataset_three"));
    }

    @Test
    public void testDropDatasetWithMultipleTables() {
        // Given: a dataset with multiple tables
        CreateDataset createAction = new CreateDataset();
        createAction.setDatasetId("test_drop_multi_tables");
        createAction.execute(bigQuery, mockConfiguration);

        // Create multiple tables
        Schema schema = Schema.of(Field.of("id", StandardSQLTypeName.INT64));

        for (int i = 1; i <= 3; i++) {
            TableId tableId = TableId.of(TEST_PROJECT_ID, "test_drop_multi_tables", "table_" + i);
            TableDefinition tableDefinition = StandardTableDefinition.of(schema);
            TableInfo tableInfo = TableInfo.newBuilder(tableId, tableDefinition).build();
            bigQuery.create(tableInfo);
        }

        // When: dropping with deleteContents
        DropDataset dropAction = new DropDataset();
        dropAction.setDatasetId("test_drop_multi_tables");
        dropAction.setDeleteContents(true);
        dropAction.execute(bigQuery, mockConfiguration);

        // Then: dataset and all tables should be dropped
        Dataset droppedDataset = bigQuery.getDataset("test_drop_multi_tables");
        assertNull(droppedDataset, "Dataset should be dropped");
    }

    @Test
    public void testDropAndRecreateDataset() {
        // Given: a dataset that is created, dropped, and recreated
        String datasetId = "test_drop_recreate";

        // Create initial dataset
        CreateDataset createAction1 = new CreateDataset();
        createAction1.setDatasetId(datasetId);
        createAction1.setDescription("First version");
        createAction1.execute(bigQuery, mockConfiguration);

        Dataset dataset1 = bigQuery.getDataset(datasetId);
        assertNotNull(dataset1);
        assertEquals(dataset1.getDescription(), "First version");

        // Drop the dataset
        DropDataset dropAction = new DropDataset();
        dropAction.setDatasetId(datasetId);
        dropAction.execute(bigQuery, mockConfiguration);

        assertNull(bigQuery.getDataset(datasetId));

        // Recreate with different description
        CreateDataset createAction2 = new CreateDataset();
        createAction2.setDatasetId(datasetId);
        createAction2.setDescription("Second version");
        createAction2.execute(bigQuery, mockConfiguration);

        Dataset dataset2 = bigQuery.getDataset(datasetId);
        assertNotNull(dataset2);
        assertEquals(dataset2.getDescription(), "Second version");
    }

    @Test
    public void testDropDatasetIdempotency() {
        // Given: a dataset
        CreateDataset createAction = new CreateDataset();
        createAction.setDatasetId("test_idempotent");
        createAction.execute(bigQuery, mockConfiguration);

        // When: dropping it twice
        DropDataset dropAction1 = new DropDataset();
        dropAction1.setDatasetId("test_idempotent");
        dropAction1.execute(bigQuery, mockConfiguration);

        // First drop should succeed
        assertNull(bigQuery.getDataset("test_idempotent"));

        // Second drop on non-existent dataset should not throw
        DropDataset dropAction2 = new DropDataset();
        dropAction2.setDatasetId("test_idempotent");
        dropAction2.execute(bigQuery, mockConfiguration);

        // Then: still should not exist (idempotent)
        assertNull(bigQuery.getDataset("test_idempotent"));
    }

    @Test
    public void testDropDatasetWithSpecialCharacters() {
        // Given: a dataset with underscores and numbers
        CreateDataset createAction = new CreateDataset();
        createAction.setDatasetId("test_drop_123_special");
        createAction.execute(bigQuery, mockConfiguration);

        // When: dropping the dataset
        DropDataset dropAction = new DropDataset();
        dropAction.setDatasetId("test_drop_123_special");
        dropAction.execute(bigQuery, mockConfiguration);

        // Then: dataset should be dropped
        assertNull(bigQuery.getDataset("test_drop_123_special"));
    }

    @Test
    public void testDropDatasetPreservesOtherDatasets() {
        // Given: multiple datasets
        CreateDataset createAction1 = new CreateDataset();
        createAction1.setDatasetId("keep_this_dataset");
        createAction1.execute(bigQuery, mockConfiguration);

        CreateDataset createAction2 = new CreateDataset();
        createAction2.setDatasetId("drop_this_dataset");
        createAction2.execute(bigQuery, mockConfiguration);

        // When: dropping one dataset
        DropDataset dropAction = new DropDataset();
        dropAction.setDatasetId("drop_this_dataset");
        dropAction.execute(bigQuery, mockConfiguration);

        // Then: only the targeted dataset should be dropped
        assertNotNull(bigQuery.getDataset("keep_this_dataset"), "Other dataset should remain");
        assertNull(bigQuery.getDataset("drop_this_dataset"), "Target dataset should be dropped");
    }
}
