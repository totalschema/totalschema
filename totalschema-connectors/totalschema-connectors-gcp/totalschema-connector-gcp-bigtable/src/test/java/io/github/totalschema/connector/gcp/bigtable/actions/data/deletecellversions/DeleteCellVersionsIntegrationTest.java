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

package io.github.totalschema.connector.gcp.bigtable.actions.data.deletecellversions;

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
 * Integration tests for {@link DeleteCellVersions} using BigTable emulator in a TestContainer.
 *
 * <p>These tests verify that DeleteCellVersions works correctly against the BigTable emulator. The
 * tests are skipped if Docker/Podman is not available.
 */
public class DeleteCellVersionsIntegrationTest extends AbstractBigtableDataIntegrationTest {

    private static final String TEST_TABLE_NAME = "test-deletecellversions-table";
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

    @Test(description = "Delete cell versions within a specific time range")
    public void testDeleteCellVersionsWithinRange() {
        // Given: a cell with multiple versions at different timestamps
        String rowKey = "log#123";
        writeCellValueWithTimestamp(rowKey, FAMILY_METADATA, "event", "event1", 1000000L);
        writeCellValueWithTimestamp(rowKey, FAMILY_METADATA, "event", "event2", 2000000L);
        writeCellValueWithTimestamp(rowKey, FAMILY_METADATA, "event", "event3", 3000000L);
        writeCellValueWithTimestamp(rowKey, FAMILY_METADATA, "event", "event4", 4000000L);
        writeCellValueWithTimestamp(rowKey, FAMILY_METADATA, "event", "event5", 5000000L);

        // Verify all versions exist
        Row rowBefore = dataClient.readRow(TEST_TABLE, ByteString.copyFromUtf8(rowKey));
        assertNotNull(rowBefore);
        List<RowCell> cellsBefore = rowBefore.getCells(FAMILY_METADATA, "event");
        assertTrue(cellsBefore.size() >= 5, "Should have at least 5 versions");

        // When: deleting versions between 1.5s and 4.5s (exclusive end)
        DeleteCellVersions deleteCellVersions = new DeleteCellVersions();
        deleteCellVersions.setTableId(TEST_TABLE_NAME);
        deleteCellVersions.setRowKey(rowKey);
        deleteCellVersions.setFamilyName(FAMILY_METADATA);
        deleteCellVersions.setColumnQualifier("event");
        deleteCellVersions.setStartTimestampMicros(1500000L);
        deleteCellVersions.setEndTimestampMicros(4500000L);

        deleteCellVersions.execute(dataClient);

        // Then: only versions outside the range should remain
        Row rowAfter = dataClient.readRow(TEST_TABLE, ByteString.copyFromUtf8(rowKey));
        assertNotNull(rowAfter);
        List<RowCell> cellsAfter = rowAfter.getCells(FAMILY_METADATA, "event");

        // Versions at 1000000 (< 1500000) and 5000000 (>= 4500000) should remain
        assertTrue(cellsAfter.size() >= 2, "Should have at least 2 versions remaining");

        boolean hasOldVersion = false;
        boolean hasNewVersion = false;
        for (RowCell cell : cellsAfter) {
            if (cell.getTimestamp() < 1500000L) {
                hasOldVersion = true;
            }
            if (cell.getTimestamp() >= 4500000L) {
                hasNewVersion = true;
            }
            // Ensure no versions in the deleted range remain
            assertTrue(
                    cell.getTimestamp() < 1500000L || cell.getTimestamp() >= 4500000L,
                    "Version at " + cell.getTimestamp() + " should have been deleted");
        }

        assertTrue(hasOldVersion, "Should have version before start timestamp");
        assertTrue(hasNewVersion, "Should have version after end timestamp");
    }

