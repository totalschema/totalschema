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

package io.github.totalschema.connector.gcp.bigtable.actions.data.setcell;

import static org.testng.Assert.*;

import com.google.cloud.bigtable.admin.v2.models.CreateTableRequest;
import com.google.cloud.bigtable.data.v2.models.Row;
import com.google.cloud.bigtable.data.v2.models.RowCell;
import com.google.cloud.bigtable.data.v2.models.TableId;
import com.google.protobuf.ByteString;
import io.github.totalschema.connector.gcp.bigtable.AbstractBigtableDataIntegrationTest;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Integration tests for {@link SetCell} using BigTable emulator in a TestContainer.
 *
 * <p>These tests verify that SetCell works correctly against the BigTable emulator. The tests are
 * skipped if Docker/Podman is not available.
 */
public class SetCellIntegrationTest extends AbstractBigtableDataIntegrationTest {

    private static final String TEST_TABLE_NAME = "test-setcell-table";
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

    @Test(description = "Set a simple cell value and verify it can be read back")
    public void testSetSimpleCell() {
        // Given: a SetCell action with a simple string value
        SetCell setCell = new SetCell();
        setCell.setTableId(TEST_TABLE_NAME);
        setCell.setRowKey("config#version");
        setCell.setFamilyName(FAMILY_METADATA);
        setCell.setColumnQualifier("version");
        setCell.setValue("2.5.0");

        // When: executing the action
        setCell.execute(dataClient);

        // Then: the value should be readable from the table
        Row row = dataClient.readRow(TEST_TABLE, ByteString.copyFromUtf8("config#version"));
        assertNotNull(row, "Row should exist after SetCell");

        List<RowCell> cells = row.getCells(FAMILY_METADATA, "version");
        assertFalse(cells.isEmpty(), "Cell should exist in the row");
        assertEquals(cells.get(0).getValue().toStringUtf8(), "2.5.0");
    }

    @Test(description = "Set multiple cells in the same row")
    public void testSetMultipleCellsInSameRow() {
        // Given: multiple SetCell actions for the same row
        SetCell setCell1 = new SetCell();
        setCell1.setTableId(TEST_TABLE_NAME);
        setCell1.setRowKey("config#app");
        setCell1.setFamilyName(FAMILY_METADATA);
        setCell1.setColumnQualifier("name");
        setCell1.setValue("MyApp");

        SetCell setCell2 = new SetCell();
        setCell2.setTableId(TEST_TABLE_NAME);
        setCell2.setRowKey("config#app");
        setCell2.setFamilyName(FAMILY_METADATA);
        setCell2.setColumnQualifier("version");
        setCell2.setValue("1.0.0");

        SetCell setCell3 = new SetCell();
        setCell3.setTableId(TEST_TABLE_NAME);
        setCell3.setRowKey("config#app");
        setCell3.setFamilyName(FAMILY_CONFIG);
        setCell3.setColumnQualifier("port");
        setCell3.setValue("8080");

        // When: executing all actions
        setCell1.execute(dataClient);
        setCell2.execute(dataClient);
        setCell3.execute(dataClient);

        // Then: all values should be readable
        Row row = dataClient.readRow(TEST_TABLE, ByteString.copyFromUtf8("config#app"));
        assertNotNull(row, "Row should exist");

        List<RowCell> nameCells = row.getCells(FAMILY_METADATA, "name");
        assertFalse(nameCells.isEmpty());
        assertEquals(nameCells.get(0).getValue().toStringUtf8(), "MyApp");

        List<RowCell> versionCells = row.getCells(FAMILY_METADATA, "version");
        assertFalse(versionCells.isEmpty());
        assertEquals(versionCells.get(0).getValue().toStringUtf8(), "1.0.0");

        List<RowCell> portCells = row.getCells(FAMILY_CONFIG, "port");
        assertFalse(portCells.isEmpty());
        assertEquals(portCells.get(0).getValue().toStringUtf8(), "8080");
    }

    @Test(description = "Set cell with custom timestamp")
    public void testSetCellWithCustomTimestamp() {
        // Given: a SetCell action with a custom timestamp
        long customTimestamp = 1692288000000000L; // Microseconds
        SetCell setCell = new SetCell();
        setCell.setTableId(TEST_TABLE_NAME);
        setCell.setRowKey("setting#timestamped");
        setCell.setFamilyName(FAMILY_CONFIG);
        setCell.setColumnQualifier("value");
        setCell.setValue("timestamped-value");
        setCell.setTimestampMicros(customTimestamp);

        // When: executing the action
        setCell.execute(dataClient);

        // Then: the value and custom timestamp should be preserved
        Row row = dataClient.readRow(TEST_TABLE, ByteString.copyFromUtf8("setting#timestamped"));
        assertNotNull(row, "Row should exist");

        List<RowCell> cells = row.getCells(FAMILY_CONFIG, "value");
        assertFalse(cells.isEmpty());
        assertEquals(cells.get(0).getValue().toStringUtf8(), "timestamped-value");
        assertEquals(cells.get(0).getTimestamp(), customTimestamp);
    }

