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

package io.github.totalschema.jdbc;

import io.github.totalschema.config.Configuration;
import io.github.totalschema.engine.cache.NamedConfigKey;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultJdbcDatabaseFactory implements JdbcDatabaseFactory {

    private static final ConcurrentHashMap<NamedConfigKey, JdbcDatabase> CACHE =
            new ConcurrentHashMap<>();

    @Override
    public JdbcDatabase getJdbcDatabase(String name, Configuration configuration) {

        NamedConfigKey cacheKey = new NamedConfigKey(name, configuration);

        return CACHE.computeIfAbsent(cacheKey, (key) -> newJdbcDatabase(name, configuration));
    }

    @Override
    public JdbcDatabase newJdbcDatabase(String name, Configuration configuration) {
        return DefaultJdbcDatabase.newInstance(name, configuration);
    }
}
