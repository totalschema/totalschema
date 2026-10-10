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

import com.google.cloud.bigtable.admin.v2.BigtableTableAdminClient;
import com.google.cloud.bigtable.admin.v2.models.Backup;
import com.google.cloud.bigtable.admin.v2.models.CreateBackupRequest;
import io.github.totalschema.connector.gcp.bigtable.actions.table.TableAction;
import io.github.totalschema.connector.gcp.bigtable.actions.table.createtable.DurationParser;
import java.util.Locale;
import org.threeten.bp.Instant;

/**
 * Creates a point-in-time backup of a BigTable table.
 *
 * <p>Backups are useful for disaster recovery, compliance archival, and pre-deployment safety. They
 * are stored with automatic expiration and can be used to restore tables via {@code RestoreTable}.
 *
 * <p>Example YAML configurations:
 *
 * <pre>
 * # Create backup with duration-based expiration
 * CreateBackup:
 *   sourceTableId: users
 *   backupId: users_backup_20260720
 *   expireTime: 30d
 *   clusterId: my-cluster
 *
 * # Create backup with explicit timestamp expiration (ISO-8601)
 * CreateBackup:
 *   sourceTableId: users
 *   backupId: users_backup_20260720
 *   expireTime: "2026-08-20T00:00:00Z"
 *   clusterId: prod-cluster-us-east1
 *
 * # Create backup with specific backup type (HOT for faster restore)
 * CreateBackup:
 *   sourceTableId: users
 *   backupId: users_backup_hot
 *   expireTime: 7d
 *   clusterId: prod-cluster-us-east1
 *   backupType: HOT
 * </pre>
 *
 * <p><strong>Supported duration formats:</strong> "7d", "24h", "90days", "30m", "3600s"
 *
 * <p><strong>Supported backup types:</strong> "STANDARD" (default), "HOT"
 *
 * <p><strong>Note:</strong> Backup creation is asynchronous. This action waits for completion
 * before returning.
 *
 * <p><strong>Note:</strong> clusterId is required because backups are cluster-scoped. Use {@code
 * gcloud bigtable clusters list --instances=YOUR_INSTANCE} to find cluster IDs.
 *
 * <p><strong>Cost consideration:</strong> Backups incur storage costs based on table size and
 * retention period. HOT backups may have different pricing than STANDARD backups.
 */
public class CreateBackup implements TableAction {

    private String sourceTableId;
    private String backupId;
    private String expireTime;
    private String clusterId;
    private String backupType; // "STANDARD" (default) or "HOT"

    @Override
    public void execute(BigtableTableAdminClient adminClient) {
        if (sourceTableId == null || sourceTableId.isEmpty()) {
            throw new IllegalArgumentException("sourceTableId is required");
        }
        if (backupId == null || backupId.isEmpty()) {
            throw new IllegalArgumentException("backupId is required");
        }
        if (expireTime == null || expireTime.isEmpty()) {
            throw new IllegalArgumentException("expireTime is required");
        }
        if (clusterId == null || clusterId.isEmpty()) {
            throw new IllegalArgumentException(
                    "clusterId is required for creating backups. Backups are cluster-scoped. "
                            + "Specify the cluster where the backup should be created.");
        }

        // Parse expireTime - can be duration (30d) or ISO-8601 timestamp
        Instant expireInstant = parseExpireTime(expireTime);

        // Create backup request
        CreateBackupRequest request =
                CreateBackupRequest.of(clusterId, backupId)
                        .setSourceTableId(sourceTableId)
                        .setExpireTime(expireInstant);

        // Set backup type if specified
        if (backupType != null && !backupType.isEmpty()) {
            Backup.BackupType type = parseBackupType(backupType);
            request.setBackupType(type);
        }

        adminClient.createBackup(request);
    }

    /**
     * Parses the backupType string into a BackupType enum.
     *
     * <p>Supported values (case-insensitive):
     *
     * <ul>
     *   <li>STANDARD - Standard backup (default if not specified)
     *   <li>HOT - Hot backup for faster restore operations
     * </ul>
     *
     * @param backupType the backup type string
     * @return the parsed BackupType
     * @throws IllegalArgumentException if the backup type is invalid
     */
    private Backup.BackupType parseBackupType(String backupType) {
        String normalized = backupType.trim().toUpperCase(Locale.ENGLISH);
        try {
            return Backup.BackupType.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid backupType: '" + backupType + "'. Supported values: STANDARD, HOT", e);
        }
    }

    /**
     * Parses the expireTime string into an Instant.
     *
     * <p>Supports two formats:
     *
     * <ul>
     *   <li>Duration string (e.g., "30d", "24h") - relative to current time
     *   <li>ISO-8601 timestamp (e.g., "2026-08-20T00:00:00Z") - absolute time
     * </ul>
     *
     * @param expireTime the expireTime string
     * @return the parsed Instant
     */
    private Instant parseExpireTime(String expireTime) {
        // Try to parse as ISO-8601 timestamp first
        try {
            return Instant.parse(expireTime);
        } catch (org.threeten.bp.format.DateTimeParseException e) {
            // Not a timestamp, try parsing as duration
            try {
                org.threeten.bp.Duration duration = DurationParser.parse(expireTime);
                return Instant.now().plus(duration);
            } catch (Exception durationException) {
                throw new IllegalArgumentException(
                        "expireTime must be either a duration (e.g., '30d', '24h') or ISO-8601 timestamp (e.g., '2026-08-20T00:00:00Z'). Got: "
                                + expireTime,
                        durationException);
            }
        }
    }

    /**
     * Gets the source table ID.
     *
     * @return the source table ID
     */
    public String getSourceTableId() {
        return sourceTableId;
    }

    /**
     * Sets the source table ID.
     *
     * @param sourceTableId the source table ID
     */
    public void setSourceTableId(String sourceTableId) {
        this.sourceTableId = sourceTableId;
    }

    /**
     * Gets the backup ID.
     *
     * @return the backup ID
     */
    public String getBackupId() {
        return backupId;
    }

    /**
     * Sets the backup ID.
     *
     * @param backupId the backup ID
     */
    public void setBackupId(String backupId) {
        this.backupId = backupId;
    }

    /**
     * Gets the expire time string.
     *
     * @return the expire time string
     */
    public String getExpireTime() {
        return expireTime;
    }

    /**
     * Sets the expire time string.
     *
     * @param expireTime the expire time string (duration or ISO-8601)
     */
    public void setExpireTime(String expireTime) {
        this.expireTime = expireTime;
    }

    /**
     * Gets the cluster ID.
     *
     * @return the cluster ID (may be null for default cluster)
     */
    public String getClusterId() {
        return clusterId;
    }

    /**
     * Sets the cluster ID.
     *
     * @param clusterId the cluster ID
     */
    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }

    /**
     * Gets the backup type.
     *
     * @return the backup type (may be null for default)
     */
    public String getBackupType() {
        return backupType;
    }

    /**
     * Sets the backup type.
     *
     * @param backupType the backup type ("STANDARD" or "HOT")
     */
    public void setBackupType(String backupType) {
        this.backupType = backupType;
    }

    @Override
    public String toString() {
        return "CreateBackup{"
                + "sourceTableId='"
                + sourceTableId
                + '\''
                + ", backupId='"
                + backupId
                + '\''
                + ", expireTime='"
                + expireTime
                + '\''
                + ", clusterId='"
                + clusterId
                + '\''
                + ", backupType='"
                + backupType
                + '\''
                + '}';
    }
}
