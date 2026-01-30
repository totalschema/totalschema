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

package io.github.totalschema.engine.core.command.impl.revert;

import io.github.totalschema.engine.api.ChangeEngine;
import io.github.totalschema.engine.core.command.api.Command;
import io.github.totalschema.engine.core.command.api.CommandContext;
import io.github.totalschema.model.RevertFile;
import io.github.totalschema.spi.change.ChangeService;
import io.github.totalschema.spi.state.StateService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExecuteRevertFilesCommand implements Command<Void> {

    protected final Logger log = LoggerFactory.getLogger(ExecuteRevertFilesCommand.class);

    private final String filterExpression;

    public ExecuteRevertFilesCommand(String filterExpression) {
        this.filterExpression = filterExpression;
    }

    @Override
    public Void execute(CommandContext context) throws InterruptedException {

        ChangeEngine changeEngine = context.get(ChangeEngine.class);

        StateService stateService = context.get(StateService.class);
        ChangeService changeService = context.get(ChangeService.class);

        List<RevertFile> revertFiles =
                changeEngine.getChangeManager().getApplicableRevertFiles(filterExpression);

        log.info("{} applicable revert files found", revertFiles.size());

        for (RevertFile revertFile : revertFiles) {

            log.info("Executing: {}", revertFile.getFile());

            changeService.execute(revertFile, context);

            stateService.registerCompletion(revertFile);

            log.info("SUCCESS executing revert: {}", revertFile.getFile());
        }

        return null;
    }
}
