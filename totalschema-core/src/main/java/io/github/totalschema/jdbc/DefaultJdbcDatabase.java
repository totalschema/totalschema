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
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private volatile boolean isClosed = false;

    public static JdbcDatabase newInstance(String name, Configuration configuration) {

        DefaultJdbcDatabase database = new DefaultJdbcDatabase(name, configuration);

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

    private void init() {

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

        return execute(
                new ConnectionAction<Integer>() {
                    @Override
                    public Integer execute(Connection connection) throws SQLException {

                        try (PreparedStatement ps = connection.prepareStatement(sql)) {

                            if (parameters != null) {
                                setPreparedStatementParameters(ps, parameters);
                            }

                            return ps.executeUpdate();
                        }
                    }
                });
    }

    @Override
    public <R> List<R> query(String sql, RowMapper<R> rowMapper, Parameter<?>... parameters)
            throws SQLException, InterruptedException {

        logSql("query", sql, parameters);

        return execute(
                new ConnectionAction<List<R>>() {
                    @Override
                    public List<R> execute(Connection connection)
                            throws InterruptedException, SQLException {

                        LinkedList<R> resultList = new LinkedList<>();

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
                    }
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

        return execute(new CheckIfTableExists(catalog, schema, tableName));
    }

    @Override
    public <R> R execute(ConnectionAction<R> action) throws InterruptedException, SQLException {

        requireOpen();

        try (Connection connection = dataSource.getConnection()) {

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

        requireOpen();

        isClosed = true;

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
        if (isClosed) {
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
