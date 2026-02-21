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
 * Represents a single, executable operation within the change engine. Commands are the fundamental
 * units of work and are processed by a chain of {@link CommandInterceptor}s and a final {@link
 * CommandInvoker}.
 *
 * <p>Implementations of this interface define a specific task, such as applying a change file or
 * validating the database state. They use the provided {@link CommandContext} to interact with the
 * engine's services and to exchange data with other components in the execution chain.
 *
 * @param <R> The type of result returned by the command's execution.
 * @see CommandContext
 * @see CommandInterceptor
 * @see CommandInvoker
 */
public interface Command<R> {

    /**
     * Executes the command's logic using the provided {@link CommandContext}.
     *
     * <p>The context serves as both a dependency injection container, providing access to core
     * engine services, and as a mechanism for passing data between {@link CommandInterceptor}s and
     * the command itself.
     *
     * @param context The execution context, which contains all necessary services and state.
     * @return The result of the command's execution.
     * @throws InterruptedException if the command execution is interrupted.
     */
    R execute(CommandContext context) throws InterruptedException;
}
