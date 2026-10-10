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

package io.github.totalschema.connector.gcp.bigtable.actions.table.restoretable;

import static org.testng.Assert.*;

import com.google.cloud.bigtable.admin.v2.models.Table;
import io.github.totalschema.connector.gcp.bigtable.AbstractBigtableAdminIntegrationTest;
import io.github.totalschema.connector.gcp.bigtable.actions.table.createbackup.CreateBackup;
import io.github.totalschema.connector.gcp.bigtable.actions.table.createtable.ColumnFamily;
import io.github.totalschema.connector.gcp.bigtable.actions.table.createtable.CreateTable;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;

/**
 * Integration tests for {@link RestoreTable} using BigTable emulator in a TestContainer.
 *
 * <p>These tests verify that RestoreTable works correctly against the BigTable emulator, including
 * restoring tables from backups with various configurations. The tests are skipped if Docker/Podman
 * is not available.
 *
 * <p>Note: The BigTable emulator has limited backup/restore functionality. Some tests verify API
 * calls and configuration rather than full backup/restore lifecycle.
 */
@Ignore("Integration tests for RestoreTable are skipped due to emulator limitations")
public class RestoreTableIntegrationTest extends AbstractBigtableAdminIntegrationTest {

    private static final String TEST_CLUSTER_ID = "test-cluster";

