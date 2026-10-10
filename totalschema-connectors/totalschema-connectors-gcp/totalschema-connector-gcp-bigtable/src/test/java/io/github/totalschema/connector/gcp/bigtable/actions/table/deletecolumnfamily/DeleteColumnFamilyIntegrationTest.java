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

package io.github.totalschema.connector.gcp.bigtable.actions.table.deletecolumnfamily;

import static org.testng.Assert.*;

import com.google.cloud.bigtable.admin.v2.models.Table;
import io.github.totalschema.connector.gcp.bigtable.AbstractBigtableAdminIntegrationTest;
import io.github.totalschema.connector.gcp.bigtable.actions.table.createtable.ColumnFamily;
import io.github.totalschema.connector.gcp.bigtable.actions.table.createtable.CreateTable;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.testng.annotations.Test;

/**
 * Integration tests for {@link DeleteColumnFamily} using BigTable emulator in a TestContainer.
 *
 * <p>These tests verify that DeleteColumnFamily works correctly against the BigTable emulator. The
 * tests are skipped if Docker/Podman is not available.
 */
public class DeleteColumnFamilyIntegrationTest extends AbstractBigtableAdminIntegrationTest {

    @Test(description = "Delete a simple column family from a table")
    public void testDeleteSimpleColumnFamily() {
        // Given: a table with a column family
        String tableId = "test-delete-simple-family";
        createTableWithFamily(tableId, "analytics");

        // And: a DeleteColumnFamily action
        DeleteColumnFamily action = new DeleteColumnFamily();
        action.setTableId(tableId);
        action.setFamilyName("analytics");

        // When: executing the action
        action.execute(adminClient);

        // Then: the family should no longer exist
        assertFalse(familyExists(tableId, "analytics"), "Family 'analytics' should be deleted");
    }

    @Test(description = "Delete a column family with maxVersions GC rule")
    public void testDeleteColumnFamilyWithMaxVersions() {
        // Given: a table with a family that has maxVersions GC rule
        String tableId = "test-delete-family-max-versions";
        ColumnFamily family = new ColumnFamily();
        family.setName("profile");
        family.setMaxVersions(5);
        createTableWithFamilies(tableId, List.of(family));

        // And: a DeleteColumnFamily action
        DeleteColumnFamily action = new DeleteColumnFamily();
        action.setTableId(tableId);
        action.setFamilyName("profile");

        // When: executing the action
        action.execute(adminClient);

        // Then: the family should be deleted
        assertFalse(familyExists(tableId, "profile"), "Family 'profile' should be deleted");
    }

    @Test(description = "Delete a column family with maxAge GC rule")
    public void testDeleteColumnFamilyWithMaxAge() {
        // Given: a table with a family that has maxAge GC rule
        String tableId = "test-delete-family-max-age";
        ColumnFamily family = new ColumnFamily();
        family.setName("cache");
        family.setMaxAge("24h");
        createTableWithFamilies(tableId, List.of(family));

        // And: a DeleteColumnFamily action
        DeleteColumnFamily action = new DeleteColumnFamily();
        action.setTableId(tableId);
        action.setFamilyName("cache");

        // When: executing the action
        action.execute(adminClient);

        // Then: the family should be deleted
        assertFalse(familyExists(tableId, "cache"), "Family 'cache' should be deleted");
    }

    @Test(description = "Delete one column family while keeping others")
    public void testDeleteOneColumnFamilyKeepingOthers() {
        // Given: a table with multiple column families
        String tableId = "test-delete-one-keep-others";
        createTableWithFamilies(tableId, "profile", "settings", "analytics");

        // And: a DeleteColumnFamily action for one family
        DeleteColumnFamily action = new DeleteColumnFamily();
        action.setTableId(tableId);
        action.setFamilyName("settings");

        // When: executing the action
        action.execute(adminClient);

        // Then: only the specified family should be deleted
        assertTrue(familyExists(tableId, "profile"), "Family 'profile' should still exist");
        assertFalse(familyExists(tableId, "settings"), "Family 'settings' should be deleted");
        assertTrue(familyExists(tableId, "analytics"), "Family 'analytics' should still exist");
    }

