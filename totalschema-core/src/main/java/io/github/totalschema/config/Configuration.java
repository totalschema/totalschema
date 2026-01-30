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

package io.github.totalschema.config;

import java.util.*;

/**
 * Configuration interface for accessing application settings and properties.
 *
 * <p>Provides methods for retrieving typed configuration values with optional semantics.
 */
public interface Configuration {

    /**
     * Retrieves a string value for the given key.
     *
     * @param key the configuration key
     * @return an Optional containing the value if present, empty otherwise
     */
    Optional<String> getString(String key);

    /**
     * Retrieves an integer value for the given key(s).
     *
     * @param key the configuration key(s)
     * @return an Optional containing the value if present, empty otherwise
     */
    Optional<Integer> getInt(String... key);

    /**
     * Retrieves a boolean value for the given key.
     *
     * @param key the configuration key
     * @return an Optional containing the value if present, empty otherwise
     */
    Optional<Boolean> getBoolean(String key);

    /**
     * Retrieves a list of string values for the given key.
     *
     * @param key the configuration key
     * @return an Optional containing the list if present, empty otherwise
     */
    Optional<List<String>> getList(String key);

    /**
     * Retrieves an enum value for the given key(s).
     *
     * @param <E> the enum type
     * @param elementType the class of the enum type
     * @param keys the configuration key(s)
     * @return an Optional containing the enum value if present, empty otherwise
     */
    <E extends Enum<E>> Optional<E> getEnumValue(Class<E> elementType, String... keys);

    /**
     * Retrieves a string value for the given key(s).
     *
     * @param keys the configuration key(s)
     * @return an Optional containing the value if present, empty otherwise
     */
    Optional<String> getString(String... keys);

    /**
     * Retrieves a long value for the given key(s).
     *
     * @param key the configuration key(s)
     * @return an Optional containing the value if present, empty otherwise
     */
    Optional<Long> getLong(String... key);

    /**
     * Returns a configuration view with entries matching the given prefix.
     *
     * @param prefix the prefix to filter by
     * @return a Configuration containing only entries with the specified prefix
     */
    Configuration getPrefixNamespace(String... prefix);

    /**
     * Returns all configuration keys.
     *
     * @return a set of all configuration keys
     */
    Set<String> getKeys();

    /**
     * Constructs a <b>NEW</b> {@code Configuration} instance, with values of the specified
     * configuration overriding values from this configuration. That is: if the specified
     * configuration contains a key, its value will be copied to the new {@code Configuration},
     * while in other cases, where only either this or the specified configuration contains a key,
     * their respective values are both copied to the new {@code Configuration}.
     *
     * <p>NOTE: <b>NEW</b> {@code Configuration} instance is returned with the merged content.
     * Neither this or the specified {@code Configuration}.
     *
     * @param otherConfiguration the other configuration, the values that of will override values in
     *     this
     * @return a new Configuration instance, with the merged configuration, with values of the
     *     specified configuration overriding values from this configuration.
     */
    Configuration addAll(Configuration otherConfiguration);

    /**
     * Returns an {@link Optional} that describes a {@link Properties} object with configuration
     * entries copied from this configuration, or, if this configuration is empty an empty {@code
     * Optional} instance is returned.
     *
     * <p>NOTE: The {@code Properties} returned (if the configuration was not empty), will be a copy
     * of this configuration. Changes of the returned {@code Properties} are <b>NOT</b> reflected in
     * this configuration.
     *
     * @return An {@link Optional} with the {@code Properties} entries copied from this
     *     configuration; otherwise, an empty {@code Optional} instance
     */
    Optional<Properties> asProperties();

    /**
     * Creates a new configuration with an additional entry.
     *
     * @param key the configuration key
     * @param value the configuration value
     * @return a new Configuration instance with the added entry
     */
    Configuration withEntry(String key, String value);

    /**
     * Checks if this configuration is empty.
     *
     * @return true if this configuration contains no entries, false otherwise
     */
    boolean isEmpty();

    /**
     * Returns an {@link Optional} that describes a {@link Map} object with configuration entries
     * copied from this configuration or, if this configuration is empty an empty {@code Optional}
     * instance is returned.
     *
     * <p>NOTE: The {@code Map} returned (if the configuration was not empty), will be a copy of
     * this configuration. Changes of the returned {@code Map} are <b>NOT</b> reflected in this
     * configuration.
     *
     * @return An {@link Optional} with the {@code Map} entries copied from this configuration;
     *     otherwise, an empty {@code Optional} instance
     */
    Optional<Map<String, String>> asMap();
}
