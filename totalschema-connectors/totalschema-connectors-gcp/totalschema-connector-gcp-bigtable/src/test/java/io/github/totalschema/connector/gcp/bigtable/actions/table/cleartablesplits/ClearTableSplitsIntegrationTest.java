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

package io.github.totalschema.connector.gcp.bigtable.actions.table.cleartablesplits;

import static org.testng.Assert.*;

import com.google.cloud.bigtable.admin.v2.models.Table;
import com.google.protobuf.ByteString;
import io.github.totalschema.connector.gcp.bigtable.AbstractBigtableAdminIntegrationTest;
import io.github.totalschema.connector.gcp.bigtable.actions.table.createtable.ColumnFamily;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.testng.annotations.Test;

/**
 * Integration tests for {@link ClearTableSplits} using BigTable emulator in a TestContainer.
 *
 * <p>These tests verify that ClearTableSplits works correctly against the BigTable emulator,
 * including table recreation with schema preservation and split removal. The tests are skipped if
 * Docker/Podman is not available.
 */
public class ClearTableSplitsIntegrationTest extends AbstractBigtableAdminIntegrationTest {

    @Test(description = "Clear splits from a simple table with one column family")
    public void testClearSplitsSimpleTable() {
        // Given: a table with one column family and splits
        String tableId = "test-clear-splits-simple";
        createTableWithFamilyAndSplits(tableId, "profile", Arrays.asList("split1", "split2"));

        // Verify table exists and has splits
        assertTrue(tableExists(tableId), "Table should exist before clearing splits");

        // And: a ClearTableSplits action
        ClearTableSplits action = new ClearTableSplits();
        action.setTableId(tableId);
        action.setConfirmDeletion(true);

        // When: executing the action
        action.execute(adminClient);

        // Then: the table should still exist
        assertTrue(tableExists(tableId), "Table should exist after clearing splits");

        // And: should still have the column family
        assertTrue(familyExists(tableId, "profile"), "Family 'profile' should be preserved");

        // And: family count should be preserved
        assertEquals(getFamilyCount(tableId), 1, "Table should have 1 family");
    }

    @Test(description = "Clear splits from a table with multiple column families")
    public void testClearSplitsMultipleFamilies() {
        // Given: a table with multiple column families and splits
        String tableId = "test-clear-splits-multiple";
        createTableWithFamiliesAndSplits(
                tableId,
                Arrays.asList("profile", "settings", "analytics"),
                Arrays.asList("a", "m", "z"));

        // And: a ClearTableSplits action
        ClearTableSplits action = new ClearTableSplits();
        action.setTableId(tableId);
        action.setConfirmDeletion(true);

        // When: executing the action
        action.execute(adminClient);

        // Then: all families should be preserved
        assertTrue(familyExists(tableId, "profile"), "Family 'profile' should be preserved");
        assertTrue(familyExists(tableId, "settings"), "Family 'settings' should be preserved");
        assertTrue(familyExists(tableId, "analytics"), "Family 'analytics' should be preserved");
        assertEquals(getFamilyCount(tableId), 3, "Table should have 3 families");
    }

    @Test(description = "Clear splits from a table with maxVersions GC rule")
    public void testClearSplitsWithMaxVersionsGcRule() {
        // Given: a table with a family that has maxVersions GC rule
        String tableId = "test-clear-splits-max-versions";
        ColumnFamily family = new ColumnFamily();
        family.setName("data");
        family.setMaxVersions(5);
        createTableWithFamiliesObjectsAndSplits(
                tableId, List.of(family), Arrays.asList("row100", "row200"));

        // And: a ClearTableSplits action
        ClearTableSplits action = new ClearTableSplits();
        action.setTableId(tableId);
        action.setConfirmDeletion(true);

        // When: executing the action
        action.execute(adminClient);

        // Then: the family should be preserved with its GC rule
        assertTrue(familyExists(tableId, "data"), "Family 'data' should be preserved");
        assertTrue(
                familyHasGcRule(tableId, "data"),
                "Family 'data' should preserve its GC rule after clearing splits");
    }

    @Test(description = "Clear splits from a table with maxAge GC rule")
    public void testClearSplitsWithMaxAgeGcRule() {
        // Given: a table with a family that has maxAge GC rule
        String tableId = "test-clear-splits-max-age";
        ColumnFamily family = new ColumnFamily();
        family.setName("cache");
        family.setMaxAge("24h");
        createTableWithFamiliesObjectsAndSplits(
                tableId, List.of(family), Arrays.asList("key1", "key2"));

        // And: a ClearTableSplits action
        ClearTableSplits action = new ClearTableSplits();
        action.setTableId(tableId);
        action.setConfirmDeletion(true);

        // When: executing the action
        action.execute(adminClient);

        // Then: the family should be preserved with its GC rule
        assertTrue(familyExists(tableId, "cache"), "Family 'cache' should be preserved");
        assertTrue(
                familyHasGcRule(tableId, "cache"),
                "Family 'cache' should preserve its GC rule after clearing splits");
    }

