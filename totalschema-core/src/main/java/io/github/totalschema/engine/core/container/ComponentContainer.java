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

package io.github.totalschema.engine.core.container;

import io.github.totalschema.engine.api.Context;
import io.github.totalschema.spi.factory.ComponentFactory;
import java.io.Closeable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A lightweight container that manages the lifecycle and dependencies of various components. It
 * supports registration of singleton instances and component factories, with lazy initialization
 * and dependency resolution.
 *
 * <p>Components are retrieved by their class type, and optionally by a qualifier for
 * disambiguation. The container is configured using the {@link ComponentContainerBuilder}.
 *
 * @see ComponentContainerBuilder
 * @see ComponentFactory
 */
public final class ComponentContainer implements Closeable, Context {

    private final Logger logger = LoggerFactory.getLogger(ComponentContainer.class);

    private final ConcurrentHashMap<ObjectSpecification, Object> objects =
            new ConcurrentHashMap<>();
    private final ConcurrentHashMap<FactorySpecification, ComponentFactory<?>> factories =
            new ConcurrentHashMap<>();
    private final ReentrantLock creationLock = new ReentrantLock();

    private final List<Closeable> closeableList = Collections.synchronizedList(new LinkedList<>());

    private final AtomicBoolean isClosed = new AtomicBoolean(false);

    /**
     * Returns a new builder for creating a {@link ComponentContainer}.
     *
     * @return A new {@link ComponentContainerBuilder} instance.
     */
    public static ComponentContainerBuilder builder() {
        return new ComponentContainerBuilder();
    }

    public void registerComponent(ObjectSpecification specification, Object component) {

        requireNotClosed();

        logger.debug("Registering component {}: {}", specification, component);

        if (component instanceof Closeable) {
            closeableList.add(0, (Closeable) component);

            logger.debug(
                    "Component {} registered for lifecycle management ({})",
                    component,
                    specification);
        }

        objects.put(specification, component);
    }

    public void registerComponentFactory(
            FactorySpecification specification, ComponentFactory<?> factory) {

        requireNotClosed();

        logger.debug("Registering factory {}: {}", specification, factory);

        if (factory instanceof Closeable) {
            closeableList.add((Closeable) factory);

            logger.debug(
                    "Factory {} registered for lifecycle management ({})", factory, specification);
        }

        factories.put(specification, factory);
    }

    @Override
    public boolean has(Class<?> clazz) {

        requireNotClosed();

        return isObjectExistForClass(clazz) || isAnyFactoryFoundThatCouldCreateClass(clazz);
    }

    private boolean isObjectExistForClass(Class<?> clazz) {
        for (ObjectSpecification objectSpecification : objects.keySet()) {
            if (clazz.isAssignableFrom(objectSpecification.getType())) {
                return true;
            }
        }

        return false;
    }

    private boolean isAnyFactoryFoundThatCouldCreateClass(Class<?> clazz) {
        for (ComponentFactory<?> factory : factories.values()) {
            if (clazz.isAssignableFrom(factory.getComponentType())) {
                return true;
            }
        }

        return false;
    }

