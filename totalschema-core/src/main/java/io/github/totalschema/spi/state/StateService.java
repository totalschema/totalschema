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

package io.github.totalschema.spi.state;

import io.github.totalschema.model.ApplyFile;
import io.github.totalschema.model.ChangeFile;
import io.github.totalschema.model.RevertFile;
import io.github.totalschema.model.StateRecord;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface StateService {

    void registerCompletion(ApplyFile applyFile);

    void registerCompletion(RevertFile revertFile);

    List<ChangeFile.Id> getAppliedChanges();

    List<StateRecord> getStateRecords();

    /**
     * Returns state records for change files that no longer exist on disk, without making any
     * changes. Only records relevant to the specified environment are considered: records that are
     * env-agnostic always qualify; env-specific records qualify only if their environment matches
     * the given one. When the environment is empty, all records are candidates regardless of their
     * environment.
     *
     * @param onDiskIds the set of change file IDs currently discovered on disk
     * @param environmentName the current environment, or empty to consider all environments
     * @return the list of orphaned state records
     */
    List<StateRecord> getOrphanedStateRecords(
            Set<ChangeFile.Id> onDiskIds, Optional<String> environmentName);

    /**
     * Removes state records for change files that no longer exist on disk. Only records relevant to
     * the specified environment are considered. When the environment is empty, all records are
     * candidates regardless of their environment.
     *
     * @param onDiskIds the set of change file IDs currently discovered on disk
     * @param environmentName the current environment, or empty to consider all environments
     * @return the list of state records that were removed
     */
    List<StateRecord> purgeOrphanedStateRecords(
            Set<ChangeFile.Id> onDiskIds, Optional<String> environmentName);
}
