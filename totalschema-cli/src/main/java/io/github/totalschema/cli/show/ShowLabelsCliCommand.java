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

import io.github.totalschema.cli.ChangeFileSelectorMixin;
import io.github.totalschema.cli.EnvironmentAwareCliCommand;
import io.github.totalschema.engine.api.ChangeEngine;
import io.github.totalschema.engine.api.ChangeFileSelector;
import io.github.totalschema.model.ApplyFile;
import io.github.totalschema.model.ChangeFile;
import io.github.totalschema.model.RevertFile;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import picocli.CommandLine;

/**
 * Diagnostic command that prints the effective labels for every change file after cascade
 * resolution. Useful for verifying that {@code totalschema-labels.yml} files are placed correctly
 * and that inheritance produces the expected result. Supports the same {@code -f} and {@code -l}
 * filters as all other commands.
 */
@CommandLine.Command(
        name = "labels",
        mixinStandardHelpOptions = true,
        description =
                "Shows the effective labels assigned to each change file after cascade resolution")
public class ShowLabelsCliCommand extends EnvironmentAwareCliCommand {

    @CommandLine.Mixin
    private ChangeFileSelectorMixin selectorMixin = new ChangeFileSelectorMixin();

    @Override
    public void run(ChangeEngine changeEngine) {
        ChangeFileSelector selector = selectorMixin.buildSelector();

        List<ApplyFile> applyFiles = changeEngine.getChangeManager().getAllApplyFiles(selector);
        List<RevertFile> revertFiles = changeEngine.getChangeManager().getAllRevertFiles(selector);

        List<ChangeFile> allFiles = new ArrayList<>();
        allFiles.addAll(applyFiles);
        allFiles.addAll(revertFiles);

        int count = 0;
        for (ChangeFile f : allFiles) {
            String relPath = f.getRelativePath().toString();
            Map<String, List<String>> labels = f.getEffectiveLabels();

            System.out.println(relPath);
            if (labels == null || labels.isEmpty()) {
                System.out.println("  (no labels)");
            } else {
                new TreeMap<>(labels)
                        .forEach(
                                (key, values) ->
                                        System.out.format(
                                                "  %s=%s%n", key, String.join(", ", values)));
            }
            count++;
        }

        System.out.format("%n%d change file(s) listed.%n", count);
    }
}