    @Override
    public <R> R get(Class<R> clazz) {
        return get(clazz, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> R get(Class<R> clazz, String qualifier, Object... additionalArgument) {

        requireNotClosed();

        ObjectSpecification objectSpecification =
                ObjectSpecification.from(clazz, qualifier, additionalArgument);

        // Fast path: check if component already exists (no locking)
        Object component = objects.get(objectSpecification);
        if (component != null) {
            return (R) component;
        }

        // Component doesn't exist, need to create it.
        // We cannot use computeIfAbsent because the factory's createComponent() method
        // may call context.get() to resolve dependencies, which would trigger
        // a nested computeIfAbsent call on the same ConcurrentHashMap.
        // This causes "Recursive update" IllegalStateException.

        return createComponent(objectSpecification);
    }

    @SuppressWarnings("unchecked")
    private <R> R createComponent(ObjectSpecification objectSpecification) {
        // Use a single global lock for all component creation.
        // Since the container is read-heavy (fast path without locking) and component
        // creation is primarily a startup concern, a single lock is simple and adequate.
        //
        // We use tryLock with a timeout to prevent deadlocks and provide better diagnostics
        // if something goes wrong.

        try {
            boolean lockAcquired = creationLock.tryLock(2, TimeUnit.MINUTES);
            if (!lockAcquired) {
                throw new IllegalStateException(
                        "Failed to acquire component creation lock within timeout: "
                                + objectSpecification
                                + ". This may indicate a deadlock or a component factory that is taking too long to initialize.");
            }

            try {
                // Double-check: another thread might have created it while we waited for the lock
                R component = (R) objects.get(objectSpecification);
                if (component != null) {
                    return component;
                }

                // Create the component (may recursively call get() for dependencies)
                component = createComponentUsingFactory(objectSpecification);

                // Store it in the map
                objects.put(objectSpecification, component);

                return component;

            } finally {
                creationLock.unlock();
            }

        } catch (InterruptedException e) {
            // Restore interrupt status
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Interrupted while waiting to create component: " + objectSpecification, e);
        }
    }

    @SuppressWarnings("unchecked")
    private <R> R createComponentUsingFactory(ObjectSpecification objectSpecification) {

        FactorySpecification factorySpecification = FactorySpecification.from(objectSpecification);

        ComponentFactory<?> factory = factories.get(factorySpecification);
        if (factory == null) {
            throw new FactoryNotFoundException(
                    "No factory found for specification: " + factorySpecification);
        }

        R newComponent;

        validateRequiredDependencies(factory, objectSpecification);

        if (objectSpecification.getArguments().isEmpty()) {
            newComponent = (R) factory.createComponent(this, List.of());
        } else {
            List<Object> argumentsList = objectSpecification.getArguments().get();
            newComponent = (R) factory.createComponent(this, argumentsList);
        }

        Objects.requireNonNull(
                newComponent, "Factory for " + factorySpecification + " returned null");

        if (newComponent instanceof Closeable) {
            closeableList.add(0, (Closeable) newComponent);
        }

        return newComponent;
    }

    /**
     * Validates that all required dependencies for a factory are available in the container.
     *
     * @param componentFactory The factory whose dependencies should be validated
     * @param objectSpecification The specification of the object being created
     * @throws IllegalStateException if any required dependency is missing
     */
    private void validateRequiredDependencies(
            ComponentFactory<?> componentFactory, ObjectSpecification objectSpecification) {

        List<Class<?>> requiredTypes = componentFactory.getRequiredContextTypes();
        if (requiredTypes == null || requiredTypes.isEmpty()) {
            return;
        }

        List<Class<?>> missingDependencies = new LinkedList<>();

        for (Class<?> requiredType : requiredTypes) {
            boolean isAvailable = has(requiredType);

            if (!isAvailable) {
                missingDependencies.add(requiredType);
            }
        }

        if (!missingDependencies.isEmpty()) {
            StringBuilder errorMessage = new StringBuilder();
            errorMessage
                    .append("Cannot create component of type '")
                    .append(objectSpecification.getType().getName())
                    .append("'");

            if (objectSpecification.getQualifier() != null) {
                errorMessage
                        .append(" with qualifier '")
                        .append(objectSpecification.getQualifier())
                        .append("'");
            }

            errorMessage.append(
                    ". The following required dependencies are not available in the container:");

            for (Class<?> missingDependency : missingDependencies) {
                errorMessage.append("\n  - ").append(missingDependency.getName());
            }

            errorMessage
                    .append("\n\nPlease ensure that factories or instances for these types are ")
                    .append(
                            "registered in the ComponentContainerBuilder before building the container.");

            throw new IllegalStateException(errorMessage.toString());
        }
    }

    @Override
    public void close() {

        requireNotClosed();

        // we set the state here, if close fails, the official state remains closed.
        boolean setWasSuccessful = this.isClosed.compareAndSet(false, true);
        if (setWasSuccessful) {
            // we could successfully set the state to closed, so we proceed with closing the
            // components.
            doClose();
        }
    }

    private void requireNotClosed() {
        if (isClosed.get()) {
            throw new IllegalStateException("Closed already");
        }
    }

    private void doClose() {

        List<Exception> collectedExceptions = closeComponents(closeableList);

        if (collectedExceptions.isEmpty()) {
            logger.debug("ComponentContainer closed successfully");

        } else {
            logger.error("Close of {} components failed", collectedExceptions.size());

            Iterator<Exception> iterator = collectedExceptions.iterator();
            Exception firstException = iterator.next();

            logger.error("Exception closing component(s)", firstException);

            if (collectedExceptions.size() == 1) {
                throw new RuntimeException("Failure closing ComponentContainer", firstException);

            } else {
                RuntimeException runtimeException =
                        new RuntimeException("Failure closing ComponentContainer", firstException);

                iterator.forEachRemaining(runtimeException::addSuppressed);

                throw runtimeException;
            }
        }
    }

    private List<Exception> closeComponents(List<Closeable> componentsToClose) {

        List<Exception> collectedExceptions = new LinkedList<>();

        logger.debug("Closing {} components", componentsToClose.size());

        for (Closeable component : componentsToClose) {
            try {
                logger.debug("Closing: {}", component);

                component.close();

            } catch (Exception ex) {

                logger.warn("Failed to close component: {}", component, ex);
                // will re-throw after all close attempts have been made,
                // to ensure best-effort closing of all resources
                collectedExceptions.add(ex);
            }
        }

        return collectedExceptions;
    }
}
