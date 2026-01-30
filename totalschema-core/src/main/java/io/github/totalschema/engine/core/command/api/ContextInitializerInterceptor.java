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

import io.github.totalschema.concurrent.LockTemplate;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Abstract interceptor that initializes the command context before execution.
 *
 * <p>This interceptor uses a lock to ensure thread-safe context initialization.
 */
public abstract class ContextInitializerInterceptor extends CommandInterceptor {

    private static final int TIMEOUT = 30;
    private static final TimeUnit TIMEOUT_UNIT = TimeUnit.SECONDS;

    private final LockTemplate lockTemplate =
            new LockTemplate(TIMEOUT, TIMEOUT_UNIT, new ReentrantLock());

    /**
     * Constructs a ContextInitializerInterceptor with the next executor.
     *
     * @param next the next executor in the chain
     */
    public ContextInitializerInterceptor(CommandExecutor next) {
        super(next);
    }

    @Override
    public final <R> R intercept(CommandContext context, Command<R> command, CommandExecutor next) {

        try {
            lockTemplate.withTryLock(() -> initializeFromContext(context));

            log.trace("Before propagating {}; current context: {}", command, context);

            R returnValue = next.execute(context, command);

            log.trace(
                    "After propagating {}; current context: {}, return value: {}",
                    command,
                    context,
                    returnValue);

            return returnValue;

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            throw new RuntimeException("interrupted", e);
        }
    }

    /**
     * Initializes resources from the command context.
     *
     * @param context the command context to initialize from
     */
    protected abstract void initializeFromContext(CommandContext context);
}
