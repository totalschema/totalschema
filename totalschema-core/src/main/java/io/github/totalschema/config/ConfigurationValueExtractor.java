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

package io.github.totalschema.config;

import java.util.function.Consumer;

/**
 * Utility class for extracting configuration values and applying them to builder methods via
 * consumer functions.
 *
 * <p>This class facilitates the common pattern of reading configuration properties and passing them
 * to builder setter methods. It supports both required and optional values, with automatic type
 * conversion for integer values.
 *
 * <p><strong>Usage Example:</strong>
 *
 * <pre>{@code
 * Configuration config = Configuration.builder()
 *     .set("projectId", "my-project")
 *     .set("location", "US")
 *     .set("connectTimeout", "5000")
 *     .build();
 *
 * BigQueryOptions.Builder builder = BigQueryOptions.newBuilder();
 * ConfigurationValueExtractor extractor = new ConfigurationValueExtractor(config);
 *
 * extractor.extractRequired("projectId", builder::setProjectId);
 * extractor.extractOptional("location", builder::setLocation);
 * extractor.extractOptionalInt("connectTimeout", value -> { ... });
 * }</pre>
 */
public final class ConfigurationValueExtractor {
    private final Configuration configuration;

    /**
     * Creates a new extractor for the given configuration.
     *
     * @param configuration the configuration to extract values from
     */
    public ConfigurationValueExtractor(Configuration configuration) {
        this.configuration = configuration;
    }

    /**
     * Extracts a required configuration value and applies it via the provided consumer.
     *
     * <p>If the configuration value is missing, an {@code IllegalArgumentException} is thrown.
     *
     * @param key the configuration key
     * @param consumer the consumer that accepts the extracted value
     * @throws IllegalArgumentException if the configuration value is missing
     */
    public void extractRequired(String key, Consumer<String> consumer) {
        String value =
                configuration
                        .getString(key)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                String.format(
                                                        "Configuration value '%s' is missing",
                                                        key)));
        consumer.accept(value);
    }

    /**
     * Extracts an optional configuration value and applies it via the provided consumer if present.
     *
     * <p>If the configuration value is not present, the consumer is not invoked.
     *
     * @param key the configuration key
     * @param consumer the consumer that accepts the extracted value if present
     */
    public void extractOptional(String key, Consumer<String> consumer) {
        configuration.getString(key).ifPresent(consumer);
    }

    /**
     * Extracts an optional integer configuration value and applies it via the provided consumer if
     * present.
     *
     * <p>The string value is automatically converted to an integer. If the value is not a valid
     * integer, an {@code IllegalArgumentException} is thrown.
     *
     * @param key the configuration key
     * @param consumer the consumer that accepts the extracted integer value if present
     * @throws IllegalArgumentException if the value is not a valid integer
     */
    public void extractOptionalInt(String key, Consumer<Integer> consumer) {
        configuration
                .getString(key)
                .ifPresent(
                        value -> {
                            try {
                                consumer.accept(Integer.parseInt(value));
                            } catch (NumberFormatException e) {
                                throw new IllegalArgumentException(
                                        String.format(
                                                "Configuration value '%s' must be an integer, got: %s",
                                                key, value),
                                        e);
                            }
                        });
    }

    /**
     * Extracts an optional long configuration value and applies it via the provided consumer if
     * present.
     *
     * <p>The string value is automatically converted to a long. If the value is not a valid long,
     * an {@code IllegalArgumentException} is thrown.
     *
     * @param key the configuration key
     * @param consumer the consumer that accepts the extracted long value if present
     * @throws IllegalArgumentException if the value is not a valid long
     */
    public void extractOptionalLong(String key, Consumer<Long> consumer) {
        configuration
                .getString(key)
                .ifPresent(
                        value -> {
                            try {
                                consumer.accept(Long.parseLong(value));
                            } catch (NumberFormatException e) {
                                throw new IllegalArgumentException(
                                        String.format(
                                                "Configuration value '%s' must be a long, got: %s",
                                                key, value),
                                        e);
                            }
                        });
    }

    /**
     * Checks if a configuration value is present for the given key.
     *
     * @param key the configuration key
     * @return true if a value is present, false otherwise
     */
    public boolean hasKey(String key) {
        return configuration.getString(key).isPresent();
    }
}
