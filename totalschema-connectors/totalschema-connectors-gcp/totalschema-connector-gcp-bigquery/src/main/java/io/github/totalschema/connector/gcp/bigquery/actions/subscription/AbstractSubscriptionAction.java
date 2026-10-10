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

package io.github.totalschema.connector.gcp.bigquery.actions.subscription;

import com.google.cloud.bigquery.dataexchange.v1beta1.ListingName;
import io.github.totalschema.config.Configuration;
import io.github.totalschema.connector.gcp.bigquery.actions.AnalyticsHubServiceClientAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for BigQuery Analytics Hub subscription actions that operate against a source
 * listing (identified by project, location, dataExchangeId, and listingId).
 *
 * <p>Provides common functionality shared by subscription actions that reference a listing to
 * subscribe to, including:
 *
 * <ul>
 *   <li>Configuration management (project, location, dataExchangeId, listingId)
 *   <li>Resource name resolution for the source listing
 * </ul>
 */
public abstract class AbstractSubscriptionAction implements AnalyticsHubServiceClientAction {

    protected static final Logger logger =
            LoggerFactory.getLogger(AbstractSubscriptionAction.class);

    protected String project;
    protected String location;
    protected String dataExchangeId;
    protected String listingId;

    /**
     * Gets the effective project ID from action config or connector defaults.
     *
     * @param connectorConfiguration the connector configuration containing default values
     * @return the effective project to use
     * @throws IllegalArgumentException if project is not specified in either location
     */
    protected String getEffectiveProject(Configuration connectorConfiguration) {
        String effectiveProject = project;
        if (effectiveProject == null || effectiveProject.isEmpty()) {
            effectiveProject =
                    connectorConfiguration
                            .getString("defaults.projectId")
                            .orElseThrow(
                                    () ->
                                            new IllegalArgumentException(
                                                    "project must be specified either in the action configuration or in connector configuration under 'defaults.projectId'"));
            logger.debug("Using default project from connector config: {}", effectiveProject);
        }
        return effectiveProject;
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
     * Builds the fully qualified Analytics Hub listing resource name (the listing being subscribed
     * to) from the effective project, location, dataExchangeId, and listingId.
     *
     * @param connectorConfiguration the connector configuration containing default values
     * @return the fully qualified listing resource name (e.g. {@code
     *     projects/p/locations/l/dataExchanges/e/listings/id})
     */
    protected String buildListingResourceName(Configuration connectorConfiguration) {
        String effectiveProject = getEffectiveProject(connectorConfiguration);
        String effectiveLocation = getEffectiveLocation(connectorConfiguration);

        return ListingName.of(effectiveProject, effectiveLocation, dataExchangeId, listingId)
                .toString();
    }

    // Getters and setters for Jackson

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDataExchangeId() {
        return dataExchangeId;
    }

    public void setDataExchangeId(String dataExchangeId) {
        this.dataExchangeId = dataExchangeId;
    }

    public String getListingId() {
        return listingId;
    }

    public void setListingId(String listingId) {
        this.listingId = listingId;
    }
}