    @Test(description = "Set cell with empty string value")
    public void testSetCellWithEmptyString() {
        // Given: a SetCell action with an empty string value
        SetCell setCell = new SetCell();
        setCell.setTableId(TEST_TABLE_NAME);
        setCell.setRowKey("config#empty");
        setCell.setFamilyName(FAMILY_METADATA);
        setCell.setColumnQualifier("empty_field");
        setCell.setValue("");

        // When: executing the action
        setCell.execute(dataClient);

        // Then: the cell should exist with empty string value
        Row row = dataClient.readRow(TEST_TABLE, ByteString.copyFromUtf8("config#empty"));
        assertNotNull(row, "Row should exist");

        List<RowCell> cells = row.getCells(FAMILY_METADATA, "empty_field");
        assertFalse(cells.isEmpty());
        assertEquals(cells.get(0).getValue().toStringUtf8(), "");
    }

    @Test(description = "Update an existing cell value (create new version)")
    public void testUpdateExistingCell() {
        // Given: a cell with an initial value
        SetCell setCell1 = new SetCell();
        setCell1.setTableId(TEST_TABLE_NAME);
        setCell1.setRowKey("config#updatable");
        setCell1.setFamilyName(FAMILY_CONFIG);
        setCell1.setColumnQualifier("value");
        setCell1.setValue("initial-value");

        setCell1.execute(dataClient);

        // When: updating the cell with a new value
        SetCell setCell2 = new SetCell();
        setCell2.setTableId(TEST_TABLE_NAME);
        setCell2.setRowKey("config#updatable");
        setCell2.setFamilyName(FAMILY_CONFIG);
        setCell2.setColumnQualifier("value");
        setCell2.setValue("updated-value");

        setCell2.execute(dataClient);

        // Then: the latest value should be the updated value
        Row row = dataClient.readRow(TEST_TABLE, "config#updatable");
        assertNotNull(row, "Row should exist");

        List<RowCell> cells = row.getCells(FAMILY_CONFIG, "value");
        assertFalse(cells.isEmpty());
        // The most recent cell should be the updated value
        assertEquals(cells.get(0).getValue().toStringUtf8(), "updated-value");
    }

    @Test(description = "Set cell value from a file")
    public void testSetCellFromFile() throws IOException {
        // Given: a temporary file with content
        File tempFile = File.createTempFile("test-setcell-", ".txt");
        try {
            String fileContent = "This is content from a file\nWith multiple lines";
            Files.write(tempFile.toPath(), fileContent.getBytes());

            SetCell setCell = new SetCell();
            setCell.setTableId(TEST_TABLE_NAME);
            setCell.setRowKey("file#content");
            setCell.setFamilyName(FAMILY_METADATA);
            setCell.setColumnQualifier("data");
            setCell.setValueFile(tempFile.getAbsolutePath());

            // When: executing the action
            setCell.execute(dataClient);

            // Then: the file content should be stored in the cell
            Row row = dataClient.readRow(TEST_TABLE, "file#content");
            assertNotNull(row, "Row should exist");

            List<RowCell> cells = row.getCells(FAMILY_METADATA, "data");
            assertFalse(cells.isEmpty());
            assertEquals(cells.get(0).getValue().toStringUtf8(), fileContent);
        } finally {
            tempFile.delete();
        }
    }

    @Test(description = "Set cell with binary data from file")
    public void testSetCellWithBinaryDataFromFile() throws IOException {
        // Given: a file with binary data
        File tempFile = File.createTempFile("test-binary-", ".bin");
        try {
            byte[] binaryData = new byte[] {0x00, 0x01, 0x02, (byte) 0xFF, (byte) 0xFE};
            Files.write(tempFile.toPath(), binaryData);

            SetCell setCell = new SetCell();
            setCell.setTableId(TEST_TABLE_NAME);
            setCell.setRowKey("binary#data");
            setCell.setFamilyName(FAMILY_CONFIG);
            setCell.setColumnQualifier("bytes");
            setCell.setValueFile(tempFile.getAbsolutePath());

            // When: executing the action
            setCell.execute(dataClient);

            // Then: the binary data should be stored correctly
            Row row = dataClient.readRow(TEST_TABLE, ByteString.copyFromUtf8("binary#data"));
            assertNotNull(row, "Row should exist");

            List<RowCell> cells = row.getCells(FAMILY_CONFIG, "bytes");
            assertFalse(cells.isEmpty());
            ByteString storedBytes = cells.get(0).getValue();
            assertTrue(
                    Arrays.equals(storedBytes.toByteArray(), binaryData),
                    "Binary data should match");
        } finally {
            if (!tempFile.delete()) {
                tempFile.deleteOnExit();
            }
        }
    }

