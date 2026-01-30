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

import io.github.totalschema.engine.core.command.api.Command;

/**
 * Abstract base class for all manager implementations. Provides access to the ChangeEngine's
 * command execution mechanism.
 */
abstract class AbstractManager {

    private final DefaultChangeEngine changeEngine;

    protected AbstractManager(DefaultChangeEngine changeEngine) {
        this.changeEngine = changeEngine;
    }

    /**
     * Executes a command through the ChangeEngine's command execution mechanism.
     *
     * @param command the command to execute
     * @param <R> the return type of the command
     * @return the result of the command execution
     */
    protected <R> R executeCommand(Command<R> command) {
        return changeEngine.executeCommand(command);
    }
}
