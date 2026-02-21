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

import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An abstract base class for implementing the interceptor pattern around {@link Command} execution.
 * Interceptors form a chain of responsibility, where each can examine, modify, or augment the
 * command execution process.
 *
 * <p>This pattern is central to the engine's architecture, enabling cross-cutting concerns such as
 * logging, transaction management, and security to be applied transparently. Each interceptor can
 * add values to the {@link CommandContext}, which are then available to subsequent interceptors and
 * the final command.
 *
 * <p>Subclasses must implement the {@link #intercept(CommandContext, Command, CommandExecutor)}
 * method to define their specific logic.
 *
 * @see Command
 * @see CommandContext
 * @see CommandInvoker
 */
public abstract class CommandInterceptor extends CommandExecutor {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    private final CommandExecutor next;

    /**
     * Constructs a {@code CommandInterceptor} that forms part of a processing chain.
     *
     * @param next The next {@link CommandExecutor} in the chain, which could be another interceptor
     *     or the final {@link CommandInvoker}. Must not be {@code null}.
     * @throws NullPointerException if {@code next} is {@code null}.
     */
    public CommandInterceptor(CommandExecutor next) {
        this.next = Objects.requireNonNull(next, "Argument next cannot be null");
    }

    @Override
    public final <R> R execute(CommandContext context, Command<R> command)
            throws InterruptedException {
        log.trace("Intercepted: {}, next: {}", command, next);
        R returnValue = intercept(context, command, next);
        log.trace("return value: {}", returnValue);
        return returnValue;
    }

    /**
     * The core method for implementing interceptor logic. It is called when a {@link Command} is
     * executed and allows the interceptor to perform actions before or after delegating to the next
     * executor in the chain.
     *
     * <p>Implementations can:
     *
     * <ul>
     *   <li>Modify the {@link CommandContext} by adding or updating values.
     *   <li>Perform pre-processing before calling {@code next.execute(context, command)}.
     *   <li>Perform post-processing after the next executor returns.
     *   <li>Short-circuit the execution chain by not calling the next executor at all.
     * </ul>
     *
     * @param <R> The return type of the command.
     * @param context The execution context, which can be modified by the interceptor.
     * @param command The command being executed.
     * @param next The next executor in the chain to which the execution should be delegated.
     * @return The result of the command's execution.
     * @throws InterruptedException if the execution is interrupted.
     */
    public abstract <R> R intercept(
            CommandContext context, Command<R> command, CommandExecutor next)
            throws InterruptedException;
}
