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

package io.github.totalschema.extensions.kotlin;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Kotlin-friendly SQL helper that provides convenience methods similar to Groovy's {@code
 * groovy.sql.Sql}.
 *
 * <p>This class wraps a JDBC {@link Connection} and provides idiomatic methods for executing SQL
 * statements, queries, and updates. It automatically handles resource cleanup and provides more
 * convenient APIs than raw JDBC.
 *
 * <p><b>Example usage in Kotlin scripts:</b>
 *
 * <pre>{@code
 * // Execute DDL
 * sql.execute("CREATE TABLE users (id INT, name VARCHAR(100))")
 *
 * // Execute with parameters
 * sql.execute("INSERT INTO users VALUES (?, ?)", 1, "John")
 *
 * // Query and get rows as maps
 * val users = sql.rows("SELECT * FROM users WHERE id > ?", 0)
 * users.forEach { println("Name: ${it["name"]}") }
 *
 * // Execute update and get affected rows
 * val count = sql.executeUpdate("DELETE FROM users WHERE id = ?", 999)
 * }</pre>
 *
 * @see Connection
 */
public final class KotlinSql {

    private final Connection connection;

    /**
     * Creates a new KotlinSql wrapper around the given connection.
     *
     * @param connection the JDBC connection to wrap
     */
    public KotlinSql(Connection connection) {
        this.connection = connection;
    }

    /**
     * Returns the underlying JDBC connection for advanced usage.
     *
     * @return the wrapped connection
     */
    public Connection getConnection() {
        return connection;
    }

