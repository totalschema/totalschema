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

package io.github.totalschema.connector.gcp.bigtable.actions.data.deletecell;

import static org.testng.Assert.*;

import com.google.cloud.bigtable.admin.v2.models.CreateTableRequest;
import com.google.cloud.bigtable.data.v2.models.Row;
import com.google.cloud.bigtable.data.v2.models.RowCell;
import com.google.cloud.bigtable.data.v2.models.RowMutation;
import com.google.cloud.bigtable.data.v2.models.TableId;
import com.google.protobuf.ByteString;
import io.github.totalschema.connector.gcp.bigtable.AbstractBigtableDataIntegrationTest;
import java.util.List;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Integration tests for {@link DeleteCell} using BigTable emulator in a TestContainer.
 *
 * <p>These tests verify that DeleteCell works correctly against the BigTable emulator. The tests
 * are skipped if Docker/Podman is not available.
 */
public class DeleteCellIntegrationTest extends AbstractBigtableDataIntegrationTest {

    private static final String TEST_TABLE_NAME = "test-deletecell-table";
    private static final TableId TEST_TABLE = TableId.of(TEST_TABLE_NAME);
    private static final String FAMILY_METADATA = "metadata";
    private static final String FAMILY_CONFIG = "config";

    @BeforeMethod
    public void createTestTable() {
        // Create a test table with column families for each test
        try {
            adminClient.deleteTable(TEST_TABLE_NAME);
        } catch (Exception e) {
            // Table might not exist, that's fine
        }

        CreateTableRequest createTableRequest =
                CreateTableRequest.of(TEST_TABLE_NAME)
                        .addFamily(FAMILY_METADATA)
                        .addFamily(FAMILY_CONFIG);

        adminClient.createTable(createTableRequest);
    }

    @Test(description = "Delete a single cell and verify all versions are removed")
    public void testDeleteSingleCell() {
        // Given: a cell with a value
        writeCellValue("config#version", FAMILY_METADATA, "version", "1.0.0");

        // Verify the cell exists
        Row rowBefore = dataClient.readRow(TEST_TABLE, ByteString.copyFromUtf8("config#version"));
        assertNotNull(rowBefore, "Row should exist before deletion");
        assertFalse(
                rowBefore.getCells(FAMILY_METADATA, "version").isEmpty(),
                "Cell should exist before deletion");

        // When: deleting the cell
        DeleteCell deleteCell = new DeleteCell();
        deleteCell.setTableId(TEST_TABLE_NAME);
        deleteCell.setRowKey("config#version");
        deleteCell.setFamilyName(FAMILY_METADATA);
        deleteCell.setColumnQualifier("version");

        deleteCell.execute(dataClient);

        // Then: the cell should be deleted
        Row rowAfter = dataClient.readRow(TEST_TABLE, ByteString.copyFromUtf8("config#version"));
        if (rowAfter != null) {
            List<RowCell> cells = rowAfter.getCells(FAMILY_METADATA, "version");
            assertTrue(cells.isEmpty(), "Cell should be deleted - no versions should remain");
        }
        // If rowAfter is null, that's also acceptable (entire row deleted)
    }

    @Test(description = "Delete a cell with multiple versions - verify all versions are removed")
    public void testDeleteCellWithMultipleVersions() {
        // Given: a cell with multiple versions
        String rowKey = "config#multi-version";
        writeCellValueWithTimestamp(rowKey, FAMILY_CONFIG, "value", "version1", 1000000L);
        writeCellValueWithTimestamp(rowKey, FAMILY_CONFIG, "value", "version2", 2000000L);
        writeCellValueWithTimestamp(rowKey, FAMILY_CONFIG, "value", "version3", 3000000L);

        // Verify multiple versions exist
        Row rowBefore = dataClient.readRow(TEST_TABLE, ByteString.copyFromUtf8(rowKey));
        assertNotNull(rowBefore, "Row should exist before deletion");
        List<RowCell> cellsBefore = rowBefore.getCells(FAMILY_CONFIG, "value");
        assertTrue(cellsBefore.size() >= 3, "Should have at least 3 versions before deletion");

        // When: deleting the cell
        DeleteCell deleteCell = new DeleteCell();
        deleteCell.setTableId(TEST_TABLE_NAME);
        deleteCell.setRowKey(rowKey);
        deleteCell.setFamilyName(FAMILY_CONFIG);
        deleteCell.setColumnQualifier("value");

        deleteCell.execute(dataClient);

        // Then: all versions should be deleted
        Row rowAfter = dataClient.readRow(TEST_TABLE, ByteString.copyFromUtf8(rowKey));
        if (rowAfter != null) {
            List<RowCell> cellsAfter = rowAfter.getCells(FAMILY_CONFIG, "value");
            assertTrue(cellsAfter.isEmpty(), "All cell versions should be deleted");
        }
    }