    @Test(description = "Delete multiple column families from the same table")
    public void testDeleteMultipleColumnFamilies() {
        // Given: a table with multiple column families
        String tableId = "test-delete-multiple-families";
        createTableWithFamilies(tableId, "profile", "settings", "analytics");

        // And: multiple DeleteColumnFamily actions
        DeleteColumnFamily action1 = new DeleteColumnFamily();
        action1.setTableId(tableId);
        action1.setFamilyName("profile");

        DeleteColumnFamily action2 = new DeleteColumnFamily();
        action2.setTableId(tableId);
        action2.setFamilyName("settings");

        // When: executing both actions
        action1.execute(adminClient);
        action2.execute(adminClient);

        // Then: both families should be deleted, but the third should remain
        assertFalse(familyExists(tableId, "profile"), "Family 'profile' should be deleted");
        assertFalse(familyExists(tableId, "settings"), "Family 'settings' should be deleted");
        assertTrue(familyExists(tableId, "analytics"), "Family 'analytics' should still exist");
    }

    @Test(description = "Delete the last column family from a table")
    public void testDeleteLastColumnFamily() {
        // Given: a table with a single column family
        String tableId = "test-delete-last-family";
        createTableWithFamily(tableId, "only_family");

        // And: a DeleteColumnFamily action
        DeleteColumnFamily action = new DeleteColumnFamily();
        action.setTableId(tableId);
        action.setFamilyName("only_family");

        // When: executing the action
        action.execute(adminClient);

        // Then: the table should exist but have no families
        assertTrue(tableExists(tableId), "Table should still exist");
        assertFalse(familyExists(tableId, "only_family"), "Family 'only_family' should be deleted");
        assertEquals(getFamilyCount(tableId), 0, "Table should have no families");
    }

    @Test(description = "Delete column family with complex GC rules (intersection)")
    public void testDeleteColumnFamilyWithComplexGcRules() {
        // Given: a table with a family that has complex GC rules
        String tableId = "test-delete-family-complex-gc";
        ColumnFamily family = new ColumnFamily();
        family.setName("audit");
        family.setMaxVersions(100);
        family.setMaxAge("365d");
        createTableWithFamilies(tableId, List.of(family));

        // And: a DeleteColumnFamily action
        DeleteColumnFamily action = new DeleteColumnFamily();
        action.setTableId(tableId);
        action.setFamilyName("audit");

        // When: executing the action
        action.execute(adminClient);

        // Then: the family with complex rules should be deleted
        assertFalse(familyExists(tableId, "audit"), "Family 'audit' should be deleted");
    }

    @Test(
            description = "Verify deleting column family from non-existent table fails",
            expectedExceptions = Exception.class)
    public void testDeleteFamilyFromNonExistentTableFails() {
        // Given: a DeleteColumnFamily action for non-existent table
        DeleteColumnFamily action = new DeleteColumnFamily();
        action.setTableId("non-existent-table");
        action.setFamilyName("analytics");

        // When/Then: should throw exception
        action.execute(adminClient);
    }

    @Test(
            description = "Verify deleting non-existent column family fails",
            expectedExceptions = Exception.class)
    public void testDeleteNonExistentColumnFamilyFails() {
        // Given: a table without the specified family
        String tableId = "test-delete-non-existent-family";
        createTableWithFamily(tableId, "existing");

        // And: a DeleteColumnFamily action for non-existent family
        DeleteColumnFamily action = new DeleteColumnFamily();
        action.setTableId(tableId);
        action.setFamilyName("non_existent");

        // When/Then: should throw exception
        action.execute(adminClient);
    }

