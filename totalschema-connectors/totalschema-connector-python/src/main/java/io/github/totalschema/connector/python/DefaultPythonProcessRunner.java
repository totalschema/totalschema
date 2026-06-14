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

import io.github.totalschema.connector.common.process.GenericProcessRunner;
import java.io.File;
import java.util.Map;

/**
 * Default implementation of {@link PythonProcessRunner} that launches Python processes via {@link
 * ProcessBuilder}.
 *
 * <p>Extends {@link GenericProcessRunner} to leverage generic process execution infrastructure,
 * while adding Python-specific behavior required by the {@link PythonProcessRunner} contract:
 *
 * <ul>
 *   <li>{@code PYTHONPATH} merging — prepends new paths instead of overwriting, preserving existing
 *       module search paths
 * </ul>
 *
 * @see PythonProcessRunner
 * @see GenericProcessRunner
 */
final class DefaultPythonProcessRunner extends GenericProcessRunner implements PythonProcessRunner {

    /**
     * Creates a runner with extra environment variables merged into the child process's
     * environment.
     *
     * @param connectorName the connector name used to prefix log messages
     * @param environmentVariables extra variables, or {@code null} if the parent environment should
     *     be inherited unchanged
     */
    DefaultPythonProcessRunner(String connectorName, Map<String, String> environmentVariables) {
        super(connectorName, environmentVariables);
    }

    /**
     * Overrides the default merge strategy to implement Python-specific {@code PYTHONPATH}
     * handling.
     *
     * <p>For the {@code PYTHONPATH} key, the new value is <em>prepended</em> to the existing value
     * (separated by the platform path separator) instead of overwriting it. This ensures that
     * injected paths (e.g., the SDK temp directory) take priority while preserving any existing
     * module search paths. All other keys are overlaid verbatim.
     *
     * @param accumulated the environment accumulated so far (inherited + connector-level)
     * @param extraEnvVars the per-invocation variables to merge
     * @return a new map with Python-specific {@code PYTHONPATH} prepending applied
     */
    @Override
    protected Map<String, String> mergeEnvironmentVariables(
            Map<String, String> accumulated, Map<String, String> extraEnvVars) {
        final Map<String, String> result = new java.util.LinkedHashMap<>(accumulated);

        for (Map.Entry<String, String> entry : extraEnvVars.entrySet()) {
            final String key = entry.getKey();
            final String value = entry.getValue();
            if (PythonConnector.PYTHONPATH_VARIABLE_NAME.equals(key)) {
                // Prepend so the SDK temp dir takes priority over any existing PYTHONPATH
                result.merge(key, value, this::prependPath);
            } else {
                result.put(key, value);
            }
        }

        return result;
    }

    /**
     * Prepends a new path component to an existing path, using the platform-specific path
     * separator.
     *
     * <p>This merge function ensures that the new path component takes priority by appearing first
     * in the resulting path string.
     *
     * @param existingPath the current path value (may contain multiple components)
     * @param newPath the new path component to prepend
     * @return the merged path with {@code newPath} prepended to {@code existingPath}
     */
    private String prependPath(String existingPath, String newPath) {
        return newPath + File.pathSeparator + existingPath;
    }

    @Override
    public String toString() {
        return "DefaultPythonProcessRunner{"
                + "connectorName='"
                + getConnectorName()
                + '\''
                + ", environmentVariables="
                + getEnvironmentVariables()
                + '}';
    }
}
