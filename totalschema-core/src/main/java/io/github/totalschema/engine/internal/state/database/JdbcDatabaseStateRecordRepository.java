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
import io.github.totalschema.engine.api.Context;
import io.github.totalschema.engine.internal.changefile.ChangeFileFactory;
import io.github.totalschema.jdbc.JdbcDatabase;
import io.github.totalschema.jdbc.JdbcDatabaseFactory;
import io.github.totalschema.jdbc.Parameter;
import io.github.totalschema.jdbc.TypeConversions;
import io.github.totalschema.model.ChangeFile;
import io.github.totalschema.model.StateRecord;
import io.github.totalschema.spi.sql.SqlDialect;
import io.github.totalschema.spi.state.StateRepository;
import java.io.IOException;
import java.sql.*;
import java.time.ZonedDateTime;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JdbcDatabaseStateRecordRepository implements StateRepository {

    private final Logger logger = LoggerFactory.getLogger(JdbcDatabaseStateRecordRepository.class);

    private static final String STATE_DATABASE_NAME = "state";

    private final int changeFileNameMaxLength;

    private final String beforeCreateInitSql;

    private final String afterCreateInitSql;

    private final String createSql;

    private final String querySql;

    private final String insertSql;

    private final String tableCatalog;

    private final String tableSchema;

    private final String tableName;

    private final String tableNameExpression;

    private final String tableNameQuote;

    private final JdbcDatabase jdbcDatabase;

    private final ChangeFileFactory changeFileFactory;

    private final SqlDialect sqlDialect;

    public static StateRepository newInstance(
            Context context, Configuration databaseConfiguration) {

        JdbcDatabaseStateRecordRepository repository =
                new JdbcDatabaseStateRecordRepository(context, databaseConfiguration);

        repository.init();

        return repository;
    }

    public JdbcDatabaseStateRecordRepository(Context context, Configuration configuration) {

        sqlDialect = context.get(SqlDialect.class);

        changeFileFactory = context.get(ChangeFileFactory.class);
        changeFileNameMaxLength = changeFileFactory.getChangeFileNameMaxLength();

        JdbcDatabaseFactory jdbcDatabaseFactory = JdbcDatabaseFactory.getInstance();

        boolean logSql = configuration.getBoolean("logSql").orElse(false);

        Configuration configWithLogSqlSet =
                configuration.withEntry("logSql", Boolean.toString(logSql));

        jdbcDatabase =
                jdbcDatabaseFactory.getJdbcDatabase(STATE_DATABASE_NAME, configWithLogSqlSet);

        tableCatalog = configuration.getString("table.catalog").orElse(null);
        tableSchema = configuration.getString("table.schema").orElse(null);

        tableName =
                configuration
                        .getString("table.name")
                        .orElse(JdbcDatabaseStateRecordRepositoryDefaultValues.STATE_TABLE_NAME);

        tableNameQuote = configuration.getString("table.name.quote").orElse(null);

        tableNameExpression =
                configuration
                        .getString("table.name.expression")
                        .orElseGet(
                                () ->
                                        getTableNameExpression(
                                                tableCatalog,
                                                tableSchema,
                                                tableName,
                                                tableNameQuote));

        beforeCreateInitSql = configuration.getString("table.beforeCreate.sql").orElse(null);

        afterCreateInitSql = configuration.getString("table.afterCreate.sql").orElse(null);

        createSql =
                configuration
                        .getString("table.create.sql")
                        .orElseGet(() -> getCreateSql(configuration));

        querySql =
                configuration
                        .getString("table.query.sql")
                        .orElseGet(() -> getQuerySql(configuration));

        insertSql =
                configuration
                        .getString("table.insert.sql")
                        .orElseGet(() -> getInsertSql(configuration));
    }

    private void init() {
        try {
            createStateTableIfNotFound();

        } catch (SQLException e) {
            throw new RuntimeException("Initialization failed", e);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("interrupted", e);
        }
    }

    protected String getTableNameExpression(
            String tableCatalog, String tableSchema, String tableName, String tableNameQuote) {

        String tableNameExpression;

        StringBuilder tableNameExpressionBuilder = new StringBuilder();
        if (tableCatalog != null && !tableCatalog.isBlank()) {
            tableNameExpressionBuilder.append(tableCatalog).append(".");
        }

        if (tableSchema != null && !tableSchema.isBlank()) {
            tableNameExpressionBuilder.append(tableSchema).append(".");
        }

        tableNameExpression = tableNameExpressionBuilder.append(tableName).toString();

        if (tableNameQuote != null) {
            tableNameExpression =
                    String.format("%s%s%s", tableNameQuote, tableNameExpression, tableNameQuote);
        }

        return tableNameExpression;
    }

    protected String getCreateSql(Configuration configuration) {
        var createSqlBuilder =
                new StringBuilder("CREATE TABLE ")
                        .append(tableNameExpression)
                        .append(" ")
                        .append("(");

        createSqlBuilder
                .append("change_file_id ")
                .append(
                        configuration
                                .getString("table.column.change_file_id.type")
                                .orElse(getChangeFieldIdColumnType()))
                .append(", ");

        createSqlBuilder
                .append("file_hash ")
                .append(
                        configuration
                                .getString("table.column.file_hash.type")
                                .orElse(getFileHashColumnType()))
                .append(", ");

        createSqlBuilder
                .append("apply_timestamp ")
                .append(
                        configuration
                                .getString("table.column.apply_timestamp.type")
                                .orElse(getApplyTimestampColumnType()))
                .append(", ");

        createSqlBuilder
                .append("applied_by ")
                .append(
                        configuration
                                .getString("table.column.applied_by.type")
                                .orElse(getAppliedByColumnType()));

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

    protected String getChangeFieldIdColumnType() {
        return sqlDialect.variableCharacterColumnExpression(changeFileNameMaxLength);
    }

    private String getFileHashColumnType() {
        return sqlDialect.variableCharacterColumnExpression(
                JdbcDatabaseStateRecordRepositoryDefaultValues.HASH_COLUMN_LENGTH);
    }

    protected String getApplyTimestampColumnType() {
        return sqlDialect.timestampColumnExpression();
    }

    protected String getAppliedByColumnType() {
        return sqlDialect.variableCharacterColumnExpression(
                JdbcDatabaseStateRecordRepositoryDefaultValues.APPLIED_BY_COLUMN_LENGTH);
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

    protected void createStateTableIfNotFound() throws SQLException, InterruptedException {

        if (!jdbcDatabase.isTableFound(tableCatalog, tableSchema, tableName)) {
            logger.info(
                    "[{}] database: State table is NOT found, creating it now: {}",
                    STATE_DATABASE_NAME,
                    tableName);

            if (beforeCreateInitSql != null) {
                jdbcDatabase.executeUpdate(beforeCreateInitSql);
            }

            jdbcDatabase.executeUpdate(createSql);

            if (afterCreateInitSql != null) {
                jdbcDatabase.executeUpdate(afterCreateInitSql);
            }

            logger.info(
                    "[{}] database: State table is created: {}",
                    STATE_DATABASE_NAME,
                    tableNameExpression);

            if (!jdbcDatabase.isTableFound(tableCatalog, tableSchema, tableName)) {

                logger.error(
                        "State table [{}] was not found in the [{}] database after the create attempt, "
                                + "while searching with tableCatalog={}, tableSchema={}, tableName={}",
                        tableNameExpression,
                        STATE_DATABASE_NAME,
                        tableCatalog,
                        tableSchema,
                        tableName);

                throw new IllegalStateException(
                        String.format(
                                "The state table %s had been created, "
                                        + "but the lookup failed to find it after that with the current configuration",
                                tableNameExpression));
            }
        } else {
            logger.info(
                    "[{}] database: State table found: {}",
                    STATE_DATABASE_NAME,
                    tableNameExpression);
        }
    }

    @Override
    public void close() throws IOException {
        jdbcDatabase.close();
    }
}
