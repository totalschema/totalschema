/*
 * totalschema: tool for managing database versioning and schema changes with ease.
 * Copyright (C) 2025-2026 totalschema development team
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

package io.github.totalschema.cli;

import io.github.totalschema.engine.api.ChangeEngine;
import picocli.CommandLine;

public abstract class DryRunSupportEnvironmentAwareCliCommand extends EnvironmentAwareCliCommand {

    @CommandLine.Option(
            names = {"--dry-run"},
            description = "Show what would be applied without executing")
    protected boolean dryRun;

    @Override
    protected final void run(ChangeEngine changeEngine) {
        if (dryRun) {
            runDry(changeEngine);
        } else {
            runActual(changeEngine);
        }
    }

    protected abstract void runActual(ChangeEngine changeEngine);

    protected abstract void runDry(ChangeEngine changeEngine);
}
