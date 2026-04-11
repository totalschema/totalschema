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

package io.github.totalschema.connector.shell.impl;

import io.github.totalschema.connector.shell.spi.ShellScriptRunner;
import io.github.totalschema.engine.internal.shell.ExternalProcessTerminalSession;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * A {@link ShellScriptRunner} that prepends a fixed interpreter prefix to every command it runs.
 *
 * <p>This class is intentionally simple: it knows nothing about operating systems, file extensions,
 * or PATH probing. All of that logic belongs in {@link DefaultShellScriptRunnerFactory}, which
 * selects the right prefix before constructing an instance of this class.
 *
 * <p>The full OS command becomes:
 *
 * <pre>{@code <commandPrefix> <scriptAbsolutePath>}</pre>
 *
 * For example, with prefix {@code ["sh"]} and script path {@code /opt/changes/0001.setup.sh}:
 *
 * <pre>{@code sh /opt/changes/0001.setup.sh}</pre>
 *
 * @see DefaultShellScriptRunnerFactory
 */
public class DefaultShellScriptRunner extends ExternalProcessTerminalSession
        implements ShellScriptRunner {

    private final String name;

    /**
     * The interpreter prefix prepended to every command. Set once at construction by {@link
     * DefaultShellScriptRunnerFactory}.
     */
    private final List<String> commandPrefix;

    /**
     * Constructs a new {@code DefaultShellScriptRunner}.
     *
     * @param name the connector name; used for logging and {@link #toString()}
     * @param commandPrefix the interpreter tokens to prepend (e.g. {@code ["sh"]} or {@code
     *     ["pwsh", "-ExecutionPolicy", "Bypass", "-File"]})
     */
    public DefaultShellScriptRunner(String name, List<String> commandPrefix) {
        this.name = name;
        this.commandPrefix = commandPrefix;
    }

    @Override
    protected List<String> buildCommand(List<String> command) {
        LinkedList<String> fullCommand = new LinkedList<>(commandPrefix);
        fullCommand.addAll(command);
        return fullCommand;
    }

    @Override
    protected void acceptOutput(String line) {
        System.out.format("[%s:output] %s%n", name, line);
    }
}
