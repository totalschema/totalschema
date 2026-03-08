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

package io.github.totalschema.engine.internal.common.repository;

import io.github.totalschema.config.Configuration;
import io.github.totalschema.jdbc.JdbcDatabase;
import io.github.totalschema.jdbc.JdbcUtils;
import io.github.totalschema.spi.sql.SqlDialect;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for JDBC-based table repositories. Provides common functionality for managing
 * database tables including table creation, existence checking, and lifecycle management.
 *
 * <p>Subclasses must implement template methods to customize table-specific behavior while
 * inheriting common table management logic.
 */
public abstract class AbstractJdbcTableRepository {

    /**
     * Interface for providing SQL configuration specific to each repository implementation.
     * Implementations should be stateless and typically implemented as static classes.
     */
    public interface SqlConfiguration {
        /**
         * Returns the default table name if not configured.
         *
         * @return the default table name
         */
        String getDefaultTableName();

        /**
         * Generates the default CREATE TABLE SQL statement.
         *
         * @param configuration the configuration object for accessing custom settings
         * @param sqlDialect the SQL dialect for database-specific operations
         * @param tableNameExpression the fully qualified table name expression
         * @return the CREATE TABLE SQL statement
         */
        String getDefaultCreateSql(
                Configuration configuration, SqlDialect sqlDialect, String tableNameExpression);
    }

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected final String databaseName;
    protected final String beforeCreateInitSql;
    protected final String afterCreateInitSql;
    protected final String tableCatalog;
    protected final String tableSchema;
    protected final String tableName;
    protected final String tableNameExpression;
    protected final String tableNameQuote;
    protected final String createSql;
    protected final JdbcDatabase jdbcDatabase;

    /**
     * Constructs an AbstractJdbcTableRepository with the specified dependencies.
     *
     * @param sqlDialect the SQL dialect for database-specific operations
     * @param jdbcDatabase the JDBC database connection instance
     * @param databaseName the database name used for logging and identification
     * @param sqlConfiguration provides SQL-specific configuration and creation logic
     * @param configuration configuration containing table and connection settings
     */
    protected AbstractJdbcTableRepository(
            SqlDialect sqlDialect,
            JdbcDatabase jdbcDatabase,
            String databaseName,
            SqlConfiguration sqlConfiguration,
            Configuration configuration) {

        this.databaseName = databaseName;
        this.jdbcDatabase = jdbcDatabase;

        // Read common table configuration
        tableCatalog = configuration.getString("table.catalog").orElse(null);
        tableSchema = configuration.getString("table.schema").orElse(null);
        tableName =
                configuration
                        .getString("table.name")
                        .orElse(sqlConfiguration.getDefaultTableName());
        tableNameQuote = configuration.getString("table.name.quote").orElse(null);

        tableNameExpression =
                configuration
                        .getString("table.name.expression")
                        .orElseGet(
                                () ->
                                        JdbcUtils.getTableNameExpression(
                                                tableCatalog,
                                                tableSchema,
                                                tableName,
                                                tableNameQuote));

        beforeCreateInitSql = configuration.getString("table.beforeCreate.sql").orElse(null);
        afterCreateInitSql = configuration.getString("table.afterCreate.sql").orElse(null);

        createSql =
                configuration
                        .getString("table.create.sql")
                        .orElseGet(
                                () ->
                                        sqlConfiguration.getDefaultCreateSql(
                                                configuration, sqlDialect, tableNameExpression));
    }

