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

import com.google.cloud.bigtable.admin.v2.BigtableTableAdminClient;
import io.github.totalschema.connector.gcp.bigtable.actions.table.TableAction;

/**
 * Deletes a BigTable backup to free storage space.
 *
 * <p>Use this action for cost management by removing old backups, or as a revert action after
 * creating unnecessary backups.
 *
 * <p>Example YAML configurations:
 *
 * <pre>
 * # Delete backup from default cluster
 * DeleteBackup:
 *   backupId: users_backup_20260701
 *
 * # Delete backup from specific cluster
 * DeleteBackup:
 *   backupId: users_backup_20260701
 *   clusterId: my-cluster
 * </pre>
 *
 * <p><strong>Warning:</strong> This action permanently deletes the backup. Once deleted, the backup
 * cannot be used to restore tables.
 *
 * <p><strong>Note:</strong> If the backup does not exist, this operation fails. Ensure the backupId
 * is correct and the backup exists in the specified cluster.
 */
public class DeleteBackup implements TableAction {

    private String backupId;
    private String clusterId;

    @Override
    public void execute(BigtableTableAdminClient adminClient) {
        if (backupId == null || backupId.isEmpty()) {
            throw new IllegalArgumentException("backupId is required");
        }

        // Delete backup from specified cluster (or default if not specified)
        String cluster =
                (clusterId != null && !clusterId.isEmpty()) ? clusterId : getDefaultClusterId();

        if (cluster != null) {
            adminClient.deleteBackup(cluster, backupId);
        } else {
            // If no cluster is specified, attempt to delete from the instance's default cluster
            // Note: This requires the backup to be in a known cluster. In production, clusterId
            // should typically be specified.
            throw new IllegalArgumentException(
                    "clusterId is required for deleting backups. Please specify the cluster where the backup is stored.");
        }
    }

    /**
     * Gets the default cluster ID when not explicitly specified.
     *
     * <p>Note: Unlike table operations, backup deletion requires an explicit cluster ID because
     * backups are cluster-scoped. This method returns null to force explicit specification.
     *
     * @return null (forces explicit clusterId specification)
     */
    private String getDefaultClusterId() {
        // Backups are cluster-scoped, so we should require explicit clusterId
        // Returning null will cause the validation error above
        return null;
    }

    /**
     * Gets the backup ID to delete.
     *
     * @return the backup ID
     */
    public String getBackupId() {
        return backupId;
    }

    /**
     * Sets the backup ID to delete.
     *
     * @param backupId the backup ID
     */
    public void setBackupId(String backupId) {
        this.backupId = backupId;
    }

    /**
     * Gets the cluster ID where the backup is stored.
     *
     * @return the cluster ID (may be null, but will cause validation error)
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

    @Override
    public String toString() {
        return "DeleteBackup{"
                + "backupId='"
                + backupId
                + '\''
                + ", clusterId='"
                + clusterId
                + '\''
                + '}';
    }
}
