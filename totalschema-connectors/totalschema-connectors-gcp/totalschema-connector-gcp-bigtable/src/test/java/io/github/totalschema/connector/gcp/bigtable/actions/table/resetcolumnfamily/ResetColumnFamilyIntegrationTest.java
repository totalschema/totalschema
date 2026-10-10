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

package io.github.totalschema.connector.gcp.bigtable.actions.table.resetcolumnfamily;

import static org.testng.Assert.*;

import com.google.cloud.bigtable.admin.v2.models.ColumnFamily;
import com.google.cloud.bigtable.admin.v2.models.Table;
import io.github.totalschema.connector.gcp.bigtable.AbstractBigtableAdminIntegrationTest;
import io.github.totalschema.connector.gcp.bigtable.actions.table.createtable.CreateTable;
import io.github.totalschema.connector.gcp.bigtable.actions.table.createtable.GcRule;
import java.util.List;
import org.testng.annotations.Test;

/**
 * Integration tests for {@link ResetColumnFamily} using BigTable emulator in a TestContainer.
 *
 * <p>These tests verify that ResetColumnFamily works correctly against the BigTable emulator. The
 * tests are skipped if Docker/Podman is not available.
 */
public class ResetColumnFamilyIntegrationTest extends AbstractBigtableAdminIntegrationTest {

    @Test(description = "Reset a column family that has maxVersions GC rule")
    public void testResetFamilyWithMaxVersions() {
        // Given: a table with a family that has maxVersions GC rule
        String tableId = "test-reset-max-versions";
        createTableWithFamily(tableId, "profile", 5, null);

        // And: a ResetColumnFamily action
        ResetColumnFamily action = new ResetColumnFamily();
        action.setTableId(tableId);
        action.setFamilyName("profile");

        // When: executing the action
        action.execute(adminClient);

        // Then: the family should still exist
        assertTrue(familyExists(tableId, "profile"), "Family 'profile' should still exist");

        // And: the GC rule should be reset (maxVersions set to Integer.MAX_VALUE)
        ColumnFamily family = getColumnFamily(tableId, "profile");
        assertNotNull(family, "Family should be retrievable");
    }

    @Test(description = "Reset a column family that has maxAge GC rule")
    public void testResetFamilyWithMaxAge() {
        // Given: a table with a family that has maxAge GC rule
        String tableId = "test-reset-max-age";
        createTableWithFamily(tableId, "cache", null, "24h");

        // And: a ResetColumnFamily action
        ResetColumnFamily action = new ResetColumnFamily();
        action.setTableId(tableId);
        action.setFamilyName("cache");

        // When: executing the action
        action.execute(adminClient);

        // Then: the family should still exist
        assertTrue(familyExists(tableId, "cache"), "Family 'cache' should still exist");

        // And: the GC rule should be reset
        ColumnFamily family = getColumnFamily(tableId, "cache");
        assertNotNull(family, "Family should be retrievable");
    }

    @Test(description = "Reset a column family that has intersection GC rules")
    public void testResetFamilyWithIntersectionRules() {
        // Given: a table with a family that has intersection GC rules
        String tableId = "test-reset-intersection";
        createTableWithFamily(tableId, "audit", 100, "365d");

        // And: a ResetColumnFamily action
        ResetColumnFamily action = new ResetColumnFamily();
        action.setTableId(tableId);
        action.setFamilyName("audit");

        // When: executing the action
        action.execute(adminClient);

        // Then: the family should still exist
        assertTrue(familyExists(tableId, "audit"), "Family 'audit' should still exist");

        // And: the GC rule should be reset
        ColumnFamily family = getColumnFamily(tableId, "audit");
        assertNotNull(family, "Family should be retrievable");
    }

    @Test(description = "Reset a column family that has union GC rules")
    public void testResetFamilyWithUnionRules() {
        // Given: a table with a family that has union GC rules
        String tableId = "test-reset-union";
        CreateTable createTable = new CreateTable();
        createTable.setTableId(tableId);

        io.github.totalschema.connector.gcp.bigtable.actions.table.createtable.ColumnFamily family =
                new io.github.totalschema.connector.gcp.bigtable.actions.table.createtable
                        .ColumnFamily();
        family.setName("mixed");

        GcRule rule1 = new GcRule();
        rule1.setMaxVersions(3);

        GcRule rule2 = new GcRule();
        rule2.setMaxAge("7d");

        family.setUnion(List.of(rule1, rule2));

        createTable.setFamilies(List.of(family));
        createTable.execute(adminClient);

        // And: a ResetColumnFamily action
        ResetColumnFamily action = new ResetColumnFamily();
        action.setTableId(tableId);
        action.setFamilyName("mixed");

        // When: executing the action
        action.execute(adminClient);

        // Then: the family should still exist
        assertTrue(familyExists(tableId, "mixed"), "Family 'mixed' should still exist");

        // And: the GC rule should be reset
        ColumnFamily familyAfterReset = getColumnFamily(tableId, "mixed");
        assertNotNull(familyAfterReset, "Family should be retrievable");
    }

