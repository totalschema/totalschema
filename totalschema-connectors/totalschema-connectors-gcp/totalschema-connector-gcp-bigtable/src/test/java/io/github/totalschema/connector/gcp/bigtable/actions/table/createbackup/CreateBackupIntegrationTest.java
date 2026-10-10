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

package io.github.totalschema.connector.gcp.bigtable.actions.table.createbackup;

import static org.testng.Assert.*;

import com.google.cloud.bigtable.admin.v2.models.Backup;
import io.github.totalschema.connector.gcp.bigtable.AbstractBigtableAdminIntegrationTest;
import io.github.totalschema.connector.gcp.bigtable.actions.table.createtable.ColumnFamily;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;

/**
 * Integration tests for {@link CreateBackup} using BigTable emulator in a TestContainer.
 *
 * <p>These tests verify that CreateBackup works correctly against the BigTable emulator, including
 * backup creation with various configurations. The tests are skipped if Docker/Podman is not
 * available.
 *
 * <p>Note: The BigTable emulator has limited backup functionality. Some tests verify API calls and
 * configuration rather than full backup lifecycle.
 */
@Ignore("Integration tests for CreateBackup are skipped due to emulator limitations")
public class CreateBackupIntegrationTest extends AbstractBigtableAdminIntegrationTest {

    private static final String TEST_CLUSTER_ID = "test-cluster";

