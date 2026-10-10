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
 * but WITHOUT ANY WARRANTY; without even t
 *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.github.totalschema.connector.gcp.bigquery.actions.statement.execute;

import com.google.cloud.bigquery.*;
import io.github.totalschema.config.Configuration;
import io.github.totalschema.connector.gcp.bigquery.BigQueryConnectorException;
import io.github.totalschema.connector.gcp.bigquery.actions.BigQueryAction;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Action for executing a BigQuery SQL query.
 *
 * <p>Example YAML:
 *
 * <pre>{@code
 * ExecuteStatement:
 *   statement: |
 *     CREATE TABLE my_dataset.my_table (
 *       id INT64,
 *       name STRING
 *     )
 *   defaultDataset: my_dataset
 *   useLegacySql: false
 * }</pre>
 */
public class ExecuteStatement implements BigQueryAction {

    private static final Logger logger = LoggerFactory.getLogger(ExecuteStatement.class);

    private String statement;
    private String defaultDataset;
    private Boolean useLegacySql;

    @Override
    public void execute(BigQuery bigQuery, Configuration connectorConfiguration) {
        logger.info("Executing statement: \n{}", statement);

        QueryJobConfiguration.Builder queryConfigBuilder =
                QueryJobConfiguration.newBuilder(statement);

        // Set use legacy SQL (default to false for standard SQL)
        queryConfigBuilder.setUseLegacySql(useLegacySql != null && useLegacySql);

        // Set default dataset if provided
        if (defaultDataset != null && !defaultDataset.isEmpty()) {
            queryConfigBuilder.setDefaultDataset(
                    DatasetId.of(bigQuery.getOptions().getProjectId(), defaultDataset));
        }

        QueryJobConfiguration queryConfig = queryConfigBuilder.build();

        try {
            // Create a job ID
            JobId jobId = JobId.of(UUID.randomUUID().toString());
            Job queryJob = bigQuery.create(JobInfo.newBuilder(queryConfig).setJobId(jobId).build());

            // Wait for the query to complete
            queryJob = queryJob.waitFor();

            if (queryJob == null) {
                throw new BigQueryConnectorException("Job no longer exists");
            }

            if (queryJob.getStatus().getError() != null) {
                throw new BigQueryConnectorException(
                        "Statement failed: " + queryJob.getStatus().getError().toString());
            }

            logger.info("Statement executed successfully");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BigQueryConnectorException("Statement execution interrupted", e);
        } catch (Exception e) {
            throw new BigQueryConnectorException("Failed to execute statement", e);
        }
    }

    // Jackson requires getters and setters

    public String getStatement() {
        return statement;
    }

    public void setStatement(String statement) {
        this.statement = statement;
    }

    public String getDefaultDataset() {
        return defaultDataset;
    }

    public void setDefaultDataset(String defaultDataset) {
        this.defaultDataset = defaultDataset;
    }

    public Boolean getUseLegacySql() {
        return useLegacySql;
    }

    public void setUseLegacySql(Boolean useLegacySql) {
        this.useLegacySql = useLegacySql;
    }

    @Override
    public String toString() {
        return "ExecuteStatement{" + "statement='" + statement + '\'' + '}';
    }
}
