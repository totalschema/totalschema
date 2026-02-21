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

import io.github.totalschema.concurrent.Locked;
import io.github.totalschema.engine.api.Context;
import io.github.totalschema.spi.ComponentFactory;
import java.io.Closeable;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
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
public final class ComponentContainer implements Context, Closeable {

    private final Logger logger = LoggerFactory.getLogger(ComponentContainer.class);

    private enum State {
        CREATED,
        READY,
        CLOSED,
        FAILED
    }

    private static final long DEFAULT_LOCK_TIMEOUT = 2;
    private static final TimeUnit DEFAULT_LOCK_TIMEOUT_UNIT = TimeUnit.MINUTES;

    private final Locked<Map<ObjectSpecification, Object>> objects;
    private final Locked<Map<FactorySpecification, ComponentFactory<?>>> factories;

    private final AtomicReference<State> state;

    /**
     * Returns a new builder for creating a {@link ComponentContainer}.
     *
     * @return A new {@link ComponentContainerBuilder} instance.
     */
    public static ComponentContainerBuilder builder() {
        return new ComponentContainerBuilder();
    }

    /**
     * Constructs a new ComponentContainer. This is package-private to enforce usage of the builder.
     */
    ComponentContainer(
            Map<ObjectSpecification, Object> objects,
            Map<FactorySpecification, ComponentFactory<?>> factories) {

        this.objects =
                Locked.of(
                        new LinkedHashMap<>(objects),
                        DEFAULT_LOCK_TIMEOUT,
                        DEFAULT_LOCK_TIMEOUT_UNIT);
        this.factories =
                Locked.of(
                        new LinkedHashMap<>(factories),
                        DEFAULT_LOCK_TIMEOUT,
                        DEFAULT_LOCK_TIMEOUT_UNIT);

        this.state = new AtomicReference<>(State.CREATED);
    }

    /** Initializes the container, creating all non-lazy components. */
    void initialize() {
        boolean setWasSuccessful = this.state.compareAndSet(State.CREATED, State.READY);
        if (!setWasSuccessful) {
            // we set the state here, if startup fails, the try-catch block below will set it to
            // FAILED,
            // so we can distinguish between failed startup and invalid state transitions
            throw new IllegalStateException(
                    "ComponentContainer can only be initialized from CREATED state. "
                            + "Current state: "
                            + this.state.get());
        }

        try {
            factories.withTryLock(this::createObjectsFromFactories);

        } catch (RuntimeException ex) {

            this.state.set(State.FAILED);

            logger.error("Failed to initialize ComponentContainer", ex);
            logger.error("Container state is '{}' after a failed initialization", this.state.get());

            throw ex;
        }
    }

    private void createObjectsFromFactories(
            Map<FactorySpecification, ComponentFactory<?>> theFactories) {
        for (Map.Entry<FactorySpecification, ComponentFactory<?>> entry : theFactories.entrySet()) {

            ComponentFactory<?> factory = entry.getValue();

            if (!factory.isLazy()) {
                List<Class<?>> argumentTypes = factory.getArgumentTypes();
                if (argumentTypes == null || argumentTypes.isEmpty()) {
                    FactorySpecification factorySpecification = entry.getKey();

                    Object createdObject =
                            getOrCreateObject(
                                    factorySpecification.getConstructedClass(),
                                    factorySpecification.getQualifier());

                    logger.debug(
                            "Created object {} for {} using factory {}",
                            createdObject,
                            factorySpecification,
                            factory);
                }
            }
        }
    }

    /**
     * Checks if a component of the given type is registered in the container.
     *
     * @param clazz The class type to check.
     * @return {@code true} if a component of the given type is registered, {@code false} otherwise.
     * @throws NullPointerException if clazz is null.
     */
    @Override
    public boolean has(Class<?> clazz) {
        Objects.requireNonNull(clazz, "clazz must not be null");
        checkState();

        return isObjectExistForClass(clazz) || isAnyFactoryFoundThatCouldCreateClass(clazz);
    }

    private boolean isObjectExistForClass(Class<?> clazz) {
        return objects.withTryLock(
                theObjects -> {
                    for (ObjectSpecification objectSpecification : theObjects.keySet()) {
                        if (clazz.isAssignableFrom(objectSpecification.getType())) {
                            return true;
                        }
                    }

                    return false;
                });
    }

    private boolean isAnyFactoryFoundThatCouldCreateClass(Class<?> clazz) {
        return factories.withTryLock(
                theFactories -> {
                    for (ComponentFactory<?> factory : theFactories.values()) {
                        if (clazz.isAssignableFrom(factory.getConstructedClass())) {
                            return true;
                        }
                    }

                    return false;
                });
    }

    /**
     * Retrieves a component of the given type from the container.
     *
     * @param clazz The class type of the component to retrieve.
     * @param <R> The type of the component.
     * @return The component instance.
     * @throws IllegalStateException if no component definition is found for the given type, or if
     *     multiple definitions are found without a qualifier.
     */
    @Override
    public <R> R get(Class<R> clazz) {
        return get(clazz, null);
    }

