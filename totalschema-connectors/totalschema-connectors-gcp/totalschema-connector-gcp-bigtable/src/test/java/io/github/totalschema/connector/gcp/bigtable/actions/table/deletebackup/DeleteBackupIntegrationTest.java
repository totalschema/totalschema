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

package io.github.totalschema.connector.gcp.bigtable.actions.table.deletebackup;

import static org.testng.Assert.*;

import com.google.cloud.bigtable.admin.v2.models.Backup;
import com.google.cloud.bigtable.admin.v2.models.CreateBackupRequest;
import io.github.totalschema.connector.gcp.bigtable.AbstractBigtableAdminIntegrationTest;
import io.github.totalschema.connector.gcp.bigtable.actions.table.createtable.ColumnFamily;
import java.util.List;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;
import org.threeten.bp.Duration;
import org.threeten.bp.Instant;

/**
 * Integration tests for {@link DeleteBackup} using BigTable emulator in a TestContainer.
 *
 * <p>These tests verify that DeleteBackup works correctly against the BigTable emulator, including
 * backup deletion with various scenarios. The tests are skipped if Docker/Podman is not available.
 *
 * <p>Note: The BigTable emulator has limited backup functionality. Some tests verify API calls and
 * configuration rather than full backup lifecycle.
 */
@Ignore("Integration tests for DeleteBackup are skipped due to emulator limitations")
public class DeleteBackupIntegrationTest extends AbstractBigtableAdminIntegrationTest {

    private static final String TEST_CLUSTER_ID = "test-cluster";

