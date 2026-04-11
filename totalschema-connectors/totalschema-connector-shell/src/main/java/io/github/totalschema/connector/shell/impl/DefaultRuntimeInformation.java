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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Locale;

/**
 * Production implementation of {@link RuntimeInformation} that reads from the real OS environment.
 *
 * <p>This class is intentionally package-private: it is an implementation detail of {@link
 * DefaultShellScriptRunnerFactory} and is not part of the public API. Tests inject a mock {@link
 * RuntimeInformation} instead of using this class directly.
 */
final class DefaultRuntimeInformation implements RuntimeInformation {

    DefaultRuntimeInformation() {}

    /**
     * {@inheritDoc}
     *
     * <p>Checks {@code os.name} system property and considers anything starting with {@code
     * "windows"} (case-insensitive) to be Windows.
     */
    @Override
    public boolean isWindows() {
        return System.getProperty("os.name").toLowerCase(Locale.ROOT).startsWith("windows");
    }

    /**
     * {@inheritDoc}
     *
     * <p>Splits {@code PATH} on the OS-specific path separator ({@code ;} on Windows, {@code :}
     * elsewhere) and checks whether {@code pwsh} (or {@code pwsh.exe} on Windows) is an executable
     * file in any of the listed directories.
     */
    @Override
    public boolean isPwshAvailable() {
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
}
