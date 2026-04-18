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
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import org.yaml.snakeyaml.Yaml;

public class YamlFileConfiguration extends AbstractConfiguration {

    private final Supplier<Optional<InputStream>> inputStreamProvider;
    private final String description;
    private final LockTemplate lockTemplate =
            new LockTemplate(1, TimeUnit.MINUTES, new ReentrantLock());

    private Map<String, String> config;

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
        ensureLoaded();
        return Optional.ofNullable(config.get(key));
    }

    @Override
    public Set<String> getKeys() {
        ensureLoaded();
        return Collections.unmodifiableSet(config.keySet());
    }

    private void ensureLoaded() {
        lockTemplate.withTryLock(this::loadAndFlattenYaml);
    }

    private void loadAndFlattenYaml() {
        if (config != null) {
            return;
        }

        Optional<InputStream> inputStreamOptional = inputStreamProvider.get();

        if (inputStreamOptional.isPresent()) {
            try {
                try (InputStream inputStream = inputStreamOptional.get()) {
                    Yaml yaml = new Yaml();

                    Map<String, Object> loadedMap = yaml.loadAs(inputStream, Map.class);

                    if (loadedMap != null) {
                        config = flattenMapToStringValues(loadedMap);
                    } else {
                        config = Collections.emptyMap();
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to read: " + this, e);
            }
        } else {
            config = Collections.emptyMap();
        }
    }

    /**
     * Flattens nested YAML structure into dot-notation keys at load time. For example: {database:
     * {host: "localhost"}} becomes "database.host" = "localhost". Only leaf values (non-Map) are
     * included.
     *
     * @param source the nested YAML data structure
     * @return a flattened map with dot-notation keys
     */
    private static Map<String, String> flattenMapToStringValues(Map<String, Object> source) {
        Map<String, String> result = new LinkedHashMap<>();
        java.util.Deque<MapEntry> stack = new java.util.ArrayDeque<>();

        // Initialize stack with root entries
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            stack.push(new MapEntry(entry.getKey(), entry.getValue(), ""));
        }

        while (!stack.isEmpty()) {
            MapEntry current = stack.pop();
            String key =
                    current.prefix.isEmpty() ? current.key : current.prefix + "." + current.key;

            if (current.value instanceof Map) {
                // Push nested entries onto stack, don't add the intermediate key itself
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedMap = (Map<String, Object>) current.value;
                for (Map.Entry<String, Object> entry : nestedMap.entrySet()) {
                    stack.push(new MapEntry(entry.getKey(), entry.getValue(), key));
                }
            } else if (current.value instanceof List) {
                // Flatten list entries as indexed keys: key.0, key.1, ...
                @SuppressWarnings("unchecked")
                List<Object> list = (List<Object>) current.value;
                for (int i = 0; i < list.size(); i++) {
                    Object item = list.get(i);
                    if (item != null) {
                        stack.push(new MapEntry(String.valueOf(i), item, key));
                    }
                }
            } else if (current.value != null) {
                // Only add leaf keys (non-Map, non-List values) as strings
                result.put(key, current.value.toString());
            }
        }

        return result;
    }

    /** Helper class for iterative map traversal. */
    private static final class MapEntry {
        final String key;
        final Object value;
        final String prefix;

        MapEntry(String key, Object value, String prefix) {
            this.key = key;
            this.value = value;
            this.prefix = prefix;
        }
    }
}
