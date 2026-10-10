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

package io.github.totalschema.connector.gcp.bigtable.actions.table.settablesplits;

import static org.testng.Assert.*;

import io.github.totalschema.connector.gcp.bigtable.AbstractBigtableAdminIntegrationTest;
import io.github.totalschema.connector.gcp.bigtable.actions.table.createtable.CreateTable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;

/**
 * Integration tests for {@link SetTableSplits} using BigTable emulator in a TestContainer.
 *
 * <p>These tests verify that SetTableSplits works correctly against the BigTable emulator. The
 * tests are skipped if Docker/Podman is not available.
 *
 * <p><strong>Note:</strong> SetTableSplits creates a new table with pre-split keys. The action will
 * fail if the table already exists, as splits can only be configured at table creation time.
 */
@Ignore(
        "Feature is not implemented in the BigTable emulator. Tests are skipped until emulator support is available.")
public class SetTableSplitsIntegrationTest extends AbstractBigtableAdminIntegrationTest {

    @Test(description = "Create table with simple split keys")
    public void testCreateTableWithSimpleSplits() {
        // Given: a SetTableSplits action with simple split keys
        String tableId = "test-splits-simple";
        SetTableSplits action = new SetTableSplits();
        action.setTableId(tableId);
        action.setSplitKeys(Arrays.asList("key1", "key2", "key3"));

        // When: executing the action
        action.execute(adminClient);

        // Then: the table should exist
        assertTrue(tableExists(tableId), "Table should be created");
    }

    @Test(description = "Create table with user ID based splits")
    public void testCreateTableWithUserIdSplits() {
        // Given: a SetTableSplits action for user table with ID-based splits
        String tableId = "test-splits-users";
        SetTableSplits action = new SetTableSplits();
        action.setTableId(tableId);
        action.setSplitKeys(Arrays.asList("user#1000", "user#2000", "user#3000", "user#4000"));

        // When: executing the action
        action.execute(adminClient);

        // Then: the table should exist
        assertTrue(tableExists(tableId), "Table should be created with user splits");
    }

    @Test(description = "Create table with multi-tenant splits")
    public void testCreateTableWithMultiTenantSplits() {
        // Given: a SetTableSplits action for multi-tenant table
        String tableId = "test-splits-multitenancy";
        SetTableSplits action = new SetTableSplits();
        action.setTableId(tableId);
        action.setSplitKeys(
                Arrays.asList(
                        "tenant_100#", "tenant_200#", "tenant_300#", "tenant_400#", "tenant_500#"));

        // When: executing the action
        action.execute(adminClient);

        // Then: the table should exist
        assertTrue(tableExists(tableId), "Table should be created with tenant splits");
    }

    @Test(description = "Create table with time-series splits")
    public void testCreateTableWithTimeSeriesSplits() {
        // Given: a SetTableSplits action for time-series data
        String tableId = "test-splits-timeseries";
        SetTableSplits action = new SetTableSplits();
        action.setTableId(tableId);
        action.setSplitKeys(
                Arrays.asList(
                        "2026-01#", "2026-02#", "2026-03#", "2026-04#", "2026-05#", "2026-06#"));

        // When: executing the action
        action.execute(adminClient);

        // Then: the table should exist
        assertTrue(tableExists(tableId), "Table should be created with time-series splits");
    }

    @Test(description = "Create table with single split key")
    public void testCreateTableWithSingleSplit() {
        // Given: a SetTableSplits action with a single split key
        String tableId = "test-splits-single";
        SetTableSplits action = new SetTableSplits();
        action.setTableId(tableId);
        action.setSplitKeys(Arrays.asList("middle"));

        // When: executing the action
        action.execute(adminClient);

        // Then: the table should exist
        assertTrue(tableExists(tableId), "Table should be created with single split");
    }

    @Test(description = "Create table with many split keys")
    public void testCreateTableWithManySplits() {
        // Given: a SetTableSplits action with many split keys
        String tableId = "test-splits-many";
        SetTableSplits action = new SetTableSplits();
        action.setTableId(tableId);

        // Create 20 split keys
        List<String> splitKeys =
                Arrays.asList(
                        "key01", "key02", "key03", "key04", "key05", "key06", "key07", "key08",
                        "key09", "key10", "key11", "key12", "key13", "key14", "key15", "key16",
                        "key17", "key18", "key19", "key20");
        action.setSplitKeys(splitKeys);

        // When: executing the action
        action.execute(adminClient);

        // Then: the table should exist
        assertTrue(tableExists(tableId), "Table should be created with many splits");
    }