    @Test(description = "Delete one cell but preserve other cells in the same row")
    public void testDeleteCellPreservesOtherCells() {
        // Given: a row with multiple cells
        String rowKey = "config#app";
        writeCellValue(rowKey, FAMILY_METADATA, "name", "MyApp");
        writeCellValue(rowKey, FAMILY_METADATA, "version", "1.0.0");
        writeCellValue(rowKey, FAMILY_CONFIG, "port", "8080");

        // When: deleting only one cell
        DeleteCell deleteCell = new DeleteCell();
        deleteCell.setTableId(TEST_TABLE_NAME);
        deleteCell.setRowKey(rowKey);
        deleteCell.setFamilyName(FAMILY_METADATA);
        deleteCell.setColumnQualifier("version");

        deleteCell.execute(dataClient);

        // Then: the deleted cell should be gone, but other cells should remain
        Row row = dataClient.readRow(TEST_TABLE, ByteString.copyFromUtf8(rowKey));
        assertNotNull(row, "Row should still exist");

        List<RowCell> versionCells = row.getCells(FAMILY_METADATA, "version");
        assertTrue(versionCells.isEmpty(), "Deleted cell should be gone");

        List<RowCell> nameCells = row.getCells(FAMILY_METADATA, "name");
        assertFalse(nameCells.isEmpty(), "Name cell should still exist");
        assertEquals(nameCells.get(0).getValue().toStringUtf8(), "MyApp");

        List<RowCell> portCells = row.getCells(FAMILY_CONFIG, "port");
        assertFalse(portCells.isEmpty(), "Port cell should still exist");
        assertEquals(portCells.get(0).getValue().toStringUtf8(), "8080");
    }

    @Test(description = "Delete cells from different rows")
    public void testDeleteCellsFromDifferentRows() {
        // Given: multiple rows with data
        writeCellValue("user#1", FAMILY_METADATA, "status", "active");
        writeCellValue("user#2", FAMILY_METADATA, "status", "inactive");
        writeCellValue("user#3", FAMILY_METADATA, "status", "active");

        // When: deleting cell from one row
        DeleteCell deleteCell = new DeleteCell();
        deleteCell.setTableId(TEST_TABLE_NAME);
        deleteCell.setRowKey("user#2");
        deleteCell.setFamilyName(FAMILY_METADATA);
        deleteCell.setColumnQualifier("status");

        deleteCell.execute(dataClient);

        // Then: only the specified row's cell should be deleted
        Row row1 = dataClient.readRow(TEST_TABLE, ByteString.copyFromUtf8("user#1"));
        assertNotNull(row1);
        assertFalse(row1.getCells(FAMILY_METADATA, "status").isEmpty());

        Row row2 = dataClient.readRow(TEST_TABLE, ByteString.copyFromUtf8("user#2"));
        if (row2 != null) {
            assertTrue(row2.getCells(FAMILY_METADATA, "status").isEmpty());
        }

        Row row3 = dataClient.readRow(TEST_TABLE, ByteString.copyFromUtf8("user#3"));
        assertNotNull(row3);
        assertFalse(row3.getCells(FAMILY_METADATA, "status").isEmpty());
    }

    @Test(description = "Delete a non-existent cell - should not throw error")
    public void testDeleteNonExistentCell() {
        // Given: a cell that doesn't exist
        String rowKey = "non-existent-row";

        // When: attempting to delete it
        DeleteCell deleteCell = new DeleteCell();
        deleteCell.setTableId(TEST_TABLE_NAME);
        deleteCell.setRowKey(rowKey);
        deleteCell.setFamilyName(FAMILY_METADATA);
        deleteCell.setColumnQualifier("value");

        // Then: should not throw an exception
        deleteCell.execute(dataClient);

        // Verify the row still doesn't exist
        Row row = dataClient.readRow(TEST_TABLE, ByteString.copyFromUtf8(rowKey));
        assertNull(row, "Row should not exist");
    }

    @Test(description = "Delete a deprecated configuration value")
    public void testDeleteDeprecatedConfig() {
        // Given: a deprecated configuration value
        String rowKey = "config#deprecated_setting";
        writeCellValue(rowKey, FAMILY_METADATA, "value", "old_value");

        // Verify it exists
        Row rowBefore = dataClient.readRow(TEST_TABLE, ByteString.copyFromUtf8(rowKey));
        assertNotNull(rowBefore);
        assertFalse(rowBefore.getCells(FAMILY_METADATA, "value").isEmpty());

        // When: deleting the deprecated config
        DeleteCell deleteCell = new DeleteCell();
        deleteCell.setTableId(TEST_TABLE_NAME);
        deleteCell.setRowKey(rowKey);
        deleteCell.setFamilyName(FAMILY_METADATA);
        deleteCell.setColumnQualifier("value");

        deleteCell.execute(dataClient);

        // Then: it should be removed
        Row rowAfter = dataClient.readRow(TEST_TABLE, ByteString.copyFromUtf8(rowKey));
        if (rowAfter != null) {
            assertTrue(rowAfter.getCells(FAMILY_METADATA, "value").isEmpty());
        }
    }

