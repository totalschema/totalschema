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

package io.github.totalschema.engine.core.command.api;

import io.github.totalschema.engine.api.Context;
import io.github.totalschema.engine.core.container.ObjectSpecification;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A type-safe, hierarchical container that serves multiple purposes in the command execution
 * framework: it acts as a shared data repository for values exchanged between commands, and as a
 * dependency injection mechanism providing access to core engine services and components. It also
 * supports hierarchical scoping, allowing child contexts to override or extend the values provided
 * by their parent contexts. It is used in the connector extension mechanism to inject dependencies
 * and configuration between connector and change executor implementations.
 *
 * <p>Normally, an ObjectsContext contains a mutable set of Objects, which also can be registered
 * later on, while it also maintains a reference to a parent context, allowing a unified lookup
 * pattern across both short lived contexts, and the core engine services. Services, engine
 * operations (commands) can add their own values for downstream commands in the processing chain.
 * The type-based key system ensures type safety and eliminates the need for string-based lookups or
 * casting.
 *
 * <h2>Hierarchical Structure</h2>
 *
 * <p>The context supports a parent-child relationship, allowing for nested scopes. When a value is
 * requested, the context first searches its own storage. If the value is not found, it delegates
 * the lookup to its parent context, if one exists. This allows for a layered approach to dependency
 * resolution, where child contexts can override or supplement the services provided by their
 * parents.
 *
 * <h2>Constraints</h2>
 *
 * <p>Only one instance per type is allowed within a single context instance. Once a value is
 * associated with a type in the current context, no new value of the same type can be added,
 * preventing accidental overwrites.
 *
 * <h2>Thread Safety</h2>
 *
 * <p>This class is thread-safe. A {@code setValue} operation on a given class has a
 * <i>happens-before</i> relationship with any subsequent {@code get} or {@code has} for that class,
 * ensuring proper visibility of values across threads.
 *
 * @see io.github.totalschema.engine.core.DefaultChangeEngine
 */
public final class ObjectsContext implements Context {

    private final Logger logger = LoggerFactory.getLogger(ObjectsContext.class);

    private final ConcurrentHashMap<ObjectSpecification, Object> values;
    private final Context parentContext;
    private final boolean allowUnqualifiedAccessToSingleComponents;

    public ObjectsContext() {
        this(null);
    }

    public ObjectsContext(Context parentContext) {
        this.parentContext = parentContext;
        this.values = new ConcurrentHashMap<>();
        this.allowUnqualifiedAccessToSingleComponents = false;
    }

    private ObjectsContext(
            Context parentContext,
            Map<ObjectSpecification, Object> values,
            boolean allowUnqualifiedAccessToSingleComponents) {
        this.parentContext = parentContext;
        this.values = new ConcurrentHashMap<>(values);
        this.allowUnqualifiedAccessToSingleComponents = allowUnqualifiedAccessToSingleComponents;
    }

    public static <C1> ObjectsContext of(Class<C1> clazz1, String qualifier1, C1 object1) {

        ObjectsContext objectsContext = new ObjectsContext();
        objectsContext.setValue(clazz1, qualifier1, object1);
        return objectsContext;
    }

    public static <C1, C2> ObjectsContext of(
            Class<C1> clazz1,
            String qualifier1,
            C1 object1,
            Class<C2> clazz2,
            String qualifier2,
            C2 object2) {

        ObjectsContext objectsContext = new ObjectsContext();
        objectsContext.setValue(clazz1, qualifier1, object1);
        objectsContext.setValue(clazz2, qualifier2, object2);
        return objectsContext;
    }

    /**
     * Checks whether a value or service is available for the specified type, looking in this
     * context and, if not found, in the parent context.
     *
     * <p>This method can be used to verify the presence of either:
     *
     * <ul>
     *   <li>Pre-injected core engine services (e.g., {@code ChangeEngine}, {@code EventDispatcher})
     *   <li>Values added by commands earlier in the processing chain
     * </ul>
     *
     * @param clazz the class type whose presence in this context is to be tested (never {@code
     *     null})
     * @return {@code true} if this context or its parent contains a mapping for the specified type;
     *     {@code false} otherwise
     * @throws NullPointerException if the {@code clazz} parameter is {@code null}
     */
    @Override
    public boolean has(Class<?> clazz) {
        Objects.requireNonNull(clazz, "Argument clazz cannot be null");

        ObjectSpecification objectSpecification = ObjectSpecification.from(clazz);

        return values.containsKey(objectSpecification)
                || (parentContext != null && parentContext.has(clazz));
    }

