/*
 * totalschema: tool for managing database versioning and schema changes with ease.
 * Copyright (C) 2025 totalschema development team
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

package io.github.totalschema.jdbc;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

public final class ResultRow {

    private final ResultSet resultSet;

    private final Map<String, Integer> indexMap;

    ResultRow(ResultSet resultSet) throws SQLException {
        this.resultSet = Objects.requireNonNull(resultSet);
        this.indexMap = getIndexMap(resultSet);
    }

    private static Map<String, Integer> getIndexMap(ResultSet resultSet) throws SQLException {
        ResultSetMetaData metaData = resultSet.getMetaData();

        final int columnCount = metaData.getColumnCount();

        Map<String, Integer> columnNameToIndex = new HashMap<>(columnCount);
        for (int i = 1; i <= columnCount; i++) {

            String column = metaData.getColumnLabel(i);
            if (column == null) {
                column = metaData.getColumnName(i);
            }

            if (column == null) {
                throw new IllegalStateException(
                        String.format("Could not map column index %s to column name", i));
            }

            columnNameToIndex.put(normalizeColumnName(column), i);
        }

        return Collections.unmodifiableMap(columnNameToIndex);
    }

    public String getString(String columnName) throws SQLException {

        return resultSet.getString(getColumnIndex(columnName));
    }

    public Timestamp getTimestamp(String columnName) throws SQLException {

        return resultSet.getTimestamp(
                getColumnIndex(columnName), Calendar.getInstance(TimeZone.getTimeZone("UTC")));
    }

    private Integer getColumnIndex(String columnName) {
        Integer columnIndex = indexMap.get(normalizeColumnName(columnName));
        if (columnIndex == null) {
            throw new IllegalStateException(
                    "The column "
                            + columnName
                            + " is unknown in this ResultRow; "
                            + "known columns and their indexes are: "
                            + indexMap);
        }
        return columnIndex;
    }

    private static String normalizeColumnName(String column) {
        return column.toUpperCase(Locale.ENGLISH);
    }
}
