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

package io.github.totalschema.connector.gcp.bigquery.actions.dataset.create;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.DatasetInfo;
import io.github.totalschema.config.Configuration;
import io.github.totalschema.connector.gcp.bigquery.actions.BigQueryAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Action for creating a BigQuery dataset.
 *
 * <p>Example YAML:
 *
 * <pre>{@code
 * CreateDataset:
 *   datasetId: my_dataset
 *   description: My dataset description
 *   location: US  # Optional - defaults to connector configuration
 * }</pre>
 *
 * <p><strong>Configuration Defaults:</strong> If {@code location} is not specified in the action
 * YAML, it will be read from the connector configuration using the key {@code defaults.location}.
 * If neither is specified, BigQuery's default location will be used.
 */
public class CreateDataset implements BigQueryAction {

    private static final Logger logger = LoggerFactory.getLogger(CreateDataset.class);

    private String datasetId;
    private String description;
    private String location;

    @Override
    public void execute(BigQuery bigQuery, Configuration connectorConfiguration) {
        logger.info("Creating dataset: {}", datasetId);

        // Use action-specific location if provided, otherwise fall back to connector default
        String effectiveLocation = location;
        if (effectiveLocation == null || effectiveLocation.isEmpty()) {
            effectiveLocation = connectorConfiguration.getString("defaults.location").orElse(null);
            if (effectiveLocation != null) {
                logger.debug("Using default location from connector config: {}", effectiveLocation);
            }
        }

        DatasetInfo.Builder datasetInfoBuilder = DatasetInfo.newBuilder(datasetId);

        if (description != null && !description.isEmpty()) {
            datasetInfoBuilder.setDescription(description);
        }

        if (effectiveLocation != null && !effectiveLocation.isEmpty()) {
            datasetInfoBuilder.setLocation(effectiveLocation);
        }

        DatasetInfo datasetInfo = datasetInfoBuilder.build();
        bigQuery.create(datasetInfo);

        logger.info("Dataset created successfully: {}", datasetId);
    }

    // Jackson requires getters and setters

    public String getDatasetId() {
        return datasetId;
    }

    public void setDatasetId(String datasetId) {
        this.datasetId = datasetId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    @Override
    public String toString() {
        return "CreateDataset{"
                + "datasetId='"
                + datasetId
                + '\''
                + ", description='"
                + description
                + '\''
                + ", location='"
                + location
                + '\''
                + '}';
    }
}
