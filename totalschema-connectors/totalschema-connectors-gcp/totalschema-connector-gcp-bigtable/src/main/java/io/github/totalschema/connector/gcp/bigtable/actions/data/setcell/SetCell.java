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

package io.github.totalschema.connector.gcp.bigtable.actions.data.setcell;

import com.google.cloud.bigtable.data.v2.BigtableDataClient;
import com.google.cloud.bigtable.data.v2.models.RowMutation;
import com.google.protobuf.ByteString;
import io.github.totalschema.connector.gcp.bigtable.actions.data.DataAction;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Sets a single cell value in a BigTable table.
 *
 * <p>This action is ideal for configuration values, feature flags, system settings, and other
 * single-value updates that need to be managed declaratively in version control.
 *
 * <p>Example YAML configurations:
 *
 * <pre>
 * # Set a configuration value (inline)
 * SetCell:
 *   tableId: system_config
 *   rowKey: config#database_version
 *   familyName: metadata
 *   columnQualifier: version
 *   value: "2.5.0"
 *
 * # Set a value from a file
 * SetCell:
 *   tableId: system_config
 *   rowKey: config#ssh_public_key
 *   familyName: keys
 *   columnQualifier: public_key
 *   valueFile: /path/to/id_rsa.pub
 *
 * # Set a value from a file with variable substitution
 * SetCell:
 *   tableId: certificates
 *   rowKey: "cert#${ENVIRONMENT}"
 *   familyName: ssl
 *   columnQualifier: certificate
 *   valueFile: ${CERT_DIR}/server.crt
 *
 * # Set a feature flag
 * SetCell:
 *   tableId: feature_flags
 *   rowKey: feature#new_ui
 *   familyName: flags
 *   columnQualifier: enabled
 *   value: "true"
 *
 * # Set with custom timestamp
 * SetCell:
 *   tableId: settings
 *   rowKey: setting#max_connections
 *   familyName: config
 *   columnQualifier: value
 *   value: "100"
 *   timestampMicros: 1692288000000000
 *
 * # Update environment-specific value using variable substitution
 * SetCell:
 *   tableId: env_config
 *   rowKey: "config#${ENVIRONMENT}"
 *   familyName: settings
 *   columnQualifier: api_endpoint
 *   value: "${API_ENDPOINT_URL}"
 * </pre>
 *
 * <p><strong>Use Cases:</strong>
 *
 * <ul>
 *   <li>Setting configuration values
 *   <li>Managing feature flags
 *   <li>Storing system metadata
 *   <li>Setting reference data
 *   <li>Initializing default values
 *   <li>Loading large text or binary data from files
 *   <li>Storing certificates, keys, or credentials
 * </ul>
 *
 * <p><strong>Note:</strong> You must specify either {@code value} or {@code valueFile}, but not
 * both.
 */
public class SetCell implements DataAction {

    private String tableId;
    private String rowKey;
    private String familyName;
    private String columnQualifier;
    private String value;
    private String valueFile;
    private Long timestampMicros;

    @Override
    public void execute(BigtableDataClient dataClient) {
        validateInputs();

        RowMutation mutation = RowMutation.create(tableId, ByteString.copyFromUtf8(rowKey));

        ByteString columnQualifierBytes = ByteString.copyFromUtf8(columnQualifier);
        ByteString valueBytes = getValueBytes();

        if (timestampMicros != null) {
            mutation.setCell(familyName, columnQualifierBytes, timestampMicros, valueBytes);
        } else {
            // Use server timestamp
            mutation.setCell(familyName, columnQualifierBytes, valueBytes);
        }

        dataClient.mutateRow(mutation);
    }

    private ByteString getValueBytes() {
        if (valueFile != null) {
            // Read value from file
            try {
                Path path = Paths.get(valueFile);
                byte[] fileBytes = Files.readAllBytes(path);
                return ByteString.copyFrom(fileBytes);
            } catch (IOException e) {
                throw new RuntimeException("Failed to read value from file: " + valueFile, e);
            }
        } else {
            // Use inline value
            return ByteString.copyFromUtf8(value);
        }
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

        // Validate that exactly one of value or valueFile is specified
        boolean hasValue = value != null;
        boolean hasValueFile = valueFile != null && !valueFile.isEmpty();

        if (!hasValue && !hasValueFile) {
            throw new IllegalArgumentException("Either 'value' or 'valueFile' must be specified");
        }
        if (hasValue && hasValueFile) {
            throw new IllegalArgumentException(
                    "Cannot specify both 'value' and 'valueFile' - choose one");
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
     * Gets the cell value.
     *
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the cell value.
     *
     * @param value the value
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Gets the path to the file containing the cell value.
     *
     * @return the file path
     */
    public String getValueFile() {
        return valueFile;
    }

    /**
     * Sets the path to the file containing the cell value.
     *
     * @param valueFile the file path
     */
    public void setValueFile(String valueFile) {
        this.valueFile = valueFile;
    }

    /**
     * Gets the timestamp in microseconds.
     *
     * @return the timestamp in microseconds, or null for server timestamp
     */
    public Long getTimestampMicros() {
        return timestampMicros;
    }

    /**
     * Sets the timestamp in microseconds.
     *
     * @param timestampMicros the timestamp in microseconds, or null for server timestamp
     */
    public void setTimestampMicros(Long timestampMicros) {
        this.timestampMicros = timestampMicros;
    }

    @Override
    public String toString() {
        return "SetCell{"
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
                + ", value='"
                + value
                + '\''
                + ", valueFile='"
                + valueFile
                + '\''
                + ", timestampMicros="
                + timestampMicros
                + '}';
    }
}
