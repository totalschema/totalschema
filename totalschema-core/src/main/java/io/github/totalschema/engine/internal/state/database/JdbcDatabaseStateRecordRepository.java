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

import io.github.totalschema.config.Configuration;
import io.github.totalschema.engine.internal.changefile.ChangeFileFactory;
import io.github.totalschema.engine.internal.common.repository.AbstractJdbcTableRepository;
import io.github.totalschema.engine.internal.sql.CreateTableBuilder;
import io.github.totalschema.jdbc.JdbcDatabase;
import io.github.totalschema.jdbc.Parameter;
import io.github.totalschema.jdbc.TypeConversions;
import io.github.totalschema.model.ChangeFile;
import io.github.totalschema.model.StateRecord;
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
 * <p>The {@link #init()} method (inherited from {@link AbstractJdbcTableRepository}) performs lazy
 * table creation with a check-then-act pattern. While this pattern has an inherent race condition,
 * it is mitigated by the fact that a singleton instance of {@code
 * JdbcDatabaseStateRecordRepository} exists per {@code ComponentContainer}/{@code ChangeEngine}.
 * The container ensures that initialization occurs exactly once during component creation, before
 * the repository is exposed to concurrent access.
 *
 * <p><b>Note:</b> This implementation assumes that the {@link JdbcDatabase} instance provided to
 * the constructor is fully initialized and thread-safe.
 *
 * @see StateRepository
 * @see AbstractJdbcTableRepository
 * @see JdbcDatabase
 */
public class JdbcDatabaseStateRecordRepository extends AbstractJdbcTableRepository
        implements StateRepository {

    private static final String STATE_DATABASE_NAME = "state";

    /** Static SQL configuration for state record repository. */
    private static class StateRecordSqlConfiguration
            implements AbstractJdbcTableRepository.SqlConfiguration {

        private final int changeFileNameMaxLength;

        StateRecordSqlConfiguration(int changeFileNameMaxLength) {
            this.changeFileNameMaxLength = changeFileNameMaxLength;
        }

        @Override
        public String getDefaultTableName() {
            return JdbcDatabaseStateRecordRepositoryDefaultValues.STATE_TABLE_NAME;
        }

        @Override
        public String getDefaultCreateSql(
                Configuration configuration, SqlDialect sqlDialect, String tableNameExpression) {
            return CreateTableBuilder.create(sqlDialect, tableNameExpression, configuration)
                    .column("change_file_id", dialect -> dialect.varchar(changeFileNameMaxLength))
                    .column(
                            "file_hash",
                            d ->
                                    d.varchar(
                                            JdbcDatabaseStateRecordRepositoryDefaultValues
                                                    .HASH_COLUMN_LENGTH))
                    .column("apply_timestamp", SqlDialect::timestamp)
                    .column(
                            "applied_by",
                            dialect ->
                                    dialect.varchar(
                                            JdbcDatabaseStateRecordRepositoryDefaultValues
                                                    .APPLIED_BY_COLUMN_LENGTH))
                    .primaryKey("change_file_id")
                    .build();
        }
    }

    private final String querySql;

    private final String insertSql;

    private final ChangeFileFactory changeFileFactory;

    public JdbcDatabaseStateRecordRepository(
            SqlDialect sqlDialect,
            JdbcDatabase jdbcDatabase,
            ChangeFileFactory changeFileFactory,
            int changeFileNameMaxLength,
            Configuration configuration) {

        super(
                sqlDialect,
                jdbcDatabase,
                STATE_DATABASE_NAME,
                new StateRecordSqlConfiguration(changeFileNameMaxLength),
                configuration);

        this.changeFileFactory = changeFileFactory;

        querySql =
                configuration
                        .getString("table.query.sql")
                        .orElseGet(() -> getQuerySql(configuration));

        insertSql =
                configuration
                        .getString("table.insert.sql")
                        .orElseGet(() -> getInsertSql(configuration));
    }

    private String getQuerySql(Configuration configuration) {
        return String.format(
                configuration
                        .getString("table.sql.query")
                        .orElse(JdbcDatabaseStateRecordRepositoryDefaultValues.QUERY_RECORDS_SQL),
                tableNameExpression);
    }

    protected String getInsertSql(Configuration configuration) {
        return String.format(
                configuration
                        .getString("table.sql.insert")
                        .orElse(JdbcDatabaseStateRecordRepositoryDefaultValues.INSERT_RECORDS_SQL),
                tableNameExpression);
    }

    @Override
    public void saveStateRecord(StateRecord stateRecord) {
        try {
            logger.debug("saveStateRecord({})", stateRecord);

            ZonedDateTime applyTimeStamp = stateRecord.getApplyTimeStamp();

            jdbcDatabase.executeUpdate(
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
            return jdbcDatabase.query(
                    querySql,
                    resultRow -> {
                        StateRecord stateRecord = new StateRecord();

                        stateRecord.setChangeFileId(
                                changeFileFactory.getIdFromString(
                                        resultRow.getString("change_file_id")));

                        stateRecord.setFileHash(resultRow.getString("file_hash"));

                        Timestamp applyTimestamp = resultRow.getTimestamp("apply_timestamp");
                        ZonedDateTime applyZonedDateTime =
                                TypeConversions.getInstance().toZonedDateTime(applyTimestamp);
                        stateRecord.setApplyTimeStamp(applyZonedDateTime);

                        stateRecord.setAppliedBy(resultRow.getString("applied_by"));

                        return stateRecord;
                    });

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
            // Generating SQL dynamically is not great, but should be more portable than a
            // SQL IN expression, where drivers might have quirks around parameter binding

            String deleteSql = getDeleteSql(changeFileMetadata.size());

            Parameter<?>[] parameters =
                    changeFileMetadata.stream()
                            .map(ChangeFile.Id::toStringRepresentation)
                            .map(Parameter::string)
                            .toArray(Parameter[]::new);

            return jdbcDatabase.executeUpdate(deleteSql, parameters);

        } catch (SQLException e) {
            throw new RuntimeException("Failure deleting StateRecords", e);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            throw new RuntimeException(e);
        }
    }

    protected String getDeleteSql(int parameterCount) {
        var deleteSqlBuilder =
                new StringBuilder("DELETE FROM ").append(tableNameExpression).append(" WHERE ");

        for (int i = 0; i < parameterCount; i++) {

            if (i != 0) {
                deleteSqlBuilder.append(" OR ");
            }

            deleteSqlBuilder.append("change_file_id").append(" = ").append("?");
        }

        return deleteSqlBuilder.toString();
    }
}
