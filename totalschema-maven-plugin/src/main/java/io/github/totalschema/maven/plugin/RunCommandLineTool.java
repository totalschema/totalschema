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

package io.github.totalschema.maven.plugin;

import io.github.totalschema.ProjectConventions;
import java.util.Arrays;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * A simple Mojo that runs the command line interface class with arguments defined by the user in
 * Maven configuration.
 */
@Mojo(name = "run", defaultPhase = LifecyclePhase.PROCESS_RESOURCES)
public class RunCommandLineTool extends AbstractMojo {

    @Parameter(property = "arguments")
    private String[] arguments;

    public void setArguments(String[] arguments) {
        this.arguments = Arrays.copyOf(arguments, arguments.length);
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        try {
            if (arguments == null || arguments.length == 0) {
                throw new MojoFailureException(
                        "No arguments specified. "
                                + "Please add an <arguments> block in the plugin <configuration> section.");
            }

            getLog().info(
                            String.format(
                                    "Running %s command with arguments: %s ",
                                    ProjectConventions.PROJECT_SYSTEM_NAME,
                                    Arrays.toString(arguments)));

            int returnCode = MainClassHolder.run(arguments);

            getLog().info("Command return code is: " + returnCode);

            if (returnCode != 0) {
                getLog().error(
                                "Non-zero return code from command line tool: "
                                        + returnCode
                                        + "; failing the build");

                throw new MojoFailureException(
                        "Command execution failed with return code: " + returnCode);
            }

        } catch (NoClassDefFoundError e) {
            throw new MojoExecutionException(
                    "Class loading error: totalschema-core is "
                            + "missing from plugin dependencies. Please add it as a plugin dependency.",
                    e);

        } catch (RuntimeException re) {
            throw new MojoExecutionException("Unexpected error executing tool", re);
        }
    }
}
