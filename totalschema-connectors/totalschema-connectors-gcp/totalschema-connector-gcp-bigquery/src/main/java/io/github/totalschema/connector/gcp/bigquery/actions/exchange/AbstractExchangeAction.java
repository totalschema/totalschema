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

package io.github.totalschema.connector.gcp.bigquery.actions.exchange;

import com.google.cloud.bigquery.dataexchange.v1beta1.DataExchangeName;
import com.google.cloud.bigquery.dataexchange.v1beta1.LocationName;
import io.github.totalschema.config.Configuration;
import io.github.totalschema.connector.gcp.bigquery.actions.AnalyticsHubServiceClientAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for BigQuery Analytics Hub data exchange actions.
 *
 * <p>Provides common functionality for creating and deleting Analytics Hub data exchanges,
 * including:
 *
 * <ul>
 *   <li>Configuration management (project, location, dataExchangeId)
 *   <li>Resource name resolution
 * </ul>
 */
public abstract class AbstractExchangeAction implements AnalyticsHubServiceClientAction {

    protected static final Logger logger = LoggerFactory.getLogger(AbstractExchangeAction.class);

    protected String project;
    protected String location;
    protected String dataExchangeId;

    /**
     * Validates the common configuration shared by all data exchange actions.
     *
     * <p>Triggers resolution of the effective {@code project} and {@code location} (from either the
     * action configuration or the connector's defaults), and ensures {@code dataExchangeId} has
     * been specified. Subclasses that override this method should call {@code super.validate} to
     * retain this base validation.
     *
     * @param connectorConfiguration the connector configuration containing default values
     * @throws IllegalArgumentException if {@code project} cannot be resolved, or if {@code
     *     dataExchangeId} is {@code null} or empty
     */
    protected void validate(Configuration connectorConfiguration) {

        // trigger validation in lookup methods
        getEffectiveProject(connectorConfiguration);
        getEffectiveLocation(connectorConfiguration);

        if (dataExchangeId == null || dataExchangeId.isEmpty()) {
            throw new IllegalArgumentException("dataExchangeId is required");
        }
    }

    /**
     * Gets the effective project ID from action config or connector defaults.
     *
     * @param connectorConfiguration the connector configuration containing default values
     * @return the effective project to use
     * @throws IllegalArgumentException if project is not specified in either location
     */
    protected final String getEffectiveProject(Configuration connectorConfiguration) {
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
    protected final String getEffectiveLocation(Configuration connectorConfiguration) {
        String effectiveLocation = location;
        if (effectiveLocation == null || effectiveLocation.isEmpty()) {
            effectiveLocation = connectorConfiguration.getString("defaults.location").orElse("US");
            logger.debug("Using default location from connector config: {}", effectiveLocation);
        }
        return effectiveLocation;
    }

    /**
     * Builds the fully qualified Analytics Hub location resource name (the parent under which data
     * exchanges are created/listed) from the effective project and location.
     *
     * @param connectorConfiguration the connector configuration containing default values
     * @return the fully qualified location resource name (e.g. {@code projects/p/locations/l})
     */
    protected final String buildLocationResourceName(Configuration connectorConfiguration) {
        String effectiveProject = getEffectiveProject(connectorConfiguration);
        String effectiveLocation = getEffectiveLocation(connectorConfiguration);

        return LocationName.of(effectiveProject, effectiveLocation).toString();
    }

    /**
     * Builds the fully qualified Analytics Hub data exchange resource name from the effective
     * project, location, and dataExchangeId.
     *
     * @param connectorConfiguration the connector configuration containing default values
     * @return the fully qualified data exchange resource name (e.g. {@code
     *     projects/p/locations/l/dataExchanges/id})
     */
    protected final String buildExchangeResourceName(Configuration connectorConfiguration) {
        String effectiveProject = getEffectiveProject(connectorConfiguration);
        String effectiveLocation = getEffectiveLocation(connectorConfiguration);

        return DataExchangeName.of(effectiveProject, effectiveLocation, dataExchangeId).toString();
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
}