    @Test(
            description = "Verify missing tableId throws IllegalArgumentException",
            expectedExceptions = IllegalArgumentException.class)
    public void testMissingTableIdFails() {
        // Given: a DeleteColumnFamily action without tableId
        DeleteColumnFamily action = new DeleteColumnFamily();
        action.setFamilyName("analytics");

        // When/Then: should throw IllegalArgumentException
        action.execute(adminClient);
    }

    @Test(
            description = "Verify missing familyName throws IllegalArgumentException",
            expectedExceptions = IllegalArgumentException.class)
    public void testMissingFamilyNameFails() {
        // Given: a DeleteColumnFamily action without familyName
        DeleteColumnFamily action = new DeleteColumnFamily();
        action.setTableId("test-table");

        // When/Then: should throw IllegalArgumentException
        action.execute(adminClient);
    }

    @Test(
            description = "Verify empty tableId throws IllegalArgumentException",
            expectedExceptions = IllegalArgumentException.class)
    public void testEmptyTableIdFails() {
        // Given: a DeleteColumnFamily action with empty tableId
        DeleteColumnFamily action = new DeleteColumnFamily();
        action.setTableId("");
        action.setFamilyName("analytics");

        // When/Then: should throw IllegalArgumentException
        action.execute(adminClient);
    }

    @Test(
            description = "Verify empty familyName throws IllegalArgumentException",
            expectedExceptions = IllegalArgumentException.class)
    public void testEmptyFamilyNameFails() {
        // Given: a DeleteColumnFamily action with empty familyName
        DeleteColumnFamily action = new DeleteColumnFamily();
        action.setTableId("test-table");
        action.setFamilyName("");

        // When/Then: should throw IllegalArgumentException
        action.execute(adminClient);
    }

    /**
     * Helper method to create a table with a single column family.
     *
     * @param tableId the ID of the table to create
     * @param familyName the name of the column family to add
     */
    private void createTableWithFamily(String tableId, String familyName) {
        ColumnFamily family = new ColumnFamily();
        family.setName(familyName);
        createTableWithFamilies(tableId, List.of(family));
    }

    /**
     * Helper method to create a table with multiple column families.
     *
     * @param tableId the ID of the table to create
     * @param familyNames the names of the column families to add
     */
    private void createTableWithFamilies(String tableId, String... familyNames) {
        List<ColumnFamily> families =
                Arrays.stream(familyNames)
                        .map(
                                name -> {
                                    ColumnFamily family = new ColumnFamily();
                                    family.setName(name);
                                    return family;
                                })
                        .collect(Collectors.toList());
        createTableWithFamilies(tableId, families);
    }

    /**
     * Helper method to create a table with specific column family configurations.
     *
     * @param tableId the ID of the table to create
     * @param families the list of column families to add
     */
    private void createTableWithFamilies(String tableId, List<ColumnFamily> families) {
        CreateTable createTable = new CreateTable();
        createTable.setTableId(tableId);
        createTable.setFamilies(families);
        createTable.execute(adminClient);
    }

    /**
     * Check if a BigTable table exists.
     *
     * @param tableId the ID of the table to check
     * @return true if the table exists, false otherwise
     */
    private boolean tableExists(String tableId) {
        try {
            adminClient.getTable(tableId);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if a column family exists in a BigTable table.
     *
     * @param tableId the ID of the table
     * @param familyName the name of the column family to check
     * @return true if the family exists, false otherwise
     */
    private boolean familyExists(String tableId, String familyName) {
        try {
            Table table = adminClient.getTable(tableId);
            return table.getColumnFamilies().stream()
                    .anyMatch(family -> family.getId().equals(familyName));
        } catch (Exception e) {
            // Table not found or other error
            return false;
        }
    }

    /**
     * Get the number of column families in a BigTable table.
     *
     * @param tableId the ID of the table
     * @return the number of column families, or -1 if table not found
     */
    private int getFamilyCount(String tableId) {
        try {
            Table table = adminClient.getTable(tableId);
            return table.getColumnFamilies().size();
        } catch (Exception e) {
            return -1;
        }
    }
}
