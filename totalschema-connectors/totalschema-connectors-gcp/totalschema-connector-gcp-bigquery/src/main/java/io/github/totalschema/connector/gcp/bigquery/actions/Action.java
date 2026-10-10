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

package io.github.totalschema.connector.gcp.bigquery.actions;

import io.github.totalschema.config.Configuration;

/**
 * Base interface for all BigQuery actions.
 *
 * <p>Actions represent declarative operations that can be performed on BigQuery and related GCP
 * services:
 *
 * <ul>
 *   <li>{@link BigQueryAction} - Query execution (DDL, DML, SELECT)
 *   <li>{@link DataTransferServiceClientAction} - Scheduled query management via Data Transfer
 *       Service
 *   <li>{@link AnalyticsHubServiceClientAction} - Analytics Hub listing management
 * </ul>
 *
 * <p>Actions are deserialized from YAML configuration files and executed via their specific service
 * clients, which are provided through the Context.
 *
 * <p><strong>Connector Configuration:</strong> The {@code connectorConfiguration} parameter
 * provides access to connector-level settings (e.g., default project ID, location, timeouts).
 * Actions can use these as defaults while allowing per-action overrides specified in the YAML file.
 *
 * @param <T> the type of service client required by this action
 */
public interface Action<T> {
    /**
     * Executes the action using the provided service client and connector configuration.
     *
     * @param client the service client to use for execution
     * @param connectorConfiguration the connector-level configuration providing defaults
     */
    void execute(T client, Configuration connectorConfiguration);
}
