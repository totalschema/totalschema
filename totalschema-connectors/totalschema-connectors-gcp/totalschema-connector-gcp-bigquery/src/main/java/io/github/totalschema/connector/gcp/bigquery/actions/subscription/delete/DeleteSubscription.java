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

package io.github.totalschema.connector.gcp.bigquery.actions.subscription.delete;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQuery.DatasetDeleteOption;
import com.google.cloud.bigquery.DatasetId;
import io.github.totalschema.config.Configuration;
import io.github.totalschema.connector.gcp.bigquery.BigQueryConnectorException;
import io.github.totalschema.connector.gcp.bigquery.actions.BigQueryAction;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Action for deleting a BigQuery Analytics Hub subscription.
 *
 * <p><strong>Implementation note:</strong> The Analytics Hub v1beta1 API (used by {@link
 * io.github.totalschema.connector.gcp.bigquery.actions.subscription.create.CreateSubscription})
 * does not expose a dedicated "delete subscription" RPC. Subscribing to a listing simply creates a
 * linked ("destination") BigQuery dataset in the subscriber's project - that linked dataset *is*
 * the subscription. Consequently, deleting a subscription means deleting that destination dataset,
 * which is a core BigQuery operation rather than an Analytics Hub one. This action therefore
 * executes against the core {@link BigQuery} client instead of the Analytics Hub client.
 *
 * <p>Example YAML:
 *
 * <pre>{@code
 * DeleteSubscription:
 *   datasetId: subscribed_dataset
 *   # Optional - defaults to the connector's own project:
 *   # project: subscriber-project
 *   deleteContents: true
 * }</pre>
 *
 * <p>By default, BigQuery will not delete a dataset that contains tables/views. Set {@code
 * deleteContents: true} to force deletion of all tables/views within the linked dataset before
 * dropping it.
 */
public class DeleteSubscription implements BigQueryAction {

    private static final Logger logger = LoggerFactory.getLogger(DeleteSubscription.class);

    private String project;
    private String datasetId;
    private Boolean deleteContents;

    @Override
    public void execute(BigQuery bigQuery, Configuration connectorConfiguration) {
        logger.info("Deleting Analytics Hub subscription (linked dataset): {}", datasetId);

        boolean shouldDeleteContents = deleteContents != null && deleteContents;
        DatasetDeleteOption[] options =
                shouldDeleteContents
                        ? new DatasetDeleteOption[] {DatasetDeleteOption.deleteContents()}
                        : new DatasetDeleteOption[0];

        boolean deleted;
        try {
            DatasetId theDatasetId =
                    Optional.ofNullable(project)
                            .filter(it -> !it.isEmpty())
                            .map(it -> DatasetId.of(it, datasetId))
                            .orElseGet(() -> DatasetId.of(datasetId));

            deleted = bigQuery.delete(theDatasetId, options);

        } catch (RuntimeException e) {
            throw new BigQueryConnectorException(
                    "Failed to delete Analytics Hub subscription: " + datasetId, e);
        }

        if (!deleted) {
            throw new BigQueryConnectorException(
                    "Analytics Hub subscription (linked dataset) not found: " + datasetId);
        }

        logger.info("Analytics Hub subscription deleted successfully: {}", datasetId);
    }

    // Jackson requires getters and setters

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

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
        return "DeleteSubscription{"
                + "project='"
                + project
                + '\''
                + ", datasetId='"
                + datasetId
                + '\''
                + ", deleteContents="
                + deleteContents
                + '}';
    }
}