    @Test(description = "Create table with numeric split keys")
    public void testCreateTableWithNumericSplits() {
        // Given: a SetTableSplits action with numeric-like split keys
        String tableId = "test-splits-numeric";
        SetTableSplits action = new SetTableSplits();
        action.setTableId(tableId);
        action.setSplitKeys(Arrays.asList("1000", "2000", "3000", "4000", "5000"));

        // When: executing the action
        action.execute(adminClient);

        // Then: the table should exist
        assertTrue(tableExists(tableId), "Table should be created with numeric splits");
    }

    @Test(description = "Create table with alphabetic split keys")
    public void testCreateTableWithAlphabeticSplits() {
        // Given: a SetTableSplits action with alphabetic split keys
        String tableId = "test-splits-alpha";
        SetTableSplits action = new SetTableSplits();
        action.setTableId(tableId);
        action.setSplitKeys(Arrays.asList("a", "d", "g", "j", "m", "p", "s", "v", "y"));

        // When: executing the action
        action.execute(adminClient);

        // Then: the table should exist
        assertTrue(tableExists(tableId), "Table should be created with alphabetic splits");
    }

    @Test(description = "Create table with mixed character split keys")
    public void testCreateTableWithMixedCharacterSplits() {
        // Given: a SetTableSplits action with mixed character split keys
        String tableId = "test-splits-mixed";
        SetTableSplits action = new SetTableSplits();
        action.setTableId(tableId);
        action.setSplitKeys(Arrays.asList("abc123", "def456", "ghi789", "jkl000"));

        // When: executing the action
        action.execute(adminClient);

        // Then: the table should exist
        assertTrue(tableExists(tableId), "Table should be created with mixed character splits");
    }

    @Test(description = "Create table with special character split keys")
    public void testCreateTableWithSpecialCharacterSplits() {
        // Given: a SetTableSplits action with special character split keys
        String tableId = "test-splits-special";
        SetTableSplits action = new SetTableSplits();
        action.setTableId(tableId);
        action.setSplitKeys(Arrays.asList("key#100", "key#200", "key@300", "key_400"));

        // When: executing the action
        action.execute(adminClient);

        // Then: the table should exist
        assertTrue(tableExists(tableId), "Table should be created with special character splits");
    }

