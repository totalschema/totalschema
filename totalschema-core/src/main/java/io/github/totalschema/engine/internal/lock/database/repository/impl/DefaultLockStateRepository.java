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
import io.github.totalschema.engine.internal.common.repository.AbstractJdbcTableRepository;
import io.github.totalschema.engine.internal.lock.database.repository.spi.LockStateRepository;
import io.github.totalschema.engine.internal.sql.CreateTableBuilder;
import io.github.totalschema.jdbc.*;
import io.github.totalschema.model.LockRecord;
import io.github.totalschema.spi.sql.SqlDialect;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;

final class DefaultLockStateRepository extends AbstractJdbcTableRepository
        implements LockStateRepository {

    private static final String LOCK_DATABASE_NAME = "lock";

    /** Static SQL configuration for lock state repository. */
    private static class LockStateSqlConfiguration
            implements AbstractJdbcTableRepository.SqlConfiguration {

        @Override
        public String getDefaultTableName() {
            return DefaultValues.LOCK_TABLE_NAME;
        }

        @Override
        public String getDefaultCreateSql(
                Configuration configuration, SqlDialect sqlDialect, String tableNameExpression) {
            // Check if user provided custom SQL
            if (configuration.getString("table.sql.create").isPresent()) {
                return String.format(
                        configuration.getString("table.sql.create").get(), tableNameExpression);
            }

            // Use builder for default SQL generation
            return CreateTableBuilder.create(sqlDialect, tableNameExpression, configuration)
                    .column("lock_id", dialect -> dialect.varchar(255))
                    .column("lock_expiration", SqlDialect::timestamp)
                    .column("locked_by", dialect -> dialect.varchar(255))
                    .build();
        }
    }

    private final String dropSql;
    private final String insertSql;
    private final String acquireSql;
    private final String renewSql;
    private final String querySql;
    private final String releaseSql;

    DefaultLockStateRepository(
            SqlDialect sqlDialect, JdbcDatabase jdbcDatabase, Configuration configuration) {
        super(
                sqlDialect,
                jdbcDatabase,
                LOCK_DATABASE_NAME,
                new LockStateSqlConfiguration(),
                configuration);

        dropSql =
                formatSqlWithTableName(
                        configuration
                                .getString("table.sql.drop")
                                .orElse(DefaultValues.DROP_TABLE_SQL));

        insertSql =
                formatSqlWithTableName(
                        configuration
                                .getString("table.sql.insert")
                                .orElse(DefaultValues.INSERT_RECORD_SQL));

        acquireSql =
                formatSqlWithTableName(
                        configuration
                                .getString("table.sql.acquire")
                                .orElse(DefaultValues.ACQUIRE_LOCK_SQL));

        renewSql =
                formatSqlWithTableName(
                        configuration
                                .getString("table.sql.renewSql")
                                .orElse(DefaultValues.RENEW_LOCK_SQL));

        querySql =
                formatSqlWithTableName(
                        configuration
                                .getString("table.sql.query")
                                .orElse(DefaultValues.QUERY_RECORD_SQL));

        releaseSql =
                formatSqlWithTableName(
                        configuration
                                .getString("table.sql.release")
                                .orElse(DefaultValues.RELEASE_LOCK_SQL));
    }

    @Override
    protected void performPostCreationSteps() throws SQLException, InterruptedException {
        jdbcDatabase.execute(insertSql);
        logger.info(
                "[{}] database: The single record used for locking is inserted to {}",
                LOCK_DATABASE_NAME,
                tableNameExpression);

        requiredLockRecordExists();
    }

    @Override
    protected void handleCreationFailure(RuntimeException e) {
        try {
            jdbcDatabase.execute(dropSql);
        } catch (RuntimeException | SQLException | InterruptedException dropException) {
            logger.error("Failure cleaning up lock table created with incorrect configuration");
            e.addSuppressed(dropException);
        }
    }

    private void requiredLockRecordExists() {
        if (getLockRecord() == null) {
            throw new IllegalStateException("No lock could be found");
        }
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
