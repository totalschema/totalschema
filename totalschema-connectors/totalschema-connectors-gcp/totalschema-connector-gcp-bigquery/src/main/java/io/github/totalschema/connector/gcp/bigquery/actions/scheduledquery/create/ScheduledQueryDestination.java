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

package io.github.totalschema.connector.gcp.bigquery.actions.scheduledquery.create;

/**
 * Configuration for the destination of a scheduled query.
 *
 * <p>Example YAML:
 *
 * <pre>{@code
 * destination:
 *   datasetId: analytics
 *   tableId: daily_sales_summary
 *   writeDisposition: WRITE_APPEND     # WRITE_APPEND (default) or WRITE_TRUNCATE
 *   partitioningField: date_column    # Optional
 * }</pre>
 *
 * <p>Write disposition options:
 *
 * <ul>
 *   <li>{@code WRITE_APPEND} - Append to the table (default)
 *   <li>{@code WRITE_TRUNCATE} - Truncate the table before writing
 * </ul>
 */
public class ScheduledQueryDestination {

    private String datasetId;
    private String tableId;
    private String writeDisposition;
    private String partitioningField;

    public ScheduledQueryDestination() {}

    public ScheduledQueryDestination(
            String datasetId, String tableId, String writeDisposition, String partitioningField) {
        this.datasetId = datasetId;
        this.tableId = tableId;
        this.writeDisposition = writeDisposition;
        this.partitioningField = partitioningField;
    }

    public String getDatasetId() {
        return datasetId;
    }

    public void setDatasetId(String datasetId) {
        this.datasetId = datasetId;
    }

    public String getTableId() {
        return tableId;
    }

    public void setTableId(String tableId) {
        this.tableId = tableId;
    }

    public String getWriteDisposition() {
        return writeDisposition;
    }

    public void setWriteDisposition(String writeDisposition) {
        this.writeDisposition = writeDisposition;
    }

    public String getPartitioningField() {
        return partitioningField;
    }

    public void setPartitioningField(String partitioningField) {
        this.partitioningField = partitioningField;
    }

    @Override
    public String toString() {
        return "ScheduledQueryDestination{"
                + "datasetId='"
                + datasetId
                + '\''
                + ", tableId='"
                + tableId
                + '\''
                + ", writeDisposition='"
                + writeDisposition
                + '\''
                + ", partitioningField='"
                + partitioningField
                + '\''
                + '}';
    }
}
