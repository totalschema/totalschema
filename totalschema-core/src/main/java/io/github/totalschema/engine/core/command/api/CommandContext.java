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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The CommandContext class is a container for other objects that are passed between the members of
 * the processing chain. One object per type is allowed; if an object type is present in the
 * context, no new value of the same type can be added.
 *
 * <p>This class is Thread-safe: a {@code setValue} operation for a given class bears a
 * <i>happens-before</i> relation with {@code get} or {@code has} for that key reporting the value
 * set.
 */
public final class CommandContext {

    private final Logger logger = LoggerFactory.getLogger(CommandContext.class);

    private final ConcurrentHashMap<Class<?>, Object> values;

    public CommandContext() {
        this(new HashMap<>());
    }

    public CommandContext(Map<Class<?>, Object> engineContext) {
        this.values = new ConcurrentHashMap<>(engineContext);
    }

    /**
     * Returns true if this context contains a mapping for the specified class.
     *
     * @param clazz the class type whose presence in this context is to be tested (never {@code
     *     null})
     * @return true if this context contains a mapping for the specified key
     * @throws NullPointerException if the clazz parameter is null
     * @throws IllegalStateException if the context does not have an association for the given class
     */
    public boolean has(Class<?> clazz) {
        Objects.requireNonNull(clazz, "Argument clazz cannot be null");

        return values.containsKey(clazz);
    }

    /**
     * Returns the value to which the specified class is mapped, or throws {@code
     * IllegalStateException} if this Context contains no mapping for the class.
     *
     * @param clazz the class type whose associated value is to be returned (never {@code null})
     * @param <R> return type
     * @return the value to which the specified class is mapped (never {@code null})
     * @throws NullPointerException if the clazz parameter is null
     * @throws IllegalStateException if the context does not have an association for the given class
     */
    public <R> R get(Class<R> clazz) {
        Objects.requireNonNull(clazz, "Argument clazz cannot be null");

        // due to the contact of setValue, this should always be possible
        @SuppressWarnings("unchecked")
        R object = (R) values.get(clazz);

        if (object == null) {
            throw new IllegalStateException(
                    "No CommandContext value found for: " + clazz.getName());
        }

        return object;
    }

    /**
     * Associates the specified value with the specified class in this Context object.
     *
     * @param clazz the class with which the specified value is to be associated (must be the class
     *     or a superclass of {@code value}) (never {@code null})
     * @param value object to be associated with the specified key (never {@code null})
     * @param <V> type of the value
     * @throws NullPointerException if the clazz or value parameter is null
     * @throws IllegalStateException if the context already has an association for the given class
     */
    public <V> void setValue(Class<? super V> clazz, V value) {
        Objects.requireNonNull(clazz, "Argument clazz cannot be null");
        Objects.requireNonNull(value, "Argument value cannot be null");

        Object existingValue = values.putIfAbsent(clazz, value);
        if (existingValue != null) {
            throw new IllegalStateException("Value exists already for: " + clazz.getName());
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
        return "CommandContext{" + "values=" + values + '}';
    }
}
