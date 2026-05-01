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

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Runs scripts via {@code sh}, the standard POSIX shell available on Unix and macOS.
 *
 * <p>The script path is passed as a file argument ({@code sh <path>}), not via {@code -c}, so no
 * execute-bit is required on the script file.
 */
final class ShScriptRunner extends GenericShellScriptRunner {

    private static final List<String> COMMAND = List.of("sh");

    ShScriptRunner(String name, Map<String, String> environmentVariables) {
        super(name, COMMAND, environmentVariables);
    }

    /**
     * Creates the temporary probe script and marks it executable, as required by POSIX shells when
     * the script is invoked directly without an explicit interpreter argument.
     */
    @Override
    protected Path createTempScript(String suffix, String content) throws IOException {
        Path script = super.createTempScript(suffix, content);
        boolean couldSetExecutable = script.toFile().setExecutable(true);
        if (couldSetExecutable) {
            throw new IllegalStateException("Failed to set execute permission on probe script: " + script);
        }
        return script;
    }

    @Override
    public void checkReady() throws InterruptedException {
        executeProbeScript("sh", "#!/bin/sh\n");
    }
}