    @Test(description = "Delete multiple cells sequentially")
    public void testDeleteMultipleCellsSequentially() {
        // Given: multiple cells to delete
        String rowKey = "cleanup#session";
        writeCellValue(rowKey, FAMILY_CONFIG, "temp1", "value1");
        writeCellValue(rowKey, FAMILY_CONFIG, "temp2", "value2");
        writeCellValue(rowKey, FAMILY_CONFIG, "temp3", "value3");

        // When: deleting them one by one
        DeleteCell deleteCell1 = new DeleteCell();
        deleteCell1.setTableId(TEST_TABLE_NAME);
        deleteCell1.setRowKey(rowKey);
        deleteCell1.setFamilyName(FAMILY_CONFIG);
        deleteCell1.setColumnQualifier("temp1");
        deleteCell1.execute(dataClient);

        DeleteCell deleteCell2 = new DeleteCell();
        deleteCell2.setTableId(TEST_TABLE_NAME);
        deleteCell2.setRowKey(rowKey);
        deleteCell2.setFamilyName(FAMILY_CONFIG);
        deleteCell2.setColumnQualifier("temp2");
        deleteCell2.execute(dataClient);

        DeleteCell deleteCell3 = new DeleteCell();
        deleteCell3.setTableId(TEST_TABLE_NAME);
        deleteCell3.setRowKey(rowKey);
        deleteCell3.setFamilyName(FAMILY_CONFIG);
        deleteCell3.setColumnQualifier("temp3");
        deleteCell3.execute(dataClient);

        // Then: all should be deleted
        Row row = dataClient.readRow(TEST_TABLE, ByteString.copyFromUtf8(rowKey));
        if (row != null) {
            assertTrue(row.getCells(FAMILY_CONFIG, "temp1").isEmpty());
            assertTrue(row.getCells(FAMILY_CONFIG, "temp2").isEmpty());
            assertTrue(row.getCells(FAMILY_CONFIG, "temp3").isEmpty());
        }
    }

    @Test(
            description = "Verify DeleteCell fails with non-existent table",
            expectedExceptions = Exception.class)
    public void testDeleteCellWithNonExistentTable() {
        // Given: a DeleteCell action for a non-existent table
        DeleteCell deleteCell = new DeleteCell();
        deleteCell.setTableId("non-existent-table");
        deleteCell.setRowKey("key");
        deleteCell.setFamilyName(FAMILY_METADATA);
        deleteCell.setColumnQualifier("value");

        // When/Then: execution should fail
        deleteCell.execute(dataClient);
    }

    @Test(
            description = "Verify DeleteCell fails with non-existent column family",
            expectedExceptions = Exception.class)
    public void testDeleteCellWithNonExistentFamily() {
        // Given: a DeleteCell action with non-existent column family
        DeleteCell deleteCell = new DeleteCell();
        deleteCell.setTableId(TEST_TABLE_NAME);
        deleteCell.setRowKey("key");
        deleteCell.setFamilyName("non-existent-family");
        deleteCell.setColumnQualifier("value");

        // When/Then: execution should fail
        deleteCell.execute(dataClient);
    }

    // Helper methods

    /**
     * Writes a cell value to the test table.
     *
     * @param rowKey the row key
     * @param familyName the column family name
     * @param columnQualifier the column qualifier
     * @param value the value to write
     */
    private void writeCellValue(
            String rowKey, String familyName, String columnQualifier, String value) {
        RowMutation mutation = RowMutation.create(TEST_TABLE, ByteString.copyFromUtf8(rowKey));
        mutation.setCell(
                familyName,
                ByteString.copyFromUtf8(columnQualifier),
                ByteString.copyFromUtf8(value));
        dataClient.mutateRow(mutation);
    }

    /**
     * Writes a cell value with a specific timestamp to the test table.
     *
     * @param rowKey the row key
     * @param familyName the column family name
     * @param columnQualifier the column qualifier
     * @param value the value to write
     * @param timestampMicros the timestamp in microseconds
     */
    private void writeCellValueWithTimestamp(
            String rowKey,
            String familyName,
            String columnQualifier,
            String value,
            long timestampMicros) {
        RowMutation mutation = RowMutation.create(TEST_TABLE, ByteString.copyFromUtf8(rowKey));
        mutation.setCell(
                familyName,
                ByteString.copyFromUtf8(columnQualifier),
                timestampMicros,
                ByteString.copyFromUtf8(value));
        dataClient.mutateRow(mutation);
    }
}
