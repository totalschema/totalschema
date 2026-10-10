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

package io.github.totalschema.spi.database.sql;

import java.sql.SQLException;
import java.util.List;

/**
 * Generic interface for executing SQL statements against a relational database connection.
 *
 * <p>Instances of this interface represent a connection to a given database and provide a
 * high-level API for executing SQL statements and queries. All operations support thread
 * interruption and proper transaction handling.
 *
 * <p>Implementations are thread-safe and manage their own connection lifecycle. Connection
 * management details, including pooling, are implementation-specific.
 */
public interface SQLDatabase {

    /**
     * Executes an SQL UPDATE, INSERT, or DELETE statement and returns the number of affected rows.
     *
     * <p>This method is designed for DML statements that modify data. For DDL statements or
     * statements where the row count is not meaningful, use {@link #execute(String, Parameter[])}
     * instead.
     *
     * <p>The statement is executed within a transaction. If auto-commit is disabled in the
     * configuration, the transaction will be committed on success or rolled back on failure.
     *
     * @param sql the SQL statement to execute, may contain '?' placeholders for parameters
     * @param parameters the parameters to bind to the SQL statement, in order of appearance
     * @return the number of rows affected by the statement
     * @throws SQLException if a database error occurs
     * @throws InterruptedException if the current thread is interrupted during execution
     * @see #execute(String, Parameter[])
     */
    int executeUpdate(String sql, Parameter<?>... parameters)
            throws SQLException, InterruptedException;

    /**
     * Executes an SQL statement (DDL, DML, or query) without returning results.
     *
     * <p>This method can execute any SQL statement including:
     *
     * <ul>
     *   <li>DDL statements (CREATE, ALTER, DROP)
     *   <li>DML statements (INSERT, UPDATE, DELETE)
     *   <li>Queries that produce a ResultSet (though results are not returned)
     * </ul>
     *
     * <p>For DML statements where you need the affected row count, use {@link
     * #executeUpdate(String, Parameter[])} instead. For queries where you need to process results,
     * use {@link #query(String, RowMapper, Parameter[])}.
     *
     * <p>The statement is executed within a transaction. If auto-commit is disabled in the
     * configuration, the transaction will be committed on success or rolled back on failure.
     *
     * @param sql the SQL statement to execute, may contain '?' placeholders for parameters
     * @param parameters the parameters to bind to the SQL statement, in order of appearance
     * @throws SQLException if a database error occurs
     * @throws InterruptedException if the current thread is interrupted during execution
     * @see #executeUpdate(String, Parameter[])
     * @see #query(String, RowMapper, Parameter[])
     */
    void execute(String sql, Parameter<?>... parameters) throws SQLException, InterruptedException;

    /**
     * Executes an SQL query and maps each result row to an object of type R.
     *
     * <p>This method is designed for SELECT statements that return data. The provided {@link
     * RowMapper} is called once for each row in the result set to transform the row data into the
     * desired object type.
     *
     * <p>The entire result set is loaded into memory and returned as an immutable list. For large
     * result sets, consider implementing a streaming approach if memory is a concern.
     *
     * <p>The query is executed within a transaction. If auto-commit is disabled in the
     * configuration, the transaction will be committed on success or rolled back on failure.
     *
     * @param <R> the type of objects to return, one per result row
     * @param sql the SQL query to execute, may contain '?' placeholders for parameters
     * @param rowMapper the function to map each result row to an object of type R
     * @param parameters the parameters to bind to the SQL query, in order of appearance
     * @return an immutable list of mapped objects, one per result row; empty list if no rows
     * @throws SQLException if a database error occurs or the row mapper throws an exception
     * @throws InterruptedException if the current thread is interrupted during execution
     * @see RowMapper
     * @see ResultRow
     */
    <R> List<R> query(String sql, RowMapper<R> rowMapper, Parameter<?>... parameters)
            throws SQLException, InterruptedException;

    /**
     * Checks whether a table exists in the database with the specified catalog, schema, and name.
     *
     * <p>This method queries the database metadata to determine table existence. It tries multiple
     * case variations (original, uppercase, lowercase) to handle databases with different case
     * sensitivity rules.
     *
     * @param catalog the catalog name; null if not applicable for the database
     * @param schema the schema name; null if not applicable for the database
     * @param tableName the table name to check; must not be null
     * @return true if the table exists, false otherwise
     * @throws SQLException if a database error occurs
     * @throws InterruptedException if the current thread is interrupted during the check
     */
    boolean isTableFound(String catalog, String schema, String tableName)
            throws SQLException, InterruptedException;
}