    /**
     * Executes a SQL statement (DDL or DML without parameters).
     *
     * @param sql the SQL statement to execute
     * @throws SQLException if a database error occurs
     */
    public void execute(String sql) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }

    /**
     * Executes a SQL statement with parameters.
     *
     * @param sql the SQL statement with ? placeholders
     * @param params the parameter values
     * @throws SQLException if a database error occurs
     */
    public void execute(String sql, Object... params) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            setParameters(stmt, params);
            stmt.execute();
        }
    }

    /**
     * Executes an update statement (INSERT, UPDATE, DELETE) and returns the number of affected
     * rows.
     *
     * @param sql the SQL statement with ? placeholders
     * @param params the parameter values
     * @return the number of rows affected
     * @throws SQLException if a database error occurs
     */
    public int executeUpdate(String sql, Object... params) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            setParameters(stmt, params);
            return stmt.executeUpdate();
        }
    }

    /**
     * Executes a query and returns all rows as a list of maps. Each map represents a row with
     * column names as keys.
     *
     * @param sql the SELECT query with ? placeholders
     * @param params the parameter values
     * @return list of rows, where each row is a map of column name to value
     * @throws SQLException if a database error occurs
     */
    public List<Map<String, Object>> rows(String sql, Object... params) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            setParameters(stmt, params);
            try (ResultSet rs = stmt.executeQuery()) {
                List<Map<String, Object>> result = new ArrayList<>();

                ResultSetMetaData metaData = rs.getMetaData();

                int columnCount = metaData.getColumnCount();

                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = metaData.getColumnName(i);
                        row.put(columnName, rs.getObject(i));
                    }
                    result.add(row);
                }
                return result;
            }
        }
    }

    /**
     * Executes a query that returns a single row as a map, or null if no row is found.
     *
     * @param sql the SELECT query with ? placeholders
     * @param params the parameter values
     * @return a map representing the first row, or null if no rows found
     * @throws SQLException if a database error occurs
     */
    public Map<String, Object> firstRow(String sql, Object... params) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            setParameters(stmt, params);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    int columnCount = rs.getMetaData().getColumnCount();
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = rs.getMetaData().getColumnName(i);
                        row.put(columnName, rs.getObject(i));
                    }
                    return row;
                } else {
                    return null;
                }
            }
        }
    }

    /**
     * Executes a batch of statements with the same SQL and different parameter sets.
     *
     * @param sql the SQL statement with ? placeholders
     * @param paramsList list of parameter arrays, one for each batch execution
     * @return array of update counts, one for each batch execution
     * @throws SQLException if a database error occurs
     */
    public int[] executeBatch(String sql, List<Object[]> paramsList) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            for (Object[] params : paramsList) {
                setParameters(stmt, params);
                stmt.addBatch();
            }
            return stmt.executeBatch();
        }
    }

    /**
     * Executes a query and processes each row with the provided callback (closure). This is more
     * memory-efficient than {@link #rows(String, Object...)} for large result sets since rows are
     * processed one at a time without building a complete list in memory.
     *
     * <p><b>Example usage in Kotlin:</b>
     *
     * <pre>{@code
     * sql.eachRow("SELECT * FROM users WHERE age > ?", 18) { row ->
     *     println("${row["name"]} is ${row["age"]} years old")
     * }
     * }</pre>
     *
     * @param sql the SELECT query with ? placeholders
     * @param params the parameter values
     * @param rowCallback callback function invoked for each row with a map of column values
     * @throws SQLException if a database error occurs
     */
    public void eachRow(String sql, Object[] params, RowCallback rowCallback) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            setParameters(stmt, params);
            try (ResultSet rs = stmt.executeQuery()) {
                int columnCount = rs.getMetaData().getColumnCount();

                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = rs.getMetaData().getColumnName(i);
                        row.put(columnName, rs.getObject(i));
                    }
                    rowCallback.process(row);
                }
            }
        }
    }

    /**
     * Executes a query and processes each row with the provided callback (closure). Convenience
     * overload without parameters.
     *
     * <p><b>Example usage in Kotlin:</b>
     *
     * <pre>{@code
     * sql.eachRow("SELECT * FROM users") { row ->
     *     println("User: ${row["name"]}")
     * }
     * }</pre>
     *
     * @param sql the SELECT query
     * @param rowCallback callback function invoked for each row with a map of column values
     * @throws SQLException if a database error occurs
     */
    public void eachRow(String sql, RowCallback rowCallback) throws SQLException {
        eachRow(sql, new Object[0], rowCallback);
    }

    /**
     * Executes a query and processes each row with the provided callback along with row metadata.
     * The callback receives both the row data and a {@link RowMetadata} object containing
     * information about the current row number and total column count.
     *
     * <p><b>Example usage in Kotlin:</b>
     *
     * <pre>{@code
     * sql.eachRowWithMetadata("SELECT * FROM users") { row, meta ->
     *     println("Row ${meta.rowNumber}: ${row["name"]} (${meta.columnCount} columns)")
     * }
     * }</pre>
     *
     * @param sql the SELECT query with ? placeholders
     * @param params the parameter values
     * @param rowCallback callback function invoked for each row with row data and metadata
     * @throws SQLException if a database error occurs
     */
    public void eachRowWithMetadata(
            String sql, Object[] params, RowCallbackWithMetadata rowCallback) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            setParameters(stmt, params);
            try (ResultSet rs = stmt.executeQuery()) {
                int columnCount = rs.getMetaData().getColumnCount();
                int rowNumber = 0;

                while (rs.next()) {
                    rowNumber++;
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = rs.getMetaData().getColumnName(i);
                        row.put(columnName, rs.getObject(i));
                    }
                    rowCallback.process(row, new RowMetadata(rowNumber, columnCount));
                }
            }
        }
    }

    /**
     * Executes a query and processes each row with the provided callback along with row metadata.
     * Convenience overload without parameters.
     *
     * @param sql the SELECT query
     * @param rowCallback callback function invoked for each row with row data and metadata
     * @throws SQLException if a database error occurs
     */
    public void eachRowWithMetadata(String sql, RowCallbackWithMetadata rowCallback)
            throws SQLException {
        eachRowWithMetadata(sql, new Object[0], rowCallback);
    }

    /**
     * Callback interface for processing individual rows in {@link #eachRow(String, Object[],
     * RowCallback)}.
     *
     * <p>In Kotlin scripts, this is typically used with lambda syntax:
     *
     * <pre>{@code
     * sql.eachRow("SELECT ...") { row -> println(row["column"]) }
     * }</pre>
     */
    @FunctionalInterface
    public interface RowCallback {
        /**
         * Processes a single row from the result set.
         *
         * @param row map of column names to values for the current row
         */
        void process(Map<String, Object> row);
    }

    /**
     * Callback interface for processing individual rows with metadata in {@link
     * #eachRowWithMetadata(String, Object[], RowCallbackWithMetadata)}.
     *
     * <p>In Kotlin scripts, this is typically used with lambda syntax:
     *
     * <pre>{@code
     * sql.eachRowWithMetadata("SELECT ...") { row, meta ->
     *     println("Row ${meta.rowNumber}: ${row["column"]}")
     * }
     * }</pre>
     */
    @FunctionalInterface
    public interface RowCallbackWithMetadata {
        /**
         * Processes a single row from the result set along with metadata.
         *
         * @param row map of column names to values for the current row
         * @param metadata metadata about the current row
         */
        void process(Map<String, Object> row, RowMetadata metadata);
    }

    /**
     * Metadata about a row being processed in {@link #eachRowWithMetadata(String, Object[],
     * RowCallbackWithMetadata)}.
     */
    public static final class RowMetadata {
        private final int rowNumber;
        private final int columnCount;

        /**
         * Creates row metadata.
         *
         * @param rowNumber the current row number (1-based)
         * @param columnCount the number of columns in the result set
         */
        public RowMetadata(int rowNumber, int columnCount) {
            this.rowNumber = rowNumber;
            this.columnCount = columnCount;
        }

        /**
         * Returns the current row number (1-based).
         *
         * @return the row number
         */
        public int getRowNumber() {
            return rowNumber;
        }

        /**
         * Returns the number of columns in the result set.
         *
         * @return the column count
         */
        public int getColumnCount() {
            return columnCount;
        }

        @Override
        public String toString() {
            return "RowMetadata{rowNumber=" + rowNumber + ", columnCount=" + columnCount + '}';
        }
    }

    private void setParameters(PreparedStatement stmt, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            stmt.setObject(i + 1, params[i]);
        }
    }

    @Override
    public String toString() {
        return "KotlinSql{connection=" + connection + '}';
    }
}
