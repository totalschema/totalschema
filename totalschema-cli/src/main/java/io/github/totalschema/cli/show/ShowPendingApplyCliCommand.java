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

package io.github.totalschema.cli.show;

import io.github.totalschema.cli.EnvironmentAwareCliCommand;
import io.github.totalschema.engine.api.ChangeEngine;
import io.github.totalschema.model.ApplyFile;
import java.util.List;
import picocli.CommandLine;

@CommandLine.Command(
        name = "pending-apply",
        mixinStandardHelpOptions = true,
        description = "lists all pending change operations applicable now")
public class ShowPendingApplyCliCommand extends EnvironmentAwareCliCommand {

    @CommandLine.Option(
            names = {"-f", "--filterExpression"},
            description = "Include change files matching this expression only")
    protected String filterExpression;

    @Override
    public void run(ChangeEngine changeEngine) {

        List<ApplyFile> allApplyFiles =
                changeEngine.getChangeManager().getAllApplyFiles(filterExpression);
        List<ApplyFile> pendingApplyFiles =
                changeEngine.getChangeManager().getPendingApplyFiles(allApplyFiles);

        System.out.format("%s pending changes found %n", pendingApplyFiles.size());

        for (ApplyFile applyFile : pendingApplyFiles) {
            System.out.format("%s %n", applyFile.getFile());
        }
    }
}
