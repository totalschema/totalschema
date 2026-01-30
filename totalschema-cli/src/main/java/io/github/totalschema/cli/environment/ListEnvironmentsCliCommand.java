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

package io.github.totalschema.cli.environment;

import io.github.totalschema.cli.CommonCliCommandBase;
import io.github.totalschema.config.environment.Environment;
import io.github.totalschema.engine.api.ChangeEngine;
import java.util.List;
import picocli.CommandLine;

@CommandLine.Command(
        name = "list",
        mixinStandardHelpOptions = true,
        description = "lists environments declared in configuration")
public class ListEnvironmentsCliCommand extends CommonCliCommandBase {

    protected void run(ChangeEngine changeEngine) {

        List<Environment> environments = changeEngine.getEnvironmentManager().getEnvironments();

        for (Environment environment : environments) {
            System.out.println(environment.getName());
        }
    }
}