    /** Clean up backups after each test to ensure test isolation. */
    @AfterMethod
    public void cleanupBackups() {
        // Best-effort cleanup: try to delete any backups created during tests
        // Note: Emulator may have limited backup cleanup support
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

    @Test(description = "Create backup with duration-based expiration")
    public void testCreateBackupWithDuration() {
        // Given: a table with one column family
        String tableId = "test-backup-duration";
        String backupId = "backup_duration_20260807";
        createTableWithFamily(tableId, "profile");

        // And: a CreateBackup action with duration expiration
        CreateBackup action = new CreateBackup();
        action.setSourceTableId(tableId);
        action.setBackupId(backupId);
        action.setExpireTime("30d");
        action.setClusterId(TEST_CLUSTER_ID);

        // When: executing the action
        action.execute(adminClient);

        // Then: the backup should be created
        assertTrue(backupExists(TEST_CLUSTER_ID, backupId), "Backup should exist");

        // And: backup should reference the source table
        Backup backup = adminClient.getBackup(TEST_CLUSTER_ID, backupId);
        assertTrue(
                backup.getSourceTableId().contains(tableId),
                "Backup should reference source table");
    }

    @Test(description = "Create backup with ISO-8601 timestamp expiration")
    public void testCreateBackupWithTimestamp() {
        // Given: a table with one column family
        String tableId = "test-backup-timestamp";
        String backupId = "backup_timestamp_20260807";
        createTableWithFamily(tableId, "settings");

        // And: a CreateBackup action with ISO-8601 timestamp expiration
        CreateBackup action = new CreateBackup();
        action.setSourceTableId(tableId);
        action.setBackupId(backupId);
        action.setExpireTime("2026-09-07T00:00:00Z");
        action.setClusterId(TEST_CLUSTER_ID);

        // When: executing the action
        action.execute(adminClient);

        // Then: the backup should be created
        assertTrue(backupExists(TEST_CLUSTER_ID, backupId), "Backup should exist");
    }

    @Test(description = "Create backup with HOT backup type")
    public void testCreateBackupWithHotType() {
        // Given: a table with one column family
        String tableId = "test-backup-hot";
        String backupId = "backup_hot_20260807";
        createTableWithFamily(tableId, "data");

        // And: a CreateBackup action with HOT type
        CreateBackup action = new CreateBackup();
        action.setSourceTableId(tableId);
        action.setBackupId(backupId);
        action.setExpireTime("7d");
        action.setClusterId(TEST_CLUSTER_ID);
        action.setBackupType("HOT");

        // When: executing the action
        action.execute(adminClient);

        // Then: the backup should be created
        assertTrue(backupExists(TEST_CLUSTER_ID, backupId), "Backup should exist");
    }

    @Test(description = "Create backup with STANDARD backup type")
    public void testCreateBackupWithStandardType() {
        // Given: a table with one column family
        String tableId = "test-backup-standard";
        String backupId = "backup_standard_20260807";
        createTableWithFamily(tableId, "analytics");

        // And: a CreateBackup action with STANDARD type
        CreateBackup action = new CreateBackup();
        action.setSourceTableId(tableId);
        action.setBackupId(backupId);
        action.setExpireTime("30d");
        action.setClusterId(TEST_CLUSTER_ID);
        action.setBackupType("STANDARD");

        // When: executing the action
        action.execute(adminClient);

        // Then: the backup should be created
        assertTrue(backupExists(TEST_CLUSTER_ID, backupId), "Backup should exist");
    }

    @Test(description = "Create multiple backups from the same table")
    public void testCreateMultipleBackupsFromSameTable() {
        // Given: a table with one column family
        String tableId = "test-backup-multiple";
        createTableWithFamily(tableId, "main");

        // And: multiple backup IDs
        String backupId1 = "backup_multiple_1";
        String backupId2 = "backup_multiple_2";

        // When: creating first backup
        CreateBackup action1 = new CreateBackup();
        action1.setSourceTableId(tableId);
        action1.setBackupId(backupId1);
        action1.setExpireTime("30d");
        action1.setClusterId(TEST_CLUSTER_ID);
        action1.execute(adminClient);

        // And: creating second backup
        CreateBackup action2 = new CreateBackup();
        action2.setSourceTableId(tableId);
        action2.setBackupId(backupId2);
        action2.setExpireTime("60d");
        action2.setClusterId(TEST_CLUSTER_ID);
        action2.execute(adminClient);

        // Then: both backups should exist
        assertTrue(backupExists(TEST_CLUSTER_ID, backupId1), "First backup should exist");
        assertTrue(backupExists(TEST_CLUSTER_ID, backupId2), "Second backup should exist");
    }

    @Test(description = "Create backup from table with multiple column families")
    public void testCreateBackupFromTableWithMultipleFamilies() {
        // Given: a table with multiple column families
        String tableId = "test-backup-multi-families";
        String backupId = "backup_multi_families";
        createTableWithFamilies(tableId, Arrays.asList("profile", "settings", "analytics"));

        // And: a CreateBackup action
        CreateBackup action = new CreateBackup();
        action.setSourceTableId(tableId);
        action.setBackupId(backupId);
        action.setExpireTime("30d");
        action.setClusterId(TEST_CLUSTER_ID);

        // When: executing the action
        action.execute(adminClient);

        // Then: the backup should be created
        assertTrue(backupExists(TEST_CLUSTER_ID, backupId), "Backup should exist");
    }

    @Test(description = "Create backup from table with GC rules")
    public void testCreateBackupFromTableWithGcRules() {
        // Given: a table with a family that has GC rules
        String tableId = "test-backup-gc-rules";
        String backupId = "backup_gc_rules";

        ColumnFamily family = new ColumnFamily();
        family.setName("versioned");
        family.setMaxVersions(5);
        family.setMaxAge("90d");
        createTableWithFamiliesObjects(tableId, List.of(family));

        // And: a CreateBackup action
        CreateBackup action = new CreateBackup();
        action.setSourceTableId(tableId);
        action.setBackupId(backupId);
        action.setExpireTime("30d");
        action.setClusterId(TEST_CLUSTER_ID);

        // When: executing the action
        action.execute(adminClient);

        // Then: the backup should be created
        assertTrue(backupExists(TEST_CLUSTER_ID, backupId), "Backup should exist");
    }

    @Test(description = "Create backup with short expiration time")
    public void testCreateBackupWithShortExpiration() {
        // Given: a table
        String tableId = "test-backup-short-expire";
        String backupId = "backup_short_expire";
        createTableWithFamily(tableId, "temp");

        // And: a CreateBackup action with short expiration (1 hour)
        CreateBackup action = new CreateBackup();
        action.setSourceTableId(tableId);
        action.setBackupId(backupId);
        action.setExpireTime("1h");
        action.setClusterId(TEST_CLUSTER_ID);

        // When: executing the action
        action.execute(adminClient);

        // Then: the backup should be created
        assertTrue(backupExists(TEST_CLUSTER_ID, backupId), "Backup should exist");
    }

    @Test(description = "Create backup with long expiration time")
    public void testCreateBackupWithLongExpiration() {
        // Given: a table
        String tableId = "test-backup-long-expire";
        String backupId = "backup_long_expire";
        createTableWithFamily(tableId, "archive");

        // And: a CreateBackup action with long expiration (365 days)
        CreateBackup action = new CreateBackup();
        action.setSourceTableId(tableId);
        action.setBackupId(backupId);
        action.setExpireTime("365d");
        action.setClusterId(TEST_CLUSTER_ID);

        // When: executing the action
        action.execute(adminClient);

        // Then: the backup should be created
        assertTrue(backupExists(TEST_CLUSTER_ID, backupId), "Backup should exist");
    }

    @Test(
            description = "Verify creating backup from non-existent table fails",
            expectedExceptions = com.google.api.gax.rpc.NotFoundException.class)
    public void testCreateBackupFromNonExistentTableFails() {
        // Given: a CreateBackup action for non-existent table
        CreateBackup action = new CreateBackup();
        action.setSourceTableId("non-existent-table");
        action.setBackupId("backup_nonexistent");
        action.setExpireTime("30d");
        action.setClusterId(TEST_CLUSTER_ID);

        // When/Then: should throw NotFoundException
        action.execute(adminClient);
    }

    @Test(
            description = "Verify missing sourceTableId throws IllegalArgumentException",
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*sourceTableId is required.*")
    public void testCreateBackupMissingSourceTableIdFails() {
        // Given: a CreateBackup action without sourceTableId
        CreateBackup action = new CreateBackup();
        action.setSourceTableId(null);
        action.setBackupId("backup_test");
        action.setExpireTime("30d");
        action.setClusterId(TEST_CLUSTER_ID);

        // When/Then: should throw IllegalArgumentException
        action.execute(adminClient);
    }

    @Test(
            description = "Verify empty sourceTableId throws IllegalArgumentException",
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*sourceTableId is required.*")
    public void testCreateBackupEmptySourceTableIdFails() {
        // Given: a CreateBackup action with empty sourceTableId
        CreateBackup action = new CreateBackup();
        action.setSourceTableId("");
        action.setBackupId("backup_test");
        action.setExpireTime("30d");
        action.setClusterId(TEST_CLUSTER_ID);

        // When/Then: should throw IllegalArgumentException
        action.execute(adminClient);
    }

    @Test(
            description = "Verify missing backupId throws IllegalArgumentException",
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*backupId is required.*")
    public void testCreateBackupMissingBackupIdFails() {
        // Given: a table
        String tableId = "test-missing-backup-id";
        createTableWithFamily(tableId, "data");

        // And: a CreateBackup action without backupId
        CreateBackup action = new CreateBackup();
        action.setSourceTableId(tableId);
        action.setBackupId(null);
        action.setExpireTime("30d");
        action.setClusterId(TEST_CLUSTER_ID);

        // When/Then: should throw IllegalArgumentException
        action.execute(adminClient);
    }

    @Test(
            description = "Verify empty backupId throws IllegalArgumentException",
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*backupId is required.*")
    public void testCreateBackupEmptyBackupIdFails() {
        // Given: a table
        String tableId = "test-empty-backup-id";
        createTableWithFamily(tableId, "data");

        // And: a CreateBackup action with empty backupId
        CreateBackup action = new CreateBackup();
        action.setSourceTableId(tableId);
        action.setBackupId("");
        action.setExpireTime("30d");
        action.setClusterId(TEST_CLUSTER_ID);

        // When/Then: should throw IllegalArgumentException
        action.execute(adminClient);
    }

    @Test(
            description = "Verify missing expireTime throws IllegalArgumentException",
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*expireTime is required.*")
    public void testCreateBackupMissingExpireTimeFails() {
        // Given: a table
        String tableId = "test-missing-expire";
        createTableWithFamily(tableId, "data");

        // And: a CreateBackup action without expireTime
        CreateBackup action = new CreateBackup();
        action.setSourceTableId(tableId);
        action.setBackupId("backup_test");
        action.setExpireTime(null);
        action.setClusterId(TEST_CLUSTER_ID);

        // When/Then: should throw IllegalArgumentException
        action.execute(adminClient);
    }

    @Test(
            description = "Verify empty expireTime throws IllegalArgumentException",
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*expireTime is required.*")
    public void testCreateBackupEmptyExpireTimeFails() {
        // Given: a table
        String tableId = "test-empty-expire";
        createTableWithFamily(tableId, "data");

        // And: a CreateBackup action with empty expireTime
        CreateBackup action = new CreateBackup();
        action.setSourceTableId(tableId);
        action.setBackupId("backup_test");
        action.setExpireTime("");
        action.setClusterId(TEST_CLUSTER_ID);

        // When/Then: should throw IllegalArgumentException
        action.execute(adminClient);
    }

    @Test(
            description = "Verify missing clusterId throws IllegalArgumentException",
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*clusterId is required.*")
    public void testCreateBackupMissingClusterIdFails() {
        // Given: a table
        String tableId = "test-missing-cluster";
        createTableWithFamily(tableId, "data");

        // And: a CreateBackup action without clusterId
        CreateBackup action = new CreateBackup();
        action.setSourceTableId(tableId);
        action.setBackupId("backup_test");
        action.setExpireTime("30d");
        action.setClusterId(null);

        // When/Then: should throw IllegalArgumentException
        action.execute(adminClient);
    }

    @Test(
            description = "Verify empty clusterId throws IllegalArgumentException",
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*clusterId is required.*")
    public void testCreateBackupEmptyClusterIdFails() {
        // Given: a table
        String tableId = "test-empty-cluster";
        createTableWithFamily(tableId, "data");

        // And: a CreateBackup action with empty clusterId
        CreateBackup action = new CreateBackup();
        action.setSourceTableId(tableId);
        action.setBackupId("backup_test");
        action.setExpireTime("30d");
        action.setClusterId("");

        // When/Then: should throw IllegalArgumentException
        action.execute(adminClient);
    }

    @Test(
            description = "Verify invalid expireTime format throws IllegalArgumentException",
            expectedExceptions = IllegalArgumentException.class)
    public void testCreateBackupInvalidExpireTimeFormatFails() {
        // Given: a table
        String tableId = "test-invalid-expire";
        createTableWithFamily(tableId, "data");

        // And: a CreateBackup action with invalid expireTime format
        CreateBackup action = new CreateBackup();
        action.setSourceTableId(tableId);
        action.setBackupId("backup_test");
        action.setExpireTime("invalid-time");
        action.setClusterId(TEST_CLUSTER_ID);

        // When/Then: should throw IllegalArgumentException
        action.execute(adminClient);
    }

    @Test(
            description = "Verify invalid backupType throws IllegalArgumentException",
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*Invalid backupType.*")
    public void testCreateBackupInvalidBackupTypeFails() {
        // Given: a table
        String tableId = "test-invalid-type";
        createTableWithFamily(tableId, "data");

        // And: a CreateBackup action with invalid backupType
        CreateBackup action = new CreateBackup();
        action.setSourceTableId(tableId);
        action.setBackupId("backup_test");
        action.setExpireTime("30d");
        action.setClusterId(TEST_CLUSTER_ID);
        action.setBackupType("INVALID_TYPE");

        // When/Then: should throw IllegalArgumentException
        action.execute(adminClient);
    }

    @Test(
            description = "Verify duplicate backup ID fails",
            expectedExceptions = com.google.api.gax.rpc.AlreadyExistsException.class)
    public void testCreateBackupDuplicateIdFails() {
        // Given: a table
        String tableId = "test-duplicate-backup";
        String backupId = "backup_duplicate";
        createTableWithFamily(tableId, "data");

        // And: an existing backup
        CreateBackup action1 = new CreateBackup();
        action1.setSourceTableId(tableId);
        action1.setBackupId(backupId);
        action1.setExpireTime("30d");
        action1.setClusterId(TEST_CLUSTER_ID);
        action1.execute(adminClient);

        // When/Then: creating backup with same ID should fail
        CreateBackup action2 = new CreateBackup();
        action2.setSourceTableId(tableId);
        action2.setBackupId(backupId);
        action2.setExpireTime("30d");
        action2.setClusterId(TEST_CLUSTER_ID);
        action2.execute(adminClient);
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
     * Check if a BigTable backup exists.
     *
     * @param clusterId the ID of the cluster
     * @param backupId the ID of the backup to check
     * @return true if the backup exists, false otherwise
     */
    private boolean backupExists(String clusterId, String backupId) {
        try {
            adminClient.getBackup(clusterId, backupId);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