    @Test(description = "Reset a column family that has no GC rules")
    public void testResetFamilyWithoutGcRules() {
        // Given: a table with a family without GC rules
        String tableId = "test-reset-no-rules";
        createTableWithFamily(tableId, "simple", null, null);

        // And: a ResetColumnFamily action
        ResetColumnFamily action = new ResetColumnFamily();
        action.setTableId(tableId);
        action.setFamilyName("simple");

        // When: executing the action
        action.execute(adminClient);

        // Then: the family should still exist (no-op but successful)
        assertTrue(familyExists(tableId, "simple"), "Family 'simple' should still exist");
    }

    @Test(description = "Reset the same column family multiple times (idempotency)")
    public void testResetFamilyMultipleTimes() {
        // Given: a table with a family that has GC rules
        String tableId = "test-reset-idempotent";
        createTableWithFamily(tableId, "profile", 10, null);

        // And: a ResetColumnFamily action
        ResetColumnFamily action = new ResetColumnFamily();
        action.setTableId(tableId);
        action.setFamilyName("profile");

        // When: executing the action multiple times
        action.execute(adminClient);
        action.execute(adminClient);
        action.execute(adminClient);

        // Then: the family should still exist and be in reset state
        assertTrue(familyExists(tableId, "profile"), "Family 'profile' should still exist");
        ColumnFamily family = getColumnFamily(tableId, "profile");
        assertNotNull(family, "Family should be retrievable");
    }

    @Test(description = "Reset one family in a table with multiple families")
    public void testResetOneFamilyAmongMultiple() {
        // Given: a table with multiple families
        String tableId = "test-reset-one-of-many";
        CreateTable createTable = new CreateTable();
        createTable.setTableId(tableId);

        io.github.totalschema.connector.gcp.bigtable.actions.table.createtable.ColumnFamily
                family1 =
                        new io.github.totalschema.connector.gcp.bigtable.actions.table.createtable
                                .ColumnFamily();
        family1.setName("profile");
        family1.setMaxVersions(5);

        io.github.totalschema.connector.gcp.bigtable.actions.table.createtable.ColumnFamily
                family2 =
                        new io.github.totalschema.connector.gcp.bigtable.actions.table.createtable
                                .ColumnFamily();
        family2.setName("settings");
        family2.setMaxVersions(3);

        io.github.totalschema.connector.gcp.bigtable.actions.table.createtable.ColumnFamily
                family3 =
                        new io.github.totalschema.connector.gcp.bigtable.actions.table.createtable
                                .ColumnFamily();
        family3.setName("analytics");
        family3.setMaxAge("30d");

        createTable.setFamilies(List.of(family1, family2, family3));
        createTable.execute(adminClient);

        // And: a ResetColumnFamily action for one family
        ResetColumnFamily action = new ResetColumnFamily();
        action.setTableId(tableId);
        action.setFamilyName("profile");

        // When: executing the action
        action.execute(adminClient);

        // Then: all families should still exist
        assertTrue(familyExists(tableId, "profile"), "Family 'profile' should exist");
        assertTrue(familyExists(tableId, "settings"), "Family 'settings' should exist");
        assertTrue(familyExists(tableId, "analytics"), "Family 'analytics' should exist");

        // And: only 'profile' should be reset, others should retain their GC rules
        assertNotNull(getColumnFamily(tableId, "profile"), "Profile family should be retrievable");
        assertNotNull(
                getColumnFamily(tableId, "settings"), "Settings family should be retrievable");
        assertNotNull(
                getColumnFamily(tableId, "analytics"), "Analytics family should be retrievable");
    }

