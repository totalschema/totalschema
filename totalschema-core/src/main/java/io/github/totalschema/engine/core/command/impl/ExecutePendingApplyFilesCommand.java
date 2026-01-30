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
import io.github.totalschema.engine.core.command.api.Command;
import io.github.totalschema.engine.core.command.api.CommandContext;
import io.github.totalschema.model.ApplyFile;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Command to execute all pending change files. Retrieves all apply files, determines which are
 * pending, and applies them in order.
 */
public final class ExecutePendingApplyFilesCommand implements Command<Void> {

    private final Logger log = LoggerFactory.getLogger(ExecutePendingApplyFilesCommand.class);

    private final String filterExpression;

    public ExecutePendingApplyFilesCommand(String filterExpression) {
        this.filterExpression = filterExpression;
    }

    @Override
    public Void execute(CommandContext context) throws InterruptedException {

        ChangeEngine changeEngine = context.get(ChangeEngine.class);
        Environment environment = context.get(Environment.class);

        List<ApplyFile> allApplyFiles =
                changeEngine.getChangeManager().getAllApplyFiles(filterExpression);

        log.info("{} change files found", allApplyFiles.size());

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

            ApplyFile applyFile = pendingApplyFiles.get(i);

            changeEngine.getChangeManager().execute(applyFile);
        }

        if (!pendingApplyFiles.isEmpty()) {
            log.info("Executed {} change files", pendingApplyFiles.size());
        }

        if (filterExpression == null) {
            log.info("SUCCESS: The {} environment is in desired state.", environment.getName());
        } else {
            log.info(
                    "SUCCESS: Apply scripts filtered by '{}' are executed against the {} environment.",
                    filterExpression,
                    environment.getName());
        }

        return null;
    }

    private void initializeConnectors(CommandContext context, List<ApplyFile> pendingApplyFiles) {

        List<String> connectorsUsed =
                pendingApplyFiles.stream()
                        .map(ApplyFile::getConnector)
                        .distinct()
                        .collect(Collectors.toList());

        for (String connector : connectorsUsed) {

            log.info("Initializing connector '{}'", connector);

            ConnectorManager connectorManager = context.get(ConnectorManager.class);
            connectorManager.getConnectorByName(connector, context);
        }
    }
}
