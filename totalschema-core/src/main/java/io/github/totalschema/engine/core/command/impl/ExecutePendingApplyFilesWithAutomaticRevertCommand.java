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

package io.github.totalschema.engine.core.command.impl;

import io.github.totalschema.engine.api.ChangeEngine;
import io.github.totalschema.engine.api.ChangeManager;
import io.github.totalschema.engine.core.command.api.Command;
import io.github.totalschema.engine.core.command.api.CommandContext;
import io.github.totalschema.model.ApplyFile;
import io.github.totalschema.model.ChangeFile;
import io.github.totalschema.model.RevertFile;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Command that executes all pending apply files and automatically triggers a full revert if any
 * apply fails.
 *
 * <p>Execution flow:
 *
 * <ol>
 *   <li>Delegates to {@link io.github.totalschema.engine.api.ChangeManager#executePendingApplies}
 *       to apply all pending changes matching the filter expression.
 *   <li>If an exception is thrown during apply, {@link
 *       io.github.totalschema.engine.api.ChangeManager#executeReverts} is invoked with the same
 *       filter expression to roll back the deployed changes.
 *   <li>If the revert itself fails, the revert exception is attached as a suppressed exception on
 *       the original apply exception and a warning is logged that the system may be in an
 *       inconsistent state.
 *   <li>In all failure cases the original exception is re-thrown (wrapped in a {@link
 *       RuntimeException}) so the caller is always notified of the failure.
 * </ol>
 *
 * @see io.github.totalschema.engine.api.ChangeManager#executePendingAppliesWithAutomaticRevert
 */
public final class ExecutePendingApplyFilesWithAutomaticRevertCommand implements Command<Void> {

    private final Logger log =
            LoggerFactory.getLogger(ExecutePendingApplyFilesWithAutomaticRevertCommand.class);

    private final String filterExpression;

    /**
     * Creates a new command instance.
     *
     * @param filterExpression wildcard/glob expression used to select which change files are
     *     considered; {@code null} selects all files
     */
    public ExecutePendingApplyFilesWithAutomaticRevertCommand(String filterExpression) {
        this.filterExpression = filterExpression;
    }

    /**
     * Executes the command: applies all pending changes and, if any apply fails, immediately
     * attempts to revert them.
     *
     * <p>Before executing any apply, every pending apply file is verified to have a corresponding
     * revert file. If any revert file is missing the command fails immediately, before any change
     * is applied, so the deployment remains in a clean state.
     *
     * @param context the command context providing access to {@link
     *     io.github.totalschema.engine.api.ChangeEngine} and other IoC-managed components
     * @return {@code null} (Void)
     * @throws InterruptedException if the thread is interrupted while waiting for a connector
     * @throws IllegalStateException if one or more pending apply files have no revert counterpart
     * @throws RuntimeException if the apply phase fails; the exception message indicates whether
     *     the subsequent revert succeeded. If the revert also fails, the revert exception is
     *     attached as a {@linkplain Throwable#addSuppressed suppressed} exception.
     */
    @Override
    public Void execute(CommandContext context) throws InterruptedException {

        ChangeEngine changeEngine = context.get(ChangeEngine.class);
        ChangeManager changeManager = changeEngine.getChangeManager();

        List<ApplyFile> allApplyFiles = changeManager.getAllApplyFiles(filterExpression);
        List<ApplyFile> pendingApplyFiles = changeManager.getPendingApplyFiles(allApplyFiles);

        requireAllPendingApplyFilesHaveRevertCounterpart(changeManager, pendingApplyFiles);

        try {
            changeManager.executePendingApplies(filterExpression);

        } catch (RuntimeException applyException) {

            log.error(
                    "Apply phase failed. Initiating automatic revert to restore a consistent state.",
                    applyException);

            try {
                changeManager.executeReverts(filterExpression);
                log.info(
                        "Automatic revert completed successfully. The system should be in a consistent state.");

            } catch (RuntimeException revertException) {
                log.error(
                        "Automatic revert failed. The system may be in an inconsistent state and requires manual intervention.",
                        revertException);
                applyException.addSuppressed(revertException);
            }

            throw new RuntimeException(
                    "Apply failed (changes reverted successfully)", applyException);
        }

        return null;
    }

    private void requireAllPendingApplyFilesHaveRevertCounterpart(
            ChangeManager changeManager, List<ApplyFile> pendingApplyFiles) {

        List<RevertFile> allRevertFiles = changeManager.getAllRevertFiles(filterExpression);

        Set<ChangeFile.Id> revertIds =
                allRevertFiles.stream()
                        .map(f -> f.getId().withChangeType(null))
                        .collect(Collectors.toSet());

        List<ApplyFile> missingRevert =
                pendingApplyFiles.stream()
                        .filter(f -> !revertIds.contains(f.getId().withChangeType(null)))
                        .collect(Collectors.toList());

        if (!missingRevert.isEmpty()) {
            missingRevert.forEach(
                    f ->
                            log.error(
                                    "Pending apply file has no revert counterpart: {}",
                                    f.getId().toStringRepresentation()));
            throw new IllegalStateException(
                    missingRevert.size()
                            + " pending apply file(s) have no revert counterpart."
                            + " Automatic revert would be impossible. Aborting.");
        }
    }
}
