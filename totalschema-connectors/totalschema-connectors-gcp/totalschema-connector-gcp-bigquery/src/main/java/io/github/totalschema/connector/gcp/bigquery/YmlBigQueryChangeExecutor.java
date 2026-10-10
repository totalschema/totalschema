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

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.dataexchange.v1beta1.AnalyticsHubServiceClient;
import com.google.cloud.bigquery.datatransfer.v1.DataTransferServiceClient;
import io.github.totalschema.config.Configuration;
import io.github.totalschema.connector.gcp.bigquery.actions.Action;
import io.github.totalschema.connector.gcp.bigquery.actions.ActionFactory;
import io.github.totalschema.connector.gcp.bigquery.actions.AnalyticsHubServiceClientAction;
import io.github.totalschema.connector.gcp.bigquery.actions.BigQueryAction;
import io.github.totalschema.connector.gcp.bigquery.actions.DataTransferServiceClientAction;
import io.github.totalschema.engine.api.Context;
import io.github.totalschema.spi.change.executor.SimpleChangeExecutor;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Executes YAML-based change files for Google Cloud BigQuery operations.
 *
 * <p>This executor processes YAML/YML files containing declarative BigQuery operations (dataset
 * creation, table creation, schema modifications, etc.) and translates them into operations via the
 * BigQuery API.
 *
 * <p><strong>Design Note:</strong> This class is intentionally <em>not</em> exposed as an IoC
 * component via {@code ServiceLoader}, as it is specific to the BigQuery connector implementation.
 * While it extends {@link SimpleChangeExecutor} for architectural consistency—allowing YAML change
 * files to be processed through the same execution pipeline as other script types (e.g., SQL)—its
 * lifecycle is managed directly by {@link BigQueryConnector} rather than the container.
 *
 * @see BigQueryConnector
 * @see Action
 * @see ActionFactory
 */
final class YmlBigQueryChangeExecutor extends SimpleChangeExecutor {

    private static final Logger logger = LoggerFactory.getLogger(YmlBigQueryChangeExecutor.class);

    private final String name;
    private final Configuration configuration;

    static boolean isChangeTypeSupported(String changeExecutorId) {
        return changeExecutorId.equalsIgnoreCase("yml")
                || changeExecutorId.equalsIgnoreCase("yaml");
    }

    YmlBigQueryChangeExecutor(String name, Configuration configuration, Context context) {
        super(context);
        this.name = name;
        this.configuration = configuration;
    }

    @Override
    public void execute(InputStream inputStream, Context context) throws InterruptedException {

        requireNonNull(inputStream, "InputStream must not be null");
        requireNonNull(context, "Context must not be null");

        Action<?> action = ActionFactory.getAction(inputStream, context);
        requireNonNull(action, "Action returned for file content must not be null");

        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException();
        }

        logger.debug("[{}] Executing: {}", name, action);

        // Determine which client to pass based on action type
        if (action instanceof DataTransferServiceClientAction) {
            DataTransferServiceClient client = context.get(DataTransferServiceClient.class);
            ((DataTransferServiceClientAction) action).execute(client, configuration);

        } else if (action instanceof AnalyticsHubServiceClientAction) {
            AnalyticsHubServiceClient client = context.get(AnalyticsHubServiceClient.class);
            ((AnalyticsHubServiceClientAction) action).execute(client, configuration);

        } else if (action instanceof BigQueryAction) {
            BigQuery bigQuery = context.get(BigQuery.class);
            ((BigQueryAction) action).execute(bigQuery, configuration);

        } else {
            throw new IllegalStateException(
                    "Unknown action type: "
                            + action.getClass().getName()
                            + ". Actions must implement either "
                            + "DataTransferServiceClientAction, AnalyticsHubServiceClientAction, or BigQueryAction.");
        }
    }

    @Override
    public String toString() {
        return "YmlBigQueryChangeExecutor{"
                + "name='"
                + name
                + '\''
                + ", configuration="
                + configuration
                + '}';
    }
}
