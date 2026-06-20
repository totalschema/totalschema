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

package io.github.totalschema.engine.api;

import io.github.totalschema.model.StateRecord;
import java.util.List;

/**
 * API for managing change execution state in totalschema. This interface provides operations for
 * querying the state history of applied changes.
 */
public interface StateManager {

    /**
     * Returns all state records showing which changes have been applied.
     *
     * @return list of state records
     */
    List<StateRecord> getStateRecords();

    /**
     * Returns state records for change files that no longer exist on disk, without making any
     * changes. Only records scoped to the current environment (env-agnostic or matching the
     * engine's environment) are considered.
     *
     * @return the list of orphaned state records
     */
    List<StateRecord> getOrphanedStateRecords();

    /**
     * Removes state records for change files that no longer exist on disk. Only records scoped to
     * the current environment (env-agnostic or matching the engine's environment) are considered.
     *
     * @return the list of state records that were removed
     */
    List<StateRecord> purgeOrphanedStateRecords();
}
