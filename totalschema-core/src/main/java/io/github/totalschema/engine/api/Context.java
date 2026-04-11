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

package io.github.totalschema.engine.api;

import java.util.Optional;

public interface Context {

    /**
     * Returns an {@link Optional} containing the value to which the specified class is mapped, or
     * an empty {@link Optional} if this Context contains no mapping for the class.
     *
     * <p>This is a convenience method that combines {@link #has(Class)} and {@link #get(Class)}
     * into a single call, returning an {@link Optional} instead of throwing an exception when the
     * component is not found.
     *
     * <p><b>Example usage:</b>
     *
     * <pre>{@code
     * Optional<Configuration> config = context.getOptional(Configuration.class);
     * if (config.isPresent()) {
     *     // Use the configuration
     *     String value = config.get().getString("key");
     * }
     *
     * // Or with Optional chaining
     * String value = context.getOptional(Configuration.class)
     *     .map(cfg -> cfg.getString("key"))
     *     .orElse("default");
     * }</pre>
     *
     * @param clazz the class type whose associated value is to be returned (never {@code null})
     * @param <R> the type of the component to retrieve
     * @return an {@link Optional} containing the component if present, or an empty {@link Optional}
     *     if no mapping exists for the specified class
     * @throws NullPointerException if the clazz parameter is null
     * @see #has(Class)
     * @see #get(Class)
     */
    default <R> Optional<R> getOptional(Class<R> clazz) {
        if (has(clazz)) {
            return Optional.of(get(clazz));
        } else {
            return Optional.empty();
        }
    }

    /**
     * Returns true if this context contains a mapping for the specified class.
     *
     * @param clazz the class type whose presence in this context is to be tested (never {@code
     *     null})
     * @return true if this context contains a mapping for the specified key
     * @throws NullPointerException if the clazz parameter is null
     */
    boolean has(Class<?> clazz);

    /**
     * Returns the value to which the specified class is mapped, or throws {@code
     * IllegalStateException} if this Context contains no mapping for the class.
     *
     * @param clazz the class type whose associated value is to be returned (never {@code null})
     * @param qualifier the qualifier for the component to be returned (can be {@code null})
     * @param additionalArgument additional arguments to be passed to the factory when creating the
     *     component (can be empty)
     * @param <R> return type
     * @return the value to which the specified class is mapped (never {@code null})
     * @throws NullPointerException if the clazz parameter is null
     * @throws IllegalStateException if the context does not have an association for the given class
     */
    <R> R get(Class<R> clazz, String qualifier, Object... additionalArgument);

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
    <R> R get(Class<R> clazz);
}
