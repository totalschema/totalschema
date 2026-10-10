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

package io.github.totalschema.engine.internal.state.database;

import static io.github.totalschema.engine.internal.state.database.StateTableColumns.*;

import io.github.totalschema.config.Configuration;
import io.github.totalschema.engine.internal.changefile.ChangeFileFactory;
import io.github.totalschema.engine.internal.common.repository.AbstractSqlTableRepository;
import io.github.totalschema.jdbc.JdbcDatabase;
import io.github.totalschema.model.ChangeFile;
import io.github.totalschema.model.StateRecord;
import io.github.totalschema.spi.database.sql.Parameter;
import io.github.totalschema.spi.database.sql.SQLDatabase;
import io.github.totalschema.spi.sql.SqlDialect;
import io.github.totalschema.spi.state.StateRepository;
import java.sql.*;
import java.time.ZonedDateTime;
import java.util.*;

/**
 * JDBC-based implementation of {@link StateRepository} that persists state records in a database
 * table.
 *
 * <p>This repository stores change execution history in a relational database, providing durable
 * and centralized state management suitable for distributed deployments.
 *
 * <h2>Thread Safety</h2>
 *
 * <p>This implementation is thread-safe and satisfies the thread-safety contract required by the
 * {@link StateRepository} interface:
 *
 * <ul>
 *   <li><b>Immutable State:</b> All instance fields are effectively immutable after construction,
 *       eliminating race conditions on field access.
 *   <li><b>Delegated Thread Safety:</b> All state operations ({@link
 *       #saveStateRecord(StateRecord)}, {@link #getAllStateRecords()}, {@link
 *       #deleteStateRecordByIds(Set)}) delegate to {@link JdbcDatabase}, which is documented as
 *       thread-safe with internal connection pooling.
 *   <li><b>Database Guarantees:</b> Thread safety for concurrent database operations is ensured by
 *       the underlying database's ACID properties and the HikariCP connection pool's thread-safe
 *       design. This provides coordination not only across threads within a single {@code
 *       ChangeEngine} instance, but also across multiple {@code ChangeEngine} instances in the same
 *       JVM (each with its own {@code ComponentContainer}) and even across distributed processes
 *       accessing the same database.
 * </ul>
 *
 * <h2>Initialization</h2>
 *
 * <p>The {@link #init()} method (inherited from {@link AbstractSqlTableRepository}) performs lazy
 * table creation with a check-then-act pattern. While this pattern has an inherent race condition,
 * it is mitigated by the fact that a singleton instance of {@code SqlDatabaseStateRecordRepository}
 * exists per {@code ComponentContainer}/{@code ChangeEngine}. The container ensures that
 * initialization occurs exactly once during component creation, before the repository is exposed to
 * concurrent access.
 *
 * <p><b>Note:</b> This implementation assumes that the {@link JdbcDatabase} instance provided to
 * the constructor is fully initialized and thread-safe.
 *
 * @see StateRepository
 * @see AbstractSqlTableRepository
 * @see JdbcDatabase
 */
