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

import java.util.List;
import java.util.Map;

/**
 * Runs {@code .ps1} scripts via Windows PowerShell 5.x ({@code powershell.exe}).
 *
 * <p>Used as a fallback when PowerShell Core ({@code pwsh}) is not found on {@code PATH}. {@code
 * -ExecutionPolicy Bypass} ensures the script is not silently blocked by the system execution
 * policy.
 */
final class WindowsPowerShellScriptRunner extends GenericShellScriptRunner {

    private static final List<String> COMMAND =
            List.of("powershell.exe", "-ExecutionPolicy", "Bypass", "-File");

    WindowsPowerShellScriptRunner(String name, Map<String, String> environmentVariables) {
        super(name, COMMAND, environmentVariables);
    }
}
