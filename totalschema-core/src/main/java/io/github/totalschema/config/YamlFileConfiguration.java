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

import io.github.totalschema.concurrent.LockTemplate;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.function.Supplier;
import org.yaml.snakeyaml.Yaml;

public class YamlFileConfiguration extends AbstractConfiguration {

    private final Supplier<Optional<InputStream>> inputStreamProvider;
    private final String description;
    private final LockTemplate lockTemplate =
            new LockTemplate(1, TimeUnit.MINUTES, new ReentrantLock());

    private Map<String, Object> yamlData;

    /**
     * Creates a YamlFileConfiguration with an injectable strategy for opening input streams.
     *
     * @param inputStreamProvider the strategy for providing input streams
     * @param description a human-readable description of the configuration source
     */
    public YamlFileConfiguration(
            Supplier<Optional<InputStream>> inputStreamProvider, String description) {
        this.inputStreamProvider =
                Objects.requireNonNull(inputStreamProvider, "inputStreamProvider cannot be null");
        this.description = Objects.requireNonNull(description, "description cannot be null");
    }

    @Override
    public String toString() {
        return description;
    }

    @Override
    public Optional<String> getString(String key) {
        return withYamlData(
                data -> {
                    Object value = getNestedValue(data, key);
                    return value != null ? Optional.of(value.toString()) : Optional.empty();
                });
    }

    @Override
    public Set<String> getKeys() {
        return withYamlData(data -> flattenKeys(data, ""));
    }

    private <R> R withYamlData(Function<Map<String, Object>, R> action) {
        return lockTemplate.withTryLock(() -> withYamlDataLoaded(action));
    }

    private <R> R withYamlDataLoaded(Function<Map<String, Object>, R> action) {
        if (yamlData == null) {
            yamlData = new LinkedHashMap<>();

            try {
                Optional<InputStream> inputStreamOptional = inputStreamProvider.get();

                if (inputStreamOptional.isPresent()) {
                    InputStream inputStream = inputStreamOptional.get();
                    try (inputStream) {
                        Yaml yaml = new Yaml();
                        Object loaded = yaml.load(inputStream);

                        if (loaded instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> loadedMap = (Map<String, Object>) loaded;
                            yamlData.putAll(loadedMap);
                        }
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to read: " + this, e);
            }
        }

        return action.apply(yamlData);
    }

    /**
     * Gets a nested value from the YAML data using dot notation. For example: "database.host" will
     * retrieve the "host" value from the "database" map.
     */
    private Object getNestedValue(Map<String, Object> data, String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }

        String[] parts = key.split("\\.");
        Object current = data;

        for (String part : parts) {
            if (current instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) current;
                current = map.get(part);
            } else {
                return null;
            }
        }

        return current;
    }

    /**
     * Flattens nested YAML structure into dot-notation keys. For example: {database: {host:
     * "localhost"}} becomes "database.host"
     */
    private Set<String> flattenKeys(Map<String, Object> data, String prefix) {
        if (data == null || data.isEmpty()) {
            return Collections.emptySet();
        }

        Set<String> keys = new java.util.HashSet<>();

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            keys.add(key);

            if (entry.getValue() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedMap = (Map<String, Object>) entry.getValue();
                keys.addAll(flattenKeys(nestedMap, key));
            }
        }

        return keys;
    }
}
