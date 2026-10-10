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

package io.github.totalschema.connector.gcp.bigtable.actions.table.deletetable;

import static org.testng.Assert.*;

import io.github.totalschema.connector.gcp.bigtable.AbstractBigtableAdminIntegrationTest;
import io.github.totalschema.connector.gcp.bigtable.actions.table.createtable.ColumnFamily;
import io.github.totalschema.connector.gcp.bigtable.actions.table.createtable.CreateTable;
import java.util.List;
import org.testng.annotations.Test;

/**
 * Integration tests for {@link DeleteTable} using BigTable emulator in a TestContainer.
 *
 * <p>These tests verify that DeleteTable works correctly against the BigTable emulator. The tests
 * are skipped if Docker/Podman is not available.
 */
public class DeleteTableIntegrationTest extends AbstractBigtableAdminIntegrationTest {

    @Test(description = "Delete a simple table without families in BigTable emulator")
    public void testDeleteSimpleTable() {
        // Given: a table exists without families
        String tableId = "test-delete-simple-table";
        createSimpleTable(tableId);
        assertTrue(tableExists(tableId), "Table should exist before deletion");

        // When: deleting the table
        DeleteTable deleteTable = new DeleteTable();
        deleteTable.setTableId(tableId);
        deleteTable.execute(adminClient);

        // Then: table should no longer exist
        assertFalse(tableExists(tableId), "Table should not exist after deletion");
    }

    @Test(description = "Delete a table with column families in BigTable emulator")
    public void testDeleteTableWithFamilies() {
        // Given: a table exists with column families
        String tableId = "test-delete-families-table";
        CreateTable createTable = new CreateTable();
        createTable.setTableId(tableId);

        ColumnFamily family1 = new ColumnFamily();
        family1.setName("profile");

        ColumnFamily family2 = new ColumnFamily();
        family2.setName("settings");

        createTable.setFamilies(List.of(family1, family2));
        createTable.execute(adminClient);

        assertTrue(tableExists(tableId), "Table should exist before deletion");

        // When: deleting the table
        DeleteTable deleteTable = new DeleteTable();
        deleteTable.setTableId(tableId);
        deleteTable.execute(adminClient);

        // Then: table should no longer exist
        assertFalse(tableExists(tableId), "Table should not exist after deletion");
    }

    @Test(description = "Delete a table with column families with GC rules")
    public void testDeleteTableWithGcRules() {
        // Given: a table exists with families and GC rules
        String tableId = "test-delete-gc-table";
        CreateTable createTable = new CreateTable();
        createTable.setTableId(tableId);

        ColumnFamily family1 = new ColumnFamily();
        family1.setName("profile");
        family1.setMaxVersions(3);

        ColumnFamily family2 = new ColumnFamily();
        family2.setName("audit");
        family2.setMaxAge("90d");

        createTable.setFamilies(List.of(family1, family2));
        createTable.execute(adminClient);

        assertTrue(tableExists(tableId), "Table should exist before deletion");

        // When: deleting the table
        DeleteTable deleteTable = new DeleteTable();
        deleteTable.setTableId(tableId);
        deleteTable.execute(adminClient);

        // Then: table should no longer exist
        assertFalse(tableExists(tableId), "Table should not exist after deletion");
    }