public class SqlDatabaseStateRecordRepository extends AbstractSqlTableRepository
        implements StateRepository {

    private static final String STATE_DATABASE_NAME = "state";

    /** Static SQL configuration for state record repository. */
    private static class StateRecordSqlConfiguration
            implements AbstractSqlTableRepository.SqlConfiguration {

        private final int changeFileNameMaxLength;

        StateRecordSqlConfiguration(int changeFileNameMaxLength) {
            this.changeFileNameMaxLength = changeFileNameMaxLength;
        }

        @Override
        public String getDefaultTableName() {
            return SqlDatabaseStateRecordRepositoryDefaultValues.STATE_TABLE_NAME;
        }

        @Override
        public String getDefaultCreateSql(
                Configuration configuration, SqlDialect sqlDialect, String tableNameExpression) {
            return sqlDialect
                    .getSqlBuilder()
                    .createTable(tableNameExpression, configuration)
                    .column(CHANGE_FILE_ID, dialect -> dialect.varchar(changeFileNameMaxLength))
                    .column(
                            FILE_HASH,
                            d ->
                                    d.varchar(
                                            SqlDatabaseStateRecordRepositoryDefaultValues
                                                    .HASH_COLUMN_LENGTH))
                    .column(APPLY_TIMESTAMP, SqlDialect::timestamp)
                    .column(
                            APPLIED_BY,
                            dialect ->
                                    dialect.varchar(
                                            SqlDatabaseStateRecordRepositoryDefaultValues
                                                    .APPLIED_BY_COLUMN_LENGTH))
                    .primaryKey(CHANGE_FILE_ID)
                    .build();
        }
    }

    private final String querySql;

    private final String insertSql;

    private final StateRecordMapper stateRecordMapper;

    public SqlDatabaseStateRecordRepository(
            SqlDialect sqlDialect,
            SQLDatabase sqlDatabase,
            ChangeFileFactory changeFileFactory,
            int changeFileNameMaxLength,
            Configuration configuration) {

        super(
                sqlDialect,
                sqlDatabase,
                STATE_DATABASE_NAME,
                new StateRecordSqlConfiguration(changeFileNameMaxLength),
                configuration);

        this.stateRecordMapper = new StateRecordMapper(changeFileFactory);

        querySql = buildQuerySql(sqlDialect, configuration);
        insertSql = buildInsertSql(sqlDialect, configuration);
    }

    /**
     * Builds the SQL statement used to query all state records. Uses the {@code table.sql.query}
     * configuration override if present (as a template containing a {@code %s} placeholder for the
     * table name expression), otherwise generates the default SQL via the dialect's {@link
     * io.github.totalschema.spi.sql.SqlBuilder}.
     *
     * @param sqlDialect the SQL dialect used to build the default SQL
     * @param configuration the configuration used to look up an override
     * @return the SQL statement used to query all state records
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
                                        .column(CHANGE_FILE_ID)
                                        .column(FILE_HASH)
                                        .column(APPLY_TIMESTAMP)
                                        .column(APPLIED_BY)
                                        .build());
    }

    /**
     * Builds the SQL statement used to insert a new state record. Uses the {@code table.sql.insert}
     * configuration override if present (as a template containing a {@code %s} placeholder for the
     * table name expression), otherwise generates the default SQL via the dialect's {@link
     * io.github.totalschema.spi.sql.SqlBuilder}.
     *
     * @param sqlDialect the SQL dialect used to build the default SQL
     * @param configuration the configuration used to look up an override
     * @return the SQL statement used to insert a new state record
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
                                        .column(CHANGE_FILE_ID)
                                        .column(FILE_HASH)
                                        .column(APPLY_TIMESTAMP)
                                        .column(APPLIED_BY)
                                        .build());
    }

    @Override
    public void saveStateRecord(StateRecord stateRecord) {
        try {
            logger.debug("saveStateRecord({})", stateRecord);

            ZonedDateTime applyTimeStamp = stateRecord.getApplyTimeStamp();

            sqlDatabase.executeUpdate(
                    insertSql,
                    Parameter.string(stateRecord.getChangeFileId().toStringRepresentation()),
                    Parameter.string(stateRecord.getFileHash()),
                    Parameter.timestamp(applyTimeStamp),
                    Parameter.string(stateRecord.getAppliedBy()));

        } catch (SQLException e) {
            throw new RuntimeException("Failure saving StateRecord: " + stateRecord, e);
        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            throw new RuntimeException(e);
        }
    }

    @Override
    public List<StateRecord> getAllStateRecords() {

        try {
            return sqlDatabase.query(querySql, stateRecordMapper);

        } catch (SQLException e) {
            throw new RuntimeException("Failure querying for StateRecords", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    @Override
    public int deleteStateRecordByIds(Set<ChangeFile.Id> changeFileMetadata) {

        try {
            String deleteSql = getDeleteSql(changeFileMetadata.size());

            Parameter<?>[] parameters =
                    changeFileMetadata.stream()
                            .map(ChangeFile.Id::toStringRepresentation)
                            .map(Parameter::string)
                            .toArray(Parameter[]::new);

            return sqlDatabase.executeUpdate(deleteSql, parameters);

        } catch (SQLException e) {
            throw new RuntimeException("Failure deleting StateRecords", e);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            throw new RuntimeException(e);
        }
    }

    protected String getDeleteSql(int parameterCount) {
        // Generating SQL dynamically via the SqlBuilder is not great, but should be more portable
        // than a SQL IN expression, where drivers might have quirks around parameter binding
        return sqlDialect
                .getSqlBuilder()
                .delete(tableNameExpression)
                .whereEqualsAny(CHANGE_FILE_ID, parameterCount)
                .build();
    }
}
