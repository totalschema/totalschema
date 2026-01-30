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

import io.github.totalschema.engine.api.ChangeEngine;
import io.github.totalschema.engine.api.ChangeManager;
import io.github.totalschema.engine.api.EnvironmentManager;
import io.github.totalschema.engine.api.StateManager;
import io.github.totalschema.engine.api.ValidationManager;
import io.github.totalschema.engine.core.command.api.Command;
import io.github.totalschema.engine.core.command.api.CommandContext;
import io.github.totalschema.engine.core.command.api.CommandExecutor;
import io.github.totalschema.engine.core.command.api.CommandInvoker;
import io.github.totalschema.engine.core.event.ChangeEngineCloseEvent;
import io.github.totalschema.engine.core.event.EventDispatcher;

/**
 * Default implementation of ChangeEngine using the Command pattern. All operations are executed as
 * commands through a CommandExecutor. Manager interfaces are provided for organizing operations by
 * domain.
 */
public class DefaultChangeEngine implements ChangeEngine {

    private final EventDispatcher eventDispatcher = new EventDispatcher();

    private final CommandExecutor commandExecutor;

    private final CommandInvoker directCommandInvoker = new CommandInvoker();

    private final EnvironmentManager environmentManager;
    private final ChangeManager changeManager;
    private final StateManager stateManager;
    private final ValidationManager validationManager;

    private final ThreadLocal<CommandContext> contextHolder = new ThreadLocal<>();

    public DefaultChangeEngine(CommandExecutor commandExecutor) {
        this.commandExecutor = commandExecutor;
        this.environmentManager = new EnvironmentManagerImpl(this);
        this.changeManager = new ChangeManagerImpl(this);
        this.stateManager = new StateManagerImpl(this);
        this.validationManager = new ValidationManagerImpl(this);
    }

    @Override
    public EnvironmentManager getEnvironmentManager() {
        return environmentManager;
    }

    @Override
    public ChangeManager getChangeManager() {
        return changeManager;
    }

    @Override
    public StateManager getStateManager() {
        return stateManager;
    }

    @Override
    public ValidationManager getValidationManager() {
        return validationManager;
    }

    <R> R executeCommand(Command<R> command) {

        try {
            R result;

            CommandContext commandContext = contextHolder.get();

            if (commandContext == null) {
                commandContext = new CommandContext();
                commandContext.setValue(ChangeEngine.class, this);
                commandContext.setValue(EventDispatcher.class, eventDispatcher);

                contextHolder.set(commandContext);

                try {
                    result = commandExecutor.execute(commandContext, command);
                } finally {
                    contextHolder.remove();
                }

            } else {
                result = directCommandInvoker.execute(commandContext, command);
            }

            return result;

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("interrupted", ex);
        }
    }

    @Override
    public void close() {
        eventDispatcher.dispatch(new ChangeEngineCloseEvent());
    }
}
