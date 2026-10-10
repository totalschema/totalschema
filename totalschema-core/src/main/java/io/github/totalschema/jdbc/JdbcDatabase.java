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

package io.github.totalschema.jdbc;

import io.github.totalschema.spi.database.sql.SQLDatabase;
import java.io.Closeable;
import java.sql.SQLException;
import javax.sql.DataSource;

/**
 * JDBC database implementation combining SQLDatabase with Closeable resource management and
 * JDBC-specific connection features.
 *
 * <p>This interface extends {@link SQLDatabase} to provide SQL execution capabilities with
 * JDBC-specific features for managing connections and accessing the underlying DataSource.
 *
 * <p>See {@link SQLDatabase} for comprehensive API documentation on SQL execution methods.
 *
 * @see SQLDatabase
 * @see Closeable
 * @see ConnectionAction
 */
public interface JdbcDatabase extends SQLDatabase, Closeable {

    /**
     * Executes a custom action with direct access to a JDBC connection from the pool.
     *
     * <p>This method provides low-level access to a pooled JDBC connection for operations that
     * cannot be expressed using the higher-level methods. The connection is automatically obtained
     * from the pool before executing the action and returned to the pool afterward.
     *
     * <p>The action is executed within a transaction. If auto-commit is disabled in the
     * configuration, the transaction will be committed on success or rolled back on failure.
     *
     * <p>The connection must not be closed by the action - it is managed automatically by this
     * method.
     *
     * <p>Example usage:
     *
     * <pre>{@code
     * String dbProduct = database.withConnection(connection -> {
     *     DatabaseMetaData metadata = connection.getMetaData();
     *     return metadata.getDatabaseProductName();
     * });
     * }</pre>
     *
     * @param <R> the type of result returned by the action
     * @param action the action to execute with the connection; must not be null
     * @return the result returned by the action
     * @throws SQLException if a database error occurs
     * @throws InterruptedException if the current thread is interrupted during execution
     * @throws NullPointerException if action is null
     * @see ConnectionAction
     */
    <R> R withConnection(ConnectionAction<R> action) throws InterruptedException, SQLException;

    /**
     * Returns the {@code DataSource} used by this {@code JdbcDatabase}.
     *
     * <p>The method {@link #withConnection(ConnectionAction)} is recommended for any use-case where
     * an operation needs to be performed with a connection, as that method automatically manages
     * the connection lifecycle. If full control over the connections is needed, the {@code
     * DataSource} returned by this method gives full control over the connections. Caller is
     * responsible for properly closing any connections obtained from the DataSource.
     *
     * <p>The returned DataSource is unmodifiable, any attempt to change its properties will result
     * in an {@link UnsupportedOperationException} being thrown.
     *
     * <p>Client code must abstain from modifying the returned connections: any attempt to call a
     * setter method on a connection is illegal.
     *
     * @return the {@code DataSource} used by this {@code JdbcDatabase} (never {@code null})
     */
    DataSource getDataSource();
}