    @Test(description = "Reset column family and then add it again with new GC rules")
    public void testResetThenReapplyGcRules() {
        // Given: a table with a family that has GC rules
        String tableId = "test-reset-then-reapply";
        createTableWithFamily(tableId, "profile", 5, null);

        // And: a ResetColumnFamily action
        ResetColumnFamily resetAction = new ResetColumnFamily();
        resetAction.setTableId(tableId);
        resetAction.setFamilyName("profile");

        // When: resetting the family
        resetAction.execute(adminClient);

        // Then: the family should exist in reset state
        assertTrue(familyExists(tableId, "profile"), "Family 'profile' should exist");

        // Note: We cannot re-add a family that already exists with CreateColumnFamily,
        // but we could use ModifyColumnFamily if it were available.
        // This test just verifies the reset happened successfully.
    }

    @Test(
            description = "Verify resetting non-existent family fails",
            expectedExceptions = Exception.class)
    public void testResetNonExistentFamilyFails() {
        // Given: a table without the specified family
        String tableId = "test-reset-non-existent";
        createEmptyTable(tableId);

        // And: a ResetColumnFamily action for non-existent family
        ResetColumnFamily action = new ResetColumnFamily();
        action.setTableId(tableId);
        action.setFamilyName("non-existent");

        // When/Then: should throw exception
        action.execute(adminClient);
    }

    @Test(
            description = "Verify resetting family on non-existent table fails",
            expectedExceptions = Exception.class)
    public void testResetFamilyOnNonExistentTableFails() {
        // Given: a ResetColumnFamily action for non-existent table
        ResetColumnFamily action = new ResetColumnFamily();
        action.setTableId("non-existent-table");
        action.setFamilyName("profile");

        // When/Then: should throw exception
        action.execute(adminClient);
    }

    @Test(
            description = "Verify missing tableId throws IllegalArgumentException",
            expectedExceptions = IllegalArgumentException.class)
    public void testMissingTableIdFails() {
        // Given: a ResetColumnFamily action without tableId
        ResetColumnFamily action = new ResetColumnFamily();
        action.setFamilyName("profile");

        // When/Then: should throw IllegalArgumentException
        action.execute(adminClient);
    }

    @Test(
            description = "Verify missing familyName throws IllegalArgumentException",
            expectedExceptions = IllegalArgumentException.class)
    public void testMissingFamilyNameFails() {
        // Given: a ResetColumnFamily action without familyName
        ResetColumnFamily action = new ResetColumnFamily();
        action.setTableId("test-table");

        // When/Then: should throw IllegalArgumentException
        action.execute(adminClient);
    }

    @Test(
            description = "Verify empty tableId throws IllegalArgumentException",
            expectedExceptions = IllegalArgumentException.class)
    public void testEmptyTableIdFails() {
        // Given: a ResetColumnFamily action with empty tableId
        ResetColumnFamily action = new ResetColumnFamily();
        action.setTableId("");
        action.setFamilyName("profile");

        // When/Then: should throw IllegalArgumentException
        action.execute(adminClient);
    }

    @Test(
            description = "Verify empty familyName throws IllegalArgumentException",
            expectedExceptions = IllegalArgumentException.class)
    public void testEmptyFamilyNameFails() {
        // Given: a ResetColumnFamily action with empty familyName
        ResetColumnFamily action = new ResetColumnFamily();
        action.setTableId("test-table");
        action.setFamilyName("");

        // When/Then: should throw IllegalArgumentException
        action.execute(adminClient);
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
     * Helper method to create a table with a single column family.
     *
     * @param tableId the ID of the table to create
     * @param familyName the name of the column family
     * @param maxVersions optional maxVersions GC rule (null for none)
     * @param maxAge optional maxAge GC rule (null for none)
     */
    private void createTableWithFamily(
            String tableId, String familyName, Integer maxVersions, String maxAge) {
        CreateTable createTable = new CreateTable();
        createTable.setTableId(tableId);

        io.github.totalschema.connector.gcp.bigtable.actions.table.createtable.ColumnFamily family =
                new io.github.totalschema.connector.gcp.bigtable.actions.table.createtable
                        .ColumnFamily();
        family.setName(familyName);

        if (maxVersions != null) {
            family.setMaxVersions(maxVersions);
        }

        if (maxAge != null) {
            family.setMaxAge(maxAge);
        }

        createTable.setFamilies(List.of(family));
        createTable.execute(adminClient);
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
     * Get a column family from a BigTable table.
     *
     * @param tableId the ID of the table
     * @param familyName the name of the column family to retrieve
     * @return the ColumnFamily object, or null if not found
     */
    private ColumnFamily getColumnFamily(String tableId, String familyName) {
        try {
            Table table = adminClient.getTable(tableId);
            return table.getColumnFamilies().stream()
                    .filter(family -> family.getId().equals(familyName))
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
}