    @Test(description = "Delete all cell versions after a specific timestamp")
    public void testDeleteCellVersionsAfterTimestamp() {
        // Given: a cell with multiple versions
        String rowKey = "session#user-123";
        writeCellValueWithTimestamp(rowKey, FAMILY_CONFIG, "data", "data1", 1000000L);
        writeCellValueWithTimestamp(rowKey, FAMILY_CONFIG, "data", "data2", 2000000L);
        writeCellValueWithTimestamp(rowKey, FAMILY_CONFIG, "data", "data3", 3000000L);
        writeCellValueWithTimestamp(rowKey, FAMILY_CONFIG, "data", "data4", 4000000L);

        // When: deleting all versions after 2.5s
        DeleteCellVersions deleteCellVersions = new DeleteCellVersions();
        deleteCellVersions.setTableId(TEST_TABLE_NAME);
        deleteCellVersions.setRowKey(rowKey);
        deleteCellVersions.setFamilyName(FAMILY_CONFIG);
        deleteCellVersions.setColumnQualifier("data");
        deleteCellVersions.setStartTimestampMicros(2500000L);
        // No end timestamp means delete to the end

        deleteCellVersions.execute(dataClient);

        // Then: only versions before start timestamp should remain
        Row rowAfter = dataClient.readRow(TEST_TABLE, ByteString.copyFromUtf8(rowKey));
        assertNotNull(rowAfter);
        List<RowCell> cellsAfter = rowAfter.getCells(FAMILY_CONFIG, "data");

        assertTrue(cellsAfter.size() >= 2, "Should have versions before cutoff");
        for (RowCell cell : cellsAfter) {
            assertTrue(
                    cell.getTimestamp() < 2500000L,
                    "Only versions before " + 2500000L + " should remain");
        }
    }

    @Test(description = "Delete all cell versions before a specific timestamp")
    public void testDeleteCellVersionsBeforeTimestamp() {
        // Given: a cell with multiple versions
        String rowKey = "log#456";
        writeCellValueWithTimestamp(rowKey, FAMILY_METADATA, "data", "old1", 1000000L);
        writeCellValueWithTimestamp(rowKey, FAMILY_METADATA, "data", "old2", 2000000L);
        writeCellValueWithTimestamp(rowKey, FAMILY_METADATA, "data", "new1", 3000000L);
        writeCellValueWithTimestamp(rowKey, FAMILY_METADATA, "data", "new2", 4000000L);

        // When: deleting all versions before 2.5s
        DeleteCellVersions deleteCellVersions = new DeleteCellVersions();
        deleteCellVersions.setTableId(TEST_TABLE_NAME);
        deleteCellVersions.setRowKey(rowKey);
        deleteCellVersions.setFamilyName(FAMILY_METADATA);
        deleteCellVersions.setColumnQualifier("data");
        // No start timestamp means delete from the beginning
        deleteCellVersions.setEndTimestampMicros(2500000L);

        deleteCellVersions.execute(dataClient);

        // Then: only versions after end timestamp should remain
        Row rowAfter = dataClient.readRow(TEST_TABLE, ByteString.copyFromUtf8(rowKey));
        assertNotNull(rowAfter);
        List<RowCell> cellsAfter = rowAfter.getCells(FAMILY_METADATA, "data");

        assertTrue(cellsAfter.size() >= 2, "Should have versions after cutoff");
        for (RowCell cell : cellsAfter) {
            assertTrue(
                    cell.getTimestamp() >= 2500000L,
                    "Only versions at or after " + 2500000L + " should remain");
        }
    }

    @Test(description = "Delete cell versions for GDPR compliance - time-based data deletion")
    public void testGdprComplianceDataDeletion() {
        // Given: audit log with events over time
        String rowKey = "audit#user-data";
        writeCellValueWithTimestamp(
                rowKey, FAMILY_METADATA, "details", "event1", 1692201600000000L);
        writeCellValueWithTimestamp(
                rowKey, FAMILY_METADATA, "details", "event2", 1692205200000000L);
        writeCellValueWithTimestamp(
                rowKey, FAMILY_METADATA, "details", "event3", 1692288000000000L);
        writeCellValueWithTimestamp(
                rowKey, FAMILY_METADATA, "details", "event4", 1692374400000000L);

        // When: deleting events within GDPR retention period
        DeleteCellVersions deleteCellVersions = new DeleteCellVersions();
        deleteCellVersions.setTableId(TEST_TABLE_NAME);
        deleteCellVersions.setRowKey(rowKey);
        deleteCellVersions.setFamilyName(FAMILY_METADATA);
        deleteCellVersions.setColumnQualifier("details");
        deleteCellVersions.setStartTimestampMicros(1692201600000000L);
        deleteCellVersions.setEndTimestampMicros(1692288000000000L);

        deleteCellVersions.execute(dataClient);

        // Then: only versions outside the GDPR period should remain
        Row rowAfter = dataClient.readRow(TEST_TABLE, ByteString.copyFromUtf8(rowKey));
        assertNotNull(rowAfter);
        List<RowCell> cellsAfter = rowAfter.getCells(FAMILY_METADATA, "details");

        for (RowCell cell : cellsAfter) {
            long timestamp = cell.getTimestamp();
            assertTrue(
                    timestamp < 1692201600000000L || timestamp >= 1692288000000L,
                    "Versions within GDPR period should be deleted");
        }
    }

