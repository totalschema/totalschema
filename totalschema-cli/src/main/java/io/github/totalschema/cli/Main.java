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

package io.github.totalschema.cli;

import io.github.totalschema.cli.command.ApplyCliCommand;
import io.github.totalschema.cli.command.RevertCliCommand;
import io.github.totalschema.cli.command.ValidateCliCommand;
import io.github.totalschema.cli.command.VersionCliCommand;
import io.github.totalschema.cli.environment.EnvironmentsSubCommands;
import io.github.totalschema.cli.secret.SecretsSubCommands;
import io.github.totalschema.cli.show.ShowSubCommands;
import io.github.totalschema.cli.state.StateSubCommands;
import io.github.totalschema.cli.variables.VariablesSubCommands;
import java.util.concurrent.Callable;
import picocli.CommandLine;

@CommandLine.Command(
        name = "totalschema",
        subcommands = {
            ApplyCliCommand.class,
            RevertCliCommand.class,
            ShowSubCommands.class,
            EnvironmentsSubCommands.class,
            VariablesSubCommands.class,
            StateSubCommands.class,
            SecretsSubCommands.class,
            ValidateCliCommand.class,
            VersionCliCommand.class
        },
        mixinStandardHelpOptions = true,
        versionProvider = io.github.totalschema.cli.command.VersionCliCommand.class)
public class Main implements Callable<Integer> {

    public static void main(String[] args) {
        int exitCode = run(args);
        System.exit(exitCode);
    }

    public static int run(String[] args) {
        return new CommandLine(new Main()).execute(args);
    }

    @Override
    public final Integer call() {
        CommandLine.usage(this, System.out);
        return -1;
    }
}
