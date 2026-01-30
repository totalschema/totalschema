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
import io.github.totalschema.engine.core.command.api.Command;
import io.github.totalschema.engine.core.command.api.CommandContext;
import io.github.totalschema.model.ChangeFile;
import io.github.totalschema.model.RevertFile;
import io.github.totalschema.spi.state.StateService;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class GetApplicableRevertFilesCommand implements Command<List<RevertFile>> {

    private final String filterExpression;

    public GetApplicableRevertFilesCommand(String filterExpression) {
        this.filterExpression = filterExpression;
    }

    @Override
    public List<RevertFile> execute(CommandContext context) {

        StateService stateService = context.get(StateService.class);
        ChangeEngine changeEngine = context.get(ChangeEngine.class);

        Set<ChangeFile.Id> appliedChangeFileMetadata =
                stateService.getAppliedChanges().stream()
                        .map(GetApplicableRevertFilesCommand::removeChangeType)
                        .collect(Collectors.toSet());

        List<RevertFile> allRevertFiles =
                changeEngine.getChangeManager().getAllRevertFiles(filterExpression);

        return allRevertFiles.stream()
                .filter(
                        rollbackFile ->
                                appliedChangeFileMetadata.contains(
                                        removeChangeType(rollbackFile.getId())))
                .collect(Collectors.toList());
    }

    private static ChangeFile.Id removeChangeType(ChangeFile.Id id) {
        return id.withChangeType(null);
    }
}
