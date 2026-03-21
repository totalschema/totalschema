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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Builder for creating {@link Configuration} instances.
 *
 * <p>Provides a fluent API for constructing configuration objects with key-value pairs.
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * Configuration config = Configuration.builder()
 *     .set("jdbc.url", "jdbc:h2:mem:testdb")
 *     .set("username", "sa")
 *     .set("password", "")
 *     .build();
 * }</pre>
 *
 * <p>This builder is mutable and not thread-safe. A single builder instance should not be shared
 * across multiple threads. However, the {@link Configuration} instance returned by {@link #build()}
 * is immutable and thread-safe.
 *
 * @see Configuration
 * @see MapConfiguration
 */
public final class ConfigurationBuilder {

    private final Map<String, String> entries;

    /**
     * Creates a new ConfigurationBuilder with an empty configuration.
     *
     * <p>This constructor is package-private to enforce usage through {@link
     * Configuration#builder()}.
     */
    ConfigurationBuilder() {
        this.entries = new HashMap<>();
    }

    /**
     * Sets a configuration value for the given key.
     *
     * <p>If the key already exists, its value will be replaced with the new value.
     *
     * @param key the configuration key (must not be null)
     * @param value the configuration value (must not be null)
     * @return this builder for method chaining
     * @throws NullPointerException if key or value is null
     */
    public ConfigurationBuilder set(String key, String value) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(value, "value must not be null");
        entries.put(key, value);
        return this;
    }

    /**
     * Sets a configuration value for the given key only if the key is not already present.
     *
     * <p>If the key already exists in this builder, this method does nothing and returns this
     * builder unchanged. This is useful for providing default values that can be overridden.
     *
     * <p><b>Usage Example:</b>
     *
     * <pre>{@code
     * Configuration config = Configuration.builder()
     *     .set("username", "admin")
     *     .setIfAbsent("username", "default")  // No effect, key already exists
     *     .setIfAbsent("password", "secret")   // Sets password since key doesn't exist
     *     .build();
     * }</pre>
     *
     * @param key the configuration key (must not be null)
     * @param value the configuration value (must not be null)
     * @return this builder for method chaining
     * @throws NullPointerException if key or value is null
     */
    public ConfigurationBuilder setIfAbsent(String key, String value) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(value, "value must not be null");
        entries.putIfAbsent(key, value);
        return this;
    }

    /**
     * Sets a configuration value for the given key with an integer value.
     *
     * <p>The integer value is converted to a string representation.
     *
     * @param key the configuration key (must not be null)
     * @param value the integer configuration value
     * @return this builder for method chaining
     * @throws NullPointerException if key is null
     */
    public ConfigurationBuilder set(String key, int value) {
        Objects.requireNonNull(key, "key must not be null");
        entries.put(key, Integer.toString(value));
        return this;
    }

    /**
     * Sets a configuration value for the given key with a long value.
     *
     * <p>The long value is converted to a string representation.
     *
     * @param key the configuration key (must not be null)
     * @param value the long configuration value
     * @return this builder for method chaining
     * @throws NullPointerException if key is null
     */
    public ConfigurationBuilder set(String key, long value) {
        Objects.requireNonNull(key, "key must not be null");
        entries.put(key, Long.toString(value));
        return this;
    }

    /**
     * Sets a configuration value for the given key with a boolean value.
     *
     * <p>The boolean value is converted to a string representation ("true" or "false").
     *
     * @param key the configuration key (must not be null)
     * @param value the boolean configuration value
     * @return this builder for method chaining
     * @throws NullPointerException if key is null
     */
    public ConfigurationBuilder set(String key, boolean value) {
        Objects.requireNonNull(key, "key must not be null");
        entries.put(key, Boolean.toString(value));
        return this;
    }

    /**
     * Sets a configuration value for the given key with an enum value.
     *
     * <p>The enum value is converted to its name string representation using {@link Enum#name()}.
     *
     * @param key the configuration key (must not be null)
     * @param value the enum configuration value (must not be null)
     * @return this builder for method chaining
     * @throws NullPointerException if key or value is null
     */
    public ConfigurationBuilder set(String key, Enum<?> value) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(value, "value must not be null");
        entries.put(key, value.name());
        return this;
    }

    /**
     * Sets a configuration value for the given key with an integer value, only if the key is not
     * already present.
     *
     * <p>The integer value is converted to a string representation.
     *
     * @param key the configuration key (must not be null)
     * @param value the integer configuration value
     * @return this builder for method chaining
     * @throws NullPointerException if key is null
     */
    public ConfigurationBuilder setIfAbsent(String key, int value) {
        Objects.requireNonNull(key, "key must not be null");
        entries.putIfAbsent(key, Integer.toString(value));
        return this;
    }

    /**
     * Sets a configuration value for the given key with a long value, only if the key is not
     * already present.
     *
     * <p>The long value is converted to a string representation.
     *
     * @param key the configuration key (must not be null)
     * @param value the long configuration value
     * @return this builder for method chaining
     * @throws NullPointerException if key is null
     */
    public ConfigurationBuilder setIfAbsent(String key, long value) {
        Objects.requireNonNull(key, "key must not be null");
        entries.putIfAbsent(key, Long.toString(value));
        return this;
    }

    /**
     * Sets a configuration value for the given key with a boolean value, only if the key is not
     * already present.
     *
     * <p>The boolean value is converted to a string representation ("true" or "false").
     *
     * @param key the configuration key (must not be null)
     * @param value the boolean configuration value
     * @return this builder for method chaining
     * @throws NullPointerException if key is null
     */
    public ConfigurationBuilder setIfAbsent(String key, boolean value) {
        Objects.requireNonNull(key, "key must not be null");
        entries.putIfAbsent(key, Boolean.toString(value));
        return this;
    }

    /**
     * Sets a configuration value for the given key with an enum value, only if the key is not
     * already present.
     *
     * <p>The enum value is converted to its name string representation using {@link Enum#name()}.
     *
     * @param key the configuration key (must not be null)
     * @param value the enum configuration value (must not be null)
     * @return this builder for method chaining
     * @throws NullPointerException if key or value is null
     */
    public ConfigurationBuilder setIfAbsent(String key, Enum<?> value) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(value, "value must not be null");
        entries.putIfAbsent(key, value.name());
        return this;
    }

    /**
     * Sets multiple configuration values from the given map.
     *
     * <p>All entries from the provided map are added to this builder. If any keys already exist,
     * their values will be replaced.
     *
     * @param entries the map of configuration entries to add (must not be null)
     * @return this builder for method chaining
     * @throws NullPointerException if entries is null or contains null keys/values
     */
    public ConfigurationBuilder setAll(Map<String, String> entries) {
        Objects.requireNonNull(entries, "entries must not be null");
        entries.forEach(this::set);
        return this;
    }

    /**
     * Sets multiple configuration values from the given Configuration.
     *
     * <p>All entries from the provided configuration are added to this builder. If any keys already
     * exist, their values will be replaced.
     *
     * @param configuration the configuration whose entries should be added (must not be null)
     * @return this builder for method chaining
     * @throws NullPointerException if configuration is null
     */
    public ConfigurationBuilder setAll(Configuration configuration) {
        Objects.requireNonNull(configuration, "configuration must not be null");
        configuration.asMap().ifPresent(this::setAll);
        return this;
    }

    /**
     * Removes a configuration entry by key.
     *
     * @param key the configuration key to remove (must not be null)
     * @return this builder for method chaining
     * @throws NullPointerException if key is null
     */
    public ConfigurationBuilder remove(String key) {
        Objects.requireNonNull(key, "key must not be null");
        entries.remove(key);
        return this;
    }

    /**
     * Removes all configuration entries.
     *
     * @return this builder for method chaining
     */
    public ConfigurationBuilder clear() {
        entries.clear();
        return this;
    }

    /**
     * Builds an immutable {@link Configuration} instance from the current builder state.
     *
     * <p>The returned configuration is immutable and thread-safe. Subsequent modifications to this
     * builder will not affect the returned configuration.
     *
     * <p>This method can be called multiple times to create multiple configuration instances with
     * the same content.
     *
     * @return a new immutable Configuration instance
     */
    public Configuration build() {
        return new MapConfiguration(new HashMap<>(entries));
    }
}