    @Test(description = "Delete specific versions while keeping others in the same row")
    public void testDeleteVersionsPreservesOtherCells() {
        // Given: a row with multiple cells, one with multiple versions
        String rowKey = "data#mixed";
        writeCellValueWithTimestamp(rowKey, FAMILY_CONFIG, "versioned", "v1", 1000000L);
        writeCellValueWithTimestamp(rowKey, FAMILY_CONFIG, "versioned", "v2", 2000000L);
        writeCellValueWithTimestamp(rowKey, FAMILY_CONFIG, "versioned", "v3", 3000000L);
        writeCellValue(rowKey, FAMILY_CONFIG, "static", "keep-this");

        // When: deleting versions from one cell
        DeleteCellVersions deleteCellVersions = new DeleteCellVersions();
        deleteCellVersions.setTableId(TEST_TABLE_NAME);
        deleteCellVersions.setRowKey(rowKey);
        deleteCellVersions.setFamilyName(FAMILY_CONFIG);
        deleteCellVersions.setColumnQualifier("versioned");
        deleteCellVersions.setStartTimestampMicros(1500000L);
        deleteCellVersions.setEndTimestampMicros(2500000L);

        deleteCellVersions.execute(dataClient);

        // Then: other cells should be unaffected
        Row row = dataClient.readRow(TEST_TABLE, ByteString.copyFromUtf8(rowKey));
        assertNotNull(row);

        List<RowCell> staticCells = row.getCells(FAMILY_CONFIG, "static");
        assertFalse(staticCells.isEmpty(), "Static cell should be preserved");
        assertEquals(staticCells.get(0).getValue().toStringUtf8(), "keep-this");

        List<RowCell> versionedCells = row.getCells(FAMILY_CONFIG, "versioned");
        assertTrue(versionedCells.size() >= 2, "Should have versions outside the range");
    }

    @Test(description = "Delete versions from non-existent cell - should not throw error")
    public void testDeleteVersionsFromNonExistentCell() {
        // Given: a cell that doesn't exist
        String rowKey = "non-existent-row";

        // When: attempting to delete versions
        DeleteCellVersions deleteCellVersions = new DeleteCellVersions();
        deleteCellVersions.setTableId(TEST_TABLE_NAME);
        deleteCellVersions.setRowKey(rowKey);
        deleteCellVersions.setFamilyName(FAMILY_METADATA);
        deleteCellVersions.setColumnQualifier("value");
        deleteCellVersions.setStartTimestampMicros(1000000L);
        deleteCellVersions.setEndTimestampMicros(2000000L);

        // Then: should not throw an exception
        deleteCellVersions.execute(dataClient);

        // Verify the row still doesn't exist
        Row row = dataClient.readRow(TEST_TABLE, ByteString.copyFromUtf8(rowKey));
        assertNull(row, "Row should not exist");
    }

