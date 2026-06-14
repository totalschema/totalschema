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

package io.github.totalschema.connector.common.process;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Top-level abstraction for executing external OS processes.
 *
 * <p>This interface defines the contract for running commands in a subprocess with configurable
 * working directory and environment variables. Implementations handle the mechanics of process
 * creation, output streaming, and exit code validation.
 *
 * <p><b>Implementations:</b>
 *
 * <ul>
 *   <li>{@link GenericProcessRunner} — generic implementation suitable for most connector types
 *   <li>Connector-specific implementations — extend the generic implementation to add
 *       domain-specific behavior (e.g., Python's {@code PYTHONPATH} prepending)
 * </ul>
 *
 * <p><b>Environment Variable Layering:</b>
 *
 * <p>Implementations typically apply environment variables in this order:
 *
 * <ol>
 *   <li>Inherited environment from the parent JVM process
 *   <li>Connector-level variables configured at construction time
 *   <li>Per-invocation variables passed to {@link #run(List, Path, Map)}
 * </ol>
 *
 * <p><b>Exception Handling:</b>
 *
 * <p>Implementations should throw {@link RuntimeException} if:
 *
 * <ul>
 *   <li>The process cannot be started (e.g., executable not found, insufficient permissions)
 *   <li>The process exits with a non-zero status code
 *   <li>An I/O error occurs while reading process output
 * </ul>
 *
 * <p><b>Thread Interruption:</b>
 *
 * <p>Both methods declare {@code throws InterruptedException} to support graceful shutdown when the
 * calling thread is interrupted while waiting for the process to complete.
 *
 * @see GenericProcessRunner
 */
public interface ProcessRunner {

    /**
     * Runs the given command in the specified working directory with no additional environment
     * variables.
     *
     * <p>This is a convenience method equivalent to calling {@link #run(List, Path, Map)} with an
     * empty map for {@code extraEnvVars}.
     *
     * @param command the command tokens to execute (e.g., {@code ["python3", "script.py"]})
     * @param workingDirectory the directory from which the process is started
     * @throws InterruptedException if the current thread is interrupted while waiting for the
     *     process to finish
     * @throws RuntimeException if the process exits with a non-zero status, or if the process
     *     cannot be started
     */
    void run(List<String> command, Path workingDirectory) throws InterruptedException;

    /**
     * Runs the given command in the specified working directory, merging {@code extraEnvVars} into
     * the process environment.
     *
     * <p>The extra environment variables are layered on top of any base environment variables
     * configured at construction time. The specific merge strategy (overlay vs. prepend for
     * path-like variables) is implementation-defined.
     *
     * @param command the command tokens to execute (e.g., {@code ["python3", "script.py"]})
     * @param workingDirectory the directory from which the process is started
     * @param extraEnvVars additional environment variables applied per invocation; may be {@code
     *     null} or empty depending on implementation
     * @throws InterruptedException if the current thread is interrupted while waiting for the
     *     process to finish
     * @throws RuntimeException if the process exits with a non-zero status, or if the process
     *     cannot be started
     */
    void run(List<String> command, Path workingDirectory, Map<String, String> extraEnvVars)
            throws InterruptedException;
}
