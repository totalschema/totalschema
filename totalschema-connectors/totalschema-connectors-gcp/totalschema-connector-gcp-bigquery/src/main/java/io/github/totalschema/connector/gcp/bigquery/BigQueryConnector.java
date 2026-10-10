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

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.dataexchange.v1beta1.AnalyticsHubServiceClient;
import com.google.cloud.bigquery.datatransfer.v1.DataTransferServiceClient;
import io.github.totalschema.config.Configuration;
import io.github.totalschema.connector.ChangeExecutorConnector;
import io.github.totalschema.engine.api.Context;
import io.github.totalschema.engine.core.command.api.ObjectsContext;
import io.github.totalschema.model.ChangeFile;
import io.github.totalschema.spi.change.executor.ChangeExecutor;
import io.github.totalschema.spi.database.sql.SQLDatabase;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * BigQuery connector for executing change scripts against Google Cloud BigQuery.
 *
 * <p>Supports multiple execution formats via file extension:
 *
 * <ul>
 *   <li>{@code .yaml} or {@code .yml} — declarative BigQuery operations (table creation, dataset
 *       management, etc.)
 *   <li>{@code .sql} — SQL queries executed via BigQuery API
 *   <li>{@code .groovy} — Groovy scripts with {@code bigQuery} binding
 *   <li>Other extensions via JSR-223 or custom executors
 * </ul>
 *
 * <h3>Configuration</h3>
 *
 * <p>Example configuration:
 *
 * <pre>{@code
 * connectors:
 *   mybigquery:
 *     type: bigquery
 *     projectId: my-gcp-project
 *     # Optional: Default dataset for queries
 *     defaultDataset: my_dataset
 *     # Optional: Default location for datasets
 *     location: US
 * }</pre>
 *
 * <p>The connector uses the BigQuery client library to execute queries and manage BigQuery
 * resources.
 */
final class BigQueryConnector extends ChangeExecutorConnector {

    /** Connector type identifier used in {@code totalschema.yml} ({@code type: bigquery}). */
    static final String CONNECTOR_TYPE = "bigquery";

    private static final Logger logger = LoggerFactory.getLogger(BigQueryConnector.class);

    BigQueryConnector(String name, Configuration configuration) {
        super(name, configuration);
    }

    @Override
    public void checkConnection(Context context, List<ChangeFile.Id> plannedChangeFileIds) {
        logger.info("[{}] Verifying BigQuery connectivity", name);
        try {
            BigQuery bigQuery = context.get(BigQuery.class, null, connectorConfiguration);

            // Test connectivity by listing datasets
            bigQuery.listDatasets().iterateAll();
            logger.debug("[{}] Connection test successful", name);

        } catch (RuntimeException ex) {
            logger.error("[{}] BigQuery connectivity check FAILED", name, ex);
            throw new BigQueryConnectorException("BigQuery connectivity check failed", ex);
        }

        logger.info("[{}] BigQuery connection verified", name);
    }

    @Override
    protected Optional<ChangeExecutor> getCustomChangeExecutor(
            String changeExecutorId, Context context) {

        if (YmlBigQueryChangeExecutor.isChangeTypeSupported(changeExecutorId)) {
            /*
            YmlBigQueryChangeExecutor is specific to BigQuery connector, so
            we do not expose it as a component. Technically, it does not have
            to be a ChangeExecutor component to function correctly, but
            following this pattern allows the rest of the code handle YML config
            in the exact same way as e.g. a Groovy ChangeExecutor.
             */
            return Optional.of(
                    new YmlBigQueryChangeExecutor(name, connectorConfiguration, context));

        } else {
            return Optional.empty();
        }
    }

    @Override
    protected Context getExecutionContext(Context parentContext) {

        BigQuery bigQuery = parentContext.get(BigQuery.class, null, connectorConfiguration);

        DataTransferServiceClient dataTransferServiceClient =
                parentContext.get(DataTransferServiceClient.class, null, connectorConfiguration);

        AnalyticsHubServiceClient analyticsHubServiceClient =
                parentContext.get(AnalyticsHubServiceClient.class, null, connectorConfiguration);

        SQLDatabase sqlDatabase = new BigQuerySQLDatabase(name, bigQuery, connectorConfiguration);

        return ObjectsContext.builder()
                .parentContext(parentContext)
                .setValue(BigQuery.class, bigQuery)
                .setValue(DataTransferServiceClient.class, dataTransferServiceClient)
                .setValue(AnalyticsHubServiceClient.class, analyticsHubServiceClient)
                .setValue(SQLDatabase.class, sqlDatabase)
                .build();
    }

    @Override
    protected Map<String, Object> getBindings(ChangeFile changeFile, Context context) {

        // Start with default bindings (configuration, environment)
        Map<String, Object> bindings = super.getBindings(changeFile, context);

        // Acquire the BigQuery client (cached by context so subsequent calls reuse it)
        BigQuery bigQuery = context.get(BigQuery.class, null, connectorConfiguration);

        // Add BigQuery-specific bindings
        bindings.put("bigQuery", bigQuery);

        return bindings;
    }

    @Override
    public String toString() {
        return "BigQuery Connector named '"
                + name
                + "'{"
                + " configuration='"
                + connectorConfiguration
                + '\''
                + '}';
    }
}
