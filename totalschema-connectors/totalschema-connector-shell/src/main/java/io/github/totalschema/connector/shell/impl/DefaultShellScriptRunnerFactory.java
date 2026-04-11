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

import io.github.totalschema.config.Configuration;
import io.github.totalschema.connector.shell.spi.ShellScriptRunner;
import io.github.totalschema.connector.shell.spi.ShellScriptRunnerFactory;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@link ShellScriptRunnerFactory}.
 *
 * <p>Selects and creates the appropriate {@link ShellScriptRunner} for a given script file based on
 * its extension and the current OS. All interpreter-detection logic lives here, keeping individual
 * runner implementations simple and focused.
 *
 * <h2>Selection rules (in priority order)</h2>
 *
 * <ol>
 *   <li><b>{@code start.command} configured</b> — the comma-separated value in the connector
 *       configuration is used as the interpreter prefix for every script, bypassing all
 *       auto-detection.
 *   <li><b>{@code .sh} extension</b> — always {@code sh}, regardless of the host OS.
 *   <li><b>{@code .ps1} extension</b> — {@code pwsh -ExecutionPolicy Bypass -File} when {@code
 *       pwsh} (PowerShell 7+) is on {@code PATH}; {@code powershell.exe -ExecutionPolicy Bypass
 *       -File} (Windows PowerShell 5.x) when {@code pwsh} is absent but the host is Windows; {@link
 *       IllegalStateException} otherwise.
 *   <li><b>{@code .bat} / {@code .cmd} extension</b> — {@code cmd.exe /c} on Windows; {@link
 *       IllegalStateException} on non-Windows.
 *   <li><b>Windows, other extensions</b> — {@code cmd.exe /c}.
 *   <li><b>Unix / macOS, other extensions</b> — {@code sh} (no {@code -c}; the path is passed as a
 *       file argument, so no execute-bit is required on the script).
 * </ol>
 *
 * <p>This is the built-in fallback used when no custom {@link ShellScriptRunnerFactory} is
 * registered via Java {@link java.util.ServiceLoader}.
 */
public class DefaultShellScriptRunnerFactory implements ShellScriptRunnerFactory {

    private static final Logger log =
            LoggerFactory.getLogger(DefaultShellScriptRunnerFactory.class);

    private final RuntimeInformation runtimeInformation;

    /**
     * Creates a new {@code DefaultShellScriptRunnerFactory} that reads the real OS environment.
     *
     * <p>Uses {@link DefaultRuntimeInformation} to probe {@code os.name} and {@code PATH}.
     */
    public DefaultShellScriptRunnerFactory() {
        this(new DefaultRuntimeInformation());
    }

    /**
     * Creates a new {@code DefaultShellScriptRunnerFactory} with an explicit {@link
     * RuntimeInformation}.
     *
     * <p><b>Package-visible for testing only.</b> Pass a mock or stub {@link RuntimeInformation} to
     * exercise interpreter-selection logic without depending on the host OS or its {@code PATH}.
     *
     * @param runtimeInformation the runtime information provider; must not be {@code null}
     */
    DefaultShellScriptRunnerFactory(RuntimeInformation runtimeInformation) {
        this.runtimeInformation = runtimeInformation;
    }

    /**
     * Returns a {@link ShellScriptRunner} for the given script file.
     *
     * <p>Specifically, returns:
     *
     * <ul>
     *   <li>a {@link GenericShellScriptRunner} built from the {@code start.command} token list, if
     *       that key is present in {@code configuration}
     *   <li>a {@link ShScriptRunner} for {@code .sh} files (on any OS)
     *   <li>a {@link PwshScriptRunner} for {@code .ps1} files when {@code pwsh} is on {@code PATH}
     *   <li>a {@link WindowsPowerShellScriptRunner} for {@code .ps1} files on Windows when {@code
     *       pwsh} is not available
     *   <li>a {@link CmdExeScriptRunner} for {@code .bat} / {@code .cmd} files on Windows, and for
     *       all other extensions on Windows
     *   <li>a {@link ShScriptRunner} on Unix / macOS for all other extensions
     * </ul>
     *
     * @param name the connector name; forwarded to the runner for logging
     * @param configuration the connector configuration; may contain {@code start.command}
     * @param fileName the script file name; its extension and the current OS drive selection
     * @return a new {@link GenericShellScriptRunner} instance; never {@code null}
     * @throws IllegalStateException if a {@code .ps1} script is requested on a non-Windows OS
     *     without {@code pwsh} on {@code PATH}, or if a {@code .bat} / {@code .cmd} script is
     *     requested on a non-Windows OS
     */
    @Override
    public ShellScriptRunner getRunner(String name, Configuration configuration, String fileName) {
        List<String> configured = configuration.getList("start.command").orElse(null);

        ShellScriptRunner shellScriptRunner;

        if (configured != null) {
            log.debug(
                    "Creating GenericShellScriptRunner for {} due to user configuration: {}",
                    fileName,
                    configured);

            shellScriptRunner = new GenericShellScriptRunner(name, configured);

        } else if (isFileNameExtension(fileName, "sh")) {
            log.debug("Creating ShScriptRunner due to file name extension: {}", fileName);
            shellScriptRunner = getShShellScriptRunner(name);

        } else if (isFileNameExtension(fileName, "ps1")) {
            log.debug("Creating PowerShell Script Runner due to file name extension: {}", fileName);
            shellScriptRunner = getPowerShellRunner(name, fileName);

        } else if (isFileNameExtension(fileName, "cmd") || isFileNameExtension(fileName, "bat")) {
            log.debug(
                    "Creating Windows CMD Script Runner due to file name extension: {}", fileName);
            shellScriptRunner = getWindowsCmdExeScriptRunner(name, fileName);

        } else {
            // File name extension does not imply the shell to use. This could be a simple ".txt",
            // or some exotic scripting language the runtime can start via the shell.
            if (!runtimeInformation.isWindows()) {
                log.debug(
                        "Creating ShScriptRunner for {} as there is no more specific rule",
                        fileName);
                shellScriptRunner = getShShellScriptRunner(name);

            } else {
                log.debug(
                        "Creating Windows CMD Script Runner for {} as there is no more specific"
                                + " rule",
                        fileName);
                shellScriptRunner = getWindowsCmdExeScriptRunner(name, fileName);
            }
        }

        log.debug("Created ShellScriptRunner '{}' for: {}", shellScriptRunner, fileName);
        return shellScriptRunner;
    }

    private static ShScriptRunner getShShellScriptRunner(String name) {
        return new ShScriptRunner(name);
    }

    private GenericShellScriptRunner getPowerShellRunner(String name, String fileName) {
        if (runtimeInformation.isPwshAvailable()) {
            return new PwshScriptRunner(name);
        } else {
            if (runtimeInformation.isWindows()) {
                return new WindowsPowerShellScriptRunner(name);
            } else {
                throw new IllegalStateException(
                        String.format(
                                "Cannot run PowerShell script (%s) on non-Windows OS when"
                                        + " PowerShell Core is not detected on PATH.",
                                fileName));
            }
        }
    }

    private CmdExeScriptRunner getWindowsCmdExeScriptRunner(String name, String fileName) {
        if (runtimeInformation.isWindows()) {
            return new CmdExeScriptRunner(name);
        } else {
            throw new IllegalStateException(
                    String.format("Cannot run CMD script (%s) on non-Windows OS.", fileName));
        }
    }

    private static boolean isFileNameExtension(String fileName, String extension) {
        return fileName.toLowerCase(Locale.ROOT).endsWith(String.format(".%s", extension));
    }
}
