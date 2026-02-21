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

import io.github.totalschema.engine.api.Context;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A type-safe, hierarchical container that serves dual purposes in the command execution framework:
 * it acts as a shared data repository for values exchanged between commands, and as a dependency
 * injection mechanism providing access to core engine services and components.
 *
 * <p>The context is automatically populated with essential infrastructure components by the {@link
 * io.github.totalschema.engine.core.DefaultChangeEngine} during command execution. This includes
 * core services, configuration objects, and other infrastructure components.
 *
 * <p>Commands can retrieve these pre-injected services or add their own values for downstream
 * commands in the processing chain. The type-based key system ensures type safety and eliminates
 * the need for string-based lookups or casting.
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
public final class CommandContext implements Context {

    private final Logger logger = LoggerFactory.getLogger(CommandContext.class);

    private final ConcurrentHashMap<Class<?>, Object> values = new ConcurrentHashMap<>();
    private final Context parentContext;

    public CommandContext() {
        this(null);
    }

    public CommandContext(Context parentContext) {
        this.parentContext = parentContext;
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

        return values.containsKey(clazz) || (parentContext != null && parentContext.has(clazz));
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

        // due to the contact of setValue, this should always be possible
        @SuppressWarnings("unchecked")
        R object = (R) values.get(clazz);

        if (object == null) {
            // if not found in current context, check parent context if available
            if (parentContext != null) {
                object = parentContext.get(clazz);
            }
        }

        if (object == null) {
            throw new IllegalStateException(
                    "No Context value found for: "
                            + clazz.getName()
                            + " in this context or any of its parents");
        }

        return object;
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

        if (parentContext == null) {
            throw new IllegalStateException(
                    "No parent context available for qualified lookup of: "
                            + clazz.getName()
                            + " with qualifier: "
                            + qualifier);
        }

        R object = parentContext.get(clazz, qualifier, additionalArgument);
        if (object == null) {
            throw new IllegalStateException(
                    "No Context value found for: "
                            + clazz.getName()
                            + " with qualifier: "
                            + qualifier);
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

        Object existingValue = values.putIfAbsent(clazz, value);
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

    private static String getCallerClassName() {
        StackTraceElement[] stElements = Thread.currentThread().getStackTrace();
        for (int i = 1; i < stElements.length; i++) {
            StackTraceElement ste = stElements[i];
            if (!ste.getClassName().equals(CommandContext.class.getName())
                    && ste.getClassName().indexOf("java.lang.Thread") != 0) {
                return ste.getClassName();
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "CommandContext{" + "values=" + values + ", parentContext=" + parentContext + '}';
    }
}
