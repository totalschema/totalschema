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

package io.github.totalschema.connector.gcp.bigtable.actions.table.cleartablesplits;

import com.google.cloud.bigtable.admin.v2.BigtableTableAdminClient;
import com.google.cloud.bigtable.admin.v2.models.CreateTableRequest;
import com.google.cloud.bigtable.admin.v2.models.Table;
import io.github.totalschema.connector.gcp.bigtable.actions.table.TableAction;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Removes pre-defined splits from a BigTable table by recreating it without split keys.
 *
 * <p><strong>⚠️ WARNING: This is a destructive operation!</strong> This action will:
 *
 * <ul>
 *   <li>Delete the existing table and all its data
 *   <li>Recreate the table with the same column families
 *   <li>Create the table without any pre-defined splits (automatic splitting only)
 * </ul>
 *
 * <p><strong>What This Action Does:</strong>
 *
 * <ul>
 *   <li>Removes custom pre-splits from a table
 *   <li>Allows BigTable to manage splits automatically based on actual data patterns
 *   <li>Useful when initial split strategy proves suboptimal
 *   <li>Recreates table preserving column family schema
 * </ul>
 *
 * <p><strong>Use Cases:</strong>
 *
 * <ul>
 *   <li>Reverting from manual splits to automatic splitting
 *   <li>Correcting a poorly chosen split strategy
 *   <li>Simplifying table configuration for unpredictable workloads
 *   <li>Development/testing environments where data can be reloaded
 * </ul>
 *
 * <p><strong>Example YAML configurations:</strong>
 *
 * <pre>
 * # Remove splits from a table
 * ClearTableSplits:
 *   tableId: users
 *
 * # Remove splits with safety confirmation
 * ClearTableSplits:
 *   tableId: users
 *   confirmDeletion: true
 * </pre>
 *
 * <p><strong>Safety Considerations:</strong>
 *
 * <ul>
 *   <li><strong>Data loss:</strong> All data in the table will be permanently deleted
 *   <li><strong>Downtime:</strong> Table will be unavailable during recreation
 *   <li><strong>Backup first:</strong> Always create a backup before running this action
 *   <li><strong>Schema preservation:</strong> Column families and GC rules are recreated
 *   <li><strong>Confirmation required:</strong> Set confirmDeletion: true to acknowledge risk
 * </ul>
 *
 * <p><strong>Recommended Workflow:</strong>
 *
 * <pre>
 * # 1. Create backup first
 * CreateBackup:
 *   sourceTableId: users
 *   backupId: users_backup_before_clear_splits
 *   expireTime: 7d
 *
 * # 2. Clear splits (data loss occurs here)
 * ClearTableSplits:
 *   tableId: users
 *   confirmDeletion: true
 *
 * # 3. Restore data if needed
 * RestoreTable:
 *   backupId: users_backup_before_clear_splits
 *   tableId: users
 * </pre>
 *
 * <p><strong>Alternative Approaches:</strong>
 *
 * <p>If you cannot afford data loss, consider:
 *
 * <ul>
 *   <li>Creating a new table without splits and copying data
 *   <li>Using Cloud Dataflow to migrate data to new table
 *   <li>Accepting existing splits and relying on automatic re-splitting over time
 * </ul>
 *
 * <p><strong>Performance Implications:</strong>
 *
 * <ul>
 *   <li>After clearing splits, BigTable will start with a single tablet
 *   <li>Automatic splitting will occur as data is written
 *   <li>Initial write performance may be lower until auto-splitting completes
 *   <li>Better for unpredictable workloads where manual splits don't help
 * </ul>
 *
 * @see <a href="https://cloud.google.com/bigtable/docs/schema-design#row-keys">BigTable Row Key
 *     Design</a>
 */
public class ClearTableSplits implements TableAction {

    private static final Logger logger = LoggerFactory.getLogger(ClearTableSplits.class);

    private String tableId;
    private Boolean confirmDeletion;

    @Override
    public void execute(BigtableTableAdminClient adminClient) {
        // Validation
        if (tableId == null || tableId.isEmpty()) {
            throw new IllegalArgumentException("tableId is required. Example: tableId: my-table");
        }

        if (confirmDeletion == null || !confirmDeletion) {
            throw new IllegalArgumentException(
                    "confirmDeletion must be set to true to proceed. "
                            + "This action will DELETE the table and all its data, then recreate it without splits. "
                            + "Example: confirmDeletion: true");
        }

        // Check if table exists
        if (!adminClient.exists(tableId)) {
            throw new IllegalArgumentException(
                    "Table '"
                            + tableId
                            + "' does not exist. Cannot clear splits on non-existent table.");
        }

        try {
            // Get the current table schema (column families)
            Table existingTable = adminClient.getTable(tableId);
            List<com.google.cloud.bigtable.admin.v2.models.ColumnFamily> families =
                    existingTable.getColumnFamilies();

            logger.warn(
                    "Deleting table '{}' to clear splits. This will PERMANENTLY DELETE ALL DATA!",
                    tableId);

            // Delete the existing table
            adminClient.deleteTable(tableId);

            logger.info("Table '{}' deleted. Recreating without splits...", tableId);

            // Recreate table without splits, preserving column families
            CreateTableRequest request = CreateTableRequest.of(tableId);

            // Add column families back, preserving GC rules
            for (com.google.cloud.bigtable.admin.v2.models.ColumnFamily family : families) {
                String familyId = family.getId();

                if (family.hasGCRule()) {
                    request.addFamily(familyId, family.getGCRule());
                } else {
                    request.addFamily(familyId);
                }
            }

            // Create table without any split keys
            adminClient.createTable(request);

            logger.info(
                    "Successfully recreated table '{}' without splits. {} column families preserved.",
                    tableId,
                    families.size());

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to clear splits for table '"
                            + tableId
                            + "'. Table may be in an inconsistent state! Error: "
                            + e.getMessage(),
                    e);
        }
    }

    public String getTableId() {
        return tableId;
    }

    public void setTableId(String tableId) {
        this.tableId = tableId;
    }

    public Boolean getConfirmDeletion() {
        return confirmDeletion;
    }

    public void setConfirmDeletion(Boolean confirmDeletion) {
        this.confirmDeletion = confirmDeletion;
    }

    @Override
    public String toString() {
        return "ClearTableSplits{"
                + "tableId='"
                + tableId
                + '\''
                + ", confirmDeletion="
                + confirmDeletion
                + '}';
    }
}
