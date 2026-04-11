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
import java.util.LinkedList;
import java.util.List;

/**
 * Base class for {@link ShellScriptRunner} implementations that launch OS processes.
 *
 * <p>The interpreter prefix is supplied at construction time and prepended to the script path on
 * every invocation, producing the full OS command:
 *
 * <pre>{@code <commandPrefix…> <scriptAbsolutePath>}</pre>
 *
 * For example, a prefix of {@code ["sh"]} and a script at {@code /opt/changes/0001.setup.sh} will
 * execute:
 *
 * <pre>{@code sh /opt/changes/0001.setup.sh}</pre>
 *
 * <p>Subclasses in this package hard-code a well-known prefix by passing it to the constructor:
 *
 * <ul>
 *   <li>{@link ShScriptRunner} — {@code sh}
 *   <li>{@link CmdExeScriptRunner} — {@code cmd.exe /c}
 *   <li>{@link PwshScriptRunner} — {@code pwsh -ExecutionPolicy Bypass -File}
 *   <li>{@link WindowsPowerShellScriptRunner} — {@code powershell.exe -ExecutionPolicy Bypass
 *       -File}
 * </ul>
 *
 * <p>For connectors with a user-configured {@code start.command}, {@link
 * DefaultShellScriptRunnerFactory} instantiates this class directly with the parsed token list,
 * bypassing the subclasses entirely.
 *
 * @see DefaultShellScriptRunnerFactory
 */
class GenericShellScriptRunner extends ExternalProcessTerminalSession implements ShellScriptRunner {

    private final String name;
    private final List<String> prefix;

    /**
     * Constructs a runner for the given connector with the supplied interpreter prefix.
     *
     * @param name the connector name; used for logging and diagnostics
     * @param prefix the interpreter tokens prepended to every command; must not be {@code null}
     */
    GenericShellScriptRunner(String name, List<String> prefix) {
        this.name = name;
        this.prefix = List.copyOf(prefix);
    }

    /**
     * Returns the interpreter prefix prepended to every command.
     *
     * <p><b>Package-visible for testing.</b> Production code must not depend on this method.
     *
     * @return the command prefix tokens; never {@code null}
     */
    List<String> getCommandPrefix() {
        return prefix;
    }

    @Override
    protected final List<String> buildActualCommand(List<String> command) {
        LinkedList<String> fullCommand = new LinkedList<>(getCommandPrefix());
        fullCommand.addAll(command);
        return fullCommand;
    }

    @Override
    protected void acceptOutput(String line) {
        System.out.format("[%s:output] %s%n", name, line);
    }

    @Override
    public String toString() {
        return getClass().getName() + "{" + "name='" + name + '\'' + ", prefix=" + prefix + '}';
    }
}
