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

package io.github.totalschema.connector.gcp.bigquery.actions.scheduledquery;

import com.google.cloud.bigquery.datatransfer.v1.DataTransferServiceClient;
import com.google.cloud.bigquery.datatransfer.v1.ListTransferConfigsRequest;
import com.google.cloud.bigquery.datatransfer.v1.LocationName;
import com.google.cloud.bigquery.datatransfer.v1.TransferConfig;
import io.github.totalschema.config.Configuration;
import io.github.totalschema.connector.gcp.bigquery.BigQueryConnectorException;
import io.github.totalschema.connector.gcp.bigquery.actions.DataTransferServiceClientAction;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for BigQuery scheduled query actions.
 *
 * <p>Provides common functionality for creating, deleting, enabling, and disabling scheduled
 * queries, including:
 *
 * <ul>
 *   <li>Configuration management (projectId, location, displayName)
 *   <li>Resource name resolution
 *   <li>Transfer config lookup
 * </ul>
 */
public abstract class AbstractScheduledQueryAction implements DataTransferServiceClientAction {

    protected static final Logger logger =
            LoggerFactory.getLogger(AbstractScheduledQueryAction.class);
    protected static final String DATA_SOURCE_ID = "scheduled_query";

    protected String projectId;
    protected String location;
    protected String displayName;

    /**
     * Gets the effective project ID from action config or connector defaults.
     *
     * @param connectorConfiguration the connector configuration containing default values
     * @return the effective project ID to use
     * @throws IllegalArgumentException if projectId is not specified in either location
     */
    protected String getEffectiveProjectId(Configuration connectorConfiguration) {
        String effectiveProjectId = projectId;
        if (effectiveProjectId == null || effectiveProjectId.isEmpty()) {
            effectiveProjectId =
                    connectorConfiguration
                            .getString("defaults.projectId")
                            .orElseThrow(
                                    () ->
                                            new IllegalArgumentException(
                                                    "projectId must be specified either in the action configuration or in connector configuration under 'defaults.projectId'"));
            logger.debug("Using default projectId from connector config: {}", effectiveProjectId);
        }
        return effectiveProjectId;
    }

    /**
     * Gets the effective location from action config or connector defaults, with "US" fallback.
     *
     * @param connectorConfiguration the connector configuration containing default values
     * @return the effective location to use
     */
    protected String getEffectiveLocation(Configuration connectorConfiguration) {
        String effectiveLocation = location;
        if (effectiveLocation == null || effectiveLocation.isEmpty()) {
            effectiveLocation = connectorConfiguration.getString("defaults.location").orElse("US");
            logger.debug("Using default location from connector config: {}", effectiveLocation);
        }
        return effectiveLocation;
    }

    /**
     * Finds a transfer config by its display name.
     *
     * <p>Searches through all transfer configs in the specified project and location for a
     * scheduled query data source with a matching display name.
     *
     * @param dataTransferServiceClient the BigQuery Data Transfer Service client
     * @param projectId the GCP project ID
     * @param location the GCP location (e.g., "US", "EU")
     * @param displayName the display name to search for
     * @return an {@link Optional} containing the matching {@link TransferConfig} if found, or an
     *     empty Optional if no matching scheduled query exists
     */
    protected Optional<TransferConfig> findTransferConfigByDisplayName(
            DataTransferServiceClient dataTransferServiceClient,
            String projectId,
            String location,
            String displayName) {

        LocationName parent = LocationName.of(projectId, location);

        ListTransferConfigsRequest listRequest =
                ListTransferConfigsRequest.newBuilder()
                        .setParent(parent.toString())
                        .addDataSourceIds(DATA_SOURCE_ID)
                        .build();

        var transferConfigs = dataTransferServiceClient.listTransferConfigs(listRequest);

        return StreamSupport.stream(transferConfigs.iterateAll().spliterator(), false)
                .filter(config -> Objects.equals(config.getDisplayName(), displayName))
                .findFirst();
    }

    /**
     * Lists all transfer configs for scheduled queries in the specified project and location.
     *
     * @param dataTransferServiceClient the BigQuery Data Transfer Service client
     * @param projectId the GCP project ID
     * @param location the GCP location (e.g., "US", "EU")
     * @return an iterable of all transfer configs for scheduled queries
     */
    protected Iterable<TransferConfig> listScheduledQueries(
            DataTransferServiceClient dataTransferServiceClient,
            String projectId,
            String location) {

        LocationName parent = LocationName.of(projectId, location);

        ListTransferConfigsRequest listRequest =
                ListTransferConfigsRequest.newBuilder()
                        .setParent(parent.toString())
                        .addDataSourceIds(DATA_SOURCE_ID)
                        .build();

        return dataTransferServiceClient.listTransferConfigs(listRequest).iterateAll();
    }

    /**
     * Builds a transfer config resource name from ID or returns the fully qualified name if already
     * provided.
     *
     * <p>Supports three modes:
     *
     * <ul>
     *   <li>If transferConfigName is provided, uses it directly
     *   <li>If displayName is provided, looks up the transfer config and uses its name
     *   <li>If transferConfigId is provided, builds the resource name from projectId, location, and
     *       ID
     * </ul>
     *
     * @param dataTransferServiceClient the BigQuery Data Transfer Service client (required if
     *     displayName is used)
     * @param transferConfigName fully qualified transfer config name (e.g.,
     *     "projects/p/locations/l/transferConfigs/id")
     * @param transferConfigId transfer config ID without project/location prefix
     * @param connectorConfiguration the connector configuration containing default values
     * @return the fully qualified transfer config resource name
     * @throws BigQueryConnectorException if the required fields are not specified or lookup fails
     */
    protected String buildTransferConfigResourceName(
            DataTransferServiceClient dataTransferServiceClient,
            String transferConfigName,
            String transferConfigId,
            Configuration connectorConfiguration) {

        // 1. Try to use fully qualified name if provided
        if (transferConfigName != null && !transferConfigName.isEmpty()) {
            logger.debug("Using fully qualified transfer config name: {}", transferConfigName);
            return transferConfigName;
        }

        // Get effective configuration values
        String effectiveProjectId = getEffectiveProjectId(connectorConfiguration);
        String effectiveLocation = getEffectiveLocation(connectorConfiguration);

        // 2. Try to use displayName by looking it up
        if (displayName != null && !displayName.isEmpty()) {
            Optional<TransferConfig> existing =
                    findTransferConfigByDisplayName(
                            dataTransferServiceClient,
                            effectiveProjectId,
                            effectiveLocation,
                            displayName);
            if (existing.isPresent()) {
                logger.debug(
                        "Found transfer config by displayName '{}': {}",
                        displayName,
                        existing.get().getName());
                return existing.get().getName();
            } else {
                throw new BigQueryConnectorException(
                        "No scheduled query found with displayName: " + displayName);
            }
        }

        // 3. Fall back to building from transferConfigId
        if (transferConfigId != null && !transferConfigId.isEmpty()) {
            String resourceName =
                    String.format(
                            "projects/%s/locations/%s/transferConfigs/%s",
                            effectiveProjectId, effectiveLocation, transferConfigId);
            logger.debug(
                    "Built transfer config name from ID: {} (project: {}, location: {})",
                    resourceName,
                    effectiveProjectId,
                    effectiveLocation);
            return resourceName;
        }

        throw new BigQueryConnectorException(
                "At least one of displayName, transferConfigId, or transferConfigName must be specified");
    }

    // Getters and setters for Jackson

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}
