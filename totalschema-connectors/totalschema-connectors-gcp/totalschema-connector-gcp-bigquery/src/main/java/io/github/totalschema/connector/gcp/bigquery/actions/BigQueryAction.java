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

import com.google.cloud.bigquery.BigQuery;
import io.github.totalschema.config.Configuration;

/**
 * Actions that execute queries against BigQuery.
 *
 * <p>Examples: ExecuteStatement, etc.
 *
 * <p>These actions require {@link BigQuery} client and execute SQL statements for:
 *
 * <ul>
 *   <li>Data manipulation (INSERT, UPDATE, DELETE)
 *   <li>Data definition (CREATE TABLE, ALTER TABLE, DROP TABLE)
 *   <li>Data querying (SELECT statements)
 *   <li>Batch operations and complex transformations
 * </ul>
 *
 * <p>Query actions are typically used for:
 *
 * <ul>
 *   <li>Schema migrations (table creation, alterations)
 *   <li>Data transformations and ETL operations
 *   <li>View and materialized view creation
 *   <li>Routine (UDF/stored procedure) definition
 * </ul>
 */
public interface BigQueryAction extends Action<BigQuery> {
    /**
     * Executes the query action with type-safe client.
     *
     * @param bigQuery the BigQuery client
     * @param connectorConfiguration the connector-level configuration providing defaults
     */
    void execute(BigQuery bigQuery, Configuration connectorConfiguration);
}
