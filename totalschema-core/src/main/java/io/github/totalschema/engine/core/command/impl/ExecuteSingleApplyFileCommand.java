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

import io.github.totalschema.engine.core.command.api.Command;
import io.github.totalschema.engine.core.command.api.CommandContext;
import io.github.totalschema.model.ApplyFile;
import io.github.totalschema.model.ChangeType;
import io.github.totalschema.spi.change.ChangeService;
import io.github.totalschema.spi.state.StateService;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Command to execute a single change file. Registers the completion in state unless the change type
 * is APPLY_ALWAYS.
 */
public final class ExecuteSingleApplyFileCommand implements Command<Void> {

    private final Logger log = LoggerFactory.getLogger(ExecuteSingleApplyFileCommand.class);

    private final ApplyFile applyFile;

    public ExecuteSingleApplyFileCommand(ApplyFile applyFile) {
        this.applyFile = applyFile;
    }

    @Override
    public Void execute(CommandContext context) throws InterruptedException {

        StateService stateService = context.get(StateService.class);
        ChangeService changeService = context.get(ChangeService.class);

        Path changeFilePath = applyFile.getFile();
        log.info("Applying: {}", changeFilePath);

        changeService.execute(applyFile, context);

        ChangeType changeType = applyFile.getChangeType();

        if (changeType != ChangeType.APPLY_ALWAYS) {
            stateService.registerCompletion(applyFile);

        } else {
            log.info(
                    "As file is {}, its completion is not registered in state: {}",
                    changeType,
                    changeFilePath);
        }

        log.info("SUCCESS executing: {}", changeFilePath);

        return null;
    }
}
