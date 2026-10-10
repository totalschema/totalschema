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

package io.github.totalschema.connector.gcp.bigquery.actions.exchange.create;

import com.google.cloud.bigquery.dataexchange.v1beta1.AnalyticsHubServiceClient;
import com.google.cloud.bigquery.dataexchange.v1beta1.CreateDataExchangeRequest;
import com.google.cloud.bigquery.dataexchange.v1beta1.DataExchange;
import io.github.totalschema.config.Configuration;
import io.github.totalschema.connector.gcp.bigquery.BigQueryConnectorException;
import io.github.totalschema.connector.gcp.bigquery.actions.exchange.AbstractExchangeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Action for creating a BigQuery Analytics Hub data exchange.
 *
 * <p>Analytics Hub data exchanges are logical containers that group one or more listings together
 * for access-control and discovery purposes.
 *
 * <p>Example YAML:
 *
 * <pre>{@code
 * CreateExchange:
 *   dataExchangeId: my_exchange
 *   displayName: My Data Exchange
 *   description: Exchange for sharing analytics datasets
 *   primaryContact: contact@example.com
 *   documentation: https://example.com/docs
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
 */
public class CreateExchange extends AbstractExchangeAction {

    private static final Logger logger = LoggerFactory.getLogger(CreateExchange.class);

    private String displayName;
    private String description;
    private String primaryContact;
    private String documentation;

    @Override
    public void execute(AnalyticsHubServiceClient client, Configuration connectorConfiguration) {

        validate(connectorConfiguration);

        logger.info("Creating Analytics Hub data exchange: {}", dataExchangeId);

        try {
            String parent = buildLocationResourceName(connectorConfiguration);

            DataExchange.Builder dataExchangeBuilder =
                    DataExchange.newBuilder()
                            .setDisplayName(displayName != null ? displayName : dataExchangeId);

            if (description != null) {
                dataExchangeBuilder.setDescription(description);
            }

            if (primaryContact != null) {
                dataExchangeBuilder.setPrimaryContact(primaryContact);
            }

            if (documentation != null) {
                dataExchangeBuilder.setDocumentation(documentation);
            }

            DataExchange dataExchange = dataExchangeBuilder.build();

            CreateDataExchangeRequest request =
                    CreateDataExchangeRequest.newBuilder()
                            .setParent(parent)
                            .setDataExchangeId(dataExchangeId)
                            .setDataExchange(dataExchange)
                            .build();

            DataExchange createdDataExchange = client.createDataExchange(request);

            logger.info(
                    "Analytics Hub data exchange created successfully: {}",
                    createdDataExchange.getName());

        } catch (RuntimeException e) {
            throw new BigQueryConnectorException(
                    "Failed to create Analytics Hub data exchange: " + dataExchangeId, e);
        }
    }

    // Jackson requires getters and setters

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPrimaryContact() {
        return primaryContact;
    }

    public void setPrimaryContact(String primaryContact) {
        this.primaryContact = primaryContact;
    }

    public String getDocumentation() {
        return documentation;
    }

    public void setDocumentation(String documentation) {
        this.documentation = documentation;
    }

    @Override
    public String toString() {
        return "CreateExchange{"
                + "project='"
                + project
                + '\''
                + ", location='"
                + location
                + '\''
                + ", dataExchangeId='"
                + dataExchangeId
                + '\''
                + ", displayName='"
                + displayName
                + '\''
                + ", description='"
                + description
                + '\''
                + '}';
    }
}
