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

package io.github.totalschema.connector.gcp.bigtable.actions.data.deletecellversions;

import com.google.cloud.bigtable.data.v2.BigtableDataClient;
import com.google.cloud.bigtable.data.v2.models.Range;
import com.google.cloud.bigtable.data.v2.models.RowMutation;
import com.google.protobuf.ByteString;
import io.github.totalschema.connector.gcp.bigtable.actions.data.DataAction;
import io.github.totalschema.connector.gcp.bigtable.actions.data.deletecell.DeleteCell;

/**
 * Deletes specific versions of a cell from a BigTable table based on timestamp ranges.
 *
 * <p>This action provides granular control over which cell versions to delete. Unlike {@link
 * DeleteCell}, which deletes ALL versions, this action requires explicit filter criteria and will
 * fail if no filter is specified, preventing accidental deletion of all versions.
 *
 * <p>Example YAML configurations:
 *
 * <pre>
 * # Delete cell versions within a specific time range (GDPR compliance)
 * DeleteCellVersions:
 *   tableId: audit_log
 *   rowKey: "log#${LOG_ID}"
 *   familyName: events
 *   columnQualifier: details
 *   startTimestampMicros: 1692201600000000  # Delete versions after this time
 *   endTimestampMicros: 1692288000000000    # Delete versions before this time
 *
 * # Delete all versions after a specific timestamp
 * DeleteCellVersions:
 *   tableId: user_sessions
 *   rowKey: "session#user-123"
 *   familyName: data
 *   columnQualifier: session_data
 *   startTimestampMicros: 1692288000000000  # Delete from this time forward
 *
 * # Delete all versions before a specific timestamp
 * DeleteCellVersions:
 *   tableId: logs
 *   rowKey: "log#456"
 *   familyName: events
 *   columnQualifier: data
 *   endTimestampMicros: 1692288000000000    # Delete up to this time
 * </pre>
 *
 * <p><strong>Use Cases:</strong>
 *
 * <ul>
 *   <li>GDPR/compliance data deletion within specific time ranges
 *   <li>Removing outdated cell versions while keeping recent ones
 *   <li>Reverting bad data written during a specific time period
 *   <li>Cleaning up historical data with precise time control
 * </ul>
 *
 * <p><strong>Safety:</strong> This action requires at least one of {@code startTimestampMicros} or
 * {@code endTimestampMicros} to be specified. If you need to delete ALL versions of a cell, use
 * {@link DeleteCell} instead.
 *
 * @see DeleteCell
 */
public class DeleteCellVersions implements DataAction {

    private String tableId;
    private String rowKey;
    private String familyName;
    private String columnQualifier;
    private Long startTimestampMicros;
    private Long endTimestampMicros;

    @Override
    public void execute(BigtableDataClient dataClient) {
        validateInputs();

        RowMutation mutation = RowMutation.create(tableId, ByteString.copyFromUtf8(rowKey));
        ByteString columnQualifierBytes = ByteString.copyFromUtf8(columnQualifier);

        // Build timestamp range based on provided parameters
        Range.TimestampRange timestampRange;

        if (startTimestampMicros != null && endTimestampMicros != null) {
            // Delete versions within specific range
            timestampRange = Range.TimestampRange.create(startTimestampMicros, endTimestampMicros);
        } else if (startTimestampMicros != null) {
            // Delete versions from start time onwards
            timestampRange = Range.TimestampRange.unbounded().startClosed(startTimestampMicros);
        } else {
            // Delete versions up to end time (endTimestampMicros is guaranteed to be set by
            // validation)
            timestampRange = Range.TimestampRange.create(0L, endTimestampMicros);
        }

        mutation.deleteCells(familyName, columnQualifierBytes, timestampRange);

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

        // Require at least one timestamp filter to prevent accidental deletion of all versions
        if (startTimestampMicros == null && endTimestampMicros == null) {
            throw new IllegalArgumentException(
                    "At least one of 'startTimestampMicros' or 'endTimestampMicros' must be"
                            + " specified. To delete ALL versions, use DeleteCell instead.");
        }

        if (startTimestampMicros != null
                && endTimestampMicros != null
                && startTimestampMicros >= endTimestampMicros) {
            throw new IllegalArgumentException(
                    "startTimestampMicros must be less than endTimestampMicros");
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

    /**
     * Gets the start timestamp in microseconds for deletion range.
     *
     * @return the start timestamp in microseconds, or null
     */
    public Long getStartTimestampMicros() {
        return startTimestampMicros;
    }

    /**
     * Sets the start timestamp in microseconds for deletion range.
     *
     * @param startTimestampMicros the start timestamp in microseconds
     */
    public void setStartTimestampMicros(Long startTimestampMicros) {
        this.startTimestampMicros = startTimestampMicros;
    }

    /**
     * Gets the end timestamp in microseconds for deletion range.
     *
     * @return the end timestamp in microseconds, or null
     */
    public Long getEndTimestampMicros() {
        return endTimestampMicros;
    }

    /**
     * Sets the end timestamp in microseconds for deletion range.
     *
     * @param endTimestampMicros the end timestamp in microseconds
     */
    public void setEndTimestampMicros(Long endTimestampMicros) {
        this.endTimestampMicros = endTimestampMicros;
    }

    @Override
    public String toString() {
        return "DeleteCellVersions{"
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
                + ", startTimestampMicros="
                + startTimestampMicros
                + ", endTimestampMicros="
                + endTimestampMicros
                + '}';
    }
}
