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

package io.github.totalschema.connector.shell.spi;

import io.github.totalschema.config.Configuration;
import io.github.totalschema.connector.shell.impl.DefaultShellScriptRunnerFactory;
import io.github.totalschema.spi.ServiceLoaderFactory;

/**
 * SPI for creating {@link ShellScriptRunner} instances for the local machine.
 *
 * <p>Implementations are discovered via Java {@link java.util.ServiceLoader}. If no implementation
 * is registered, {@link DefaultShellScriptRunnerFactory} is used as the built-in fallback.
 *
 * <p>This extension point allows integrators to replace the default process-based runner with an
 * alternative implementation (e.g. a sandboxed executor or a test double).
 *
 * <p><b>Registration:</b> add the fully-qualified class name to {@code
 * META-INF/services/io.github.totalschema.connector.shell.spi.ShellScriptRunnerFactory}.
 */
public interface ShellScriptRunnerFactory {

    /**
     * Returns the active {@link ShellScriptRunnerFactory} instance.
     *
     * <p>Queries the {@link java.util.ServiceLoader} for a registered implementation. If none is
     * found, falls back to {@link DefaultShellScriptRunnerFactory}.
     *
     * @return the active factory; never {@code null}
     */
    static ShellScriptRunnerFactory getInstance() {
        return ServiceLoaderFactory.getSingleService(ShellScriptRunnerFactory.class)
                .orElseGet(DefaultShellScriptRunnerFactory::new);
    }

    /**
     * Returns a {@link ShellScriptRunner} appropriate for the given script file.
     *
     * <p>Implementations are expected to inspect {@code fileName} (typically its extension) and the
     * current OS, then return a runner configured for that combination. For example, the built-in
     * implementation returns:
     *
     * <ul>
     *   <li>a PowerShell runner for {@code .ps1} files (on any OS)
     *   <li>a {@code cmd.exe /c} runner on Windows (for all non-{@code .ps1} scripts)
     *   <li>a {@code sh} runner on Unix / macOS (for all non-{@code .ps1} scripts)
     * </ul>
     *
     * <p>When {@code start.command} is present in {@code configuration}, implementations should use
     * it as the interpreter prefix unconditionally, bypassing all extension and OS detection.
     *
     * @param name the connector name; used for logging and diagnostics
     * @param configuration the connector configuration block; may contain a {@code start.command}
     *     override that takes precedence over any auto-detection
     * @param fileName the name of the script file about to be executed (e.g. {@code
     *     "0001.setup.apply.myshell.sh"}); used to determine the correct interpreter
     * @return a ready-to-use {@link ShellScriptRunner} for the given file; never {@code null}
     */
    ShellScriptRunner getRunner(String name, Configuration configuration, String fileName);
}
