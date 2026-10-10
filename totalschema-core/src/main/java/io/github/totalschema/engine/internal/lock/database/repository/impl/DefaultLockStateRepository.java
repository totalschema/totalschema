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

package io.github.totalschema.engine.internal.lock.database.repository.impl;

import io.github.totalschema.config.Configuration;
import io.github.totalschema.engine.internal.common.repository.AbstractSqlTableRepository;
import io.github.totalschema.engine.internal.lock.database.repository.spi.LockStateRepository;
import io.github.totalschema.jdbc.*;
import io.github.totalschema.model.LockRecord;
import io.github.totalschema.spi.database.sql.Parameter;
import io.github.totalschema.spi.database.sql.SQLDatabase;
import io.github.totalschema.spi.sql.SqlDialect;
import java.sql.SQLException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;

final class DefaultLockStateRepository extends AbstractSqlTableRepository
        implements LockStateRepository {

    private static final String LOCK_DATABASE_NAME = "lock";

    /** Static SQL configuration for lock state repository. */
    private static class LockStateSqlConfiguration
            implements AbstractSqlTableRepository.SqlConfiguration {

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

            // Use the SqlBuilder for default SQL generation
            return sqlDialect
                    .getSqlBuilder()
                    .createTable(tableNameExpression, configuration)
                    .column(LockTableColumns.LOCK_ID, dialect -> dialect.varchar(255))
                    .column(LockTableColumns.LOCK_EXPIRATION, SqlDialect::timestamp)
                    .column(LockTableColumns.LOCKED_BY, dialect -> dialect.varchar(255))
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
            SqlDialect sqlDialect, SQLDatabase jdbcDatabase, Configuration configuration) {
        super(
                sqlDialect,
                jdbcDatabase,
                LOCK_DATABASE_NAME,
                new LockStateSqlConfiguration(),
                configuration);

        dropSql = buildDropSql(sqlDialect, configuration);
        insertSql = buildInsertSql(sqlDialect, configuration);
        acquireSql = buildAcquireSql(sqlDialect, configuration);
        renewSql = buildRenewSql(sqlDialect, configuration);
        querySql = buildQuerySql(sqlDialect, configuration);
        releaseSql = buildReleaseSql(sqlDialect, configuration);
    }

    /**
     * Builds the SQL statement used to drop the lock table. Uses the {@code table.sql.drop}
     * configuration override if present (as a template containing a {@code %s} placeholder for the
     * table name expression), otherwise generates the default SQL via the dialect's {@link
     * io.github.totalschema.spi.sql.SqlBuilder}.
     *
     * @param sqlDialect the SQL dialect used to build the default SQL
     * @param configuration the configuration used to look up an override
     * @return the SQL statement used to drop the lock table
     */
    private String buildDropSql(SqlDialect sqlDialect, Configuration configuration) {
        return configuration
                .getString("table.sql.drop")
                .map(this::formatSqlWithTableName)
                .orElseGet(() -> sqlDialect.getSqlBuilder().dropTable(tableNameExpression).build());
    }

    /**
     * Builds the SQL statement used to insert the single lock record. Uses the {@code
     * table.sql.insert} configuration override if present (as a template containing a {@code %s}
     * placeholder for the table name expression), otherwise generates the default SQL via the
     * dialect's {@link io.github.totalschema.spi.sql.SqlBuilder}.
     *
     * @param sqlDialect the SQL dialect used to build the default SQL
     * @param configuration the configuration used to look up an override
     * @return the SQL statement used to insert the single lock record
     */
    private String buildInsertSql(SqlDialect sqlDialect, Configuration configuration) {
        return configuration
                .getString("table.sql.insert")
                .map(this::formatSqlWithTableName)
                .orElseGet(
                        () ->
                                sqlDialect
                                        .getSqlBuilder()
                                        .insertInto(tableNameExpression)
                                        .columnWithValue(LockTableColumns.LOCK_ID, "NULL")
                                        .columnWithValue(LockTableColumns.LOCK_EXPIRATION, "NULL")
                                        .columnWithValue(LockTableColumns.LOCKED_BY, "NULL")
                                        .build());
    }

    /**
     * Builds the SQL statement used to acquire the lock. Uses the {@code table.sql.acquire}
     * configuration override if present (as a template containing a {@code %s} placeholder for the
     * table name expression), otherwise generates the default SQL via the dialect's {@link
     * io.github.totalschema.spi.sql.SqlBuilder}.
     *
     * @param sqlDialect the SQL dialect used to build the default SQL
     * @param configuration the configuration used to look up an override
     * @return the SQL statement used to acquire the lock
     */
    private String buildAcquireSql(SqlDialect sqlDialect, Configuration configuration) {
        return configuration
                .getString("table.sql.acquire")
                .map(this::formatSqlWithTableName)
                .orElseGet(
                        () ->
                                sqlDialect
                                        .getSqlBuilder()
                                        .update(tableNameExpression)
                                        .set(LockTableColumns.LOCK_ID, "?")
                                        .set(LockTableColumns.LOCK_EXPIRATION, "?")
                                        .set(LockTableColumns.LOCKED_BY, "?")
                                        .where(
                                                LockTableColumns.LOCK_ID
                                                        + " IS NULL OR "
                                                        + LockTableColumns.LOCK_EXPIRATION
                                                        + " < ?")
                                        .build());
    }

    /**
     * Builds the SQL statement used to renew the lock expiration. Uses the {@code
     * table.sql.renewSql} configuration override if present (as a template containing a {@code %s}
     * placeholder for the table name expression), otherwise generates the default SQL via the
     * dialect's {@link io.github.totalschema.spi.sql.SqlBuilder}.
     *
     * @param sqlDialect the SQL dialect used to build the default SQL
     * @param configuration the configuration used to look up an override
     * @return the SQL statement used to renew the lock expiration
     */
    private String buildRenewSql(SqlDialect sqlDialect, Configuration configuration) {
        return configuration
                .getString("table.sql.renewSql")
                .map(this::formatSqlWithTableName)
                .orElseGet(
                        () ->
                                sqlDialect
                                        .getSqlBuilder()
                                        .update(tableNameExpression)
                                        .set(LockTableColumns.LOCK_EXPIRATION, "?")
                                        .where(LockTableColumns.LOCK_ID + " = ?")
                                        .build());
    }

    /**
     * Builds the SQL statement used to query the current lock record. Uses the {@code
     * table.sql.query} configuration override if present (as a template containing a {@code %s}
     * placeholder for the table name expression), otherwise generates the default SQL via the
     * dialect's {@link io.github.totalschema.spi.sql.SqlBuilder}.
     *
     * @param sqlDialect the SQL dialect used to build the default SQL
     * @param configuration the configuration used to look up an override
     * @return the SQL statement used to query the current lock record
     */
    private String buildQuerySql(SqlDialect sqlDialect, Configuration configuration) {
        return configuration
                .getString("table.sql.query")
                .map(this::formatSqlWithTableName)
                .orElseGet(
                        () ->
                                sqlDialect
                                        .getSqlBuilder()
                                        .select(tableNameExpression)
                                        .column(LockTableColumns.LOCK_ID)
                                        .column(LockTableColumns.LOCK_EXPIRATION)
                                        .column(LockTableColumns.LOCKED_BY)
                                        .build());
    }

    /**
     * Builds the SQL statement used to release the lock. Uses the {@code table.sql.release}
     * configuration override if present (as a template containing a {@code %s} placeholder for the
     * table name expression), otherwise generates the default SQL via the dialect's {@link
     * io.github.totalschema.spi.sql.SqlBuilder}.
     *
     * @param sqlDialect the SQL dialect used to build the default SQL
     * @param configuration the configuration used to look up an override
     * @return the SQL statement used to release the lock
     */
    private String buildReleaseSql(SqlDialect sqlDialect, Configuration configuration) {
        return configuration
                .getString("table.sql.release")
                .map(this::formatSqlWithTableName)
                .orElseGet(
                        () ->
                                sqlDialect
                                        .getSqlBuilder()
                                        .update(tableNameExpression)
                                        .set(LockTableColumns.LOCK_ID, "NULL")
                                        .where(LockTableColumns.LOCK_ID + " = ?")
                                        .build());
    }

    @Override
    protected void performPostCreationSteps() throws SQLException, InterruptedException {
        sqlDatabase.execute(insertSql);
        logger.info(
                "[{}] database: The single record used for locking is inserted to {}",
                LOCK_DATABASE_NAME,
                tableNameExpression);

        requiredLockRecordExists();
    }

    @Override
    protected void handleCreationFailure(RuntimeException e) {
        try {
            sqlDatabase.execute(dropSql);
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
                sqlDatabase.executeUpdate(
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
                sqlDatabase.executeUpdate(
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
                    sqlDatabase.query(querySql, LockRecordRowMapper.INSTANCE);

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

        int changedRows = sqlDatabase.executeUpdate(releaseSql, Parameter.string(lockId));

        if (changedRows != 1) {
            throw new IllegalStateException("Unexpected number of rows changed: " + changedRows);
        }
    }
}