    /**
     * Initializes the repository by ensuring the required table exists. This method follows a
     * template pattern allowing subclasses to customize the initialization process.
     *
     * @throws RuntimeException if initialization fails
     */
    public final void init() {
        try {
            initInternal();
        } catch (SQLException e) {
            throw new RuntimeException("Initialization failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("interrupted", e);
        }
    }

    /**
     * Internal initialization method that performs the actual table creation logic.
     *
     * @throws SQLException if a database error occurs
     * @throws InterruptedException if the thread is interrupted during initialization
     */
    private void initInternal() throws SQLException, InterruptedException {
        if (isTableNotFound()) {
            logger.info(
                    "[{}] database: Table is NOT found, creating it now: {}",
                    databaseName,
                    tableName);

            executeBeforeCreateHooks();

            jdbcDatabase.executeUpdate(createSql);

            executeAfterCreateHooks();

            logger.info("[{}] database: Table is created: {}", databaseName, tableNameExpression);

            try {
                validateTableCreation();
                performPostCreationSteps();
            } catch (RuntimeException e) {
                handleCreationFailure(e);
                throw e;
            }
        } else {
            logger.info("[{}] database: Table found: {}", databaseName, tableNameExpression);
        }
    }

    /**
     * Checks if the table does not exist in the database.
     *
     * @return true if the table is not found, false otherwise
     * @throws SQLException if a database error occurs
     * @throws InterruptedException if the thread is interrupted
     */
    protected final boolean isTableNotFound() throws SQLException, InterruptedException {
        return !jdbcDatabase.isTableFound(tableCatalog, tableSchema, tableName);
    }

    /**
     * Executes SQL statements before table creation. Default implementation executes
     * beforeCreateInitSql if configured.
     *
     * @throws SQLException if a database error occurs
     * @throws InterruptedException if the thread is interrupted
     */
    protected void executeBeforeCreateHooks() throws SQLException, InterruptedException {
        if (beforeCreateInitSql != null) {
            jdbcDatabase.executeUpdate(beforeCreateInitSql);
        }
    }

    /**
     * Executes SQL statements after table creation. Default implementation executes
     * afterCreateInitSql if configured.
     *
     * @throws SQLException if a database error occurs
     * @throws InterruptedException if the thread is interrupted
     */
    protected void executeAfterCreateHooks() throws SQLException, InterruptedException {
        if (afterCreateInitSql != null) {
            jdbcDatabase.executeUpdate(afterCreateInitSql);
        }
    }

    /**
     * Validates that the table was successfully created and can be found with the current
     * configuration.
     *
     * @throws IllegalStateException if the table cannot be found after creation
     * @throws SQLException if a database error occurs
     * @throws InterruptedException if the thread is interrupted
     */
    protected void validateTableCreation() throws SQLException, InterruptedException {
        if (isTableNotFound()) {
            logger.error(
                    "Table [{}] was not found in the [{}] database after the create attempt, "
                            + "while searching with tableCatalog={}, tableSchema={}, tableName={}",
                    tableNameExpression,
                    databaseName,
                    tableCatalog,
                    tableSchema,
                    tableName);

            throw new IllegalStateException(
                    String.format(
                            "The table %s had been created, "
                                    + "but the lookup failed to find it after that with the current configuration",
                            tableNameExpression));
        }
    }

    /**
     * Performs additional steps after table creation and validation. Subclasses can override to
     * insert initial data or perform other setup tasks.
     *
     * @throws SQLException if a database error occurs
     * @throws InterruptedException if the thread is interrupted
     */
    protected void performPostCreationSteps() throws SQLException, InterruptedException {
        // Default implementation does nothing
    }

    /**
     * Handles failures that occur during table creation. Subclasses can override to implement
     * cleanup logic.
     *
     * @param e the exception that caused the failure
     */
    protected void handleCreationFailure(RuntimeException e) {
        // Default implementation does nothing
    }

    /**
     * Utility method to get a column type from configuration with a fallback default.
     *
     * @param configuration the configuration object
     * @param columnName the name of the column
     * @param defaultType the default type to use if not configured
     * @return the configured or default column type
     */
    protected final String getColumnType(
            Configuration configuration, String columnName, String defaultType) {
        return configuration.getString("table.columns." + columnName + ".type").orElse(defaultType);
    }

    /**
     * Formats a SQL statement template with the table name expression.
     *
     * @param sqlTemplate the SQL template containing a %s placeholder for the table name
     * @return the formatted SQL statement
     */
    protected final String formatSqlWithTableName(String sqlTemplate) {
        return String.format(sqlTemplate, tableNameExpression);
    }
}