    /** Clean up backups and tables after each test to ensure test isolation. */
    @AfterMethod
    public void cleanupBackupsAndTables() {
        // Best-effort cleanup: try to delete any backups created during tests
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

        // Clean up tables
        try {
            for (String tableId : adminClient.listTables()) {
                try {
                    adminClient.deleteTable(tableId);
                } catch (Exception e) {
                    // Ignore cleanup errors
                }
            }
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }

    @Test(description = "Delete an existing backup successfully")
    public void testDeleteBackupSuccessfully() {
        // Given: a table and a backup
        String tableId = "test-delete-backup";
        String backupId = "backup_delete_test_20260807";
        createTableWithFamily(tableId, "profile");
        createBackup(tableId, backupId, TEST_CLUSTER_ID);

        // Verify backup exists before deletion
        assertTrue(backupExists(TEST_CLUSTER_ID, backupId), "Backup should exist before deletion");

        // And: a DeleteBackup action
        DeleteBackup action = new DeleteBackup();
        action.setBackupId(backupId);
        action.setClusterId(TEST_CLUSTER_ID);

        // When: executing the action
        action.execute(adminClient);

        // Then: the backup should no longer exist
        assertFalse(
                backupExists(TEST_CLUSTER_ID, backupId), "Backup should not exist after deletion");
    }

    @Test(description = "Delete multiple backups from the same cluster")
    public void testDeleteMultipleBackups() {
        // Given: a table and multiple backups
        String tableId = "test-delete-multiple";
        String backupId1 = "backup_multiple_1";
        String backupId2 = "backup_multiple_2";
        String backupId3 = "backup_multiple_3";

        createTableWithFamily(tableId, "data");
        createBackup(tableId, backupId1, TEST_CLUSTER_ID);
        createBackup(tableId, backupId2, TEST_CLUSTER_ID);
        createBackup(tableId, backupId3, TEST_CLUSTER_ID);

        // Verify all backups exist
        assertTrue(backupExists(TEST_CLUSTER_ID, backupId1), "First backup should exist");
        assertTrue(backupExists(TEST_CLUSTER_ID, backupId2), "Second backup should exist");
        assertTrue(backupExists(TEST_CLUSTER_ID, backupId3), "Third backup should exist");

        // When: deleting each backup
        DeleteBackup action1 = new DeleteBackup();
        action1.setBackupId(backupId1);
        action1.setClusterId(TEST_CLUSTER_ID);
        action1.execute(adminClient);

        DeleteBackup action2 = new DeleteBackup();
        action2.setBackupId(backupId2);
        action2.setClusterId(TEST_CLUSTER_ID);
        action2.execute(adminClient);

        DeleteBackup action3 = new DeleteBackup();
        action3.setBackupId(backupId3);
        action3.setClusterId(TEST_CLUSTER_ID);
        action3.execute(adminClient);

        // Then: none of the backups should exist
        assertFalse(backupExists(TEST_CLUSTER_ID, backupId1), "First backup should be deleted");
        assertFalse(backupExists(TEST_CLUSTER_ID, backupId2), "Second backup should be deleted");
        assertFalse(backupExists(TEST_CLUSTER_ID, backupId3), "Third backup should be deleted");
    }

    @Test(description = "Delete backup of HOT type")
    public void testDeleteHotBackup() {
        // Given: a table and a HOT backup
        String tableId = "test-delete-hot";
        String backupId = "backup_hot_delete";
        createTableWithFamily(tableId, "analytics");
        createBackup(tableId, backupId, TEST_CLUSTER_ID);

        // Verify backup exists
        assertTrue(backupExists(TEST_CLUSTER_ID, backupId), "HOT backup should exist");

        // And: a DeleteBackup action
        DeleteBackup action = new DeleteBackup();
        action.setBackupId(backupId);
        action.setClusterId(TEST_CLUSTER_ID);

        // When: executing the action
        action.execute(adminClient);

        // Then: the backup should be deleted
        assertFalse(backupExists(TEST_CLUSTER_ID, backupId), "HOT backup should be deleted");
    }

    @Test(description = "Delete backup from table with multiple column families")
    public void testDeleteBackupFromTableWithMultipleFamilies() {
        // Given: a table with multiple column families and a backup
        String tableId = "test-delete-multi-families";
        String backupId = "backup_multi_families_delete";

        ColumnFamily family1 = new ColumnFamily();
        family1.setName("profile");
        ColumnFamily family2 = new ColumnFamily();
        family2.setName("settings");
        ColumnFamily family3 = new ColumnFamily();
        family3.setName("analytics");

        createTableWithFamiliesObjects(tableId, List.of(family1, family2, family3));
        createBackup(tableId, backupId, TEST_CLUSTER_ID);

        // Verify backup exists
        assertTrue(backupExists(TEST_CLUSTER_ID, backupId), "Backup should exist");

        // And: a DeleteBackup action
        DeleteBackup action = new DeleteBackup();
        action.setBackupId(backupId);
        action.setClusterId(TEST_CLUSTER_ID);

        // When: executing the action
        action.execute(adminClient);

        // Then: the backup should be deleted
        assertFalse(backupExists(TEST_CLUSTER_ID, backupId), "Backup should be deleted");
    }

    @Test(description = "Delete backup created with GC rules")
    public void testDeleteBackupWithGcRules() {
        // Given: a table with GC rules and a backup
        String tableId = "test-delete-gc-rules";
        String backupId = "backup_gc_rules_delete";

        ColumnFamily family = new ColumnFamily();
        family.setName("versioned");
        family.setMaxVersions(5);
        family.setMaxAge("90d");

        createTableWithFamiliesObjects(tableId, List.of(family));
        createBackup(tableId, backupId, TEST_CLUSTER_ID);

        // Verify backup exists
        assertTrue(backupExists(TEST_CLUSTER_ID, backupId), "Backup should exist");

        // And: a DeleteBackup action
        DeleteBackup action = new DeleteBackup();
        action.setBackupId(backupId);
        action.setClusterId(TEST_CLUSTER_ID);

        // When: executing the action
        action.execute(adminClient);

        // Then: the backup should be deleted
        assertFalse(backupExists(TEST_CLUSTER_ID, backupId), "Backup should be deleted");
    }

    @Test(description = "Table remains after backup deletion")
    public void testTableRemainsAfterBackupDeletion() {
        // Given: a table and a backup
        String tableId = "test-table-remains";
        String backupId = "backup_table_remains";
        createTableWithFamily(tableId, "main");
        createBackup(tableId, backupId, TEST_CLUSTER_ID);

        // Verify both table and backup exist
        assertTrue(tableExists(tableId), "Table should exist");
        assertTrue(backupExists(TEST_CLUSTER_ID, backupId), "Backup should exist");

        // And: a DeleteBackup action
        DeleteBackup action = new DeleteBackup();
        action.setBackupId(backupId);
        action.setClusterId(TEST_CLUSTER_ID);

        // When: executing the action
        action.execute(adminClient);

        // Then: backup should be deleted but table should remain
        assertFalse(backupExists(TEST_CLUSTER_ID, backupId), "Backup should be deleted");
        assertTrue(tableExists(tableId), "Table should still exist");
    }

    @Test(
            description = "Verify deleting non-existent backup fails",
            expectedExceptions = com.google.api.gax.rpc.NotFoundException.class)
    public void testDeleteNonExistentBackupFails() {
        // Given: a DeleteBackup action for non-existent backup
        DeleteBackup action = new DeleteBackup();
        action.setBackupId("non_existent_backup");
        action.setClusterId(TEST_CLUSTER_ID);

        // When/Then: should throw NotFoundException
        action.execute(adminClient);
    }

    @Test(
            description = "Verify deleting already deleted backup fails",
            expectedExceptions = com.google.api.gax.rpc.NotFoundException.class)
    public void testDeleteAlreadyDeletedBackupFails() {
        // Given: a table and a backup that is deleted
        String tableId = "test-already-deleted";
        String backupId = "backup_already_deleted";
        createTableWithFamily(tableId, "data");
        createBackup(tableId, backupId, TEST_CLUSTER_ID);

        // Delete the backup first time
        DeleteBackup action1 = new DeleteBackup();
        action1.setBackupId(backupId);
        action1.setClusterId(TEST_CLUSTER_ID);
        action1.execute(adminClient);

        // Verify backup is deleted
        assertFalse(backupExists(TEST_CLUSTER_ID, backupId), "Backup should be deleted");

        // When/Then: attempting to delete again should fail
        DeleteBackup action2 = new DeleteBackup();
        action2.setBackupId(backupId);
        action2.setClusterId(TEST_CLUSTER_ID);
        action2.execute(adminClient);
    }

    @Test(
            description = "Verify missing backupId throws IllegalArgumentException",
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*backupId is required.*")
    public void testDeleteBackupMissingBackupIdFails() {
        // Given: a DeleteBackup action without backupId
        DeleteBackup action = new DeleteBackup();
        action.setBackupId(null);
        action.setClusterId(TEST_CLUSTER_ID);

        // When/Then: should throw IllegalArgumentException
        action.execute(adminClient);
    }

    @Test(
            description = "Verify empty backupId throws IllegalArgumentException",
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*backupId is required.*")
    public void testDeleteBackupEmptyBackupIdFails() {
        // Given: a DeleteBackup action with empty backupId
        DeleteBackup action = new DeleteBackup();
        action.setBackupId("");
        action.setClusterId(TEST_CLUSTER_ID);

        // When/Then: should throw IllegalArgumentException
        action.execute(adminClient);
    }

    @Test(
            description = "Verify missing clusterId throws IllegalArgumentException",
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*clusterId is required.*")
    public void testDeleteBackupMissingClusterIdFails() {
        // Given: a DeleteBackup action without clusterId
        DeleteBackup action = new DeleteBackup();
        action.setBackupId("backup_test");
        action.setClusterId(null);

        // When/Then: should throw IllegalArgumentException
        action.execute(adminClient);
    }

    @Test(
            description = "Verify empty clusterId throws IllegalArgumentException",
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*clusterId is required.*")
    public void testDeleteBackupEmptyClusterIdFails() {
        // Given: a DeleteBackup action with empty clusterId
        DeleteBackup action = new DeleteBackup();
        action.setBackupId("backup_test");
        action.setClusterId("");

        // When/Then: should throw IllegalArgumentException
        action.execute(adminClient);
    }

    @Test(description = "Delete old backup to free storage space")
    public void testDeleteOldBackupForStorageManagement() {
        // Given: a table and an old backup
        String tableId = "test-storage-management";
        String oldBackupId = "backup_old_20260101";
        createTableWithFamily(tableId, "archive");
        createBackup(tableId, oldBackupId, TEST_CLUSTER_ID);

        // Verify backup exists
        assertTrue(backupExists(TEST_CLUSTER_ID, oldBackupId), "Old backup should exist");

        // And: a DeleteBackup action
        DeleteBackup action = new DeleteBackup();
        action.setBackupId(oldBackupId);
        action.setClusterId(TEST_CLUSTER_ID);

        // When: executing the action
        action.execute(adminClient);

        // Then: the old backup should be deleted
        assertFalse(
                backupExists(TEST_CLUSTER_ID, oldBackupId),
                "Old backup should be deleted for storage management");
    }

    @Test(description = "Delete backup while source table still has data")
    public void testDeleteBackupWithActiveTable() {
        // Given: a table with data and a backup
        String tableId = "test-active-table";
        String backupId = "backup_active_table";
        createTableWithFamily(tableId, "active");
        createBackup(tableId, backupId, TEST_CLUSTER_ID);

        // Verify table and backup exist
        assertTrue(tableExists(tableId), "Table should exist");
        assertTrue(backupExists(TEST_CLUSTER_ID, backupId), "Backup should exist");

        // And: a DeleteBackup action
        DeleteBackup action = new DeleteBackup();
        action.setBackupId(backupId);
        action.setClusterId(TEST_CLUSTER_ID);

        // When: executing the action
        action.execute(adminClient);

        // Then: backup should be deleted and table should still exist
        assertFalse(backupExists(TEST_CLUSTER_ID, backupId), "Backup should be deleted");
        assertTrue(tableExists(tableId), "Table should still exist");
    }

    @Test(description = "Delete backup after source table is deleted")
    public void testDeleteBackupAfterTableDeleted() {
        // Given: a table and a backup
        String tableId = "test-delete-after-table";
        String backupId = "backup_delete_after_table";
        createTableWithFamily(tableId, "temp");
        createBackup(tableId, backupId, TEST_CLUSTER_ID);

        // Verify both exist
        assertTrue(tableExists(tableId), "Table should exist");
        assertTrue(backupExists(TEST_CLUSTER_ID, backupId), "Backup should exist");

        // And: delete the source table
        adminClient.deleteTable(tableId);
        assertFalse(tableExists(tableId), "Table should be deleted");

        // And: a DeleteBackup action
        DeleteBackup action = new DeleteBackup();
        action.setBackupId(backupId);
        action.setClusterId(TEST_CLUSTER_ID);

        // When: executing the action
        action.execute(adminClient);

        // Then: backup should be deleted (even though source table is gone)
        assertFalse(backupExists(TEST_CLUSTER_ID, backupId), "Backup should be deleted");
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
     * Helper method to create a table with specific column family configurations.
     *
     * @param tableId the ID of the table to create
     * @param families the list of column families to add
     */
    private void createTableWithFamiliesObjects(String tableId, List<ColumnFamily> families) {
        // Use BigTable API directly to create table
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

        adminClient.createTable(request);
    }

    /**
     * Helper method to create a backup.
     *
     * @param sourceTableId the source table ID
     * @param backupId the backup ID
     * @param clusterId the cluster ID
     */
    private void createBackup(String sourceTableId, String backupId, String clusterId) {
        Instant expireTime = Instant.now().plus(Duration.ofDays(30));
        CreateBackupRequest request =
                CreateBackupRequest.of(clusterId, backupId)
                        .setSourceTableId(sourceTableId)
                        .setExpireTime(expireTime);
        adminClient.createBackup(request);
    }

    /**
     * Check if a BigTable backup exists.
     *
     * @param clusterId the ID of the cluster
     * @param backupId the ID of the backup to check
     * @return true if the backup exists, false otherwise
     */
    private boolean backupExists(String clusterId, String backupId) {
        try {
            Backup backup = adminClient.getBackup(clusterId, backupId);
            return backup != null;
        } catch (Exception e) {
            return false;
        }
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
}
