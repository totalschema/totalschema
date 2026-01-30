/*
 * totalschema: tool for managing database versioning and schema changes with ease.
 * Copyright (C) 2025 totalschema development team
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

package io.github.totalschema.engine.api;

import io.github.totalschema.config.environment.Environment;
import java.util.List;
import java.util.Map;

/**
 * API for managing environments and variables in totalschema. This interface provides operations
 * for querying available environments and their associated variables.
 */
public interface EnvironmentManager {

    /**
     * Returns all available environments defined in configuration.
     *
     * @return list of environments (e.g., DEV, QA, PROD)
     */
    List<Environment> getEnvironments();

    /**
     * Returns all variables for a specific environment, including global variables and
     * environment-specific overrides.
     *
     * @param environment the environment name
     * @return map of variable names to values
     */
    Map<String, String> getVariables(String environment);
}
