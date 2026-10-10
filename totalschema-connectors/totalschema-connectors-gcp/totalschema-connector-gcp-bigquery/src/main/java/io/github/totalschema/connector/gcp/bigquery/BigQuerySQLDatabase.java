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

package io.github.totalschema.connector.gcp.bigquery;

import static java.util.Objects.requireNonNull;

import com.google.cloud.bigquery.*;
import io.github.totalschema.config.Configuration;
import io.github.totalschema.spi.database.sql.AbstractSQLDatabase;
import io.github.totalschema.spi.database.sql.Parameter;
import io.github.totalschema.spi.database.sql.ResultRow;
import io.github.totalschema.spi.database.sql.RowMapper;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** SQLDatabase implementation backed by BigQuery query jobs. */
final class BigQuerySQLDatabase extends AbstractSQLDatabase {

    private final BigQuery bigQuery;
    private final String defaultDataset;

    BigQuerySQLDatabase(String name, BigQuery bigQuery, Configuration configuration) {
        super(name, configuration);

        this.bigQuery = requireNonNull(bigQuery, "bigQuery must not be null");
        this.defaultDataset = configuration.getString("defaultDataset").orElse(null);
    }

    @Override
    public void execute(String sql, Parameter<?>... parameters)
            throws SQLException, InterruptedException {

        logStatement("execute", sql, parameters);

        executeStatement(sql, parameters);
    }

    @Override
    public int executeUpdate(String sql, Parameter<?>... parameters)
            throws SQLException, InterruptedException {

        logStatement("executeUpdate", sql, parameters);

        Job queryJob = executeStatement(sql, parameters);

        JobStatistics.QueryStatistics queryJobStatistics = extractQueryJobStatistics(queryJob);

        try {
            Long numDmlAffectedRows = queryJobStatistics.getNumDmlAffectedRows();

            long totalRows = Objects.requireNonNullElse(numDmlAffectedRows, 0L);

            return Math.toIntExact(totalRows);

        } catch (ArithmeticException ex) {
            // Row count exceeds Integer.MAX_VALUE
            return Integer.MAX_VALUE;
        }
    }

    private static JobStatistics.QueryStatistics extractQueryJobStatistics(Job queryJob)
            throws SQLException {

        JobStatistics jobStatistics = queryJob.getStatistics();

        if (jobStatistics instanceof JobStatistics.QueryStatistics queryStatistics) {
            return queryStatistics;

        } else {
            throw new SQLException(
                    String.format(
                            "Query did not returns a QueryStatistics, but %s for: %s",
                            jobStatistics, queryJob));
        }
    }

    @Override
    public <R> List<R> query(String sql, RowMapper<R> rowMapper, Parameter<?>... parameters)
            throws SQLException, InterruptedException {

        requireNonNull(sql, "sql must not be null");
        requireNonNull(rowMapper, "rowMapper must not be null");

        logStatement("query", sql, parameters);

        Job queryJob = executeStatement(sql, parameters);

        try {
            TableResult tableResult = queryJob.getQueryResults();
            List<R> results = new ArrayList<>();

            for (FieldValueList row : tableResult.iterateAll()) {
                ResultRow resultRow = new BigQueryResultRow(tableResult.getSchema(), row);
                R mappedRow = rowMapper.mapResultRow(resultRow);
                results.add(mappedRow);
            }

            return results;

        } catch (RuntimeException ex) {
            throw new SQLException("Failed to query and map results", ex);
        }
    }

    /**
     * Executes a SQL statement and returns the completed job.
     *
     * @param sql the SQL statement to execute
     * @param parameters the parameters to bind to the query
     * @return the completed BigQuery job
     * @throws SQLException if query execution fails
     * @throws InterruptedException if the thread is interrupted during execution
     */
    private Job executeStatement(String sql, Parameter<?>... parameters)
            throws SQLException, InterruptedException {

        requireNonNull(sql, "sql must not be null");

        QueryJobConfiguration.Builder queryConfigBuilder =
                QueryJobConfiguration.newBuilder(sql).setUseLegacySql(false);

        if (defaultDataset != null) {
            queryConfigBuilder.setDefaultDataset(
                    DatasetId.of(bigQuery.getOptions().getProjectId(), defaultDataset));
        }

        if (parameters != null && parameters.length > 0) {
            List<QueryParameterValue> mappedParameters =
                    Arrays.stream(parameters)
                            .map(BigQuerySQLDatabase::mapToQueryParameterValue)
                            .toList();

            queryConfigBuilder.setPositionalParameters(mappedParameters);
        }

        QueryJobConfiguration queryConfig = queryConfigBuilder.build();

        try {
            JobId jobId = JobId.of(UUID.randomUUID().toString());

            JobInfo jobInfo = JobInfo.newBuilder(queryConfig).setJobId(jobId).build();

            Job queryJob = bigQuery.create(jobInfo);
            queryJob = queryJob.waitFor();

            if (queryJob == null) {
                throw new SQLException("BigQuery job no longer exists");
            }

            JobStatus jobStatus = queryJob.getStatus();

            if (jobStatus == null) {
                // should never happen, as the query job must be completed by the time Job.waitFor
                // returns
                throw new IllegalStateException(
                        "BigQuery job status is null after query job completion");
            }

            if (jobStatus.getError() != null) {
                throw new SQLException("Statement failed: " + jobStatus.getError());
            }

            return queryJob;

        } catch (RuntimeException ex) {
            throw new SQLException("Failed to execute SQL statement", ex);
        }
    }

    private static QueryParameterValue mapToQueryParameterValue(Parameter<?> inputParameter) {

        return switch (inputParameter) {
            case Parameter.StringParameter parameter ->
                    QueryParameterValue.string(parameter.getValue());

            case Parameter.TimestampParameter parameter -> {
                Timestamp value = parameter.getValue();
                long time = value.getTime();

                // java.sql.Timestamp.getTime() returns milliseconds,
                // but BigQuery API expects microseconds
                long microsecondValue = time * 1000;

                yield QueryParameterValue.timestamp(microsecondValue);
            }
        };
    }

    @Override
    public boolean isTableFound(String catalog, String schema, String tableName)
            throws SQLException {

        requireNonNull(catalog, "catalog (project ID) must not be null");
        requireNonNull(schema, "schema (dataset) must not be null");
        requireNonNull(tableName, "tableName must not be null");

        try {
            TableId tableId = TableId.of(catalog, schema, tableName);
            Table table = bigQuery.getTable(tableId);
            return table != null;
        } catch (RuntimeException ex) {
            throw new SQLException(
                    "Failed to check table existence for: "
                            + catalog
                            + "."
                            + schema
                            + "."
                            + tableName,
                    ex);
        }
    }
}
