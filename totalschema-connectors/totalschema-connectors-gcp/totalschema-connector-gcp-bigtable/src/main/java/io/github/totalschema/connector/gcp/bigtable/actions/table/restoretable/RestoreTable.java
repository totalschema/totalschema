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

import com.google.cloud.bigtable.admin.v2.BigtableTableAdminClient;
import com.google.cloud.bigtable.admin.v2.models.RestoreTableRequest;
import io.github.totalschema.connector.gcp.bigtable.actions.table.TableAction;

/**
 * Restores a BigTable table from a backup.
 *
 * <p>This action is essential for disaster recovery workflows, allowing you to restore tables from
 * previously created backups. The restored table will have the same schema as the backup.
 *
 * <p>Example YAML configurations:
 *
 * <pre>
 * # Restore to a new table
 * RestoreTable:
 *   backupId: users_backup_20260720
 *   tableId: users_restored
 *   clusterId: prod-cluster-us-east1
 *
 * # Restore in-place (requires overwrite flag)
 * RestoreTable:
 *   backupId: users_backup_20260720
 *   tableId: users
 *   clusterId: prod-cluster-us-east1
 *   overwrite: true
 *
 * # Restore to staging environment
 * RestoreTable:
 *   backupId: users_backup_prod
 *   tableId: users_staging
 *   clusterId: staging-cluster
 * </pre>
 *
 * <p><strong>Important considerations:</strong>
 *
 * <ul>
 *   <li>The backup must exist in the specified cluster
 *   <li>Restored table inherits the backup's schema (column families and GC rules)
 *   <li>If the target table already exists, you must set {@code overwrite: true}
 *   <li>Restore operations can take time for large tables
 *   <li>The cluster must have sufficient capacity for the restored table
 * </ul>
 *
 * <p><strong>Use cases:</strong>
 *
 * <ul>
 *   <li>Disaster recovery - restore after data loss or corruption
 *   <li>Rollback - revert to a known good state after failed deployment
 *   <li>Clone data - copy production data to staging/dev environments
 *   <li>Testing - restore from backup to test recovery procedures
 * </ul>
 *
 * <p><strong>Warning:</strong> Using {@code overwrite: true} will permanently delete the existing
 * table and all its data before restoring. This operation cannot be undone.
 */
public class RestoreTable implements TableAction {

    private String backupId;
    private String tableId;
    private String clusterId;
    private Boolean overwrite;

    @Override
    public void execute(BigtableTableAdminClient adminClient) {
        if (backupId == null || backupId.isEmpty()) {
            throw new IllegalArgumentException("backupId is required");
        }
        if (tableId == null || tableId.isEmpty()) {
            throw new IllegalArgumentException("tableId is required");
        }
        if (clusterId == null || clusterId.isEmpty()) {
            throw new IllegalArgumentException(
                    "clusterId is required for restoring tables. Backups are cluster-scoped. "
                            + "Specify the cluster where the backup is stored.");
        }

        // Check if we need to delete existing table first
        if (Boolean.TRUE.equals(overwrite)) {
            if (adminClient.exists(tableId)) {
                adminClient.deleteTable(tableId);
            }
        }

        // Create restore request
        RestoreTableRequest request =
                RestoreTableRequest.of(clusterId, backupId).setTableId(tableId);

        // Execute restore - this may throw an exception if backup doesn't exist
        // or if table already exists and overwrite was not set
        try {
            adminClient.restoreTable(request);
        } catch (Exception e) {
            // Wrap and rethrow with more context
            throw new RuntimeException(
                    "Failed to restore table '"
                            + tableId
                            + "' from backup '"
                            + backupId
                            + "': "
                            + e.getMessage(),
                    e);
        }
    }

    /**
     * Gets the backup ID to restore from.
     *
     * @return the backup ID
     */
    public String getBackupId() {
        return backupId;
    }

    /**
     * Sets the backup ID to restore from.
     *
     * @param backupId the backup ID
     */
    public void setBackupId(String backupId) {
        this.backupId = backupId;
    }

    /**
     * Gets the target table ID for the restored table.
     *
     * @return the table ID
     */
    public String getTableId() {
        return tableId;
    }

    /**
     * Sets the target table ID for the restored table.
     *
     * @param tableId the table ID
     */
    public void setTableId(String tableId) {
        this.tableId = tableId;
    }

    /**
     * Gets the cluster ID where the backup is stored.
     *
     * @return the cluster ID
     */
    public String getClusterId() {
        return clusterId;
    }

    /**
     * Sets the cluster ID where the backup is stored.
     *
     * @param clusterId the cluster ID
     */
    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }

    /**
     * Gets whether to overwrite an existing table.
     *
     * @return true if existing table should be overwritten, false or null otherwise
     */
    public Boolean getOverwrite() {
        return overwrite;
    }

    /**
     * Sets whether to overwrite an existing table.
     *
     * <p>If set to true, any existing table with the same name will be deleted before restore. This
     * is a destructive operation and should be used with caution.
     *
     * @param overwrite true to overwrite existing table, false otherwise
     */
    public void setOverwrite(Boolean overwrite) {
        this.overwrite = overwrite;
    }

    @Override
    public String toString() {
        return "RestoreTable{"
                + "backupId='"
                + backupId
                + '\''
                + ", tableId='"
                + tableId
                + '\''
                + ", clusterId='"
                + clusterId
                + '\''
                + ", overwrite="
                + overwrite
                + '}';
    }
}