    /**
     * Retrieves a component of the given type and qualifier from the container.
     *
     * @param clazz The class type of the component to retrieve.
     * @param qualifier The qualifier to disambiguate between multiple components of the same type.
     * @param <R> The type of the component.
     * @return The component instance.
     * @throws IllegalStateException if no component definition is found for the given type and
     *     qualifier, or if the container has not been initialized.
     */
    @Override
    public <R> R get(Class<R> clazz, String qualifier, Object... additionalArgument) {
        checkState();

        return getOrCreateObject(clazz, qualifier, additionalArgument);
    }

    @SuppressWarnings("unchecked")
    private <R> R getOrCreateObject(
            Class<R> clazz, String qualifier, Object... additionalArgument) {

        ObjectSpecification objectSpecification =
                ObjectSpecification.from(clazz, qualifier, additionalArgument);

        return objects.withTryLock(
                theObjects -> {
                    R contextObject = (R) theObjects.get(objectSpecification);

                    if (contextObject == null) {
                        contextObject = (R) this.createObject(objectSpecification);
                        theObjects.put(objectSpecification, contextObject);
                    }

                    return contextObject;
                });
    }

    private Object createObject(ObjectSpecification objectSpecification) {

        FactorySpecification factorySpecification = FactorySpecification.from(objectSpecification);

        return factories.withTryLock(
                theFactories -> {
                    ComponentFactory<?> componentFactory = theFactories.get(factorySpecification);
                    if (componentFactory == null) {
                        throw new IllegalStateException(
                                "No component definition found for type: "
                                        + objectSpecification.getType()
                                        + " and qualifier: "
                                        + objectSpecification.getQualifier());
                    }

                    Object returnValue;

                    if (objectSpecification.getArguments().isPresent()) {
                        List<Object> additionalArgument = objectSpecification.getArguments().get();

                        Object[] argumentArray = additionalArgument.toArray(new Object[0]);

                        returnValue = componentFactory.newComponent(this, argumentArray);

                    } else {
                        returnValue = componentFactory.newComponent(this);
                    }

                    return returnValue;
                });
    }

    @Override
    public void close() {

        checkState();

        // we set the state here, if close fails, the official state remain closed.
        boolean setWasSuccessful = this.state.compareAndSet(State.READY, State.CLOSED);
        if (!setWasSuccessful) {
            // despite our previous check, another thread might have commenced closing the
            // container, to make sure only one thread performs the close logic, we check
            // the state transition was successful, if not, we just return, as the other
            // thread will perform the close logic
            return;
        }

        List<Closeable> reverseEnlistmentOrderList =
                getCloseableComponentsInReverseEnlistmentOrder();

        List<Exception> collectedExceptions = closeComponents(reverseEnlistmentOrderList);

        if (collectedExceptions.isEmpty()) {
            logger.debug("ComponentContainer closed successfully");

        } else {
            logger.error("Close of {} components failed", collectedExceptions.size());

            Iterator<Exception> iterator = collectedExceptions.iterator();
            Exception firstException = iterator.next();

            if (collectedExceptions.size() == 1) {
                logger.error("Close of component failed", firstException);
                throw new RuntimeException("Failure closing ComponentContainer", firstException);

            } else {
                logger.error("Close of components failed", firstException);

                RuntimeException runtimeException =
                        new RuntimeException("Failure closing ComponentContainer", firstException);
                iterator.forEachRemaining(runtimeException::addSuppressed);

                throw runtimeException;
            }
        }
    }

    private List<Exception> closeComponents(List<Closeable> reverseEnlistmentOrderList) {
        List<Exception> collectedExceptions = new LinkedList<>();

        logger.debug("Closing {} components", reverseEnlistmentOrderList.size());

        for (Closeable component : reverseEnlistmentOrderList) {
            try {
                logger.debug("Closing: {}", component);

                component.close();

            } catch (Exception ex) {

                logger.warn("Failed to close resource: {}", component, ex);
                // will re-throw after all close attempts have been made,
                // to ensure best-effort closing of all resources
                collectedExceptions.add(ex);
            }
        }
        return collectedExceptions;
    }

    private List<Closeable> getCloseableComponentsInReverseEnlistmentOrder() {

        LinkedList<Closeable> reverseEnlistmentOrderList = new LinkedList<>();

        List<Closeable> objectsInReverseEnlistmentOrder =
                objects.withTryLock(
                        theObjects -> {
                            return getCloseableObjectsInReverseOrder(theObjects.values());
                        });

        reverseEnlistmentOrderList.addAll(objectsInReverseEnlistmentOrder);

        List<Closeable> factoriesInReverseEnlistmentOrder =
                factories.withTryLock(
                        theFactories -> {
                            return getCloseableObjectsInReverseOrder(theFactories.values());
                        });

        reverseEnlistmentOrderList.addAll(factoriesInReverseEnlistmentOrder);

        return reverseEnlistmentOrderList;
    }

    private List<Closeable> getCloseableObjectsInReverseOrder(Collection<?> input) {

        List<Closeable> objects =
                input.stream()
                        .filter(it -> it instanceof Closeable)
                        .map(Closeable.class::cast)
                        // we will reverse in-place, so we need a mutable list!
                        .collect(Collectors.toCollection(LinkedList::new));

        Collections.reverse(objects);

        return objects;
    }

    private void checkState() {
        State currentState = state.get();
        if (currentState != State.READY) {
            throw new IllegalStateException("Operation is not allowed in state: " + currentState);
        }
    }
}
