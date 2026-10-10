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

package io.github.totalschema.connector.gcp.bigtable.actions.table.deletetable;

import com.google.cloud.bigtable.admin.v2.BigtableTableAdminClient;
import io.github.totalschema.connector.gcp.bigtable.actions.table.TableAction;

/**
 * Deletes a BigTable table.
 *
 * <p>Example YAML configurations:
 *
 * <pre>
 * # Simple table deletion
 * DeleteTable:
 *   tableId: users
 *
 * # Delete table as revert action
 * DeleteTable:
 *   tableId: temporary_table
 * </pre>
 *
 * <p><strong>Warning:</strong> This action permanently deletes the table and all its data. This
 * operation cannot be undone.
 */
public class DeleteTable implements TableAction {

    private String tableId;

    @Override
    public void execute(BigtableTableAdminClient adminClient) {
        if (tableId == null || tableId.isEmpty()) {
            throw new IllegalArgumentException("tableId is required");
        }

        adminClient.deleteTable(tableId);
    }

    /**
     * Gets the table ID to delete.
     *
     * @return the table ID
     */
    public String getTableId() {
        return tableId;
    }

    /**
     * Sets the table ID to delete.
     *
     * @param tableId the table ID
     */
    public void setTableId(String tableId) {
        this.tableId = tableId;
    }

    @Override
    public String toString() {
        return "DeleteTable{" + "tableId='" + tableId + '\'' + '}';
    }
}