    @Test(
            description = "Verify creating table when it already exists fails",
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*already exists.*")
    public void testCreateTableWhenAlreadyExistsFails() {
        // Given: an existing table
        String tableId = "test-splits-already-exists";
        createEmptyTable(tableId);

        // And: a SetTableSplits action for the same table
        SetTableSplits action = new SetTableSplits();
        action.setTableId(tableId);
        action.setSplitKeys(Arrays.asList("key1", "key2"));

        // When/Then: should throw IllegalArgumentException
        action.execute(adminClient);
    }

    @Test(
            description = "Verify split keys not in order fails",
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*must be in ascending.*")
    public void testSplitKeysNotInOrderFails() {
        // Given: a SetTableSplits action with unordered split keys
        String tableId = "test-splits-unordered";
        SetTableSplits action = new SetTableSplits();
        action.setTableId(tableId);
        action.setSplitKeys(Arrays.asList("key3", "key1", "key2")); // Not in order

        // When/Then: should throw IllegalArgumentException
        action.execute(adminClient);
    }

    @Test(
            description = "Verify duplicate split keys fail",
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*must be in ascending.*")
    public void testDuplicateSplitKeysFails() {
        // Given: a SetTableSplits action with duplicate split keys
        String tableId = "test-splits-duplicates";
        SetTableSplits action = new SetTableSplits();
        action.setTableId(tableId);
        action.setSplitKeys(Arrays.asList("key1", "key2", "key2", "key3")); // Duplicate "key2"

        // When/Then: should throw IllegalArgumentException
        action.execute(adminClient);
    }

    @Test(
            description = "Verify descending split keys fail",
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*must be in ascending.*")
    public void testDescendingSplitKeysFails() {
        // Given: a SetTableSplits action with descending split keys
        String tableId = "test-splits-descending";
        SetTableSplits action = new SetTableSplits();
        action.setTableId(tableId);
        action.setSplitKeys(Arrays.asList("key5", "key4", "key3", "key2", "key1"));

        // When/Then: should throw IllegalArgumentException
        action.execute(adminClient);
    }

    @Test(
            description = "Verify missing tableId throws IllegalArgumentException",
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*tableId is required.*")
    public void testMissingTableIdFails() {
        // Given: a SetTableSplits action without tableId
        SetTableSplits action = new SetTableSplits();
        action.setTableId(null);
        action.setSplitKeys(Arrays.asList("key1", "key2"));

        // When/Then: should throw IllegalArgumentException
        action.execute(adminClient);
    }

    @Test(
            description = "Verify empty tableId throws IllegalArgumentException",
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*tableId is required.*")
    public void testEmptyTableIdFails() {
        // Given: a SetTableSplits action with empty tableId
        SetTableSplits action = new SetTableSplits();
        action.setTableId("");
        action.setSplitKeys(Arrays.asList("key1", "key2"));

        // When/Then: should throw IllegalArgumentException
        action.execute(adminClient);
    }

    @Test(
            description = "Verify missing splitKeys throws IllegalArgumentException",
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*splitKeys is required.*")
    public void testMissingSplitKeysFails() {
        // Given: a SetTableSplits action without splitKeys
        SetTableSplits action = new SetTableSplits();
        action.setTableId("test-table");
        action.setSplitKeys(null);

        // When/Then: should throw IllegalArgumentException
        action.execute(adminClient);
    }

    @Test(
            description = "Verify empty splitKeys throws IllegalArgumentException",
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*splitKeys is required.*")
    public void testEmptySplitKeysFails() {
        // Given: a SetTableSplits action with empty splitKeys
        SetTableSplits action = new SetTableSplits();
        action.setTableId("test-table");
        action.setSplitKeys(Collections.emptyList());

        // When/Then: should throw IllegalArgumentException
        action.execute(adminClient);
    }

    @Test(description = "Verify SetTableSplits toString contains all fields")
    public void testSetTableSplitsToString() {
        // Given: a SetTableSplits action with values
        SetTableSplits action = new SetTableSplits();
        action.setTableId("test-table");
        action.setSplitKeys(Arrays.asList("key1", "key2", "key3"));

        // When: calling toString
        String result = action.toString();

        // Then: it should contain all fields
        assertTrue(result.contains("test-table"), "toString should contain tableId");
        assertTrue(result.contains("key1"), "toString should contain split keys");
        assertTrue(result.contains("SetTableSplits"), "toString should contain class name");
    }

    @Test(description = "Verify SetTableSplits getters and setters")
    public void testSetTableSplitsGettersSetters() {
        // Given: a SetTableSplits action
        SetTableSplits action = new SetTableSplits();

        // When: setting values
        action.setTableId("my-table");
        List<String> splitKeys = Arrays.asList("split1", "split2", "split3");
        action.setSplitKeys(splitKeys);

        // Then: getters should return the values
        assertEquals(action.getTableId(), "my-table", "tableId should match");
        assertEquals(action.getSplitKeys(), splitKeys, "splitKeys should match");
        assertEquals(action.getSplitKeys().size(), 3, "should have 3 split keys");
    }

    @Test(description = "Create table with large number of split keys")
    public void testCreateTableWithLargeSplitCount() {
        // Given: a SetTableSplits action with 50 split keys
        String tableId = "test-splits-large-count";
        SetTableSplits action = new SetTableSplits();
        action.setTableId(tableId);

        // Create 50 split keys
        String[] splits = new String[50];
        for (int i = 0; i < 50; i++) {
            splits[i] = String.format("key%03d", (i + 1) * 100);
        }
        action.setSplitKeys(Arrays.asList(splits));

        // When: executing the action
        action.execute(adminClient);

        // Then: the table should exist
        assertTrue(tableExists(tableId), "Table should be created with 50 splits");
    }

    @Test(description = "Create table with very short split keys")
    public void testCreateTableWithVeryShortSplitKeys() {
        // Given: a SetTableSplits action with very short split keys
        String tableId = "test-splits-short-keys";
        SetTableSplits action = new SetTableSplits();
        action.setTableId(tableId);
        action.setSplitKeys(Arrays.asList("a", "b", "c", "d", "e"));

        // When: executing the action
        action.execute(adminClient);

        // Then: the table should exist
        assertTrue(tableExists(tableId), "Table should be created with short split keys");
    }

    @Test(description = "Create table with very long split keys")
    public void testCreateTableWithVeryLongSplitKeys() {
        // Given: a SetTableSplits action with very long split keys
        String tableId = "test-splits-long-keys";
        SetTableSplits action = new SetTableSplits();
        action.setTableId(tableId);
        action.setSplitKeys(
                Arrays.asList(
                        "very_long_split_key_number_001_with_lots_of_characters",
                        "very_long_split_key_number_002_with_lots_of_characters",
                        "very_long_split_key_number_003_with_lots_of_characters"));

        // When: executing the action
        action.execute(adminClient);

        // Then: the table should exist
        assertTrue(tableExists(tableId), "Table should be created with long split keys");
    }

    @Test(description = "Create table with Unicode split keys")
    public void testCreateTableWithUnicodeSplitKeys() {
        // Given: a SetTableSplits action with Unicode split keys
        String tableId = "test-splits-unicode";
        SetTableSplits action = new SetTableSplits();
        action.setTableId(tableId);
        action.setSplitKeys(Arrays.asList("café", "naïve", "résumé", "jalapeño"));

        // When: executing the action
        action.execute(adminClient);

        // Then: the table should exist
        assertTrue(tableExists(tableId), "Table should be created with Unicode split keys");
    }

    @Test(description = "Create table with split keys containing spaces")
    public void testCreateTableWithSpacesInSplitKeys() {
        // Given: a SetTableSplits action with split keys containing spaces
        String tableId = "test-splits-spaces";
        SetTableSplits action = new SetTableSplits();
        action.setTableId(tableId);
        action.setSplitKeys(Arrays.asList("key 100", "key 200", "key 300"));

        // When: executing the action
        action.execute(adminClient);

        // Then: the table should exist
        assertTrue(
                tableExists(tableId), "Table should be created with split keys containing spaces");
    }

    @Test(description = "Create table with hierarchical split keys")
    public void testCreateTableWithHierarchicalSplitKeys() {
        // Given: a SetTableSplits action with hierarchical split keys
        String tableId = "test-splits-hierarchical";
        SetTableSplits action = new SetTableSplits();
        action.setTableId(tableId);
        action.setSplitKeys(
                Arrays.asList(
                        "country/us/state/ca/",
                        "country/us/state/ny/",
                        "country/us/state/tx/",
                        "country/uk/",
                        "country/jp/"));

        // When: executing the action
        action.execute(adminClient);

        // Then: the table should exist
        assertTrue(tableExists(tableId), "Table should be created with hierarchical split keys");
    }

    @Test(description = "Create table with date-based split keys")
    public void testCreateTableWithDateBasedSplitKeys() {
        // Given: a SetTableSplits action with date-based split keys
        String tableId = "test-splits-dates";
        SetTableSplits action = new SetTableSplits();
        action.setTableId(tableId);
        action.setSplitKeys(
                Arrays.asList(
                        "2026-01-01",
                        "2026-02-01",
                        "2026-03-01",
                        "2026-04-01",
                        "2026-05-01",
                        "2026-06-01",
                        "2026-07-01",
                        "2026-08-01",
                        "2026-09-01",
                        "2026-10-01",
                        "2026-11-01",
                        "2026-12-01"));

        // When: executing the action
        action.execute(adminClient);

        // Then: the table should exist
        assertTrue(tableExists(tableId), "Table should be created with date-based split keys");
    }

    /**
     * Helper method to create an empty table for testing.
     *
     * @param tableId the ID of the table to create
     */
    private void createEmptyTable(String tableId) {
        CreateTable createTable = new CreateTable();
        createTable.setTableId(tableId);
        createTable.execute(adminClient);
    }

    /**
     * Check if a table exists.
     *
     * @param tableId the ID of the table to check
     * @return true if the table exists, false otherwise
     */
    private boolean tableExists(String tableId) {
        try {
            return adminClient.exists(tableId);
        } catch (Exception e) {
            return false;
        }
    }
}
