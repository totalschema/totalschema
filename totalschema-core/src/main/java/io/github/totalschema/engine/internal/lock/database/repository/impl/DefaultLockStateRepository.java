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

package io.github.totalschema.engine.internal.lock.database.repository.impl;

import io.github.totalschema.config.Configuration;
import io.github.totalschema.engine.internal.lock.database.repository.spi.LockStateRepository;
import io.github.totalschema.jdbc.*;
import io.github.totalschema.model.LockRecord;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class DefaultLockStateRepository implements LockStateRepository {

    private static final String LOCK_DATABASE_NAME = "lock";

    private final Logger logger = LoggerFactory.getLogger(DefaultLockStateRepository.class);

    private final String beforeCreateInitSql;
    private final String createSql;
    private final String dropSql;
    private final String insertSql;
    private final String acquireSql;
    private final String renewSql;
    private final String querySql;
    private final String releaseSql;
    private final String tableCatalog;
    private final String tableSchema;
    private final String tableName;
    private final String tableNameExpression;
    private final String tableNameQuote;

    private final JdbcDatabase jdbcDatabase;

    public static DefaultLockStateRepository newInstance(Configuration configuration)
            throws SQLException, InterruptedException {

        DefaultLockStateRepository repository = new DefaultLockStateRepository(configuration);

        repository.init();

        return repository;
    }

    private DefaultLockStateRepository(Configuration configuration) {
        beforeCreateInitSql = configuration.getString("table.beforeCreate.sql").orElse(null);
        tableCatalog = configuration.getString("table.catalog").orElse(null);
        tableSchema = configuration.getString("table.schema").orElse(null);
        tableName = configuration.getString("table.name").orElse(DefaultValues.LOCK_TABLE_NAME);
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
        createSql =
                String.format(
                        configuration
                                .getString("table.sql.create")
                                .orElse(DefaultValues.CREATE_TABLE_SQL),
                        tableNameExpression);
        dropSql =
                String.format(
                        configuration
                                .getString("table.sql.drop")
                                .orElse(DefaultValues.DROP_TABLE_SQL),
                        tableNameExpression);
        insertSql =
                String.format(
                        configuration
                                .getString("table.sql.insert")
                                .orElse(DefaultValues.INSERT_RECORD_SQL),
                        tableNameExpression);
        acquireSql =
                String.format(
                        configuration
                                .getString("table.sql.acquire")
                                .orElse(DefaultValues.ACQUIRE_LOCK_SQL),
                        tableNameExpression);
        renewSql =
                String.format(
                        configuration
                                .getString("table.sql.renewSql")
                                .orElse(DefaultValues.RENEW_LOCK_SQL),
                        tableNameExpression);
        querySql =
                String.format(
                        configuration
                                .getString("table.sql.query")
                                .orElse(DefaultValues.QUERY_RECORD_SQL),
                        tableNameExpression);
        releaseSql =
                String.format(
                        configuration
                                .getString("table.sql.release")
                                .orElse(DefaultValues.RELEASE_LOCK_SQL),
                        tableNameExpression);

        boolean logSql = configuration.getBoolean("logSql").orElse(false);

        Configuration configWithLogSqlSet =
                configuration.withEntry("logSql", Boolean.toString(logSql));

        JdbcDatabaseFactory jdbcDatabaseFactory = JdbcDatabaseFactory.getInstance();
        jdbcDatabase = jdbcDatabaseFactory.getJdbcDatabase(LOCK_DATABASE_NAME, configWithLogSqlSet);
    }

    private void init() throws SQLException, InterruptedException {

        if (isLockTableNotFound()) {
            logger.info(
                    "[{}] database: Lock table is NOT found, creating it now: {}",
                    LOCK_DATABASE_NAME,
                    tableName);

            if (beforeCreateInitSql != null) {
                jdbcDatabase.executeUpdate(beforeCreateInitSql);
            }

            jdbcDatabase.executeUpdate(createSql);

            logger.info(
                    "[{}] database: Lock table is created: {}",
                    LOCK_DATABASE_NAME,
                    tableNameExpression);

            jdbcDatabase.executeUpdate(insertSql);
            logger.info(
                    "[{}] database: The single record used for locking is inserted to {}",
                    LOCK_DATABASE_NAME,
                    tableNameExpression);

            try {
                requiredTableCanBeFoundWithConfig();
                requiredLockRecordExists();
            } catch (RuntimeException runtimeException) {
                try {
                    jdbcDatabase.executeUpdate(dropSql);
                } catch (RuntimeException dropException) {
                    logger.error(
                            "Failure cleaning up lock table created with incorrect configuration");
                    runtimeException.addSuppressed(dropException);
                }

                throw runtimeException;
            }

        } else {
            logger.info(
                    "[{}] database: Lock table found: {}", LOCK_DATABASE_NAME, tableNameExpression);
        }
    }

    private void requiredTableCanBeFoundWithConfig() throws SQLException, InterruptedException {
        if (isLockTableNotFound()) {

            logger.error(
                    "Lock table [{}] was not found after the create attempt, "
                            + "while searching with tableCatalog={}, tableSchema={}, tableName={}",
                    tableNameExpression,
                    tableCatalog,
                    tableSchema,
                    tableName);

            throw new IllegalStateException(
                    String.format(
                            "The lock table %s had been created, "
                                    + "but the lookup failed to find it after that with the current configuration",
                            tableNameExpression));
        }
    }

    private void requiredLockRecordExists() {
        if (getLockRecord() == null) {
            throw new IllegalStateException("No lock could be found");
        }
    }

    private boolean isLockTableNotFound() throws SQLException, InterruptedException {
        return !jdbcDatabase.isTableFound(tableCatalog, tableSchema, tableName);
    }

    @Override
    public boolean updateIdAndExpirationIfOwnerIsNullOrExpirationIsReached(
            String lockId, ZonedDateTime lockExpiration) throws SQLException, InterruptedException {

        Objects.requireNonNull(lockId);
        Objects.requireNonNull(lockExpiration);

        ZonedDateTime now = ZonedDateTime.now().withZoneSameInstant(ZoneOffset.UTC);

        String userName = System.getProperty("user.name");

        int changedRows =
                jdbcDatabase.executeUpdate(
                        acquireSql,
                        Parameter.string(lockId),
                        Parameter.timestamp(lockExpiration),
                        Parameter.string(userName),
                        Parameter.timestamp(now));

        switch (changedRows) {
            case 0:
                return false;
            case 1:
                return true;
            default:
                throw new IllegalStateException(
                        "Unexpected number of rows changed: " + changedRows);
        }
    }

    @Override
    public boolean updateLockExpiration(String lockId, ZonedDateTime lockExpiration)
            throws SQLException, InterruptedException {

        Objects.requireNonNull(lockId);
        Objects.requireNonNull(lockExpiration);

        int changedRows =
                jdbcDatabase.executeUpdate(
                        renewSql, Parameter.timestamp(lockExpiration), Parameter.string(lockId));

        switch (changedRows) {
            case 0:
                return false;
            case 1:
                return true;
            default:
                throw new IllegalStateException(
                        "Unexpected number of rows changed: " + changedRows);
        }
    }

    @Override
    public LockRecord getLockRecord() {
        try {
            List<LockRecord> lockRecords =
                    jdbcDatabase.query(
                            querySql,
                            resultRow -> {
                                LockRecord lockRecord = new LockRecord();

                                lockRecord.setLockId(resultRow.getString("lock_id"));

                                Timestamp expirationTimestamp =
                                        resultRow.getTimestamp("lock_expiration");
                                ZonedDateTime expirationZonedDateTime =
                                        TypeConversions.getInstance()
                                                .toZonedDateTime(expirationTimestamp);
                                lockRecord.setLockExpiration(expirationZonedDateTime);

                                lockRecord.setLockedByUserId(resultRow.getString("locked_by"));

                                return lockRecord;
                            });

            if (lockRecords.size() == 1) {
                logger.info("Lock record found: {}", lockRecords.get(0));
            } else {
                if (lockRecords.isEmpty()) {
                    throw new IllegalStateException(
                            "No lock record was found in: " + tableNameExpression);
                } else {
                    throw new IllegalStateException(
                            "Multiple lock records were: "
                                    + tableNameExpression
                                    + ": "
                                    + lockRecords);
                }
            }

            return lockRecords.get(0);

        } catch (SQLException e) {
            throw new RuntimeException("Failure querying for the LockRecord", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    @Override
    public void updateIdToNull(String lockId) throws SQLException, InterruptedException {

        int changedRows = jdbcDatabase.executeUpdate(releaseSql, Parameter.string(lockId));

        if (changedRows != 1) {
            throw new IllegalStateException("Unexpected number of rows changed: " + changedRows);
        }
    }
}