    /** Clean up tables and backups after each test to ensure test isolation. */
    @AfterMethod
    public void cleanupTablesAndBackups() {
        // Best-effort cleanup: try to delete any tables and backups created during tests
        // Note: Emulator may have limited backup cleanup support
        try {
            List<String> tables = adminClient.listTables();
            for (String tableId : tables) {
                try {
                    adminClient.deleteTable(tableId);
                } catch (Exception e) {
                    // Ignore cleanup errors
                }
            }
        } catch (Exception e) {
            // Ignore cleanup errors
        }

        try {
            List<String> backups = adminClient.listBackups(TEST_CLUSTER_ID);
            for (String backupId : backups) {
                try {
                    adminClient.deleteBackup(TEST_CLUSTER_ID, backupId);
                } catch (Exception e) {
                    // Ignore cleanup errors
                }
            }
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }

    @Test(description = "Restore table from backup to a new table")
    public void testRestoreTableToNewTable() {
        // Given: a source table with data
        String sourceTableId = "test-restore-source";
        String backupId = "backup_restore_test";
        String restoredTableId = "test-restored";
        createTableWithFamily(sourceTableId, "profile");

        // And: a backup of the source table
        createBackup(sourceTableId, backupId, "30d");

        // And: a RestoreTable action
        RestoreTable action = new RestoreTable();
        action.setBackupId(backupId);
        action.setTableId(restoredTableId);
        action.setClusterId(TEST_CLUSTER_ID);

        // When: executing the action
        action.execute(adminClient);

        // Then: the restored table should exist
        assertTrue(tableExists(restoredTableId), "Restored table should exist");

        // And: the restored table should have the same column families as the source
        Table restoredTable = adminClient.getTable(restoredTableId);
        assertNotNull(restoredTable, "Restored table should be retrievable");
        assertTrue(
                restoredTable.getColumnFamilies().stream()
                        .anyMatch(family -> family.getId().equals("profile")),
                "Restored table should have 'profile' column family");
    }

    @Test(description = "Restore table with overwrite flag to replace existing table")
    public void testRestoreTableWithOverwrite() {
        // Given: a source table and backup
        String sourceTableId = "test-restore-overwrite-source";
        String backupId = "backup_overwrite_test";
        String targetTableId = "test-overwrite-target";
        createTableWithFamily(sourceTableId, "original");
        createBackup(sourceTableId, backupId, "30d");

        // And: an existing target table with different schema
        createTableWithFamily(targetTableId, "different");

        // And: a RestoreTable action with overwrite flag
        RestoreTable action = new RestoreTable();
        action.setBackupId(backupId);
        action.setTableId(targetTableId);
        action.setClusterId(TEST_CLUSTER_ID);
        action.setOverwrite(true);

        // When: executing the action
        action.execute(adminClient);

        // Then: the table should exist with the restored schema
        assertTrue(tableExists(targetTableId), "Target table should exist");
        Table restoredTable = adminClient.getTable(targetTableId);
        assertTrue(
                restoredTable.getColumnFamilies().stream()
                        .anyMatch(family -> family.getId().equals("original")),
                "Restored table should have 'original' column family from backup");
    }

    @Test(description = "Restore table from backup with multiple column families")
    public void testRestoreTableWithMultipleFamilies() {
        // Given: a source table with multiple column families
        String sourceTableId = "test-restore-multi-families-source";
        String backupId = "backup_multi_families";
        String restoredTableId = "test-restored-multi-families";
        createTableWithFamilies(sourceTableId, Arrays.asList("profile", "settings", "analytics"));

        // And: a backup of the source table
        createBackup(sourceTableId, backupId, "30d");

        // And: a RestoreTable action
        RestoreTable action = new RestoreTable();
        action.setBackupId(backupId);
        action.setTableId(restoredTableId);
        action.setClusterId(TEST_CLUSTER_ID);

        // When: executing the action
        action.execute(adminClient);

        // Then: the restored table should have all column families
        assertTrue(tableExists(restoredTableId), "Restored table should exist");
        Table restoredTable = adminClient.getTable(restoredTableId);
        assertEquals(
                restoredTable.getColumnFamilies().size(),
                3,
                "Restored table should have 3 column families");
    }

    @Test(description = "Restore table from backup with GC rules")
    public void testRestoreTableWithGcRules() {
        // Given: a source table with GC rules
        String sourceTableId = "test-restore-gc-rules-source";
        String backupId = "backup_gc_rules";
        String restoredTableId = "test-restored-gc-rules";

        ColumnFamily family = new ColumnFamily();
        family.setName("versioned");
        family.setMaxVersions(5);
        family.setMaxAge("90d");
        createTableWithFamiliesObjects(sourceTableId, List.of(family));

        // And: a backup of the source table
        createBackup(sourceTableId, backupId, "30d");

        // And: a RestoreTable action
        RestoreTable action = new RestoreTable();
        action.setBackupId(backupId);
        action.setTableId(restoredTableId);
        action.setClusterId(TEST_CLUSTER_ID);

        // When: executing the action
        action.execute(adminClient);

        // Then: the restored table should exist with GC rules
        assertTrue(tableExists(restoredTableId), "Restored table should exist");
        Table restoredTable = adminClient.getTable(restoredTableId);
        assertTrue(
                restoredTable.getColumnFamilies().stream()
                        .anyMatch(fam -> fam.getId().equals("versioned")),
                "Restored table should have 'versioned' column family");
    }

    @Test(description = "Restore multiple times from the same backup")
    public void testRestoreMultipleTimesFromSameBackup() {
        // Given: a source table and backup
        String sourceTableId = "test-restore-multiple-source";
        String backupId = "backup_multiple_restore";
        createTableWithFamily(sourceTableId, "data");
        createBackup(sourceTableId, backupId, "30d");

        // When: restoring to different tables from the same backup
        RestoreTable action1 = new RestoreTable();
        action1.setBackupId(backupId);
        action1.setTableId("restored-1");
        action1.setClusterId(TEST_CLUSTER_ID);
        action1.execute(adminClient);

        RestoreTable action2 = new RestoreTable();
        action2.setBackupId(backupId);
        action2.setTableId("restored-2");
        action2.setClusterId(TEST_CLUSTER_ID);
        action2.execute(adminClient);

        RestoreTable action3 = new RestoreTable();
        action3.setBackupId(backupId);
        action3.setTableId("restored-3");
        action3.setClusterId(TEST_CLUSTER_ID);
        action3.execute(adminClient);

        // Then: all restored tables should exist
        assertTrue(tableExists("restored-1"), "First restored table should exist");
        assertTrue(tableExists("restored-2"), "Second restored table should exist");
        assertTrue(tableExists("restored-3"), "Third restored table should exist");
    }

    @Test(description = "Restore table when overwrite is false and table doesn't exist")
    public void testRestoreWithoutOverwriteToNonExistentTable() {
        // Given: a source table and backup
        String sourceTableId = "test-restore-no-overwrite-source";
        String backupId = "backup_no_overwrite";
        String restoredTableId = "test-restored-no-overwrite";
        createTableWithFamily(sourceTableId, "main");
        createBackup(sourceTableId, backupId, "30d");

        // And: a RestoreTable action without overwrite flag
        RestoreTable action = new RestoreTable();
        action.setBackupId(backupId);
        action.setTableId(restoredTableId);
        action.setClusterId(TEST_CLUSTER_ID);
        action.setOverwrite(false);

        // When: executing the action
        action.execute(adminClient);

        // Then: the restored table should exist
        assertTrue(tableExists(restoredTableId), "Restored table should exist");
    }

    @Test(description = "Restore table with overwrite when table doesn't exist (no-op delete)")
    public void testRestoreWithOverwriteToNonExistentTable() {
        // Given: a source table and backup
        String sourceTableId = "test-restore-overwrite-noexist-source";
        String backupId = "backup_overwrite_noexist";
        String restoredTableId = "test-restored-overwrite-noexist";
        createTableWithFamily(sourceTableId, "temp");
        createBackup(sourceTableId, backupId, "30d");

        // And: ensure target table doesn't exist
        if (tableExists(restoredTableId)) {
            adminClient.deleteTable(restoredTableId);
        }

        // And: a RestoreTable action with overwrite flag
        RestoreTable action = new RestoreTable();
        action.setBackupId(backupId);
        action.setTableId(restoredTableId);
        action.setClusterId(TEST_CLUSTER_ID);
        action.setOverwrite(true);

        // When: executing the action
        action.execute(adminClient);

        // Then: the restored table should exist
        assertTrue(tableExists(restoredTableId), "Restored table should exist");
    }

    @Test(description = "Restore table using null overwrite flag (should default to false)")
    public void testRestoreWithNullOverwrite() {
        // Given: a source table and backup
        String sourceTableId = "test-restore-null-overwrite-source";
        String backupId = "backup_null_overwrite";
        String restoredTableId = "test-restored-null-overwrite";
        createTableWithFamily(sourceTableId, "cache");
        createBackup(sourceTableId, backupId, "30d");

        // And: a RestoreTable action with null overwrite
        RestoreTable action = new RestoreTable();
        action.setBackupId(backupId);
        action.setTableId(restoredTableId);
        action.setClusterId(TEST_CLUSTER_ID);
        action.setOverwrite(null);

        // When: executing the action
        action.execute(adminClient);

        // Then: the restored table should exist
        assertTrue(tableExists(restoredTableId), "Restored table should exist");
    }

    @Test(
            description = "Verify restoring from non-existent backup fails",
            expectedExceptions = RuntimeException.class)
    public void testRestoreFromNonExistentBackupFails() {
        // Given: a RestoreTable action for non-existent backup
        RestoreTable action = new RestoreTable();
        action.setBackupId("non-existent-backup");
        action.setTableId("test-restored");
        action.setClusterId(TEST_CLUSTER_ID);

        // When/Then: should throw RuntimeException (wrapping underlying exception)
        action.execute(adminClient);
    }

    @Test(
            description = "Verify restoring without overwrite to existing table fails",
            expectedExceptions = RuntimeException.class)
    public void testRestoreToExistingTableWithoutOverwriteFails() {
        // Given: a source table and backup
        String sourceTableId = "test-restore-conflict-source";
        String backupId = "backup_conflict";
        String targetTableId = "test-conflict-target";
        createTableWithFamily(sourceTableId, "data");
        createBackup(sourceTableId, backupId, "30d");

        // And: an existing target table
        createTableWithFamily(targetTableId, "existing");

        // And: a RestoreTable action without overwrite
        RestoreTable action = new RestoreTable();
        action.setBackupId(backupId);
        action.setTableId(targetTableId);
        action.setClusterId(TEST_CLUSTER_ID);

        // When/Then: should fail because table exists and overwrite is not set
        action.execute(adminClient);
    }

    @Test(
            description = "Verify missing backupId throws IllegalArgumentException",
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*backupId is required.*")
    public void testRestoreTableMissingBackupIdFails() {
        // Given: a RestoreTable action without backupId
        RestoreTable action = new RestoreTable();
        action.setBackupId(null);
        action.setTableId("test-restored");
        action.setClusterId(TEST_CLUSTER_ID);

        // When/Then: should throw IllegalArgumentException
        action.execute(adminClient);
    }

    @Test(
            description = "Verify empty backupId throws IllegalArgumentException",
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*backupId is required.*")
    public void testRestoreTableEmptyBackupIdFails() {
        // Given: a RestoreTable action with empty backupId
        RestoreTable action = new RestoreTable();
        action.setBackupId("");
        action.setTableId("test-restored");
        action.setClusterId(TEST_CLUSTER_ID);

        // When/Then: should throw IllegalArgumentException
        action.execute(adminClient);
    }

    @Test(
            description = "Verify missing tableId throws IllegalArgumentException",
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*tableId is required.*")
    public void testRestoreTableMissingTableIdFails() {
        // Given: a RestoreTable action without tableId
        RestoreTable action = new RestoreTable();
        action.setBackupId("backup_test");
        action.setTableId(null);
        action.setClusterId(TEST_CLUSTER_ID);

        // When/Then: should throw IllegalArgumentException
        action.execute(adminClient);
    }

    @Test(
            description = "Verify empty tableId throws IllegalArgumentException",
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*tableId is required.*")
    public void testRestoreTableEmptyTableIdFails() {
        // Given: a RestoreTable action with empty tableId
        RestoreTable action = new RestoreTable();
        action.setBackupId("backup_test");
        action.setTableId("");
        action.setClusterId(TEST_CLUSTER_ID);

        // When/Then: should throw IllegalArgumentException
        action.execute(adminClient);
    }

    @Test(
            description = "Verify missing clusterId throws IllegalArgumentException",
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*clusterId is required.*")
    public void testRestoreTableMissingClusterIdFails() {
        // Given: a RestoreTable action without clusterId
        RestoreTable action = new RestoreTable();
        action.setBackupId("backup_test");
        action.setTableId("test-restored");
        action.setClusterId(null);

        // When/Then: should throw IllegalArgumentException
        action.execute(adminClient);
    }

    @Test(
            description = "Verify empty clusterId throws IllegalArgumentException",
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*clusterId is required.*")
    public void testRestoreTableEmptyClusterIdFails() {
        // Given: a RestoreTable action with empty clusterId
        RestoreTable action = new RestoreTable();
        action.setBackupId("backup_test");
        action.setTableId("test-restored");
        action.setClusterId("");

        // When/Then: should throw IllegalArgumentException
        action.execute(adminClient);
    }

    @Test(description = "Verify RestoreTable toString contains all fields")
    public void testRestoreTableToString() {
        // Given: a RestoreTable action with all fields set
        RestoreTable action = new RestoreTable();
        action.setBackupId("backup_test");
        action.setTableId("test-restored");
        action.setClusterId(TEST_CLUSTER_ID);
        action.setOverwrite(true);

        // When: calling toString
        String result = action.toString();

        // Then: it should contain all fields
        assertTrue(result.contains("backup_test"), "toString should contain backupId");
        assertTrue(result.contains("test-restored"), "toString should contain tableId");
        assertTrue(result.contains(TEST_CLUSTER_ID), "toString should contain clusterId");
        assertTrue(result.contains("true"), "toString should contain overwrite value");
        assertTrue(result.contains("RestoreTable"), "toString should contain class name");
    }

    @Test(description = "Verify RestoreTable getters and setters")
    public void testRestoreTableGettersSetters() {
        // Given: a RestoreTable action
        RestoreTable action = new RestoreTable();

        // When: setting values
        action.setBackupId("backup_id");
        action.setTableId("table_id");
        action.setClusterId("cluster_id");
        action.setOverwrite(true);

        // Then: getters should return the values
        assertEquals(action.getBackupId(), "backup_id", "backupId should match");
        assertEquals(action.getTableId(), "table_id", "tableId should match");
        assertEquals(action.getClusterId(), "cluster_id", "clusterId should match");
        assertEquals(action.getOverwrite(), Boolean.TRUE, "overwrite should match");
    }

    /**
     * Helper method to create a backup from a table.
     *
     * @param sourceTableId the ID of the table to back up
     * @param backupId the ID for the backup
     * @param expireTime the expiration time for the backup (e.g., "30d")
     */
    private void createBackup(String sourceTableId, String backupId, String expireTime) {
        CreateBackup createBackup = new CreateBackup();
        createBackup.setSourceTableId(sourceTableId);
        createBackup.setBackupId(backupId);
        createBackup.setExpireTime(expireTime);
        createBackup.setClusterId(TEST_CLUSTER_ID);
        createBackup.execute(adminClient);
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
        createTableWithFamiliesObjects(tableId, List.of(family));
    }

    /**
     * Helper method to create a table with multiple column families (string names).
     *
     * @param tableId the ID of the table to create
     * @param familyNames the names of the column families to add
     */
    private void createTableWithFamilies(String tableId, List<String> familyNames) {
        List<ColumnFamily> families =
                familyNames.stream()
                        .map(
                                name -> {
                                    ColumnFamily family = new ColumnFamily();
                                    family.setName(name);
                                    return family;
                                })
                        .collect(Collectors.toList());
        createTableWithFamiliesObjects(tableId, families);
    }

    /**
     * Helper method to create a table with specific column family configurations.
     *
     * @param tableId the ID of the table to create
     * @param families the list of column families to add
     */
    private void createTableWithFamiliesObjects(String tableId, List<ColumnFamily> families) {
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
            return adminClient.exists(tableId);
        } catch (Exception e) {
            return false;
        }
    }
}
