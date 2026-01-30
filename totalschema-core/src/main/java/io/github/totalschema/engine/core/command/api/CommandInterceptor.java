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
 * Abstract base class for command interceptors in the chain of responsibility pattern.
 *
 * <p>Interceptors can perform actions before and after command execution, modify the context, or
 * prevent execution altogether.
 */
public abstract class CommandInterceptor extends CommandExecutor {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    private final CommandExecutor next;

    /**
     * Constructs a CommandInterceptor with the next executor in the chain.
     *
     * @param next the next executor in the chain
     * @throws NullPointerException if next is null
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
     * Intercepts command execution and may delegate to the next executor.
     *
     * @param <R> the return type of the command
     * @param context the execution context
     * @param command the command to execute
     * @param next the next executor in the chain
     * @return the result of command execution
     * @throws InterruptedException if the execution is interrupted
     */
    protected abstract <R> R intercept(
            CommandContext context, Command<R> command, CommandExecutor next)
            throws InterruptedException;
}
