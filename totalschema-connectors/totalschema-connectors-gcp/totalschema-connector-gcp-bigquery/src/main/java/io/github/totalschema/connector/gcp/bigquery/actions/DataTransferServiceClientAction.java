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

import com.google.cloud.bigquery.datatransfer.v1.DataTransferServiceClient;
import io.github.totalschema.config.Configuration;

/**
 * Actions that manage BigQuery scheduled queries via the Data Transfer Service.
 *
 * <p>Examples: CreateScheduledQuery, DeleteScheduledQuery, DisableScheduledQuery, etc.
 *
 * <p>These actions use the BigQuery Data Transfer Service to manage recurring query execution.
 * Unlike query actions that execute SQL directly, scheduled query actions configure automated,
 * recurring query execution schedules.
 *
 * <p>Scheduled query operations typically include:
 *
 * <ul>
 *   <li>Creating scheduled queries with cron-like schedules
 *   <li>Enabling/disabling scheduled execution
 *   <li>Deleting scheduled query configurations
 *   <li>Updating query parameters and schedules
 * </ul>
 *
 * <p><strong>Note:</strong> The Data Transfer Service is a separate API from BigQuery's core API.
 * These actions manage the automation layer for query execution, not the queries themselves.
 */
public interface DataTransferServiceClientAction extends Action<DataTransferServiceClient> {
    /**
     * Executes the scheduled query action with type-safe client.
     *
     * @param client the Data Transfer Service client
     * @param connectorConfiguration the connector-level configuration providing defaults
     */
    void execute(DataTransferServiceClient client, Configuration connectorConfiguration);
}
