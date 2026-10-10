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

package io.github.totalschema.connector.gcp.bigtable.actions.table.createtable;

import static org.testng.Assert.*;

import io.github.totalschema.connector.gcp.bigtable.AbstractBigtableAdminIntegrationTest;
import java.util.List;
import org.testng.annotations.Test;

/**
 * Integration tests for {@link CreateTable} using BigTable emulator in a TestContainer.
 *
 * <p>These tests verify that CreateTable works correctly against the BigTable emulator. The tests
 * are skipped if Docker/Podman is not available.
 */
public class CreateTableIntegrationTest extends AbstractBigtableAdminIntegrationTest {

    @Test(description = "Create a simple table without families in BigTable emulator")
    public void testCreateSimpleTable() {
        // Given: a valid CreateTable action without families
        CreateTable createTable = new CreateTable();
        createTable.setTableId("test-simple-table");

        // When: executing the action
        createTable.execute(adminClient);

        // Then: table should be created and queryable
        assertTrue(tableExists("test-simple-table"), "Table should exist after creation");
    }

    @Test(description = "Create a table with column families in BigTable emulator")
    public void testCreateTableWithFamilies() {
        // Given: a CreateTable action with column families
        CreateTable createTable = new CreateTable();
        createTable.setTableId("test-families-table");

        ColumnFamily family1 = new ColumnFamily();
        family1.setName("profile");

        ColumnFamily family2 = new ColumnFamily();
        family2.setName("settings");

        createTable.setFamilies(List.of(family1, family2));

        // When: executing the action
        createTable.execute(adminClient);

        // Then: table should exist with the specified families
        assertTrue(tableExists("test-families-table"), "Table should exist after creation");
    }

    @Test(
            description = "Create a table with column families with GC rules",
            dependsOnMethods = {"testCreateTableWithFamilies"})
    public void testCreateTableWithGcRules() {
        // Given: a CreateTable action with families and GC rules
        CreateTable createTable = new CreateTable();
        createTable.setTableId("test-gc-table");

        ColumnFamily family1 = new ColumnFamily();
        family1.setName("profile");
        family1.setMaxVersions(3);

        ColumnFamily family2 = new ColumnFamily();
        family2.setName("audit");
        family2.setMaxAge("90d");

        createTable.setFamilies(List.of(family1, family2));

        // When: executing the action
        createTable.execute(adminClient);

        // Then: table should exist
        assertTrue(tableExists("test-gc-table"), "Table should exist after creation");
    }

    @Test(description = "Create multiple tables in sequence")
    public void testCreateMultipleTables() {
        // Given: multiple CreateTable actions
        CreateTable createTable1 = new CreateTable();
        createTable1.setTableId("test-multi-table-1");

        CreateTable createTable2 = new CreateTable();
        createTable2.setTableId("test-multi-table-2");

        CreateTable createTable3 = new CreateTable();
        createTable3.setTableId("test-multi-table-3");

        // When: executing all actions
        createTable1.execute(adminClient);
        createTable2.execute(adminClient);
        createTable3.execute(adminClient);

        // Then: all tables should be created
        assertTrue(tableExists("test-multi-table-1"), "First table should exist");
        assertTrue(tableExists("test-multi-table-2"), "Second table should exist");
        assertTrue(tableExists("test-multi-table-3"), "Third table should exist");
    }

    @Test(description = "Verify table creation with null families is handled correctly")
    public void testCreateTableWithNullFamilies() {
        // Given: a CreateTable action with null families
        CreateTable createTable = new CreateTable();
        createTable.setTableId("test-null-families");
        createTable.setFamilies(null);

        // When: executing the action
        createTable.execute(adminClient);

        // Then: table should be created
        assertTrue(tableExists("test-null-families"), "Table should exist even with null families");
    }

    @Test(
            description = "Verify duplicate table creation fails appropriately",
            expectedExceptions = Exception.class)
    public void testCreateDuplicateTableFails() {
        // Given: a CreateTable action
        CreateTable createTable = new CreateTable();
        createTable.setTableId("test-duplicate-table");

        // When: executing the action twice
        createTable.execute(adminClient);

        // Then: second execution should fail
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
