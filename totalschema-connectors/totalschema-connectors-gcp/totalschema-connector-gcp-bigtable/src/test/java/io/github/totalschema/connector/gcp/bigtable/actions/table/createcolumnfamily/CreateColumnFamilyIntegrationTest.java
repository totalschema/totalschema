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

package io.github.totalschema.connector.gcp.bigtable.actions.table.createcolumnfamily;

import static org.testng.Assert.*;

import com.google.cloud.bigtable.admin.v2.models.Table;
import io.github.totalschema.connector.gcp.bigtable.AbstractBigtableAdminIntegrationTest;
import io.github.totalschema.connector.gcp.bigtable.actions.table.createtable.ColumnFamily;
import io.github.totalschema.connector.gcp.bigtable.actions.table.createtable.CreateTable;
import io.github.totalschema.connector.gcp.bigtable.actions.table.createtable.GcRule;
import java.util.List;
import org.testng.annotations.Test;

/**
 * Integration tests for {@link CreateColumnFamily} using BigTable emulator in a TestContainer.
 *
 * <p>These tests verify that CreateColumnFamily works correctly against the BigTable emulator. The
 * tests are skipped if Docker/Podman is not available.
 */
public class CreateColumnFamilyIntegrationTest extends AbstractBigtableAdminIntegrationTest {

    @Test(description = "Add a simple column family without GC rules to an existing table")
    public void testAddSimpleColumnFamily() {
        // Given: an existing table
        String tableId = "test-add-simple-family";
        createEmptyTable(tableId);

        // And: a CreateColumnFamily action
        CreateColumnFamily action = new CreateColumnFamily();
        action.setTableId(tableId);
        action.setFamilyName("analytics");

        // When: executing the action
        action.execute(adminClient);

        // Then: the family should exist
        assertTrue(familyExists(tableId, "analytics"), "Family 'analytics' should exist");
    }

    @Test(description = "Add a column family with maxVersions GC rule")
    public void testAddColumnFamilyWithMaxVersions() {
        // Given: an existing table
        String tableId = "test-add-family-max-versions";
        createEmptyTable(tableId);

        // And: a CreateColumnFamily action with maxVersions
        CreateColumnFamily action = new CreateColumnFamily();
        action.setTableId(tableId);
        action.setFamilyName("profile");
        action.setMaxVersions(5);

        // When: executing the action
        action.execute(adminClient);

        // Then: the family should exist with the GC rule
        assertTrue(familyExists(tableId, "profile"), "Family 'profile' should exist");
    }

    @Test(description = "Add a column family with maxAge GC rule")
    public void testAddColumnFamilyWithMaxAge() {
        // Given: an existing table
        String tableId = "test-add-family-max-age";
        createEmptyTable(tableId);

        // And: a CreateColumnFamily action with maxAge
        CreateColumnFamily action = new CreateColumnFamily();
        action.setTableId(tableId);
        action.setFamilyName("cache");
        action.setMaxAge("24h");

        // When: executing the action
        action.execute(adminClient);

        // Then: the family should exist with the GC rule
        assertTrue(familyExists(tableId, "cache"), "Family 'cache' should exist");
    }

    @Test(description = "Add a column family with both maxVersions and maxAge (intersection)")
    public void testAddColumnFamilyWithIntersectionRules() {
        // Given: an existing table
        String tableId = "test-add-family-intersection";
        createEmptyTable(tableId);

        // And: a CreateColumnFamily action with both maxVersions and maxAge
        CreateColumnFamily action = new CreateColumnFamily();
        action.setTableId(tableId);
        action.setFamilyName("audit");
        action.setMaxVersions(100);
        action.setMaxAge("365d");

        // When: executing the action
        action.execute(adminClient);

        // Then: the family should exist with intersection GC rule
        assertTrue(familyExists(tableId, "audit"), "Family 'audit' should exist");
    }

    @Test(description = "Add a column family with union GC rules")
    public void testAddColumnFamilyWithUnionRules() {
        // Given: an existing table
        String tableId = "test-add-family-union";
        createEmptyTable(tableId);

        // And: a CreateColumnFamily action with union rules
        CreateColumnFamily action = new CreateColumnFamily();
        action.setTableId(tableId);
        action.setFamilyName("mixed");

        GcRule rule1 = new GcRule();
        rule1.setMaxVersions(3);

        GcRule rule2 = new GcRule();
        rule2.setMaxAge("7d");

        action.setUnion(List.of(rule1, rule2));

        // When: executing the action
        action.execute(adminClient);

        // Then: the family should exist with union GC rule
        assertTrue(familyExists(tableId, "mixed"), "Family 'mixed' should exist");
    }