    /**
     * Retrieves a value or service from the context by its type. If the value is not found in the
     * current context, the lookup is delegated to the parent context.
     *
     * <p>This method provides type-safe access to:
     *
     * <ul>
     *   <li>Core engine services automatically injected by {@link
     *       io.github.totalschema.engine.core.DefaultChangeEngine}
     *   <li>Infrastructure components (e.g., {@code SecretsManager}, {@code Environment})
     *   <li>Values set by commands earlier in the processing chain
     * </ul>
     *
     * <p>The returned value is guaranteed to be non-null and of the requested type, eliminating the
     * need for null checks or casting.
     *
     * @param clazz the class type whose associated value is to be returned (never {@code null})
     * @param <R> the return type, inferred from the {@code clazz} parameter
     * @return the value or service instance associated with the specified type (never {@code null})
     * @throws NullPointerException if the {@code clazz} parameter is {@code null}
     * @throws IllegalStateException if no value is associated with the specified type in this
     *     context or any of its parents
     */
    @Override
    public <R> R get(Class<R> clazz) {
        Objects.requireNonNull(clazz, "Argument clazz cannot be null");

        ObjectSpecification objectSpecification = ObjectSpecification.from(clazz);

        return get(objectSpecification);
    }

    /**
     * Retrieves a qualified value or service from the parent context. This method is used for
     * resolving components that are identified by both a type and a string-based qualifier, such as
     * different database connections.
     *
     * <p><b>Note:</b> This method only performs the lookup in the parent context and is intended
     * for internal use where qualified resolution is required.
     *
     * @param clazz The class type of the value to retrieve.
     * @param qualifier The qualifier to distinguish between multiple instances of the same type.
     * @param <R> The expected return type.
     * @return The resolved object from the parent context.
     * @throws IllegalStateException if no parent context is available, or if the value is not found
     *     in the parent context.
     */
    @Override
    public <R> R get(Class<R> clazz, String qualifier, Object... additionalArgument) {

        ObjectSpecification objectSpecification =
                ObjectSpecification.from(clazz, qualifier, additionalArgument);

        return get(objectSpecification);
    }

    @Override
    public <R> R get(ObjectSpecification objectSpecification) {
        @SuppressWarnings("unchecked")
        R object = (R) values.get(objectSpecification);
        if (object == null && parentContext != null) {
            object = parentContext.get(objectSpecification);
        }

        if (object == null) {
            throw new IllegalStateException("No Context value found for: " + objectSpecification);
        }
        return object;
    }

    /**
     * Associates a value with a type in the current context, making it available to subsequent
     * commands in the processing chain.
     *
     * <p>This method allows commands to share data with downstream commands by registering values
     * that can be retrieved using {@link #get(Class)}. The type-based key system ensures type
     * safety and prevents ambiguity.
     *
     * <p><strong>Note:</strong> This method only affects the current context and cannot be used to
     * override values in parent contexts. Each type can only be associated with one value within
     * this specific context instance.
     *
     * @param clazz the class type to associate with the value (must be the exact class or a
     *     superclass of {@code value}) (never {@code null})
     * @param value the object to be associated with the specified type (never {@code null})
     * @param <V> the type of the value being stored
     * @throws NullPointerException if either {@code clazz} or {@code value} is {@code null}
     * @throws IllegalStateException if a value is already associated with the specified type in
     *     this context
     */
    public <V> void setValue(Class<? super V> clazz, V value) {
        Objects.requireNonNull(clazz, "Argument clazz cannot be null");
        Objects.requireNonNull(value, "Argument value cannot be null");

        Object existingValue = values.putIfAbsent(ObjectSpecification.from(clazz), value);
        if (existingValue != null) {
            throw new IllegalStateException(
                    "Value for type "
                            + clazz.getName()
                            + " already exists in this context. Overwriting is not permitted.");
        }

        if (logger.isDebugEnabled()) {
            logger.debug("{} set value in context: {}", getCallerClassName(), value);
        }
    }

    public <V> void setValue(Class<? super V> clazz, String qualifier, V value) {
        Objects.requireNonNull(clazz, "Argument clazz cannot be null");
        Objects.requireNonNull(qualifier, "Argument qualifier cannot be null");
        Objects.requireNonNull(value, "Argument value cannot be null");

        Object existingValue =
                values.putIfAbsent(ObjectSpecification.from(clazz, qualifier), value);
        if (existingValue != null) {
            throw new IllegalStateException(
                    "Value for type "
                            + clazz.getName()
                            + " with qualifier "
                            + qualifier
                            + " already exists in this context. Overwriting is not permitted.");
        }

        if (logger.isDebugEnabled()) {
            logger.debug("{} set value in context: {}", getCallerClassName(), value);
        }
    }

