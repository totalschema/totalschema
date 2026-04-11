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
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;

/**
 * Default implementation of {@link ShellScriptRunnerFactory}.
 *
 * <p>Selects and creates the appropriate {@link ShellScriptRunner} for a given script file based on
 * its extension and the current OS. All interpreter-detection logic lives here, keeping individual
 * runner implementations simple and focused.
 *
 * <h3>Selection rules (in priority order)</h3>
 *
 * <ol>
 *   <li><b>{@code start.command} configured</b> — the comma-separated value in the connector
 *       configuration is used as the interpreter prefix for every script, bypassing all
 *       auto-detection.
 *   <li><b>{@code .ps1} extension</b> — {@code pwsh -ExecutionPolicy Bypass -File} is used if
 *       {@code pwsh} (PowerShell 7+) is found on {@code PATH}; otherwise falls back to {@code
 *       powershell.exe -ExecutionPolicy Bypass -File} (Windows PowerShell 5.x).
 *   <li><b>{@code .bat} / {@code .cmd} extension</b> — {@code cmd.exe /c}.
 *   <li><b>Windows, other extensions</b> — {@code cmd.exe /c}.
 *   <li><b>Unix / macOS, other extensions</b> — {@code sh} (no {@code -c}; the path is passed as a
 *       file argument, so no execute-bit is required on the script).
 * </ol>
 *
 * <p>This is the built-in fallback used when no custom {@link ShellScriptRunnerFactory} is
 * registered via Java {@link java.util.ServiceLoader}.
 */
public class DefaultShellScriptRunnerFactory implements ShellScriptRunnerFactory {

    /**
     * Returns a {@link DefaultShellScriptRunner} configured with the interpreter prefix appropriate
     * for {@code fileName}.
     *
     * @param name the connector name; forwarded to the runner for logging
     * @param configuration the connector configuration; may contain {@code start.command}
     * @param fileName the script file name; its extension drives interpreter selection
     * @return a new {@link DefaultShellScriptRunner}; never {@code null}
     */
    @Override
    public ShellScriptRunner getRunner(String name, Configuration configuration, String fileName) {
        List<String> prefix = resolvePrefix(configuration, fileName);
        return new DefaultShellScriptRunner(name, prefix);
    }

    // -------------------------------------------------------------------------
    // Prefix resolution
    // -------------------------------------------------------------------------

    private static List<String> resolvePrefix(Configuration configuration, String fileName) {
        List<String> configured = configuration.getList("start.command").orElse(null);
        if (configured != null) {
            return configured;
        }

        String lower = fileName.toLowerCase(Locale.ROOT);

        if (lower.endsWith(".ps1")) {
            return powerShellPrefix();
        }

        if (lower.endsWith(".bat") || lower.endsWith(".cmd") || isWindows()) {
            return List.of("cmd.exe", "/c");
        }

        // Unix / macOS: pass the script path as a file argument to sh.
        // No -c flag: "sh <file>" reads and executes the file directly,
        // which works even without the execute-bit set on the script.
        return List.of("sh");
    }

    /**
     * Returns the PowerShell interpreter prefix, preferring PowerShell Core ({@code pwsh},
     * cross-platform) when it is available on {@code PATH}, and falling back to Windows PowerShell
     * ({@code powershell.exe}) otherwise.
     *
     * <p>{@code -ExecutionPolicy Bypass} is always included so scripts are not silently blocked by
     * the system execution policy.
     */
    private static List<String> powerShellPrefix() {
        if (isPwshAvailable()) {
            return List.of("pwsh", "-ExecutionPolicy", "Bypass", "-File");
        }
        return List.of("powershell.exe", "-ExecutionPolicy", "Bypass", "-File");
    }

    /**
     * Checks whether PowerShell Core ({@code pwsh}) is reachable on the system {@code PATH}.
     *
     * <p>Each entry in {@code PATH} is checked using {@link File#pathSeparator}, which resolves to
     * the OS-specific separator ({@code ;} on Windows, {@code :} on Unix) without requiring
     * explicit platform detection.
     *
     * @return {@code true} if {@code pwsh} (or {@code pwsh.exe} on Windows) is found in any
     *     {@code PATH} directory; {@code false} otherwise
     */
    private static boolean isPwshAvailable() {
        String pathEnv = System.getenv("PATH");
        if (pathEnv == null) {
            return false;
        }

        String executableFile = isWindows() ? "pwsh.exe" : "pwsh";

        for (String dir : pathEnv.split(File.pathSeparator)) {
            if (Files.isExecutable(Paths.get(dir, executableFile))) {
                return true;
            }
        }
        return false;
    }

    /** Returns {@code true} when running on a Windows operating system. */
    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase(Locale.ROOT).startsWith("windows");
    }
}
