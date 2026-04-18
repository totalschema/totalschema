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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default {@link PythonProcessRunner} that launches OS processes via {@link ProcessBuilder}.
 *
 * <p>Standard output and standard error are merged into a single stream ({@code
 * redirectErrorStream(true)}) and logged line-by-line at {@code INFO} level, preserving the
 * chronological order of all process output. A non-zero exit code results in a {@link
 * RuntimeException}.
 */
final class DefaultPythonProcessRunner implements PythonProcessRunner {

    private static final Logger log = LoggerFactory.getLogger(DefaultPythonProcessRunner.class);

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
    DefaultPythonProcessRunner(String connectorName, Map<String, String> environmentVariables) {
        this.connectorName = Objects.requireNonNull(connectorName, "connectorName is null");
        this.environmentVariables =
                environmentVariables != null ? Map.copyOf(environmentVariables) : null;
    }

    @Override
    public void run(List<String> command, Path workingDirectory, Map<String, String> extraEnvVars)
            throws InterruptedException {

        log.info("[{}] Executing: {}", connectorName, command);

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(workingDirectory.toFile());
        pb.redirectErrorStream(true);

        Map<String, String> effectiveEnvironment =
                getEffectiveEnvironment(pb.environment(), extraEnvVars);

        log.debug("[{}] effective Environment variables: {}", connectorName, effectiveEnvironment);
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
     * <p>Layers are applied in this order (later layers win for plain overrides):
     *
     * <ol>
     *   <li>{@code inherited} — the environment inherited from the parent JVM process (supplied by
     *       {@link ProcessBuilder#environment()} before any modifications).
     *   <li>{@link #environmentVariables} — connector-level variables configured at construction
     *       time (e.g. {@code environmentVariables} in {@code totalschema.yml}).
     *   <li>{@code extraEnvVars} — per-invocation variables (e.g. the SDK temp-dir {@code
     *       PYTHONPATH}). For the {@code PYTHONPATH} key the extra value is <em>prepended</em> to
     *       whatever {@code PYTHONPATH} is already in the accumulated environment; all other keys
     *       are overlaid verbatim.
     * </ol>
     *
     * @param inherited the base environment from {@link ProcessBuilder#environment()}
     * @param extraEnvVars per-invocation additions; may be {@code null} or empty
     * @return a new map representing the complete effective environment
     */
    private Map<String, String> getEffectiveEnvironment(
            Map<String, String> inherited, Map<String, String> extraEnvVars) {

        final Map<String, String> effectiveEnvironment = new java.util.LinkedHashMap<>(inherited);

        if (environmentVariables != null) {
            effectiveEnvironment.putAll(environmentVariables);
        }

        if (extraEnvVars != null) {
            for (Map.Entry<String, String> entry : extraEnvVars.entrySet()) {
                final String key = entry.getKey();
                final String value = entry.getValue();
                if (PythonConnector.PYTHONPATH_VARIABLE_NAME.equals(key)) {
                    // Prepend so the SDK temp dir takes priority over any existing PYTHONPATH
                    effectiveEnvironment.merge(
                            key, value, (existing, extra) -> extra + File.pathSeparator + existing);
                } else {
                    effectiveEnvironment.put(key, value);
                }
            }
        }

        return effectiveEnvironment;
    }

    @Override
    public String toString() {
        return "DefaultPythonProcessRunner{"
                + "connectorName='"
                + connectorName
                + '\''
                + ", environmentVariables="
                + environmentVariables
                + '}';
    }
}
