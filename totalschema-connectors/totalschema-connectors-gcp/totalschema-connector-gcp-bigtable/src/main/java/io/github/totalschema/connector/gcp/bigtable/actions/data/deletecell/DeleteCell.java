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

package io.github.totalschema.connector.gcp.bigtable.actions.data.deletecell;

import com.google.cloud.bigtable.data.v2.BigtableDataClient;
import com.google.cloud.bigtable.data.v2.models.RowMutation;
import com.google.protobuf.ByteString;
import io.github.totalschema.connector.gcp.bigtable.actions.data.DataAction;
import io.github.totalschema.connector.gcp.bigtable.actions.data.deletecellversions.DeleteCellVersions;

/**
 * Deletes a single cell (all versions) from a BigTable table.
 *
 * <p>This action permanently deletes ALL versions of the specified cell. For more granular control
 * over which versions to delete, use {@link DeleteCellVersions} instead.
 *
 * <p>Example YAML configurations:
 *
 * <pre>
 * # Delete a configuration value (all versions)
 * DeleteCell:
 *   tableId: system_config
 *   rowKey: config#deprecated_setting
 *   familyName: metadata
 *   columnQualifier: value
 *
 * # Delete a feature flag (all versions)
 * DeleteCell:
 *   tableId: feature_flags
 *   rowKey: feature#old_ui
 *   familyName: flags
 *   columnQualifier: enabled
 *
 * # Delete temporary setting (all versions)
 * DeleteCell:
 *   tableId: settings
 *   rowKey: setting#temp_value
 *   familyName: config
 *   columnQualifier: temp
 * </pre>
 *
 * <p><strong>Use Cases:</strong>
 *
 * <ul>
 *   <li>Removing deprecated configuration values
 *   <li>Disabling feature flags
 *   <li>Cleaning up temporary data
 *   <li>Removing specific metadata
 * </ul>
 *
 * <p><strong>Warning:</strong> This action deletes ALL versions of the cell. If you need to delete
 * only specific versions based on timestamp ranges, use {@code DeleteCellVersions} instead.
 *
 * @see DeleteCellVersions
 */
public class DeleteCell implements DataAction {

    private String tableId;
    private String rowKey;
    private String familyName;
    private String columnQualifier;

    @Override
    public void execute(BigtableDataClient dataClient) {
        validateInputs();

        RowMutation mutation = RowMutation.create(tableId, ByteString.copyFromUtf8(rowKey));
        ByteString columnQualifierBytes = ByteString.copyFromUtf8(columnQualifier);

        // Delete all versions of the cell
        mutation.deleteCells(familyName, columnQualifierBytes);

        dataClient.mutateRow(mutation);
    }

    private void validateInputs() {
        if (tableId == null || tableId.isEmpty()) {
            throw new IllegalArgumentException("tableId is required");
        }
        if (rowKey == null || rowKey.isEmpty()) {
            throw new IllegalArgumentException("rowKey is required");
        }
        if (familyName == null || familyName.isEmpty()) {
            throw new IllegalArgumentException("familyName is required");
        }
        if (columnQualifier == null || columnQualifier.isEmpty()) {
            throw new IllegalArgumentException("columnQualifier is required");
        }
    }

    /**
     * Gets the table ID.
     *
     * @return the table ID
     */
    public String getTableId() {
        return tableId;
    }

    /**
     * Sets the table ID.
     *
     * @param tableId the table ID
     */
    public void setTableId(String tableId) {
        this.tableId = tableId;
    }

    /**
     * Gets the row key.
     *
     * @return the row key
     */
    public String getRowKey() {
        return rowKey;
    }

    /**
     * Sets the row key.
     *
     * @param rowKey the row key
     */
    public void setRowKey(String rowKey) {
        this.rowKey = rowKey;
    }

    /**
     * Gets the column family name.
     *
     * @return the family name
     */
    public String getFamilyName() {
        return familyName;
    }

    /**
     * Sets the column family name.
     *
     * @param familyName the family name
     */
    public void setFamilyName(String familyName) {
        this.familyName = familyName;
    }

    /**
     * Gets the column qualifier.
     *
     * @return the column qualifier
     */
    public String getColumnQualifier() {
        return columnQualifier;
    }

    /**
     * Sets the column qualifier.
     *
     * @param columnQualifier the column qualifier
     */
    public void setColumnQualifier(String columnQualifier) {
        this.columnQualifier = columnQualifier;
    }

    @Override
    public String toString() {
        return "DeleteCell{"
                + "tableId='"
                + tableId
                + '\''
                + ", rowKey='"
                + rowKey
                + '\''
                + ", familyName='"
                + familyName
                + '\''
                + ", columnQualifier='"
                + columnQualifier
                + '\''
                + '}';
    }
}
