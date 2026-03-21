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

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.totalschema.config.Configuration;
import io.github.totalschema.config.MisconfigurationException;
import io.github.totalschema.util.StringUtils;
import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@link JdbcDatabase} providing connection pooling and SQL execution
 * capabilities using HikariCP.
 *
 * <p>This class is the primary JDBC abstraction layer for the totalschema library, used for
 * database locking, schema management, and change tracking. It provides a high-level API for
 * executing SQL statements with automatic connection pooling, transaction management, and proper
 * resource cleanup.
 *
 * <h2>Key Features</h2>
 *
 * <ul>
 *   <li><b>Connection Pooling:</b> Uses HikariCP for high-performance connection pooling with
 *       configurable pool size, timeouts, and connection lifecycle management
 *   <li><b>Transaction Management:</b> Supports both auto-commit and manual transaction modes with
 *       automatic commit/rollback on success/failure
 *   <li><b>Thread Safety:</b> All operations are thread-safe. Multiple threads can safely execute
 *       queries concurrently. Close operation is idempotent and thread-safe using atomic
 *       compare-and-set semantics
 *   <li><b>Interruption Support:</b> All operations check for thread interruption and respond
 *       promptly to cancellation requests, making them suitable for long-running operations
 *   <li><b>Resource Management:</b> Automatic resource cleanup using try-with-resources.
 *       Connections are obtained from the pool for each operation and automatically returned
 *       afterward
 *   <li><b>Error Handling:</b> Comprehensive exception handling with proper exception chaining,
 *       suppressed exceptions, and cleanup on failure
 * </ul>
 *
 * <h2>Lifecycle</h2>
 *
 * <p>Instances must be created using the static factory method {@link #newInstance(String,
 * Configuration)} which ensures proper initialization of the connection pool and validates
 * connectivity before returning. The constructor is private to prevent direct instantiation and
 * eliminate two-phase initialization hazards.
 *
 * <p>Example initialization:
 *
 * <pre>{@code
 * Configuration config = Configuration.builder()
 *     .set("jdbc.url", "jdbc:h2:mem:testdb")
 *     .set("username", "sa")
 *     .set("password", "")
 *     .build();
 *
 * try (JdbcDatabase database = DefaultJdbcDatabase.newInstance("mydb", config)) {
 *     // Use database
 * }
 * }</pre>
 *
 * <h2>Configuration</h2>
 *
 * <p>The following configuration options are supported:
 *
 * <ul>
 *   <li><b>jdbc.url</b> (required): JDBC connection URL
 *   <li><b>jdbc.driver.class</b> (optional): JDBC driver class name (auto-detected for JDBC 4.0+)
 *   <li><b>username</b> (optional): Database username
 *   <li><b>password</b> (optional): Database password
 *   <li><b>autoCommit</b> (optional, default: true): Enable auto-commit mode
 *   <li><b>logSql</b> (optional, default: true): Enable SQL statement logging
 *   <li><b>jdbc.properties.*</b> (optional): Additional JDBC properties
 *   <li><b>connectionTest.timeout</b> (optional, default: 30): Connection test timeout
 *   <li><b>connectionTest.timeoutUnit</b> (optional, default: SECONDS): Connection test timeout
 *       unit
 *   <li><b>connectionTest.query</b> (optional): Custom connection test query
 *   <li><b>pool.maximumPoolSize</b> (optional, default: 1): Maximum number of connections in pool
 *   <li><b>pool.minimumIdle</b> (optional, default: 1): Minimum number of idle connections
 *   <li><b>pool.connectionTimeout</b> (optional, default: 30000): Connection acquisition timeout
 *       (ms)
 *   <li><b>pool.idleTimeout</b> (optional, default: 600000): Idle connection timeout (ms, 10
 *       minutes)
 *   <li><b>pool.maxLifetime</b> (optional, default: 1800000): Maximum connection lifetime (ms, 30
 *       minutes)
 * </ul>
 *
 * <h2>Thread Safety</h2>
 *
 * <p>This class is fully thread-safe:
 *
 * <ul>
 *   <li>Multiple threads can execute queries concurrently - HikariCP handles connection pooling
 *   <li>The close operation uses {@link AtomicBoolean#compareAndSet(boolean, boolean)} for
 *       thread-safe, idempotent closure
 *   <li>The requireOpen() check provides fast fail-fast behavior when database is closed
 *   <li>Connection acquisition from HikariCP is thread-safe and handles concurrent close gracefully
 * </ul>
 *
 * <h2>Transaction Handling</h2>
 *
 * <p>When auto-commit is enabled (default), each SQL statement is executed in its own transaction
 * and automatically committed upon successful completion.
 *
 * <p>When auto-commit is disabled, transactions are managed automatically:
 *
 * <ul>
 *   <li>A transaction is committed upon successful completion of the operation
 *   <li>A transaction is rolled back if a {@link SQLException} occurs
 *   <li>A transaction is rolled back if an {@link InterruptedException} occurs
 *   <li>Rollback failures are captured as suppressed exceptions
 * </ul>
 *
 * <h2>Interruption Support</h2>
 *
 * <p>All operations check for thread interruption at appropriate points:
 *
 * <ul>
 *   <li>After acquiring a connection from the pool
 *   <li>Before executing SQL statements (executeUpdate, execute)
 *   <li>During query result processing (in the result set loop)
 * </ul>
 *
 * <p>When interrupted, operations throw {@link InterruptedException}, properly clean up resources
 * (rollback if needed), and restore the interrupt flag on the current thread.
 *
 * <h2>Performance Considerations</h2>
 *
 * <ul>
 *   <li>Connection pooling eliminates the overhead of creating new connections for each operation
 *   <li>HikariCP is one of the fastest connection pool implementations available
 *   <li>Thread-safe operations use lock-free atomic operations where possible
 *   <li>No unnecessary synchronization that would serialize concurrent operations
 *   <li>Connection leak detection helps identify resource leaks in development (60 second
 *       threshold)
 * </ul>
 *
 * <h2>Usage Examples</h2>
 *
 * <pre>{@code
 * // Execute an update
 * int rows = database.executeUpdate(
 *     "UPDATE locks SET lock_expiration = ? WHERE lock_id = ?",
 *     Parameter.timestamp(expiration),
 *     Parameter.string(lockId)
 * );
 *
 * // Execute a query
 * List<LockRecord> locks = database.query(
 *     "SELECT lock_id, lock_expiration FROM locks",
 *     row -> new LockRecord(
 *         row.getString("lock_id"),
 *         row.getTimestamp("lock_expiration")
 *     )
 * );
 *
 * // Execute with direct connection access
 * String dbProduct = database.withConnection(connection ->
 *     connection.getMetaData().getDatabaseProductName()
 * );
 * }</pre>
 *
 * <p>This class is package-private and not intended for direct use outside the jdbc package. Access
 * is provided through the {@link JdbcDatabase} interface and {@link JdbcDatabaseComponentFactory}.
 *
 * @see JdbcDatabase
 * @see JdbcDatabaseComponentFactory
 * @see ConnectionAction
 * @see Parameter
 * @see RowMapper
 */
final class DefaultJdbcDatabase implements JdbcDatabase {

    private static final Logger log = LoggerFactory.getLogger(DefaultJdbcDatabase.class);

    private static final class DefaultValues {
        private static final boolean AUTO_COMMIT = true;

        private static final int CONNECTION_TEST_TIMEOUT = 30;
        private static final TimeUnit CONNECTION_TEST_UNIT = TimeUnit.SECONDS;

        // HikariCP pool defaults
        private static final int MAXIMUM_POOL_SIZE = 1;
        private static final int MINIMUM_IDLE = 1;
        private static final long CONNECTION_TIMEOUT_MS = 30000;
        private static final long IDLE_TIMEOUT_MS = 600000; // 10 minutes
        private static final long MAX_LIFETIME_MS = 1800000; // 30 minutes
    }

    private final String name;

    private final String driverClass;
    private final String jdbcUrl;
    private final String username;
    private final String password;

    private final boolean logSql;

    private final Properties properties;

    private final boolean autoCommit;

    private final int connectionTestTimeout;
    private final TimeUnit connectionTestTimeoutUnit;

    private final String connectionTestQuery;

    // HikariCP pool configuration
    private final int maximumPoolSize;
    private final int minimumIdle;
    private final long connectionTimeoutMs;
    private final long idleTimeoutMs;
    private final long maxLifetimeMs;

    private final HikariDataSource dataSource;

    private final AtomicBoolean isClosed = new AtomicBoolean(false);

    public static DefaultJdbcDatabase newInstance(
            String name, Configuration connectorConfiguration) {
        DefaultJdbcDatabase database = new DefaultJdbcDatabase(name, connectorConfiguration);
        database.init();
        return database;
    }

    private DefaultJdbcDatabase(String name, Configuration connectorConfiguration) {
        this.name = name;

        this.jdbcUrl =
                connectorConfiguration
                        .getString("jdbc.url")
                        .orElseThrow(
                                () ->
                                        MisconfigurationException.forMessage(
                                                "Missing value for connector '%s': '%s' in context %s",
                                                name, "jdbc.url", connectorConfiguration));

        this.driverClass = connectorConfiguration.getString("jdbc.driver.class").orElse(null);

        this.username = connectorConfiguration.getString("username").orElse(null);
        this.password = connectorConfiguration.getString("password").orElse(null);

        this.logSql = connectorConfiguration.getBoolean("logSql").orElse(true);

        this.autoCommit =
                connectorConfiguration.getBoolean("autoCommit").orElse(DefaultValues.AUTO_COMMIT);

        this.properties =
                connectorConfiguration
                        .getPrefixNamespace("jdbc.properties")
                        .asProperties()
                        .orElse(null);

        this.connectionTestTimeout =
                connectorConfiguration
                        .getInt("connectionTest", "timeout")
                        .orElse(DefaultValues.CONNECTION_TEST_TIMEOUT);

        this.connectionTestTimeoutUnit =
                connectorConfiguration
                        .getEnumValue(TimeUnit.class, "connectionTest", "timeoutUnit")
                        .orElse(DefaultValues.CONNECTION_TEST_UNIT);

        this.connectionTestQuery =
                connectorConfiguration
                        .getString("connectionTest", "query")
                        .map(String::trim)
                        .map(StringUtils::emptyToNull)
                        .orElse(null);

        // Read HikariCP pool configuration with defaults
        this.maximumPoolSize =
                connectorConfiguration
                        .getInt("pool", "maximumPoolSize")
                        .orElse(DefaultValues.MAXIMUM_POOL_SIZE);

        this.minimumIdle =
                connectorConfiguration
                        .getInt("pool", "minimumIdle")
                        .orElse(DefaultValues.MINIMUM_IDLE);

        this.connectionTimeoutMs =
                connectorConfiguration
                        .getLong("pool", "connectionTimeout")
                        .orElse(DefaultValues.CONNECTION_TIMEOUT_MS);

        this.idleTimeoutMs =
                connectorConfiguration
                        .getLong("pool", "idleTimeout")
                        .orElse(DefaultValues.IDLE_TIMEOUT_MS);

        this.maxLifetimeMs =
                connectorConfiguration
                        .getLong("pool", "maxLifetime")
                        .orElse(DefaultValues.MAX_LIFETIME_MS);

        // Load driver class if specified
        if (driverClass != null) {
            try {
                log.info("Loading driver class {} ...", driverClass);
                Class.forName(driverClass);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("JDBC driver class not found: " + driverClass, e);
            }
        }

        // Initialize HikariCP DataSource
        dataSource = createDataSource();
    }

    private HikariDataSource createDataSource() {
        HikariConfig config = new HikariConfig();

        config.setJdbcUrl(jdbcUrl);

        if (username != null) {
            config.setUsername(username);
        }

        if (password != null) {
            config.setPassword(password);
        }

        if (driverClass != null) {
            config.setDriverClassName(driverClass);
        }

        // Set additional properties if provided
        if (properties != null) {
            config.setDataSourceProperties(properties);
        }

        // Configure pool size (configurable with defaults)
        config.setMaximumPoolSize(maximumPoolSize);
        config.setMinimumIdle(minimumIdle);

        // Connection timeout settings (configurable with defaults)
        config.setConnectionTimeout(connectionTimeoutMs);
        config.setIdleTimeout(idleTimeoutMs);
        config.setMaxLifetime(maxLifetimeMs);

        // Set connection test query if provided
        if (connectionTestQuery != null) {
            config.setConnectionTestQuery(connectionTestQuery);
        }

        // Set auto-commit
        config.setAutoCommit(autoCommit);

        // Pool name for identification
        config.setPoolName(String.format("[%s] database connection pool", name));

        // Enable connection leak detection in debug mode
        config.setLeakDetectionThreshold(60000); // 60 seconds

        return new HikariDataSource(config);
    }

    void init() {

        try {
            log.info("[{}] database: Connecting to: {}", name, jdbcUrl);
            // Test the connection
            try (Connection testConnection = dataSource.getConnection()) {
                boolean connectionIsValid =
                        testConnection.isValid(
                                (int) connectionTestTimeoutUnit.toSeconds(connectionTestTimeout));

                if (!connectionIsValid) {
                    throw new IllegalStateException("Connection pool failed validation");
                }
            }

            log.info("[{}] database: connection established", name);

            log.info(
                    "[{}] database: SQL statement logging has been {}",
                    name,
                    logSql ? "enabled" : "disabled");

        } catch (SQLException | IllegalStateException ex) {
            if (dataSource != null) {
                dataSource.close();
            }
            throw new RuntimeException(
                    String.format("Failed to connect to Jdbc Database '%s'", name), ex);
        }
    }

    @Override
    public int executeUpdate(String sql, Parameter<?>... parameters)
            throws SQLException, InterruptedException {

        logSql("executeUpdate", sql, parameters);

        return withConnection(
                connection -> {
                    try (PreparedStatement ps = connection.prepareStatement(sql)) {

                        if (parameters != null) {
                            setPreparedStatementParameters(ps, parameters);
                        }

                        if (Thread.currentThread().isInterrupted()) {
                            throw new InterruptedException();
                        }

                        return ps.executeUpdate();
                    }
                });
    }

    @Override
    public void execute(String sql, Parameter<?>... parameters)
            throws SQLException, InterruptedException {

        logSql("execute", sql, parameters);

        withConnection(
                connection -> {
                    try (PreparedStatement ps = connection.prepareStatement(sql)) {

                        if (parameters != null) {
                            setPreparedStatementParameters(ps, parameters);
                        }

                        if (Thread.currentThread().isInterrupted()) {
                            throw new InterruptedException();
                        }

                        boolean isResultSet = ps.execute();

                        if (isResultSet) {
                            log.debug("[{}] database: statement yielded a ResultSet", name);
                        } else {
                            int updateCount = ps.getUpdateCount();
                            log.debug("[{}] database: update count: {}", name, updateCount);
                        }

                        log.info("[{}] database: statement executed successfully", name);

                        return (Void) null; // cast for lambda compatibility
                    }
                });
    }

    @Override
    public <R> List<R> query(String sql, RowMapper<R> rowMapper, Parameter<?>... parameters)
            throws SQLException, InterruptedException {

        logSql("query", sql, parameters);

        return withConnection(
                connection -> {
                    List<R> resultList = new ArrayList<>();

                    try (PreparedStatement ps = connection.prepareStatement(sql)) {

                        setPreparedStatementParameters(ps, parameters);

                        try (ResultSet resultSet = ps.executeQuery()) {
                            // we create one ResultRow instance as the cursor
                            // of the underlying ResultSet is moved forward
                            ResultRow resultRow = new ResultRow(resultSet);

                            while (resultSet.next()) {
                                if (Thread.currentThread().isInterrupted()) {
                                    throw new InterruptedException();
                                }

                                R result = rowMapper.mapResultRow(resultRow);

                                resultList.add(result);
                            }
                        }
                    }

                    return Collections.unmodifiableList(resultList);
                });
    }

    private void logSql(String operationType, String sql, Parameter<?>[] parameters) {

        if (logSql && log.isInfoEnabled()) {

            log.info("[{}] database: {} SQL: {}", name, operationType, sql);

            if (parameters != null && parameters.length > 0) {

                String parameterString =
                        Stream.of(parameters)
                                .map(Parameter::toString)
                                .collect(Collectors.joining(", "));

                log.info("[{}] database: {} parameters: {}", name, operationType, parameterString);
            }
        }
    }

    private void setPreparedStatementParameters(PreparedStatement ps, Parameter<?>[] parameters)
            throws SQLException {

        if (parameters != null) {
            for (int i = 0; i < parameters.length; i++) {

                final int sqlIndex = i + 1;

                Parameter<?> parameter = parameters[i];

                parameter.setValue(ps, sqlIndex);
            }
        }
    }

    @Override
    public boolean isTableFound(String catalog, String schema, String tableName)
            throws SQLException, InterruptedException {

        return withConnection(new CheckIfTableExists(catalog, schema, tableName));
    }

    @Override
    public <R> R withConnection(ConnectionAction<R> action)
            throws InterruptedException, SQLException {

        requireOpen();

        try (Connection connection = dataSource.getConnection()) {

            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException();
            }

            return executeWithConnection(action, connection);
        }
    }

    private <R> R executeWithConnection(ConnectionAction<R> action, Connection connection)
            throws SQLException, InterruptedException {

        Objects.requireNonNull(action, "action is null");
        Objects.requireNonNull(connection, "connection is null");

        try {
            R result = action.execute(connection);

            if (!autoCommit) {
                connection.commit();
            }

            return result;

        } catch (SQLException sqlException) {

            if (!autoCommit) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackException) {
                    sqlException.addSuppressed(rollbackException);
                }
            }

            throw sqlException;

        } catch (InterruptedException interruptedException) {

            try {
                if (!autoCommit) {
                    connection.rollback();
                }
            } catch (SQLException sqlException) {
                interruptedException.addSuppressed(sqlException);
            }

            Thread.currentThread().interrupt();

            throw interruptedException;
        }
    }

    @Override
    public void close() throws IOException {

        boolean wasSet = isClosed.compareAndSet(false, true);
        if (!wasSet) {
            return; // was closed already, do nothing
        }

        try {
            if (!dataSource.isClosed()) {
                log.info("[{}] database: Closing connection pool", name);
                dataSource.close();
            }
        } catch (Exception exception) {
            throw new IOException("Exception while closing connection pool", exception);
        }
    }

    private void requireOpen() {
        if (isClosed.get()) {
            throw new IllegalStateException("Database is closed");
        }
    }

    @Override
    public String toString() {
        return "JDBC Database '"
                + name
                + "'{"
                + " jdbcUrl='"
                + jdbcUrl
                + '\''
                + ", username='"
                + username
                + '\''
                + ", password='"
                + StringUtils.maskPassword(password)
                + '\''
                + ", driverClass='"
                + driverClass
                + '\''
                + ", autoCommit="
                + autoCommit
                + '}';
    }
}
