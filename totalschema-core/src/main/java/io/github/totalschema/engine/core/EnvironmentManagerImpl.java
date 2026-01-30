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

package io.github.totalschema.engine.core;

import io.github.totalschema.config.environment.Environment;
import io.github.totalschema.engine.api.EnvironmentManager;
import io.github.totalschema.engine.core.command.impl.ListEnvironmentsCommand;
import io.github.totalschema.engine.core.command.impl.ListVariablesCommand;
import java.util.List;
import java.util.Map;

/**
 * Default implementation of EnvironmentManager that delegates operations to commands executed
 * through the ChangeEngine.
 */
public class EnvironmentManagerImpl extends AbstractManager implements EnvironmentManager {

    public EnvironmentManagerImpl(DefaultChangeEngine changeEngine) {
        super(changeEngine);
    }

    @Override
    public List<Environment> getEnvironments() {
        return executeCommand(new ListEnvironmentsCommand());
    }

    @Override
    public Map<String, String> getVariables(String environment) {
        return executeCommand(new ListVariablesCommand(environment));
    }
}