    @Test(description = "Revert bad data by deleting versions in a specific time window")
    public void testRevertBadDataByTimestamp() {
        // Given: data with a bad write in the middle
        String rowKey = "config#revertable";
        writeCellValueWithTimestamp(rowKey, FAMILY_CONFIG, "value", "good1", 1000000L);
        writeCellValueWithTimestamp(rowKey, FAMILY_CONFIG, "value", "bad", 2000000L);
        writeCellValueWithTimestamp(rowKey, FAMILY_CONFIG, "value", "good2", 3000000L);

        // When: reverting the bad data by timestamp
        DeleteCellVersions deleteCellVersions = new DeleteCellVersions();
        deleteCellVersions.setTableId(TEST_TABLE_NAME);
        deleteCellVersions.setRowKey(rowKey);
        deleteCellVersions.setFamilyName(FAMILY_CONFIG);
        deleteCellVersions.setColumnQualifier("value");
        deleteCellVersions.setStartTimestampMicros(1900000L);
        deleteCellVersions.setEndTimestampMicros(2100000L);

        deleteCellVersions.execute(dataClient);

        // Then: the bad version should be gone, good versions should remain
        Row row = dataClient.readRow(TEST_TABLE, ByteString.copyFromUtf8(rowKey));
        assertNotNull(row);
        List<RowCell> cells = row.getCells(FAMILY_CONFIG, "value");

        assertTrue(cells.size() >= 2, "Should have at least 2 good versions");
        for (RowCell cell : cells) {
            String value = cell.getValue().toStringUtf8();
            assertFalse(value.equals("bad"), "Bad version should be deleted");
        }
    }

    @Test(
            description = "Verify validation fails without timestamp filters",
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*At least one.*must be specified.*")
    public void testValidationRequiresTimestampFilter() {
        // Given: a DeleteCellVersions action without timestamps
        DeleteCellVersions deleteCellVersions = new DeleteCellVersions();
        deleteCellVersions.setTableId(TEST_TABLE_NAME);
        deleteCellVersions.setRowKey("key");
        deleteCellVersions.setFamilyName(FAMILY_METADATA);
        deleteCellVersions.setColumnQualifier("value");
        // No timestamps set

        // When/Then: execution should fail with validation error
        deleteCellVersions.execute(dataClient);
    }

    @Test(
            description = "Verify validation fails when start >= end",
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*startTimestampMicros must be less than.*")
    public void testValidationStartMustBeLessThanEnd() {
        // Given: a DeleteCellVersions action with invalid timestamp range
        DeleteCellVersions deleteCellVersions = new DeleteCellVersions();
        deleteCellVersions.setTableId(TEST_TABLE_NAME);
        deleteCellVersions.setRowKey("key");
        deleteCellVersions.setFamilyName(FAMILY_METADATA);
        deleteCellVersions.setColumnQualifier("value");
        deleteCellVersions.setStartTimestampMicros(3000000L);
        deleteCellVersions.setEndTimestampMicros(1000000L); // end < start

        // When/Then: execution should fail with validation error
        deleteCellVersions.execute(dataClient);
    }

    @Test(
            description = "Verify DeleteCellVersions fails with non-existent table",
            expectedExceptions = Exception.class)
    public void testDeleteCellVersionsWithNonExistentTable() {
        // Given: a DeleteCellVersions action for a non-existent table
        DeleteCellVersions deleteCellVersions = new DeleteCellVersions();
        deleteCellVersions.setTableId("non-existent-table");
        deleteCellVersions.setRowKey("key");
        deleteCellVersions.setFamilyName(FAMILY_METADATA);
        deleteCellVersions.setColumnQualifier("value");
        deleteCellVersions.setStartTimestampMicros(1000000L);

        // When/Then: execution should fail
        deleteCellVersions.execute(dataClient);
    }

    @Test(
            description = "Verify DeleteCellVersions fails with non-existent column family",
            expectedExceptions = Exception.class)
    public void testDeleteCellVersionsWithNonExistentFamily() {
        // Given: a DeleteCellVersions action with non-existent column family
        DeleteCellVersions deleteCellVersions = new DeleteCellVersions();
        deleteCellVersions.setTableId(TEST_TABLE_NAME);
        deleteCellVersions.setRowKey("key");
        deleteCellVersions.setFamilyName("non-existent-family");
        deleteCellVersions.setColumnQualifier("value");
        deleteCellVersions.setEndTimestampMicros(2000000L);

        // When/Then: execution should fail
        deleteCellVersions.execute(dataClient);
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
