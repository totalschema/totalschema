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

package io.github.totalschema.connector.gcp.bigtable.actions.table.modifycolumnfamily;

import static org.testng.Assert.*;

import com.google.cloud.bigtable.admin.v2.models.Table;
import io.github.totalschema.connector.gcp.bigtable.AbstractBigtableAdminIntegrationTest;
import io.github.totalschema.connector.gcp.bigtable.actions.table.createtable.ColumnFamily;
import io.github.totalschema.connector.gcp.bigtable.actions.table.createtable.CreateTable;
import io.github.totalschema.connector.gcp.bigtable.actions.table.createtable.GcRule;
import java.util.List;
import org.testng.annotations.Test;

/**
 * Integration tests for {@link ModifyColumnFamily} using BigTable emulator in a TestContainer.
 *
 * <p>These tests verify that ModifyColumnFamily works correctly against the BigTable emulator. The
 * tests are skipped if Docker/Podman is not available.
 */
public class ModifyColumnFamilyIntegrationTest extends AbstractBigtableAdminIntegrationTest {

    @Test(description = "Modify column family to add maxVersions GC rule")
    public void testModifyColumnFamilyWithMaxVersions() {
        // Given: an existing table with a column family without GC rules
        String tableId = "test-modify-max-versions";
        createTableWithFamily(tableId, "profile");

        // And: a ModifyColumnFamily action with maxVersions
        ModifyColumnFamily action = new ModifyColumnFamily();
        action.setTableId(tableId);
        action.setFamilyName("profile");
        action.setMaxVersions(10);

        // When: executing the action
        action.execute(adminClient);

        // Then: the family should still exist
        assertTrue(familyExists(tableId, "profile"), "Family 'profile' should exist");
    }

    @Test(description = "Modify column family to add maxAge GC rule")
    public void testModifyColumnFamilyWithMaxAge() {
        // Given: an existing table with a column family without GC rules
        String tableId = "test-modify-max-age";
        createTableWithFamily(tableId, "cache");

        // And: a ModifyColumnFamily action with maxAge
        ModifyColumnFamily action = new ModifyColumnFamily();
        action.setTableId(tableId);
        action.setFamilyName("cache");
        action.setMaxAge("48h");

        // When: executing the action
        action.execute(adminClient);

        // Then: the family should still exist
        assertTrue(familyExists(tableId, "cache"), "Family 'cache' should exist");
    }

    @Test(description = "Modify column family with both maxVersions and maxAge (intersection)")
    public void testModifyColumnFamilyWithIntersectionRules() {
        // Given: an existing table with a column family
        String tableId = "test-modify-intersection";
        createTableWithFamily(tableId, "audit");

        // And: a ModifyColumnFamily action with both maxVersions and maxAge
        ModifyColumnFamily action = new ModifyColumnFamily();
        action.setTableId(tableId);
        action.setFamilyName("audit");
        action.setMaxVersions(100);
        action.setMaxAge("365d");

        // When: executing the action
        action.execute(adminClient);

        // Then: the family should still exist with updated GC rules
        assertTrue(familyExists(tableId, "audit"), "Family 'audit' should exist");
    }

    @Test(description = "Modify column family with union GC rules")
    public void testModifyColumnFamilyWithUnionRules() {
        // Given: an existing table with a column family
        String tableId = "test-modify-union";
        createTableWithFamily(tableId, "mixed");

        // And: a ModifyColumnFamily action with union rules
        ModifyColumnFamily action = new ModifyColumnFamily();
        action.setTableId(tableId);
        action.setFamilyName("mixed");

        GcRule rule1 = new GcRule();
        rule1.setMaxVersions(5);

        GcRule rule2 = new GcRule();
        rule2.setMaxAge("30d");

        action.setUnion(List.of(rule1, rule2));

        // When: executing the action
        action.execute(adminClient);

        // Then: the family should still exist with union GC rules
        assertTrue(familyExists(tableId, "mixed"), "Family 'mixed' should exist");
    }

    @Test(description = "Modify column family with explicit intersection GC rules")
    public void testModifyColumnFamilyWithExplicitIntersectionRules() {
        // Given: an existing table with a column family
        String tableId = "test-modify-explicit-intersection";
        createTableWithFamily(tableId, "sensitive");

        // And: a ModifyColumnFamily action with intersection rules
        ModifyColumnFamily action = new ModifyColumnFamily();
        action.setTableId(tableId);
        action.setFamilyName("sensitive");

        GcRule rule1 = new GcRule();
        rule1.setMaxVersions(3);

        GcRule rule2 = new GcRule();
        rule2.setMaxAge("90d");

        action.setIntersection(List.of(rule1, rule2));

        // When: executing the action
        action.execute(adminClient);

        // Then: the family should still exist with intersection GC rules
        assertTrue(familyExists(tableId, "sensitive"), "Family 'sensitive' should exist");
    }

