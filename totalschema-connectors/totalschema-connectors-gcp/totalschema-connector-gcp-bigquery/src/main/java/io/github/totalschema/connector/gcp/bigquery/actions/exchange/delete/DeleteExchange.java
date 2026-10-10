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

package io.github.totalschema.connector.gcp.bigquery.actions.exchange.delete;

import com.google.cloud.bigquery.dataexchange.v1beta1.AnalyticsHubServiceClient;
import com.google.cloud.bigquery.dataexchange.v1beta1.DeleteDataExchangeRequest;
import io.github.totalschema.config.Configuration;
import io.github.totalschema.connector.gcp.bigquery.BigQueryConnectorException;
import io.github.totalschema.connector.gcp.bigquery.actions.exchange.AbstractExchangeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Action for deleting a BigQuery Analytics Hub data exchange.
 *
 * <p>Example YAML:
 *
 * <pre>{@code
 * DeleteExchange:
 *   dataExchangeId: my_exchange
 *   # Optional - defaults to connector configuration:
 *   # project: my-project
 *   # location: US
 * }</pre>
 *
 * <p><strong>Configuration Defaults:</strong>
 *
 * <ul>
 *   <li>{@code project} - If not specified in YAML, uses {@code defaults.projectId} from connector
 *       configuration. Required.
 *   <li>{@code location} - If not specified in YAML, uses {@code defaults.location} from connector
 *       configuration. Defaults to "US" if neither is specified.
 * </ul>
 *
 * <p><strong>Note:</strong> A data exchange must have all its listings deleted before it can be
 * deleted. If the exchange still contains listings, deletion will fail.
 */
public class DeleteExchange extends AbstractExchangeAction {

    private static final Logger logger = LoggerFactory.getLogger(DeleteExchange.class);

    @Override
    public void execute(AnalyticsHubServiceClient client, Configuration connectorConfiguration) {

        validate(connectorConfiguration);

        logger.info("Deleting Analytics Hub data exchange: {}", dataExchangeId);

        String resourceName = buildExchangeResourceName(connectorConfiguration);

        try {
            DeleteDataExchangeRequest request =
                    DeleteDataExchangeRequest.newBuilder().setName(resourceName).build();

            client.deleteDataExchange(request);

            logger.info("Analytics Hub data exchange deleted successfully: {}", resourceName);

        } catch (RuntimeException e) {
            throw new BigQueryConnectorException(
                    "Failed to delete Analytics Hub data exchange: " + resourceName, e);
        }
    }

    @Override
    public String toString() {
        return "DeleteExchange{"
                + "project='"
                + project
                + '\''
                + ", location='"
                + location
                + '\''
                + ", dataExchangeId='"
                + dataExchangeId
                + '\''
                + '}';
    }
}
