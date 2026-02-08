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

package io.github.totalschema.engine.core.command.api;

/**
 * Abstract base class for executing commands.
 *
 * <p>Provides a template for command execution with optional context.
 */
public abstract class CommandExecutor {

    /**
     * Executes a command with the given context.
     *
     * @param <R> the return type of the command
     * @param context the execution context
     * @param command the command to execute
     * @return the result of command execution
     * @throws InterruptedException if the execution is interrupted
     */
    public abstract <R> R execute(CommandContext context, Command<R> command)
            throws InterruptedException;
}
