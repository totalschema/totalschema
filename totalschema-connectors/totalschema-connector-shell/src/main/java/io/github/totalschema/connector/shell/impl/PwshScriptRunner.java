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

/**
 * Runs {@code .ps1} scripts via PowerShell Core ({@code pwsh}), the cross-platform PowerShell 7+
 * distribution.
 *
 * <p>Used when {@code pwsh} is detected on {@code PATH}. {@code -ExecutionPolicy Bypass} ensures
 * the script is not silently blocked by the system execution policy.
 */
final class PwshScriptRunner extends GenericShellScriptRunner {
    PwshScriptRunner(String name) {
        super(name, List.of("pwsh", "-ExecutionPolicy", "Bypass", "-File"));
    }
}