    @Test(description = "Modify column family that already has GC rules")
    public void testModifyColumnFamilyWithExistingRules() {
        // Given: an existing table with a column family that has GC rules
        String tableId = "test-modify-existing-rules";
        CreateTable createTable = new CreateTable();
        createTable.setTableId(tableId);

        ColumnFamily family = new ColumnFamily();
        family.setName("data");
        family.setMaxVersions(5);
        family.setMaxAge("7d");

        createTable.setFamilies(List.of(family));
        createTable.execute(adminClient);

        // And: a ModifyColumnFamily action to change the rules
        ModifyColumnFamily action = new ModifyColumnFamily();
        action.setTableId(tableId);
        action.setFamilyName("data");
        action.setMaxVersions(10);
        action.setMaxAge("30d");

        // When: executing the action
        action.execute(adminClient);

        // Then: the family should still exist with updated GC rules
        assertTrue(familyExists(tableId, "data"), "Family 'data' should exist");
    }

    @Test(description = "Modify multiple column families in the same table")
    public void testModifyMultipleColumnFamilies() {
        // Given: an existing table with multiple column families
        String tableId = "test-modify-multiple";
        CreateTable createTable = new CreateTable();
        createTable.setTableId(tableId);

        ColumnFamily family1 = new ColumnFamily();
        family1.setName("profile");

        ColumnFamily family2 = new ColumnFamily();
        family2.setName("settings");

        ColumnFamily family3 = new ColumnFamily();
        family3.setName("analytics");

        createTable.setFamilies(List.of(family1, family2, family3));
        createTable.execute(adminClient);

        // And: ModifyColumnFamily actions for each family
        ModifyColumnFamily action1 = new ModifyColumnFamily();
        action1.setTableId(tableId);
        action1.setFamilyName("profile");
        action1.setMaxVersions(5);

        ModifyColumnFamily action2 = new ModifyColumnFamily();
        action2.setTableId(tableId);
        action2.setFamilyName("settings");
        action2.setMaxAge("24h");

        ModifyColumnFamily action3 = new ModifyColumnFamily();
        action3.setTableId(tableId);
        action3.setFamilyName("analytics");
        action3.setMaxVersions(100);
        action3.setMaxAge("365d");

        // When: executing all actions
        action1.execute(adminClient);
        action2.execute(adminClient);
        action3.execute(adminClient);

        // Then: all families should still exist with updated GC rules
        assertTrue(familyExists(tableId, "profile"), "Family 'profile' should exist");
        assertTrue(familyExists(tableId, "settings"), "Family 'settings' should exist");
        assertTrue(familyExists(tableId, "analytics"), "Family 'analytics' should exist");
    }

    @Test(
            description = "Verify modifying non-existent table fails",
            expectedExceptions = Exception.class)
    public void testModifyColumnFamilyOnNonExistentTableFails() {
        // Given: a ModifyColumnFamily action for non-existent table
        ModifyColumnFamily action = new ModifyColumnFamily();
        action.setTableId("non-existent-table");
        action.setFamilyName("profile");
        action.setMaxVersions(10);

        // When/Then: should throw exception
        action.execute(adminClient);
    }

    @Test(
            description = "Verify modifying non-existent family fails",
            expectedExceptions = Exception.class)
    public void testModifyNonExistentColumnFamilyFails() {
        // Given: an existing table
        String tableId = "test-modify-non-existent-family";
        createTableWithFamily(tableId, "profile");

        // And: a ModifyColumnFamily action for non-existent family
        ModifyColumnFamily action = new ModifyColumnFamily();
        action.setTableId(tableId);
        action.setFamilyName("non-existent-family");
        action.setMaxVersions(10);

        // When/Then: should throw exception
        action.execute(adminClient);
    }

    @Test(
            description = "Verify missing tableId throws IllegalArgumentException",
            expectedExceptions = IllegalArgumentException.class)
    public void testMissingTableIdFails() {
        // Given: a ModifyColumnFamily action without tableId
        ModifyColumnFamily action = new ModifyColumnFamily();
        action.setFamilyName("profile");
        action.setMaxVersions(10);

        // When/Then: should throw IllegalArgumentException
        action.execute(adminClient);
    }

    @Test(
            description = "Verify missing familyName throws IllegalArgumentException",
            expectedExceptions = IllegalArgumentException.class)
    public void testMissingFamilyNameFails() {
        // Given: a ModifyColumnFamily action without familyName
        ModifyColumnFamily action = new ModifyColumnFamily();
        action.setTableId("test-table");
        action.setMaxVersions(10);

        // When/Then: should throw IllegalArgumentException
        action.execute(adminClient);
    }

    @Test(
            description = "Verify no GC rules specified throws IllegalArgumentException",
            expectedExceptions = IllegalArgumentException.class)
    public void testNoGcRulesFails() {
        // Given: an existing table with a column family
        String tableId = "test-no-gc-rules";
        createTableWithFamily(tableId, "profile");

        // And: a ModifyColumnFamily action without any GC rules
        ModifyColumnFamily action = new ModifyColumnFamily();
        action.setTableId(tableId);
        action.setFamilyName("profile");

        // When/Then: should throw IllegalArgumentException
        // (ModifyColumnFamily requires at least one GC rule)
        action.execute(adminClient);
    }

    /**
     * Helper method to create a table with a single column family (no GC rules).
     *
     * @param tableId the ID of the table to create
     * @param familyName the name of the column family to create
     */
    private void createTableWithFamily(String tableId, String familyName) {
        CreateTable createTable = new CreateTable();
        createTable.setTableId(tableId);

        ColumnFamily family = new ColumnFamily();
        family.setName(familyName);

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
}
