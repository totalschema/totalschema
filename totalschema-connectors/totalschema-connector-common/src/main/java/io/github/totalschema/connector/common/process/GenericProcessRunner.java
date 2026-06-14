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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generic implementation of {@link ProcessRunner} for executing OS processes via {@link
 * ProcessBuilder}.
 *
 * <p>Provides complete process execution infrastructure including:
 *
 * <ul>
 *   <li>Launching processes with {@link ProcessBuilder}
 *   <li>Merging stdout and stderr into a single stream for logging
 *   <li>Line-by-line output logging at {@code INFO} level
 *   <li>Environment variable layering (inherited + connector-level + per-invocation)
 *   <li>Exit code validation (non-zero throws {@link RuntimeException})
 * </ul>
 *
 * <p>This class is fully functional and can be used directly without subclassing. Subclasses may
 * override {@link #mergeEnvironmentVariables(Map, Map)} to customize environment variable merging
 * behavior (e.g., prepending to {@code PATH}-like variables instead of overwriting).
 *
 * <p>See {@link ProcessRunner} for details on the environment variable layering strategy and
 * exception handling contract.
 */
public class GenericProcessRunner implements ProcessRunner {

    private static final Logger log = LoggerFactory.getLogger(GenericProcessRunner.class);

    private final String connectorName;
    private final Map<String, String> environmentVariables; // nullable — absent when not configured

    /**
     * Creates a runner with extra environment variables merged into the child process's
     * environment.
     *
     * @param connectorName the connector name used to prefix log messages
     * @param environmentVariables extra variables, or {@code null} if the parent environment should
     *     be inherited unchanged
     */
    public GenericProcessRunner(String connectorName, Map<String, String> environmentVariables) {
        this.connectorName = Objects.requireNonNull(connectorName, "connectorName is null");
        this.environmentVariables =
                environmentVariables != null ? Map.copyOf(environmentVariables) : null;
    }

    @Override
    public void run(List<String> command, Path workingDirectory) throws InterruptedException {
        run(command, workingDirectory, Collections.emptyMap());
    }

    /**
     * Runs the given command in the specified working directory, merging {@code extraEnvVars} into
     * the process environment.
     *
     * @param command the command tokens to execute
     * @param workingDirectory the directory from which the process is started
     * @param extraEnvVars additional environment variables applied per invocation; may be {@code
     *     null} or empty
     * @throws InterruptedException if the current thread is interrupted while waiting for the
     *     process to finish
     * @throws RuntimeException if the process exits with a non-zero status, or if the process
     *     cannot be started
     */
    public void run(List<String> command, Path workingDirectory, Map<String, String> extraEnvVars)
            throws InterruptedException {

        log.info("[{}] Executing: {}", connectorName, command);

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(workingDirectory.toFile());
        pb.redirectErrorStream(true);

        Map<String, String> effectiveEnvironment =
                buildEffectiveEnvironment(pb.environment(), extraEnvVars);

        log.debug("[{}] effective Environment variables: {}", connectorName, effectiveEnvironment);
        pb.environment().clear();
        pb.environment().putAll(effectiveEnvironment);

        Process process;
        try {
            process = pb.start();
        } catch (IOException e) {
            throw new RuntimeException("Failed to start process: " + command, e);
        }

        // Read stdout (stderr merged) synchronously. The loop completes when the process
        // exits and closes its output stream, so waitFor() returns immediately afterwards.
        try (BufferedReader reader =
                new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.info("[{}] {}", connectorName, line);
            }
        } catch (IOException e) {
            process.destroy();
            throw new RuntimeException(
                    "Failed reading process output for: " + String.join(" ", command), e);
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException(
                    "["
                            + connectorName
                            + "] Process exited with status "
                            + exitCode
                            + " for: "
                            + String.join(" ", command));
        }
    }

    /**
     * Builds the effective environment map for a process invocation.
     *
     * <p>Layers are applied in this order:
     *
     * <ol>
     *   <li>{@code inherited} — the environment inherited from the parent JVM process (supplied by
     *       {@link ProcessBuilder#environment()} before any modifications).
     *   <li>Connector-level variables configured at construction time.
     *   <li>{@code extraEnvVars} — per-invocation variables.
     * </ol>
     *
     * <p>The final merge of {@code extraEnvVars} is delegated to {@link
     * #mergeEnvironmentVariables(Map, Map)} to allow subclasses to customize the merge strategy
     * (for example, prepending path-like variables instead of overwriting them).
     *
     * @param inherited the base environment from {@link ProcessBuilder#environment()}
     * @param extraEnvVars per-invocation additions; may be {@code null} or empty
     * @return a new map representing the complete effective environment
     */
    private Map<String, String> buildEffectiveEnvironment(
            Map<String, String> inherited, Map<String, String> extraEnvVars) {

        final Map<String, String> accumulated = new java.util.LinkedHashMap<>(inherited);

        if (environmentVariables != null) {
            accumulated.putAll(environmentVariables);
        }

        if (extraEnvVars != null && !extraEnvVars.isEmpty()) {
            return mergeEnvironmentVariables(accumulated, extraEnvVars);
        }

        return accumulated;
    }

    /**
     * Merges per-invocation environment variables into the accumulated environment.
     *
     * <p>The default implementation performs a simple overlay (later values win). Subclasses may
     * override this to implement custom merge strategies for specific keys (e.g., prepending to
     * {@code PATH} or {@code PYTHONPATH} instead of overwriting).
     *
     * @param accumulated the environment accumulated so far (inherited + connector-level)
     * @param extraEnvVars the per-invocation variables to merge
     * @return a new map representing the final effective environment
     */
    protected Map<String, String> mergeEnvironmentVariables(
            Map<String, String> accumulated, Map<String, String> extraEnvVars) {
        final Map<String, String> result = new java.util.LinkedHashMap<>(accumulated);
        result.putAll(extraEnvVars);
        return result;
    }

    /**
     * Returns the connector name used for log message prefixes.
     *
     * @return the connector name
     */
    protected String getConnectorName() {
        return connectorName;
    }

    /**
     * Returns the connector-level environment variables configured at construction time, or {@code
     * null} if none were provided.
     *
     * @return the environment variables, or {@code null}
     */
    protected Map<String, String> getEnvironmentVariables() {
        return environmentVariables;
    }
}
