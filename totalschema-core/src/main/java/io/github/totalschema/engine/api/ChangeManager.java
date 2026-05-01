/*
 * totalschema: tool for managing database versioning and schema changes with ease.
 * Copyright (C) 2025 totalschema development team
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

import io.github.totalschema.model.ApplyFile;
import io.github.totalschema.model.RevertFile;
import java.util.List;

/**
 * API for managing change files and their execution in totalschema. This interface provides
 * operations for discovering, filtering, and executing apply and revert operations.
 */
public interface ChangeManager {

    /**
     * Returns all apply files matching the given selector (path filter + label filters).
     *
     * @param selector the selector specifying path and/or label filters; must not be null
     * @return list of apply files
     */
    List<ApplyFile> getAllApplyFiles(ChangeFileSelector selector);

    /**
     * Returns all revert files matching the given selector.
     *
     * @param selector the selector specifying path and/or label filters; must not be null
     * @return list of revert files
     */
    List<RevertFile> getAllRevertFiles(ChangeFileSelector selector);

    /**
     * Returns apply files that are pending execution (not yet applied or changed since last apply).
     *
     * @param allApplyFiles all apply files to check
     * @return list of pending apply files
     */
    List<ApplyFile> getPendingApplyFiles(List<ApplyFile> allApplyFiles);

    /**
     * Returns revert files that can be applied (have corresponding applied changes).
     *
     * @param selector the selector specifying path and/or label filters; must not be null
     * @return list of applicable revert files
     */
    List<RevertFile> getApplicableRevertFiles(ChangeFileSelector selector);

    /**
     * Executes all pending changes matching the selector. If any change fails, already applied
     * changes from this run are automatically reverted before the exception is propagated.
     *
     * @param selector the selector; must not be null
     */
    void executePendingAppliesWithAutomaticRevert(ChangeFileSelector selector);

    /**
     * Executes all pending changes matching the selector.
     *
     * @param selector the selector; must not be null
     */
    void executePendingApplies(ChangeFileSelector selector);

    /**
     * Executes revert operations for changes matching the selector.
     *
     * @param selector the selector; must not be null
     */
    void executeReverts(ChangeFileSelector selector);

    /**
     * Applies a single change file immediately.
     *
     * @param applyFile the change file to apply
     */
    void execute(ApplyFile applyFile);
}
