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

package io.github.totalschema.connector.gcp.bigquery.actions.subscription.create;

import com.google.cloud.bigquery.dataexchange.v1beta1.AnalyticsHubServiceClient;
import com.google.cloud.bigquery.dataexchange.v1beta1.DestinationDataset;
import com.google.cloud.bigquery.dataexchange.v1beta1.DestinationDatasetReference;
import com.google.cloud.bigquery.dataexchange.v1beta1.SubscribeListingRequest;
import com.google.protobuf.StringValue;
import io.github.totalschema.config.Configuration;
import io.github.totalschema.connector.gcp.bigquery.BigQueryConnectorException;
import io.github.totalschema.connector.gcp.bigquery.actions.subscription.AbstractSubscriptionAction;
import java.util.Optional;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Action for creating a BigQuery Analytics Hub subscription to a listing.
 *
 * <p>Subscribing to a listing creates a linked ("destination") BigQuery dataset in the subscriber's
 * own project. That dataset behaves like a normal read-only BigQuery dataset backed live by the
 * publisher's source data, without copying or moving any data.
 *
 * <p>Example YAML:
 *
 * <pre>{@code
 * CreateSubscription:
 *   dataExchangeId: my_exchange
 *   listingId: my_listing
 *   # Optional - defaults to connector configuration:
 *   # project: my-project
 *   # location: US
 *   destinationDataset:
 *     project: subscriber-project     # Optional - defaults to the action's/connector's project
 *     datasetId: subscribed_dataset
 *     location: US                    # Optional - defaults to the action's/connector's location
 *     friendlyName: Subscribed Dataset
 *     description: Linked dataset created from an Analytics Hub subscription
 * }</pre>
 *
 * <p><strong>Configuration Defaults:</strong>
 *
 * <ul>
 *   <li>{@code project} - Project of the listing being subscribed to. If not specified in YAML,
 *       uses {@code defaults.projectId} from connector configuration. Required.
 *   <li>{@code location} - Location of the listing being subscribed to. If not specified in YAML,
 *       uses {@code defaults.location} from connector configuration. Defaults to "US" if neither is
 *       specified.
 * </ul>
 */
public class CreateSubscription extends AbstractSubscriptionAction {

    private static final Logger logger = LoggerFactory.getLogger(CreateSubscription.class);

    private DestinationDatasetConfig destinationDataset;

    /** Represents the destination BigQuery dataset to create for the subscriber. */
    public static class DestinationDatasetConfig {
        private String project;
        private String datasetId;
        private String location;
        private String friendlyName;
        private String description;

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

        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
        }

        public String getFriendlyName() {
            return friendlyName;
        }

        public void setFriendlyName(String friendlyName) {
            this.friendlyName = friendlyName;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        @Override
        public String toString() {
            return "DestinationDatasetConfig{"
                    + "project='"
                    + project
                    + '\''
                    + ", datasetId='"
                    + datasetId
                    + '\''
                    + ", location='"
                    + location
                    + '\''
                    + '}';
        }
    }

    @Override
    public void execute(AnalyticsHubServiceClient client, Configuration connectorConfiguration) {
        logger.info(
                "Creating Analytics Hub subscription to listing: {} in data exchange: {}",
                listingId,
                dataExchangeId);

        String effectiveProject = getEffectiveProject(connectorConfiguration);
        String effectiveLocation = getEffectiveLocation(connectorConfiguration);

        String listingName = buildListingResourceName(connectorConfiguration);

        try {
            SubscribeListingRequest.Builder requestBuilder =
                    SubscribeListingRequest.newBuilder().setName(listingName);

            if (destinationDataset != null) {
                DestinationDataset destinationDatasetRequestObject =
                        getDestinationDatasetRequestObject(effectiveProject, effectiveLocation);

                requestBuilder.setDestinationDataset(destinationDatasetRequestObject);
            }

            client.subscribeListing(requestBuilder.build());

            logger.info(
                    "Analytics Hub subscription created successfully for listing: {}", listingName);

        } catch (RuntimeException e) {
            throw new BigQueryConnectorException(
                    "Failed to create Analytics Hub subscription to listing: " + listingName, e);
        }
    }

    private @NonNull DestinationDataset getDestinationDatasetRequestObject(
            String effectiveProject, String effectiveLocation) {

        if (destinationDataset == null) {
            // method call must be guarded by null check
            throw new IllegalStateException("destinationDataset must not be null");
        }

        String destinationDatasetProjectId =
                Optional.ofNullable(destinationDataset.getProject()).orElse(effectiveProject);

        DestinationDatasetReference datasetReference =
                DestinationDatasetReference.newBuilder()
                        .setProjectId(destinationDatasetProjectId)
                        .setDatasetId(destinationDataset.getDatasetId())
                        .build();

        DestinationDataset.Builder destinationDatasetBuilder =
                DestinationDataset.newBuilder().setDatasetReference(datasetReference);

        String destinationLocation =
                Optional.ofNullable(destinationDataset.getLocation()).orElse(effectiveLocation);

        destinationDatasetBuilder.setLocation(destinationLocation);

        Optional.ofNullable(destinationDataset.getFriendlyName())
                .map(StringValue::of)
                .ifPresent(destinationDatasetBuilder::setFriendlyName);

        Optional.ofNullable(destinationDataset.getDescription())
                .map(StringValue::of)
                .ifPresent(destinationDatasetBuilder::setDescription);

        return destinationDatasetBuilder.build();
    }

    // Jackson requires getters and setters

    public DestinationDatasetConfig getDestinationDataset() {
        return destinationDataset;
    }

    public void setDestinationDataset(DestinationDatasetConfig destinationDataset) {
        this.destinationDataset = destinationDataset;
    }

    @Override
    public String toString() {
        return "CreateSubscription{"
                + "project='"
                + project
                + '\''
                + ", location='"
                + location
                + '\''
                + ", dataExchangeId='"
                + dataExchangeId
                + '\''
                + ", listingId='"
                + listingId
                + '\''
                + ", destinationDataset="
                + destinationDataset
                + '}';
    }
}