    @Test(description = "Add a column family with explicit intersection GC rules")
    public void testAddColumnFamilyWithExplicitIntersectionRules() {
        // Given: an existing table
        String tableId = "test-add-family-explicit-intersection";
        createEmptyTable(tableId);

        // And: a CreateColumnFamily action with intersection rules
        CreateColumnFamily action = new CreateColumnFamily();
        action.setTableId(tableId);
        action.setFamilyName("important");

        GcRule rule1 = new GcRule();
        rule1.setMaxVersions(50);

        GcRule rule2 = new GcRule();
        rule2.setMaxAge("180d");

        action.setIntersection(List.of(rule1, rule2));

        // When: executing the action
        action.execute(adminClient);

        // Then: the family should exist with intersection GC rule
        assertTrue(familyExists(tableId, "important"), "Family 'important' should exist");
    }

    @Test(description = "Add multiple column families to the same table")
    public void testAddMultipleColumnFamilies() {
        // Given: an existing table
        String tableId = "test-add-multiple-families";
        createEmptyTable(tableId);

        // And: multiple CreateColumnFamily actions
        CreateColumnFamily action1 = new CreateColumnFamily();
        action1.setTableId(tableId);
        action1.setFamilyName("profile");

        CreateColumnFamily action2 = new CreateColumnFamily();
        action2.setTableId(tableId);
        action2.setFamilyName("settings");

        CreateColumnFamily action3 = new CreateColumnFamily();
        action3.setTableId(tableId);
        action3.setFamilyName("analytics");

        // When: executing all actions
        action1.execute(adminClient);
        action2.execute(adminClient);
        action3.execute(adminClient);

        // Then: all families should exist
        assertTrue(familyExists(tableId, "profile"), "Family 'profile' should exist");
        assertTrue(familyExists(tableId, "settings"), "Family 'settings' should exist");
        assertTrue(familyExists(tableId, "analytics"), "Family 'analytics' should exist");
    }

    @Test(description = "Add column family to table that already has families")
    public void testAddFamilyToTableWithExistingFamilies() {
        // Given: a table with existing families
        String tableId = "test-add-to-existing";
        CreateTable createTable = new CreateTable();
        createTable.setTableId(tableId);

        ColumnFamily initialFamily = new ColumnFamily();
        initialFamily.setName("initial");

        createTable.setFamilies(List.of(initialFamily));
        createTable.execute(adminClient);

        // And: a CreateColumnFamily action to add another family
        CreateColumnFamily action = new CreateColumnFamily();
        action.setTableId(tableId);
        action.setFamilyName("additional");

        // When: executing the action
        action.execute(adminClient);

        // Then: both families should exist
        assertTrue(familyExists(tableId, "initial"), "Family 'initial' should exist");
        assertTrue(familyExists(tableId, "additional"), "Family 'additional' should exist");
    }

    @Test(
            description = "Verify adding column family to non-existent table fails",
            expectedExceptions = Exception.class)
    public void testAddFamilyToNonExistentTableFails() {
        // Given: a CreateColumnFamily action for non-existent table
        CreateColumnFamily action = new CreateColumnFamily();
        action.setTableId("non-existent-table");
        action.setFamilyName("analytics");

        // When/Then: should throw exception
        action.execute(adminClient);
    }

    @Test(
            description = "Verify adding duplicate column family fails",
            expectedExceptions = Exception.class)
    public void testAddDuplicateColumnFamilyFails() {
        // Given: an existing table with a family
        String tableId = "test-duplicate-family";
        CreateTable createTable = new CreateTable();
        createTable.setTableId(tableId);

        ColumnFamily family = new ColumnFamily();
        family.setName("duplicate");

        createTable.setFamilies(List.of(family));
        createTable.execute(adminClient);

        // And: a CreateColumnFamily action to add the same family again
        CreateColumnFamily action = new CreateColumnFamily();
        action.setTableId(tableId);
        action.setFamilyName("duplicate");

        // When/Then: should throw exception
        action.execute(adminClient);
    }

    @Test(
            description = "Verify missing tableId throws IllegalArgumentException",
            expectedExceptions = IllegalArgumentException.class)
    public void testMissingTableIdFails() {
        // Given: a CreateColumnFamily action without tableId
        CreateColumnFamily action = new CreateColumnFamily();
        action.setFamilyName("analytics");

        // When/Then: should throw IllegalArgumentException
        action.execute(adminClient);
    }

    @Test(
            description = "Verify missing familyName throws IllegalArgumentException",
            expectedExceptions = IllegalArgumentException.class)
    public void testMissingFamilyNameFails() {
        // Given: a CreateColumnFamily action without familyName
        CreateColumnFamily action = new CreateColumnFamily();
        action.setTableId("test-table");

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
