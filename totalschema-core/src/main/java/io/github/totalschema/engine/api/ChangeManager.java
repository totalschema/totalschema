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
     * Returns all apply files matching the filter expression.
     *
     * @param filterExpression filter for selecting files (can be null for all)
     * @return list of apply files
     */
    List<ApplyFile> getAllApplyFiles(String filterExpression);

    /**
     * Returns all revert files matching the filter expression.
     *
     * @param filterExpression filter for selecting files (can be null for all)
     * @return list of revert files
     */
    List<RevertFile> getAllRevertFiles(String filterExpression);

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
     * @param filterExpression filter for selecting files (can be null for all)
     * @return list of applicable revert files
     */
    List<RevertFile> getApplicableRevertFiles(String filterExpression);

    /**
     * Executes all pending changes matching the filter expression.
     *
     * @param filterExpression filter for selecting changes (can be null for all)
     */
    void executePendingApplies(String filterExpression);

    /**
     * Executes revert operations for changes matching the filter expression.
     *
     * @param filterExpression filter for selecting reverts (can be null for all)
     */
    void executeReverts(String filterExpression);

    /**
     * Applies a single change file immediately.
     *
     * @param applyFile the change file to apply
     */
    void execute(ApplyFile applyFile);
}
