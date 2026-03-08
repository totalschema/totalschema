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

package io.github.totalschema.engine.internal.state.database;

import io.github.totalschema.config.Configuration;
import io.github.totalschema.engine.internal.changefile.ChangeFileFactory;
import io.github.totalschema.engine.internal.common.repository.AbstractJdbcTableRepository;
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
            var createSqlBuilder =
                    new StringBuilder("CREATE TABLE ")
                            .append(tableNameExpression)
                            .append(" ")
                            .append("(");

            createSqlBuilder
                    .append("change_file_id ")
                    .append(
                            getColumnType(
                                    configuration,
                                    "change_file_id",
                                    getChangeFieldIdColumnType(sqlDialect)))
                    .append(", ");

            createSqlBuilder
                    .append("file_hash ")
                    .append(
                            getColumnType(
                                    configuration, "file_hash", getFileHashColumnType(sqlDialect)))
                    .append(", ");

            createSqlBuilder
                    .append("apply_timestamp ")
                    .append(
                            getColumnType(
                                    configuration,
                                    "apply_timestamp",
                                    getApplyTimestampColumnType(sqlDialect)))
                    .append(", ");

            createSqlBuilder
                    .append("applied_by ")
                    .append(
                            getColumnType(
                                    configuration,
                                    "applied_by",
                                    getAppliedByColumnType(sqlDialect)));

            if (!configuration.getBoolean("table.primaryKeyClause.omit").orElse(false)) {
                createSqlBuilder
                        .append(", ")
                        .append(
                                configuration
                                        .getString("table.primaryKeyClause.definition")
                                        .orElse("PRIMARY KEY(change_file_id)"));
            }

            createSqlBuilder.append(")");

            return createSqlBuilder.toString();
        }

        private String getColumnType(
                Configuration configuration, String columnName, String defaultType) {
            return configuration
                    .getString("table.columns." + columnName + ".type")
                    .orElse(defaultType);
        }

        private String getChangeFieldIdColumnType(SqlDialect sqlDialect) {
            return sqlDialect.variableCharacterColumnExpression(changeFileNameMaxLength);
        }

        private String getFileHashColumnType(SqlDialect sqlDialect) {
            return sqlDialect.variableCharacterColumnExpression(
                    JdbcDatabaseStateRecordRepositoryDefaultValues.HASH_COLUMN_LENGTH);
        }

        private String getApplyTimestampColumnType(SqlDialect sqlDialect) {
            return sqlDialect.timestampColumnExpression();
        }

        private String getAppliedByColumnType(SqlDialect sqlDialect) {
            return sqlDialect.variableCharacterColumnExpression(
                    JdbcDatabaseStateRecordRepositoryDefaultValues.APPLIED_BY_COLUMN_LENGTH);
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
