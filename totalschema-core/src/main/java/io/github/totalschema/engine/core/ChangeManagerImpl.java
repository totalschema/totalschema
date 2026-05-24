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

package io.github.totalschema.engine.core;

import io.github.totalschema.engine.api.ChangeFileSelector;
import io.github.totalschema.engine.api.ChangeManager;
import io.github.totalschema.engine.core.command.impl.*;
import io.github.totalschema.engine.core.command.impl.revert.ExecuteRevertFilesCommand;
import io.github.totalschema.model.ApplyFile;
import io.github.totalschema.model.RevertFile;
import java.util.List;

/**
 * Default implementation of ChangeManager that delegates operations to commands executed through
 * the ChangeEngine.
 */
public class ChangeManagerImpl extends AbstractManager implements ChangeManager {

    public ChangeManagerImpl(DefaultChangeEngine changeEngine) {
        super(changeEngine);
    }

    /** {@inheritDoc} */
    @Override
    public List<ApplyFile> getAllApplyFiles(ChangeFileSelector selector) {
        return executeCommand(new GetApplyFilesCommand(selector));
    }

    /** {@inheritDoc} */
    @Override
    public List<RevertFile> getAllRevertFiles(ChangeFileSelector selector) {
        return executeCommand(new GetRevertFilesCommand(selector));
    }

    /** {@inheritDoc} */
    @Override
    public List<ApplyFile> getPendingApplyFiles(List<ApplyFile> allApplyFiles) {
        return executeCommand(new GetPendingApplyFilesCommand(allApplyFiles));
    }

    /** {@inheritDoc} */
    @Override
    public List<RevertFile> getApplicableRevertFiles(ChangeFileSelector selector) {
        return executeCommand(new GetApplicableRevertFilesCommand(selector));
    }

    /** {@inheritDoc} */
    @Override
    public void executePendingAppliesWithAutomaticRevert(ChangeFileSelector selector) {
        executeCommand(new ExecutePendingApplyFilesWithAutomaticRevertCommand(selector));
    }

    /** {@inheritDoc} */
    @Override
    public void executePendingApplies(ChangeFileSelector selector) {
        executeCommand(new ExecutePendingApplyFilesCommand(selector));
    }

    /** {@inheritDoc} */
    @Override
    public void executeReverts(ChangeFileSelector selector) {
        executeCommand(new ExecuteRevertFilesCommand(selector));
    }

    /** {@inheritDoc} */
    @Override
    public void execute(ApplyFile applyFile) {
        executeCommand(new ExecuteSingleApplyFileCommand(applyFile));
    }
}
