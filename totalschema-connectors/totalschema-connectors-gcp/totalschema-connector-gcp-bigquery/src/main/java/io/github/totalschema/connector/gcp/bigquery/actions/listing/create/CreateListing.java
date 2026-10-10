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

package io.github.totalschema.connector.gcp.bigquery.actions.listing.create;

import com.google.cloud.bigquery.dataexchange.v1beta1.AnalyticsHubServiceClient;
import com.google.cloud.bigquery.dataexchange.v1beta1.CreateListingRequest;
import com.google.cloud.bigquery.dataexchange.v1beta1.DataExchangeName;
import com.google.cloud.bigquery.dataexchange.v1beta1.Listing;
import io.github.totalschema.config.Configuration;
import io.github.totalschema.connector.gcp.bigquery.actions.listing.AbstractListingAction;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Action for creating a BigQuery Analytics Hub listing.
 *
 * <p>Analytics Hub listings allow you to share datasets with others through a data exchange.
 *
 * <p>Example YAML:
 *
 * <pre>{@code
 * CreateListing:
 *   dataExchangeId: my_exchange
 *   listingId: my_listing
 *   displayName: My Dataset Listing
 *   description: Shared dataset for analytics
 *   primaryContact: contact@example.com
 *   documentation: https://example.com/docs
 *   # Optional - defaults to connector configuration:
 *   # project: my-project
 *   # location: US
 *   categories:
 *     - CATEGORY_OTHERS
 *   sourceDataset:
 *     project: my-project  # Optional - defaults to action's project
 *     dataset: my_dataset
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
public class CreateListing extends AbstractListingAction {

    private static final Logger logger = LoggerFactory.getLogger(CreateListing.class);

    private String displayName;
    private String description;
    private String primaryContact;
    private String documentation;
    private List<String> categories;
    private SourceDataset sourceDataset;

    /** Represents the source BigQuery dataset to be shared. */
    public static class SourceDataset {
        private String project;
        private String dataset;

        public String getProject() {
            return project;
        }

        public void setProject(String project) {
            this.project = project;
        }

        public String getDataset() {
            return dataset;
        }

        public void setDataset(String dataset) {
            this.dataset = dataset;
        }
    }

    @Override
    public void execute(AnalyticsHubServiceClient client, Configuration connectorConfiguration) {
        logger.info(
                "Creating Analytics Hub listing: {} in data exchange: {}",
                listingId,
                dataExchangeId);

        try {
            // Get effective project (action-specific or connector default)
            String effectiveProject = getEffectiveProject(connectorConfiguration);

            // Get effective location (action-specific or connector default, fallback to "US")
            String effectiveLocation = getEffectiveLocation(connectorConfiguration);

            // Build the data exchange parent path
            DataExchangeName parent =
                    DataExchangeName.of(effectiveProject, effectiveLocation, dataExchangeId);

            // Build the listing
            Listing.Builder listingBuilder =
                    Listing.newBuilder()
                            .setDisplayName(displayName != null ? displayName : listingId);

            if (description != null) {
                listingBuilder.setDescription(description);
            }

            if (primaryContact != null) {
                listingBuilder.setPrimaryContact(primaryContact);
            }

            if (documentation != null) {
                listingBuilder.setDocumentation(documentation);
            }

            if (categories != null && !categories.isEmpty()) {
                for (String category : categories) {
                    listingBuilder.addCategories(Listing.Category.valueOf(category));
                }
            }

            // Set the BigQuery dataset source
            if (sourceDataset != null) {
                String datasetPath =
                        String.format(
                                "projects/%s/datasets/%s",
                                sourceDataset.getProject() != null
                                        ? sourceDataset.getProject()
                                        : effectiveProject,
                                sourceDataset.getDataset());

                Listing.BigQueryDatasetSource bigQuerySource =
                        Listing.BigQueryDatasetSource.newBuilder().setDataset(datasetPath).build();

                listingBuilder.setBigqueryDataset(bigQuerySource);
            }

            Listing listing = listingBuilder.build();

            // Create the listing request
            CreateListingRequest request =
                    CreateListingRequest.newBuilder()
                            .setParent(parent.toString())
                            .setListingId(listingId)
                            .setListing(listing)
                            .build();

            // Execute the creation
            Listing createdListing = client.createListing(request);

            logger.info("Analytics Hub listing created successfully: {}", createdListing.getName());

        } catch (RuntimeException e) {
            throw new RuntimeException("Failed to create Analytics Hub listing: " + listingId, e);
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

    public List<String> getCategories() {
        return categories;
    }

    public void setCategories(List<String> categories) {
        this.categories = categories;
    }

    public SourceDataset getSourceDataset() {
        return sourceDataset;
    }

    public void setSourceDataset(SourceDataset sourceDataset) {
        this.sourceDataset = sourceDataset;
    }

    @Override
    public String toString() {
        return "CreateListing{"
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
                + ", displayName='"
                + displayName
                + '\''
                + ", description='"
                + description
                + '\''
                + '}';
    }
}
