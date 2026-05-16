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

import io.github.totalschema.config.environment.Environment;
import io.github.totalschema.connector.ConnectorManager;
import io.github.totalschema.engine.api.ChangeEngine;
import io.github.totalschema.engine.api.ChangeFileSelector;
import io.github.totalschema.engine.core.command.api.Command;
import io.github.totalschema.engine.core.command.api.CommandContext;
import io.github.totalschema.model.ApplyFile;
import io.github.totalschema.model.ChangeFile;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Command to execute all pending change files. Retrieves all apply files, determines which are
 * pending, and applies them in order.
 */
public final class ExecutePendingApplyFilesCommand implements Command<Void> {

    private final Logger log = LoggerFactory.getLogger(ExecutePendingApplyFilesCommand.class);

    private final ChangeFileSelector selector;

    public ExecutePendingApplyFilesCommand(ChangeFileSelector selector) {
        this.selector = selector != null ? selector : ChangeFileSelector.empty();
    }

    @Override
    public Void execute(CommandContext context) throws InterruptedException {

        ChangeEngine changeEngine = context.get(ChangeEngine.class);
        Environment environment = context.get(Environment.class);

        List<ApplyFile> allApplyFiles = changeEngine.getChangeManager().getAllApplyFiles(selector);

        log.info("Found {} change file(s) — {}", allApplyFiles.size(), selector.getDescription());

        List<ApplyFile> pendingApplyFiles =
                changeEngine.getChangeManager().getPendingApplyFiles(allApplyFiles);

        int totalPending = pendingApplyFiles.size();
        log.info(
                "{} out of {} change files are pending application",
                totalPending,
                allApplyFiles.size());

        initializeConnectors(context, pendingApplyFiles);

        for (int i = 0; i < totalPending; i++) {
            int outputIndex = i + 1;
            log.info(
                    "Executing change file #{} out of {}, remaining: {}",
                    outputIndex,
                    totalPending,
                    totalPending - outputIndex);
            changeEngine.getChangeManager().execute(pendingApplyFiles.get(i));
        }

        if (!pendingApplyFiles.isEmpty()) {
            log.info("Executed {} change files", pendingApplyFiles.size());
        }

        if (selector.isEmpty()) {
            log.info("SUCCESS: The {} environment is in desired state.", environment.getName());
        } else {
            log.info(
                    "SUCCESS: Apply scripts matching the given selector are executed"
                            + " against the {} environment.",
                    environment.getName());
        }

        return null;
    }

    private void initializeConnectors(CommandContext context, List<ApplyFile> pendingApplyFiles)
            throws InterruptedException {

        Map<String, List<ChangeFile.Id>> changeFileIdsByConnector =
                pendingApplyFiles.stream()
                        .collect(
                                Collectors.groupingBy(
                                        ApplyFile::getConnector,
                                        Collectors.mapping(ApplyFile::getId, Collectors.toList())));

        log.info("Connectors required for the changes: {}", changeFileIdsByConnector.keySet());

        ConnectorManager connectorManager = context.get(ConnectorManager.class);

        for (Map.Entry<String, List<ChangeFile.Id>> entry : changeFileIdsByConnector.entrySet()) {
            log.info("Initializing connector '{}'", entry.getKey());
            connectorManager.checkConnector(entry.getKey(), context, entry.getValue());
        }
    }
}
