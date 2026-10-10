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

package io.github.totalschema.connector.gcp.bigquery.actions.dataset;

import com.google.cloud.bigquery.BigQuery;
import io.github.totalschema.connector.gcp.bigquery.actions.Action;

/**
 * Actions that operate on BigQuery datasets.
 *
 * <p>Examples: CreateDataset, DropDataset, UpdateDataset, etc.
 *
 * <p>These actions require {@link BigQuery} client and operate at the dataset level (container for
 * tables, views, and other BigQuery objects).
 *
 * <p>Dataset operations typically include:
 *
 * <ul>
 *   <li>Creating/deleting datasets
 *   <li>Updating dataset properties (description, location, labels)
 *   <li>Managing dataset access controls
 *   <li>Configuring dataset-level settings (default table expiration, encryption)
 * </ul>
 */
public interface DatasetAction extends Action<BigQuery> {
    /**
     * Executes the dataset-level action with type-safe client.
     *
     * @param bigQuery the BigQuery client
     */
    void execute(BigQuery bigQuery);
}
