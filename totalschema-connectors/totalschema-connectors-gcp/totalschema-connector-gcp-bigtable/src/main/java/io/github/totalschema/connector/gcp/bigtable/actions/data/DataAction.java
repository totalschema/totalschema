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

package io.github.totalschema.connector.gcp.bigtable.actions.data;

import com.google.cloud.bigtable.data.v2.BigtableDataClient;
import io.github.totalschema.connector.gcp.bigtable.actions.Action;
import io.github.totalschema.connector.gcp.bigtable.actions.table.TableAction;

/**
 * Actions that operate on BigTable data.
 *
 * <p>Examples: Reading rows, writing data, bulk mutations, conditional updates, etc.
 *
 * <p>These actions require {@link BigtableDataClient} and perform read/write operations on table
 * data within a specific BigTable instance configured in the connector.
 *
 * <p>Unlike {@link TableAction} which focuses on schema/table management (DDL operations), {@link
 * DataAction} focuses on data manipulation (DML operations).
 */
public interface DataAction extends Action<BigtableDataClient> {
    /**
     * Executes the data-level action with type-safe client.
     *
     * @param dataClient the BigTable data client for read/write operations
     */
    void execute(BigtableDataClient dataClient);
}
