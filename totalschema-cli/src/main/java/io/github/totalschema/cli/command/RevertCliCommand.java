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

package io.github.totalschema.cli.command;

import io.github.totalschema.cli.DryRunSupportEnvironmentAwareCliCommand;
import io.github.totalschema.cli.LabelFilterMixin;
import io.github.totalschema.engine.api.ChangeEngine;
import io.github.totalschema.engine.api.ChangeFileSelector;
import io.github.totalschema.model.RevertFile;
import java.util.List;
import picocli.CommandLine;

@CommandLine.Command(name = "revert", description = "reverts applied changes")
public class RevertCliCommand extends DryRunSupportEnvironmentAwareCliCommand {

    @CommandLine.Option(
            names = {"-f", "--filterExpression"},
            description = "Include change files matching this expression only")
    protected String filterExpression;

    @CommandLine.Mixin private LabelFilterMixin labelFilterMixin = new LabelFilterMixin();

    @Override
    protected void runActual(ChangeEngine changeEngine) {
        ChangeFileSelector selector = labelFilterMixin.buildSelector(filterExpression);
        changeEngine.getChangeManager().executeReverts(selector);
    }

    @Override
    protected void runDry(ChangeEngine changeEngine) {
        ChangeFileSelector selector = labelFilterMixin.buildSelector(filterExpression);
        List<RevertFile> revertFiles =
                changeEngine.getChangeManager().getApplicableRevertFiles(selector);

        System.out.format("%s revertable changes would be executed%n", revertFiles.size());

        for (RevertFile revertFile : revertFiles) {
            System.out.format("%s%n", revertFile.getFile());
        }
    }
}
