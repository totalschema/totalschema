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

package io.github.totalschema.connector.gcp.bigquery;

import static java.util.Objects.requireNonNull;

import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.Schema;
import io.github.totalschema.spi.database.sql.ResultRow;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * BigQuery implementation of ResultRow backed by a FieldValueList.
 *
 * <p>Adapts BigQuery's FieldValueList to the ResultRow interface for compatibility with row mapping
 * operations.
 */
final class BigQueryResultRow implements ResultRow {

    private final FieldValueList row;
    private final Map<String, Integer> columnIndexMap;

    /**
     * Constructs a BigQueryResultRow from a FieldValueList and its schema.
     *
     * @param schema the schema describing the columns
     * @param row the field values for this row
     */
    BigQueryResultRow(Schema schema, FieldValueList row) {
        this.row = requireNonNull(row, "row must not be null");
        this.columnIndexMap =
                buildColumnIndexMap(requireNonNull(schema, "schema must not be null"));
    }

    private static Map<String, Integer> buildColumnIndexMap(Schema schema) {
        Map<String, Integer> indexMap = new HashMap<>();
        List<Field> fields = schema.getFields();
        for (int i = 0; i < fields.size(); i++) {
            String columnName = normalizeColumnName(fields.get(i).getName());
            indexMap.put(columnName, i);
        }
        return Collections.unmodifiableMap(indexMap);
    }

    @Override
    public String getString(String columnName) throws SQLException {
        int index = getColumnIndex(columnName);
        FieldValue fieldValue = row.get(index);
        if (fieldValue.isNull()) {
            return null;
        }
        return fieldValue.getStringValue();
    }

    @Override
    public Timestamp getTimestamp(String columnName) throws SQLException {
        int index = getColumnIndex(columnName);
        FieldValue fieldValue = row.get(index);
        if (fieldValue.isNull()) {
            return null;
        }
        // BigQuery stores timestamps as microseconds since epoch
        long microseconds = fieldValue.getTimestampValue();
        long milliseconds = microseconds / 1000;
        return new Timestamp(milliseconds);
    }

    private int getColumnIndex(String columnName) throws SQLException {
        String normalizedName = normalizeColumnName(columnName);
        Integer index = columnIndexMap.get(normalizedName);
        if (index == null) {
            throw new SQLException(
                    "Column '"
                            + columnName
                            + "' not found. Available columns: "
                            + columnIndexMap.keySet());
        }
        return index;
    }

    private static String normalizeColumnName(String columnName) {
        return columnName.toUpperCase(Locale.ENGLISH);
    }
}
