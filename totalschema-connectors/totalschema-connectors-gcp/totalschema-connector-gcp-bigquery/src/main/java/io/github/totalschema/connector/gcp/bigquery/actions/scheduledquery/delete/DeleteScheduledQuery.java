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

package io.github.totalschema.connector.gcp.bigquery.actions.scheduledquery.delete;

import com.google.cloud.bigquery.datatransfer.v1.DataTransferServiceClient;
import com.google.cloud.bigquery.datatransfer.v1.DeleteTransferConfigRequest;
import io.github.totalschema.config.Configuration;
import io.github.totalschema.connector.gcp.bigquery.BigQueryConnectorException;
import io.github.totalschema.connector.gcp.bigquery.actions.scheduledquery.AbstractScheduledQueryAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Action for deleting a BigQuery scheduled query (transfer config).
 *
 * <p>Example YAML (by display name):
 *
 * <pre>{@code
 * DeleteScheduledQuery:
 *   displayName: Daily Sales Summary
 *   # Optional - defaults to connector configuration:
 *   # projectId: my-gcp-project
 *   # location: US
 * }</pre>
 *
 * <p>Or by transfer config ID:
 *
 * <pre>{@code
 * DeleteScheduledQuery:
 *   transferConfigId: my_scheduled_query_config_id
 *   # Optional - defaults to connector configuration:
 *   # projectId: my-gcp-project
 *   # location: US
 * }</pre>
 *
 * <p>Or by fully qualified resource name:
 *
 * <pre>{@code
 * DeleteScheduledQuery:
 *   transferConfigName: projects/my-project/locations/us/transferConfigs/12345678-1234-1234-1234-123456789012
 * }</pre>
 *
 * <p><strong>Configuration Defaults:</strong>
 *
 * <ul>
 *   <li>{@code projectId} - If not specified in YAML, uses {@code defaults.projectId} from
 *       connector configuration. Required when using {@code displayName} or {@code
 *       transferConfigId}.
 *   <li>{@code location} - If not specified in YAML, uses {@code defaults.location} from connector
 *       configuration. Defaults to "US" if neither is specified.
 * </ul>
 */
public class DeleteScheduledQuery extends AbstractScheduledQueryAction {

    private static final Logger logger = LoggerFactory.getLogger(DeleteScheduledQuery.class);

    private String transferConfigId;
    private String transferConfigName;

    @Override
    public void execute(
            DataTransferServiceClient dataTransferServiceClient,
            Configuration connectorConfiguration) {
        logger.info("Deleting scheduled query");

        String resourceName =
                buildTransferConfigResourceName(
                        dataTransferServiceClient,
                        transferConfigName,
                        transferConfigId,
                        connectorConfiguration);

        try {
            DeleteTransferConfigRequest request =
                    DeleteTransferConfigRequest.newBuilder().setName(resourceName).build();

            dataTransferServiceClient.deleteTransferConfig(request);

            logger.info("Scheduled query deleted successfully: {}", resourceName);

        } catch (Exception e) {
            throw new BigQueryConnectorException(
                    "Failed to delete scheduled query: " + resourceName, e);
        }
    }

    // Jackson requires getters and setters

    public String getTransferConfigId() {
        return transferConfigId;
    }

    public void setTransferConfigId(String transferConfigId) {
        this.transferConfigId = transferConfigId;
    }

    public String getTransferConfigName() {
        return transferConfigName;
    }

    public void setTransferConfigName(String transferConfigName) {
        this.transferConfigName = transferConfigName;
    }

    @Override
    public String toString() {
        return "DeleteScheduledQuery{"
                + "displayName='"
                + displayName
                + '\''
                + ", transferConfigId='"
                + transferConfigId
                + '\''
                + ", transferConfigName='"
                + transferConfigName
                + '\''
                + ", location='"
                + location
                + '\''
                + '}';
    }
}
