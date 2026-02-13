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

package io.github.totalschema.engine.cache;

import io.github.totalschema.config.Configuration;
import java.util.Map;
import java.util.Objects;

/**
 * A composite key used for caching and indexing resource types that are identified by both a name
 * and their associated configuration properties.
 *
 * <p>This class is designed to be used as a key in {@link java.util.HashMap} and similar
 * collections, where resources need to be uniquely identified by the combination of their name and
 * configuration settings. Two resources with the same name but different configurations will be
 * treated as distinct entries.
 *
 * <p>The class is immutable and properly implements {@link #equals(Object)} and {@link #hashCode()}
 * to ensure correct behavior when used as a map key. The configuration map is defensively copied to
 * maintain immutability.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * Map<NamedConfigKey, Resource> resourceCache = new HashMap<>();
 * NamedConfigKey key = new NamedConfigKey("myResource", configuration);
 * resourceCache.put(key, resource);
 * }</pre>
 *
 * @see Configuration
 */
public final class NamedConfigKey {

    /** The name identifier for the resource. */
    private final String name;

    /**
     * The configuration properties associated with the resource, stored as an immutable map. May be
     * {@code null} if no configuration is provided.
     */
    private final Map<String, String> configMap;

    /**
     * Constructs a new {@code NamedConfigKey} with the specified name and configuration.
     *
     * @param name the name identifier for the resource
     * @param configuration the configuration object containing the resource's settings
     */
    public NamedConfigKey(String name, Configuration configuration) {
        this(name, configuration.asMap().orElse(null));
    }

    /**
     * Constructs a new {@code NamedConfigKey} with the specified name and configuration map.
     *
     * <p>The configuration map is defensively copied to ensure immutability. If the provided map is
     * {@code null}, the configuration will be stored as {@code null}.
     *
     * @param name the name identifier for the resource
     * @param configMap the configuration properties as a map, or {@code null} if no configuration
     *     is provided
     */
    public NamedConfigKey(String name, Map<String, String> configMap) {
        this.name = name;
        this.configMap = configMap != null ? Map.copyOf(configMap) : null;
    }

    /**
     * Compares this key with another object for equality.
     *
     * <p>Two {@code NamedConfigKey} instances are considered equal if they have the same name and
     * equivalent configuration maps.
     *
     * @param o the object to compare with
     * @return {@code true} if the objects are equal, {@code false} otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NamedConfigKey cacheKey = (NamedConfigKey) o;
        return Objects.equals(name, cacheKey.name) && Objects.equals(configMap, cacheKey.configMap);
    }

    /**
     * Returns a hash code value for this key.
     *
     * <p>The hash code is computed based on both the name and configuration map to ensure proper
     * distribution in hash-based collections.
     *
     * @return a hash code value for this key
     */
    @Override
    public int hashCode() {
        return Objects.hash(name, configMap);
    }
}
