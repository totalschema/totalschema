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

package io.github.totalschema.connector.python;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Abstraction over OS process execution used by {@link PythonConnector}.
 *
 * <p>Keeping this behind an interface allows unit tests to inject a mock, exercising all connector
 * logic without spawning real OS processes. The production implementation is {@link
 * DefaultPythonProcessRunner}.
 */
interface PythonProcessRunner {

    /**
     * Runs the given command in the specified working directory, streaming all output to the logger
     * prefixed with the connector name supplied at construction time.
     *
     * @param command the command tokens to execute (e.g. {@code ["python3", "/tmp/script.py"]})
     * @param workingDirectory the directory from which the process is started
     * @throws InterruptedException if the current thread is interrupted while waiting for the
     *     process to finish
     * @throws RuntimeException if the process exits with a non-zero status, or if the process
     *     cannot be started
     */
    default void run(List<String> command, Path workingDirectory) throws InterruptedException {
        run(command, workingDirectory, Collections.emptyMap());
    }

    /**
     * Runs the given command in the specified working directory, merging {@code extraEnvVars} into
     * the process environment on top of any base environment variables configured at construction
     * time.
     *
     * <p>The {@code PYTHONPATH} key receives special treatment: its value is <em>prepended</em> to
     * whatever {@code PYTHONPATH} is already present in the process environment (inherited from the
     * parent process plus any base {@code environmentVariables} configured on the connector),
     * separated by the platform path separator. All other keys in {@code extraEnvVars} are overlaid
     * verbatim.
     *
     * @param command the command tokens to execute (e.g. {@code ["python3", "/tmp/script.py"]})
     * @param workingDirectory the directory from which the process is started
     * @param extraEnvVars additional environment variables applied per invocation; must not be
     *     {@code null} (use an empty map when there are no extras)
     * @throws InterruptedException if the current thread is interrupted while waiting for the
     *     process to finish
     * @throws RuntimeException if the process exits with a non-zero status, or if the process
     *     cannot be started
     */
    void run(List<String> command, Path workingDirectory, Map<String, String> extraEnvVars)
            throws InterruptedException;
}
