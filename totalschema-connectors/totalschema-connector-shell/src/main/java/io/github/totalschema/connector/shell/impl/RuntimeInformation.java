/*
 * totalschema: tool for managing database versioning and schema changes with ease.
 * Copyright (C) 2026 totalschema development team
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

/**
 * Abstracts OS- and environment-specific checks used by {@link DefaultShellScriptRunnerFactory} to
 * select the right interpreter prefix.
 *
 * <p>The real implementation ({@link DefaultRuntimeInformation}) reads {@code os.name} and probes
 * {@code PATH}. Tests supply a mock or a simple stub so that every selection branch can be
 * exercised independently of the host machine.
 */
interface RuntimeInformation {

    /**
     * Returns {@code true} when the current process is running on a Windows operating system.
     *
     * @return {@code true} on Windows; {@code false} on Unix / macOS
     */
    boolean isWindows();

    /**
     * Returns {@code true} when PowerShell Core ({@code pwsh}) is reachable on {@code PATH}.
     *
     * <p>On Windows the lookup file is {@code pwsh.exe}; elsewhere it is {@code pwsh}.
     *
     * @return {@code true} if {@code pwsh} is executable in any {@code PATH} directory
     */
    boolean isPwshAvailable();
}