    private static String getCallerClassName() {
        StackTraceElement[] stElements = Thread.currentThread().getStackTrace();
        for (int i = 1; i < stElements.length; i++) {
            StackTraceElement ste = stElements[i];
            if (!ste.getClassName().equals(ObjectsContext.class.getName())
                    && ste.getClassName().indexOf("java.lang.Thread") != 0) {
                return ste.getClassName();
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "ObjectsContext{" + "values=" + values + ", parentContext=" + parentContext + '}';
    }

    /**
     * Creates a new builder initialized with the current state of this context. The builder allows
     * creating a modified copy of this context while preserving the original instance.
     *
     * @return a builder containing this context's parent and all current values
     */
    public Builder toBuilder() {
        return new Builder(parentContext, values, allowUnqualifiedAccessToSingleComponents);
    }

    /**
     * Returns a new builder for constructing an {@link ObjectsContext} instance.
     *
     * @return a new empty builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * A builder for constructing {@link ObjectsContext} instances with a fluent API. Allows
     * incremental population of values and parent context configuration.
     *
     * <p>Example usage:
     *
     * <pre>{@code
     * Context context = ObjectsContext.builder()
     *     .parentContext(parentCtx)
     *     .setValue(String.class, "myValue")
     *     .setValue(Integer.class, 42)
     *     .build();
     * }</pre>
     */
    public static class Builder {
        private Context parentContext;
        private final Map<ObjectSpecification, Object> values = new HashMap<>();
        private boolean allowUnqualifiedAccessToSingleComponents;

        private Builder() {}

        private Builder(
                Context parentContext,
                Map<ObjectSpecification, Object> values,
                boolean allowUnqualifiedAccessToSingleComponents) {
            this.parentContext = parentContext;
            this.values.putAll(values);
            this.allowUnqualifiedAccessToSingleComponents =
                    allowUnqualifiedAccessToSingleComponents;
        }

        /**
         * Sets the parent context for the context being built.
         *
         * @param parentContext the parent context (may be {@code null})
         * @return this builder for method chaining
         */
        public Builder parentContext(Context parentContext) {
            this.parentContext = parentContext;
            return this;
        }

        /**
         * Associates a value with a type in the context being built.
         *
         * @param clazz the class type to associate with the value (never {@code null})
         * @param value the object to be associated with the specified type (never {@code null})
         * @param <V> the type of the value being stored
         * @return this builder for method chaining
         * @throws NullPointerException if either {@code clazz} or {@code value} is {@code null}
         * @throws IllegalStateException if a value is already associated with the specified type
         */
        public <V> Builder setValue(Class<? super V> clazz, V value) {
            Objects.requireNonNull(clazz, "Argument clazz cannot be null");
            Objects.requireNonNull(value, "Argument value cannot be null");

            ObjectSpecification spec = ObjectSpecification.from(clazz);
            if (values.containsKey(spec)) {
                throw new IllegalStateException(
                        "Value for type " + clazz.getName() + " already exists in builder.");
            }
            values.put(spec, value);
            return this;
        }

        /**
         * Associates a qualified value with a type in the context being built.
         *
         * @param clazz the class type to associate with the value (never {@code null})
         * @param qualifier the qualifier to distinguish between multiple instances (never {@code
         *     null})
         * @param value the object to be associated with the specified type and qualifier (never
         *     {@code null})
         * @param <V> the type of the value being stored
         * @return this builder for method chaining
         * @throws NullPointerException if {@code clazz}, {@code qualifier}, or {@code value} is
         *     {@code null}
         * @throws IllegalStateException if a value is already associated with the specified type
         *     and qualifier
         */
        public <V> Builder setValue(Class<? super V> clazz, String qualifier, V value) {
            Objects.requireNonNull(clazz, "Argument clazz cannot be null");
            Objects.requireNonNull(qualifier, "Argument qualifier cannot be null");
            Objects.requireNonNull(value, "Argument value cannot be null");

            ObjectSpecification spec = ObjectSpecification.from(clazz, qualifier);
            if (values.containsKey(spec)) {
                throw new IllegalStateException(
                        "Value for type "
                                + clazz.getName()
                                + " with qualifier "
                                + qualifier
                                + " already exists in builder.");
            }
            values.put(spec, value);
            return this;
        }

        public Builder allowUnqualifiedAccessToSingleComponents(boolean newValue) {
            this.allowUnqualifiedAccessToSingleComponents = newValue;

            return this;
        }

        /**
         * Builds and returns a new {@link ObjectsContext} instance with the configured parent and
         * values.
         *
         * @return a new context instance
         */
        public ObjectsContext build() {
            if (allowUnqualifiedAccessToSingleComponents) {
                initializeUnqualifiedAccess();
            }

            return new ObjectsContext(
                    parentContext, values, allowUnqualifiedAccessToSingleComponents);
        }

        private void initializeUnqualifiedAccess() {

            Set<ObjectSpecification> objectSpecificationsSnapshot = new HashSet<>(values.keySet());

            objectSpecificationsSnapshot.stream()
                    .collect(Collectors.groupingBy(ObjectSpecification::getType))
                    .values()
                    .stream()
                    .filter(specs -> specs.size() == 1)
                    .map(specs -> specs.get(0))
                    .filter(spec -> spec.getQualifier() != null) // Still using String internally
                    .forEach(
                            spec -> {
                                ObjectSpecification defaultSpec = spec.withQualifier(null);
                                values.computeIfAbsent(defaultSpec, k -> values.get(spec));
                            });
        }
    }
}
