/*
 * totalschema: tool for managing database versioning and schema changes with ease.
 * Copyright (C) 2025-2026 totalschema development team
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

import static java.util.Objects.requireNonNull;

import io.github.totalschema.engine.api.*;
import io.github.totalschema.engine.core.command.api.*;
import io.github.totalschema.engine.core.container.ComponentContainer;
import io.github.totalschema.engine.core.event.ChangeEngineCloseEvent;
import io.github.totalschema.engine.core.event.EventDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of ChangeEngine using the Command pattern. All operations are executed as
 * commands through a CommandExecutor. Manager interfaces are provided for organizing operations by
 * domain.
 */
public class DefaultChangeEngine implements ChangeEngine {

    private static final Logger log = LoggerFactory.getLogger(DefaultChangeEngine.class);

    private final EventDispatcher eventDispatcher;

    private final CommandExecutor commandExecutor;

    private final CommandInvoker directCommandInvoker = new CommandInvoker();

    private final ComponentContainer componentContainer;

    private final EnvironmentManager environmentManager;
    private final ChangeManager changeManager;
    private final StateManager stateManager;
    private final ValidationManager validationManager;

    private final ThreadLocal<ObjectsContext> commandContextHolder = new ThreadLocal<>();

    public DefaultChangeEngine(
            CommandExecutor commandExecutor,
            ComponentContainer componentContainer,
            EventDispatcher eventDispatcher) {

        requireNonNull(commandExecutor, "commandExecutor must not be null");
        requireNonNull(componentContainer, "componentContainer must not be null");
        requireNonNull(eventDispatcher, "eventDispatcher must not be null");

        this.commandExecutor = commandExecutor;
        this.componentContainer = componentContainer;
        this.eventDispatcher = eventDispatcher;

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

            ObjectsContext objectsContext = commandContextHolder.get();

            if (objectsContext == null) {
                objectsContext = new ObjectsContext(componentContainer);
                objectsContext.setValue(ChangeEngine.class, this);

                commandContextHolder.set(objectsContext);

                try {
                    result = commandExecutor.execute(objectsContext, command);
                } finally {
                    commandContextHolder.remove();
                }

            } else {
                result = directCommandInvoker.execute(objectsContext, command);
            }

            return result;

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("interrupted", ex);
        }
    }

    @Override
    public void close() {
        log.debug("Dispatching ChangeEngineCloseEvent");
        eventDispatcher.dispatch(new ChangeEngineCloseEvent());

        log.debug("Closing component container");
        this.componentContainer.close();

        log.debug("Clear shutdown of ChangeEngine completed");
    }
}
