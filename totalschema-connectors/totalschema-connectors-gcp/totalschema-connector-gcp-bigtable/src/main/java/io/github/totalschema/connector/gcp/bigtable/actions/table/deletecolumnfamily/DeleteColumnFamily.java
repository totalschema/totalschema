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

package io.github.totalschema.connector.gcp.bigtable.actions.table.deletecolumnfamily;

import com.google.cloud.bigtable.admin.v2.BigtableTableAdminClient;
import io.github.totalschema.connector.gcp.bigtable.actions.table.TableAction;

/**
 * Removes a column family from a BigTable table.
 *
 * <p>Example YAML configurations:
 *
 * <pre>
 * # Delete a column family
 * DeleteColumnFamily:
 *   tableId: users
 *   familyName: old_analytics
 *
 * # Delete family as revert action
 * DeleteColumnFamily:
 *   tableId: users
 *   familyName: temporary
 * </pre>
 *
 * <p><strong>Warning:</strong> This action permanently deletes the column family and all its data.
 * All cells in this family are deleted immediately. This operation cannot be undone.
 *
 * <p>The table must exist and contain the specified family for this action to succeed.
 */
public class DeleteColumnFamily implements TableAction {

    private String tableId;
    private String familyName;

    @Override
    public void execute(BigtableTableAdminClient adminClient) {
        if (tableId == null || tableId.isEmpty()) {
            throw new IllegalArgumentException("tableId is required");
        }
        if (familyName == null || familyName.isEmpty()) {
            throw new IllegalArgumentException("familyName is required");
        }

        // Create ModifyColumnFamiliesRequest to drop the family
        com.google.cloud.bigtable.admin.v2.models.ModifyColumnFamiliesRequest request =
                com.google.cloud.bigtable.admin.v2.models.ModifyColumnFamiliesRequest.of(tableId);

        request.dropFamily(familyName);

        adminClient.modifyFamilies(request);
    }

    /**
     * Gets the table ID containing the family to delete.
     *
     * @return the table ID
     */
    public String getTableId() {
        return tableId;
    }

    /**
     * Sets the table ID containing the family to delete.
     *
     * @param tableId the table ID
     */
    public void setTableId(String tableId) {
        this.tableId = tableId;
    }

    /**
     * Gets the column family name to delete.
     *
     * @return the family name
     */
    public String getFamilyName() {
        return familyName;
    }

    /**
     * Sets the column family name to delete.
     *
     * @param familyName the family name
     */
    public void setFamilyName(String familyName) {
        this.familyName = familyName;
    }

    @Override
    public String toString() {
        return "DeleteColumnFamily{"
                + "tableId='"
                + tableId
                + '\''
                + ", familyName='"
                + familyName
                + '\''
                + '}';
    }
}
