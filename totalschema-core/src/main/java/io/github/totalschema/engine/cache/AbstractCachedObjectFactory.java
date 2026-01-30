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
import io.github.totalschema.engine.core.command.api.CommandContext;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Abstract factory that provides thread-safe caching of created objects based on name and
 * configuration.
 *
 * <p>This factory uses a concurrent hash map to cache instances, preventing duplicate creation of
 * objects with the same name and configuration. The cache key is a composite of the object name and
 * its configuration, ensuring that different configurations result in different cached instances.
 *
 * <p>Subclasses must implement the {@link #createNewObject(String, Configuration, CommandContext)}
 * method to define how new instances are created when they are not found in the cache.
 *
 * <p>This implementation is thread-safe and suitable for use in concurrent environments.
 *
 * @param <S> the type of object this factory creates and caches
 * @author totalschema development team
 * @since 1.0-SNAPSHOT
 */
public abstract class AbstractCachedObjectFactory<S> {

    private final ConcurrentHashMap<NamedConfigKey, S> cache = new ConcurrentHashMap<>();

    /**
     * Gets a cached object or creates a new one if not present in the cache.
     *
     * <p>This method performs a thread-safe lookup in the cache using a composite key consisting of
     * the object name and configuration. If the object is already cached, it returns the cached
     * instance. Otherwise, it atomically creates a new instance using {@link
     * #createNewObject(String, Configuration, CommandContext)}, caches it, and returns it.
     *
     * <p>The atomic computation ensures that even in concurrent environments, only one instance
     * will be created for a given name-configuration pair, preventing race conditions.
     *
     * @param name the unique name identifier for the object
     * @param configuration the configuration used to create and identify the object
     * @param context the command context providing runtime information and dependencies
     * @return the cached object if present, or a newly created and cached object
     * @throws NullPointerException if name, configuration, or context is null
     */
    protected final S getMemoizedObject(
            String name, Configuration configuration, CommandContext context) {

        return cache.computeIfAbsent(
                new NamedConfigKey(name, configuration),
                (namedConfigKey) -> createNewObject(name, configuration, context));
    }

    /**
     * Creates a new object instance when not found in the cache.
     *
     * <p>Subclasses must implement this method to define the creation logic for new objects. This
     * method is called by {@link #getMemoizedObject(String, Configuration, CommandContext)} only
     * when an object with the given name and configuration is not already cached.
     *
     * <p>Implementations should ensure that this method is deterministic - calling it with the same
     * parameters should produce functionally equivalent objects, as the caching mechanism relies on
     * this assumption.
     *
     * @param name the unique name identifier for the object to create
     * @param configuration the configuration to use for object creation
     * @param context the command context providing runtime information and dependencies
     * @return a newly created object of type S, must not be null
     * @throws RuntimeException if object creation fails for any reason
     */
    protected abstract S createNewObject(
            String name, Configuration configuration, CommandContext context);
}
