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

import io.github.totalschema.cli.EnvironmentAwareCliCommand;
import io.github.totalschema.engine.api.ChangeEngine;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

@CommandLine.Command(
        name = "validate",
        mixinStandardHelpOptions = true,
        description = "validate workspace files against state")
public class ValidateCliCommand extends EnvironmentAwareCliCommand {

    private final Logger log = LoggerFactory.getLogger(ValidateCliCommand.class);

    @CommandLine.Option(
            names = {"-f", "--filterExpression"},
            description = "Include change files matching this expression only")
    protected String filterExpression;

    @Override
    public void run(ChangeEngine changeEngine) {

        List<Exception> exceptions =
                changeEngine.getValidationManager().validateChangeFiles(filterExpression);

        if (exceptions.isEmpty()) {
            log.info("Validation SUCCESSFUL: no errors found");
        } else {
            log.info("Validation FAILED: {} errors found", exceptions.size());

            RuntimeException runtimeException =
                    new RuntimeException(
                            String.format("Validation FAILED: %s errors found", exceptions.size()));

            for (Exception ex : exceptions) {
                runtimeException.addSuppressed(ex);
            }

            throw runtimeException;
        }
    }
}