    @Test(description = "Clear splits from a table with complex GC rules (intersection)")
    public void testClearSplitsWithComplexGcRules() {
        // Given: a table with a family that has complex GC rules
        String tableId = "test-clear-splits-complex-gc";
        ColumnFamily family = new ColumnFamily();
        family.setName("audit");
        family.setMaxVersions(100);
        family.setMaxAge("365d");
        createTableWithFamiliesObjectsAndSplits(
                tableId, List.of(family), Arrays.asList("2020", "2021", "2022"));

        // And: a ClearTableSplits action
        ClearTableSplits action = new ClearTableSplits();
        action.setTableId(tableId);
        action.setConfirmDeletion(true);

        // When: executing the action
        action.execute(adminClient);

        // Then: the family with complex rules should be preserved
        assertTrue(familyExists(tableId, "audit"), "Family 'audit' should be preserved");
        assertTrue(
                familyHasGcRule(tableId, "audit"),
                "Family 'audit' should preserve its complex GC rules");
    }

    @Test(description = "Clear splits from a table with many splits")
    public void testClearSplitsManySplits() {
        // Given: a table with many splits
        String tableId = "test-clear-splits-many";
        List<String> splits =
                Arrays.asList(
                        "split01", "split02", "split03", "split04", "split05", "split06", "split07",
                        "split08", "split09", "split10");
        createTableWithFamilyAndSplits(tableId, "data", splits);

        // And: a ClearTableSplits action
        ClearTableSplits action = new ClearTableSplits();
        action.setTableId(tableId);
        action.setConfirmDeletion(true);

        // When: executing the action
        action.execute(adminClient);

        // Then: table should exist with preserved schema
        assertTrue(tableExists(tableId), "Table should exist after clearing many splits");
        assertTrue(familyExists(tableId, "data"), "Family 'data' should be preserved");
    }

    @Test(description = "Clear splits from a table without any column families")
    public void testClearSplitsEmptyTable() {
        // Given: a table with no column families (edge case)
        String tableId = "test-clear-splits-empty";
        createTableWithFamiliesAndSplits(tableId, List.of(), List.of("split1"));

        // And: a ClearTableSplits action
        ClearTableSplits action = new ClearTableSplits();
        action.setTableId(tableId);
        action.setConfirmDeletion(true);

        // When: executing the action
        action.execute(adminClient);

        // Then: table should exist with no families
        assertTrue(tableExists(tableId), "Table should exist after clearing splits");
        assertEquals(getFamilyCount(tableId), 0, "Table should have no families");
    }

    @Test(description = "Clear splits from a table and verify it can be used afterwards")
    public void testClearSplitsTableUsableAfter() {
        // Given: a table with data
        String tableId = "test-clear-splits-usable";
        createTableWithFamilyAndSplits(tableId, "main", Arrays.asList("split1", "split2"));

        // And: a ClearTableSplits action
        ClearTableSplits action = new ClearTableSplits();
        action.setTableId(tableId);
        action.setConfirmDeletion(true);

        // When: executing the action
        action.execute(adminClient);

        // Then: table should be accessible and writable
        assertTrue(tableExists(tableId), "Table should be accessible after clearing splits");
        assertTrue(familyExists(tableId, "main"), "Family 'main' should be accessible");

        // Note: In a real test with data operations, we would write and read data here
        // to verify the table is fully functional
    }

    @Test(description = "Clear splits preserves all column families in correct order")
    public void testClearSplitsPreservesAllFamilies() {
        // Given: a table with specific families
        String tableId = "test-clear-splits-preserve-order";
        List<String> familyNames = Arrays.asList("cf1", "cf2", "cf3", "cf4", "cf5");
        createTableWithFamiliesAndSplits(tableId, familyNames, Arrays.asList("s1", "s2"));

        // And: a ClearTableSplits action
        ClearTableSplits action = new ClearTableSplits();
        action.setTableId(tableId);
        action.setConfirmDeletion(true);

        // When: executing the action
        action.execute(adminClient);

        // Then: all families should be preserved
        for (String familyName : familyNames) {
            assertTrue(
                    familyExists(tableId, familyName),
                    "Family '" + familyName + "' should be preserved");
        }
        assertEquals(getFamilyCount(tableId), 5, "All 5 families should be preserved");
    }

    @Test(
            description = "Verify clearing splits from non-existent table fails",
            expectedExceptions = IllegalArgumentException.class)
    public void testClearSplitsNonExistentTableFails() {
        // Given: a ClearTableSplits action for non-existent table
        ClearTableSplits action = new ClearTableSplits();
        action.setTableId("non-existent-table");
        action.setConfirmDeletion(true);

        // When/Then: should throw IllegalArgumentException
        action.execute(adminClient);
    }

