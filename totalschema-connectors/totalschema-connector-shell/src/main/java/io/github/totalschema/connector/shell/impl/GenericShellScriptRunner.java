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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
     * Constructs a runner for the given connector with the supplied interpreter prefix and
     * additional environment variables.
     *
     * @param name the connector name; used for logging and diagnostics
     * @param prefix the interpreter tokens prepended to every command; must not be {@code null}
     * @param environmentVariables extra variables merged into the child process's environment, or
     *     {@code null} if the parent environment should be inherited unchanged
     */
    GenericShellScriptRunner(
            String name, List<String> prefix, Map<String, String> environmentVariables) {
        super(environmentVariables);
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

    /**
     * Creates a temporary no-op script with the given {@code extension} and {@code content} via
     * {@link #createTempScript}, executes it through this runner, then deletes the file regardless
     * of outcome.
     *
     * <p>Intended to be called by {@link #checkReady()} implementations in subclasses — each
     * subclass passes the extension and content appropriate for its interpreter (e.g. {@code "sh"}
     * for POSIX shells, {@code "cmd"} for {@code cmd.exe}, {@code "ps1"} for PowerShell).
     *
     * @param extension the file extension for the temporary script, without the leading dot (e.g.
     *     {@code "sh"}, {@code "cmd"}, {@code "ps1"}); the dot is prepended automatically
     * @param content the script content; must be a valid no-op for the target interpreter
     * @throws InterruptedException if the probe execution is interrupted
     */
    protected final void executeProbeScript(String extension, String content)
            throws InterruptedException {
        Path tempScript = null;
        try {
            tempScript = createTempScript(extension, content);
            execute(List.of(tempScript.toAbsolutePath().toString()));
        } catch (IOException e) {
            throw new RuntimeException(
                    "Shell runner '" + name + "' readiness check failed: " + e.getMessage(), e);
        } finally {
            if (tempScript != null) {
                try {
                    Files.deleteIfExists(tempScript);
                } catch (IOException ignored) {
                    // best-effort cleanup
                }
            }
        }
    }

    /**
     * Creates a temporary script file with the given {@code extension} and {@code content}.
     *
     * <p>Subclasses may override this method to apply additional setup after the file is written —
     * for example, setting the executable bit for POSIX shells — by calling {@code super} and then
     * modifying the returned path.
     *
     * @param extension the file extension, without the leading dot (e.g. {@code "sh"}, {@code
     *     "cmd"}, {@code "ps1"}); the dot is prepended automatically
     * @param content the script content to write
     * @return the path of the newly created temporary file
     * @throws IOException if the file cannot be created or written
     */
    protected Path createTempScript(String extension, String content) throws IOException {
        Path tempScript = Files.createTempFile("totalschema-shell-check-", "." + extension);
        Files.writeString(tempScript, content);
        return tempScript;
    }

    /**
     * Verifies that this runner's interpreter is accessible by executing a minimal POSIX no-op
     * script.
     *
     * <p>This default implementation is a best-effort fallback for user-configured {@code
     * start.command} runners whose interpreter type is unknown. Named subclasses (e.g. {@link
     * ShScriptRunner}, {@link CmdExeScriptRunner}) override this method and call {@link
     * #executeProbeScript} with the extension and content that match their specific interpreter.
     *
     * @throws InterruptedException if the probe execution is interrupted
     */
    @Override
    public void checkReady() throws InterruptedException {
        executeProbeScript("sh", "#!/bin/sh\n");
    }

    @Override
    public String toString() {
        return getClass().getName() + "{" + "name='" + name + '\'' + ", prefix=" + prefix + '}';
    }
}