    @Test(description = "Set cells in different rows and families")
    public void testSetCellsInDifferentRows() {
        // Given: SetCell actions for different rows
        SetCell setCell1 = new SetCell();
        setCell1.setTableId(TEST_TABLE_NAME);
        setCell1.setRowKey("user#1");
        setCell1.setFamilyName(FAMILY_METADATA);
        setCell1.setColumnQualifier("name");
        setCell1.setValue("Alice");

        SetCell setCell2 = new SetCell();
        setCell2.setTableId(TEST_TABLE_NAME);
        setCell2.setRowKey("user#2");
        setCell2.setFamilyName(FAMILY_METADATA);
        setCell2.setColumnQualifier("name");
        setCell2.setValue("Bob");

        SetCell setCell3 = new SetCell();
        setCell3.setTableId(TEST_TABLE_NAME);
        setCell3.setRowKey("user#3");
        setCell3.setFamilyName(FAMILY_METADATA);
        setCell3.setColumnQualifier("name");
        setCell3.setValue("Charlie");

        // When: executing all actions
        setCell1.execute(dataClient);
        setCell2.execute(dataClient);
        setCell3.execute(dataClient);

        // Then: all rows should exist with correct values
        Row row1 = dataClient.readRow(TEST_TABLE, ByteString.copyFromUtf8("user#1"));
        assertNotNull(row1);
        assertEquals(
                row1.getCells(FAMILY_METADATA, "name").get(0).getValue().toStringUtf8(), "Alice");

        Row row2 = dataClient.readRow(TEST_TABLE, ByteString.copyFromUtf8("user#2"));
        assertNotNull(row2);
        assertEquals(
                row2.getCells(FAMILY_METADATA, "name").get(0).getValue().toStringUtf8(), "Bob");

        Row row3 = dataClient.readRow(TEST_TABLE, ByteString.copyFromUtf8("user#3"));
        assertNotNull(row3);
        assertEquals(
                row3.getCells(FAMILY_METADATA, "name").get(0).getValue().toStringUtf8(), "Charlie");
    }

    @Test(description = "Set cell with special characters in value")
    public void testSetCellWithSpecialCharacters() {
        // Given: a SetCell action with special characters
        String specialValue = "Special: !@#$%^&*()_+-={}[]|\\:\";<>?,./\n\t";
        SetCell setCell = new SetCell();
        setCell.setTableId(TEST_TABLE_NAME);
        setCell.setRowKey("special#chars");
        setCell.setFamilyName(FAMILY_CONFIG);
        setCell.setColumnQualifier("special_value");
        setCell.setValue(specialValue);

        // When: executing the action
        setCell.execute(dataClient);

        // Then: the special characters should be preserved
        Row row = dataClient.readRow(TEST_TABLE, ByteString.copyFromUtf8("special#chars"));
        assertNotNull(row, "Row should exist");

        List<RowCell> cells = row.getCells(FAMILY_CONFIG, "special_value");
        assertFalse(cells.isEmpty());
        assertEquals(cells.get(0).getValue().toStringUtf8(), specialValue);
    }

    @Test(description = "Set cell with unicode characters")
    public void testSetCellWithUnicodeCharacters() {
        // Given: a SetCell action with unicode characters
        String unicodeValue = "Hello 世界 🌍 Привет مرحبا";
        SetCell setCell = new SetCell();
        setCell.setTableId(TEST_TABLE_NAME);
        setCell.setRowKey("unicode#text");
        setCell.setFamilyName(FAMILY_METADATA);
        setCell.setColumnQualifier("message");
        setCell.setValue(unicodeValue);

        // When: executing the action
        setCell.execute(dataClient);

        // Then: the unicode characters should be preserved
        Row row = dataClient.readRow(TEST_TABLE, ByteString.copyFromUtf8("unicode#text"));
        assertNotNull(row, "Row should exist");

        List<RowCell> cells = row.getCells(FAMILY_METADATA, "message");
        assertFalse(cells.isEmpty());
        assertEquals(cells.get(0).getValue().toStringUtf8(), unicodeValue);
    }

    @Test(
            description = "Verify SetCell fails with non-existent table",
            expectedExceptions = Exception.class)
    public void testSetCellWithNonExistentTable() {
        // Given: a SetCell action for a non-existent table
        SetCell setCell = new SetCell();
        setCell.setTableId("non-existent-table");
        setCell.setRowKey("key");
        setCell.setFamilyName(FAMILY_METADATA);
        setCell.setColumnQualifier("value");
        setCell.setValue("data");

        // When/Then: execution should fail
        setCell.execute(dataClient);
    }

    @Test(
            description = "Verify SetCell fails with non-existent column family",
            expectedExceptions = Exception.class)
    public void testSetCellWithNonExistentFamily() {
        // Given: a SetCell action with non-existent column family
        SetCell setCell = new SetCell();
        setCell.setTableId(TEST_TABLE_NAME);
        setCell.setRowKey("key");
        setCell.setFamilyName("non-existent-family");
        setCell.setColumnQualifier("value");
        setCell.setValue("data");

        // When/Then: execution should fail
        setCell.execute(dataClient);
    }
}