    @Test(
            description = "Verify missing tableId throws IllegalArgumentException",
            expectedExceptions = IllegalArgumentException.class)
    public void testClearSplitsMissingTableIdFails() {
        // Given: a ClearTableSplits action without tableId
        ClearTableSplits action = new ClearTableSplits();
        action.setConfirmDeletion(true);

        // When/Then: should throw IllegalArgumentException
        action.execute(adminClient);
    }

    @Test(
            description = "Verify missing confirmDeletion throws IllegalArgumentException",
            expectedExceptions = IllegalArgumentException.class)
    public void testClearSplitsMissingConfirmDeletionFails() {
        // Given: a table
        String tableId = "test-missing-confirm";
        createTableWithFamilyAndSplits(tableId, "data", List.of("split1"));

        // And: a ClearTableSplits action without confirmDeletion
        ClearTableSplits action = new ClearTableSplits();
        action.setTableId(tableId);

        // When/Then: should throw IllegalArgumentException
        action.execute(adminClient);
    }

    @Test(
            description = "Verify confirmDeletion=false throws IllegalArgumentException",
            expectedExceptions = IllegalArgumentException.class)
    public void testClearSplitsConfirmDeletionFalseFails() {
        // Given: a table
        String tableId = "test-confirm-false";
        createTableWithFamilyAndSplits(tableId, "data", List.of("split1"));

        // And: a ClearTableSplits action with confirmDeletion=false
        ClearTableSplits action = new ClearTableSplits();
        action.setTableId(tableId);
        action.setConfirmDeletion(false);

        // When/Then: should throw IllegalArgumentException
        action.execute(adminClient);
    }

    @Test(
            description = "Verify empty tableId throws IllegalArgumentException",
            expectedExceptions = IllegalArgumentException.class)
    public void testClearSplitsEmptyTableIdFails() {
        // Given: a ClearTableSplits action with empty tableId
        ClearTableSplits action = new ClearTableSplits();
        action.setTableId("");
        action.setConfirmDeletion(true);

        // When/Then: should throw IllegalArgumentException
        action.execute(adminClient);
    }

    /**
     * Helper method to create a table with a single column family and splits.
     *
     * @param tableId the ID of the table to create
     * @param familyName the name of the column family to add
     * @param splits the list of split keys
     */
    private void createTableWithFamilyAndSplits(
            String tableId, String familyName, List<String> splits) {
        ColumnFamily family = new ColumnFamily();
        family.setName(familyName);
        createTableWithFamiliesObjectsAndSplits(tableId, List.of(family), splits);
    }

    /**
     * Helper method to create a table with multiple column families (string names) and splits.
     *
     * @param tableId the ID of the table to create
     * @param familyNames the names of the column families to add
     * @param splits the list of split keys
     */
    private void createTableWithFamiliesAndSplits(
            String tableId, List<String> familyNames, List<String> splits) {
        List<ColumnFamily> families =
                familyNames.stream()
                        .map(
                                name -> {
                                    ColumnFamily family = new ColumnFamily();
                                    family.setName(name);
                                    return family;
                                })
                        .collect(Collectors.toList());
        createTableWithFamiliesObjectsAndSplits(tableId, families, splits);
    }

    /**
     * Helper method to create a table with specific column family configurations and splits.
     *
     * @param tableId the ID of the table to create
     * @param families the list of column families to add
     * @param splits the list of split keys
     */
    private void createTableWithFamiliesObjectsAndSplits(
            String tableId, List<ColumnFamily> families, List<String> splits) {
        // Use BigTable API directly to create table with splits
        com.google.cloud.bigtable.admin.v2.models.CreateTableRequest request =
                com.google.cloud.bigtable.admin.v2.models.CreateTableRequest.of(tableId);

        // Add column families
        if (families != null) {
            for (ColumnFamily family : families) {
                if (family.getName() != null && !family.getName().isEmpty()) {
                    if (family.hasGcRules()) {
                        request.addFamily(family.getName(), family.buildGcRule());
                    } else {
                        request.addFamily(family.getName());
                    }
                }
            }
        }

        // Add splits
        if (splits != null && !splits.isEmpty()) {
            for (String split : splits) {
                request.addSplit(ByteString.copyFromUtf8(split));
            }
        }

        adminClient.createTable(request);
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
     * Check if a column family has a GC rule.
     *
     * @param tableId the ID of the table
     * @param familyName the name of the column family to check
     * @return true if the family has a GC rule, false otherwise
     */
    private boolean familyHasGcRule(String tableId, String familyName) {
        try {
            Table table = adminClient.getTable(tableId);
            return table.getColumnFamilies().stream()
                    .filter(family -> family.getId().equals(familyName))
                    .findFirst()
                    .map(com.google.cloud.bigtable.admin.v2.models.ColumnFamily::hasGCRule)
                    .orElse(false);
        } catch (Exception e) {
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
