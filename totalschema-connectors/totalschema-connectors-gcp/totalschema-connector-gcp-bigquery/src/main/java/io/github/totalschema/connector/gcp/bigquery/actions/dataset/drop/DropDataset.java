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

package io.github.totalschema.connector.gcp.bigquery.actions.dataset.drop;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQuery.DatasetDeleteOption;
import io.github.totalschema.config.Configuration;
import io.github.totalschema.connector.gcp.bigquery.actions.BigQueryAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Action for dropping (deleting) a BigQuery dataset.
 *
 * <p>Example YAML:
 *
 * <pre>{@code
 * DropDataset:
 *   datasetId: my_dataset
 *   deleteContents: true
 * }</pre>
 *
 * <p>By default, BigQuery will not delete a dataset that contains tables. Set {@code
 * deleteContents: true} to force deletion of all tables within the dataset before dropping it.
 */
public class DropDataset implements BigQueryAction {

    private static final Logger logger = LoggerFactory.getLogger(DropDataset.class);

    private String datasetId;
    private Boolean deleteContents;

    @Override
    public void execute(BigQuery bigQuery, Configuration connectorConfiguration) {
        logger.info("Dropping dataset: {}", datasetId);

        boolean shouldDeleteContents = deleteContents != null && deleteContents;

        if (shouldDeleteContents) {
            logger.debug(
                    "deleteContents is enabled, will delete all tables in dataset: {}", datasetId);
            bigQuery.delete(datasetId, DatasetDeleteOption.deleteContents());
        } else {
            logger.debug("deleteContents is disabled, will fail if dataset contains tables");
            bigQuery.delete(datasetId);
        }

        logger.info("Dataset dropped successfully: {}", datasetId);
    }

    // Jackson requires getters and setters

    public String getDatasetId() {
        return datasetId;
    }

    public void setDatasetId(String datasetId) {
        this.datasetId = datasetId;
    }

    public Boolean getDeleteContents() {
        return deleteContents;
    }

    public void setDeleteContents(Boolean deleteContents) {
        this.deleteContents = deleteContents;
    }

    @Override
    public String toString() {
        return "DropDataset{"
                + "datasetId='"
                + datasetId
                + '\''
                + ", deleteContents="
                + deleteContents
                + '}';
    }
}