    @Test(description = "Delete multiple tables in sequence")
    public void testDeleteMultipleTables() {
        // Given: multiple tables exist
        String tableId1 = "test-delete-multi-table-1";
        String tableId2 = "test-delete-multi-table-2";
        String tableId3 = "test-delete-multi-table-3";

        createSimpleTable(tableId1);
        createSimpleTable(tableId2);
        createSimpleTable(tableId3);

        assertTrue(tableExists(tableId1), "First table should exist before deletion");
        assertTrue(tableExists(tableId2), "Second table should exist before deletion");
        assertTrue(tableExists(tableId3), "Third table should exist before deletion");

        // When: deleting all tables
        DeleteTable deleteTable1 = new DeleteTable();
        deleteTable1.setTableId(tableId1);
        deleteTable1.execute(adminClient);

        DeleteTable deleteTable2 = new DeleteTable();
        deleteTable2.setTableId(tableId2);
        deleteTable2.execute(adminClient);

        DeleteTable deleteTable3 = new DeleteTable();
        deleteTable3.setTableId(tableId3);
        deleteTable3.execute(adminClient);

        // Then: all tables should be deleted
        assertFalse(tableExists(tableId1), "First table should not exist after deletion");
        assertFalse(tableExists(tableId2), "Second table should not exist after deletion");
        assertFalse(tableExists(tableId3), "Third table should not exist after deletion");
    }

    @Test(
            description = "Verify deleting a non-existent table fails appropriately",
            expectedExceptions = Exception.class)
    public void testDeleteNonExistentTableFails() {
        // Given: a table that does not exist
        String tableId = "test-delete-non-existent-table";
        assertFalse(tableExists(tableId), "Table should not exist initially");

        // When/Then: attempting to delete it should fail
        DeleteTable deleteTable = new DeleteTable();
        deleteTable.setTableId(tableId);
        deleteTable.execute(adminClient);
    }

    @Test(description = "Delete table and verify it cannot be queried afterwards")
    public void testDeleteTablePreventsQuery() {
        // Given: a table exists
        String tableId = "test-delete-prevent-query";
        createSimpleTable(tableId);
        assertTrue(tableExists(tableId), "Table should exist before deletion");

        // When: deleting the table
        DeleteTable deleteTable = new DeleteTable();
        deleteTable.setTableId(tableId);
        deleteTable.execute(adminClient);

        // Then: attempting to query the table should fail
        assertFalse(tableExists(tableId), "Table should not exist after deletion");

        try {
            adminClient.getTable(tableId);
            fail("Expected exception when querying deleted table");
        } catch (Exception e) {
            // Expected - table no longer exists
            assertTrue(
                    e.getMessage().contains("NOT_FOUND") || e.getMessage().contains("not found"),
                    "Exception should indicate table not found");
        }
    }

    @Test(description = "Delete table immediately after creation")
    public void testCreateThenImmediatelyDelete() {
        // Given: a table is created
        String tableId = "test-delete-immediate";
        createSimpleTable(tableId);
        assertTrue(tableExists(tableId), "Table should exist after creation");

        // When: immediately deleting the table
        DeleteTable deleteTable = new DeleteTable();
        deleteTable.setTableId(tableId);
        deleteTable.execute(adminClient);

        // Then: table should be deleted
        assertFalse(tableExists(tableId), "Table should not exist after immediate deletion");
    }

    @Test(
            description = "Verify double deletion of the same table fails",
            expectedExceptions = Exception.class)
    public void testDoubleDeleteFails() {
        // Given: a table exists and is deleted once
        String tableId = "test-delete-double";
        createSimpleTable(tableId);
        assertTrue(tableExists(tableId), "Table should exist before deletion");

        DeleteTable deleteTable = new DeleteTable();
        deleteTable.setTableId(tableId);
        deleteTable.execute(adminClient);

        assertFalse(tableExists(tableId), "Table should not exist after first deletion");

        // When/Then: attempting to delete it again should fail
        deleteTable.execute(adminClient);
    }

    /**
     * Helper method to create a simple table without families.
     *
     * @param tableId the ID of the table to create
     */
    private void createSimpleTable(String tableId) {
        CreateTable createTable = new CreateTable();
        createTable.setTableId(tableId);
        createTable.execute(adminClient);
    }

    /**
     * Check if a table exists in the BigTable instance.
     *
     * @param tableId the ID of the table to check
     * @return true if the table exists, false otherwise
     */
    private boolean tableExists(String tableId) {
        try {
            adminClient.getTable(tableId);
            return true;
        } catch (Exception e) {
            // Table not found or other error
            return false;
        }
    }
}
