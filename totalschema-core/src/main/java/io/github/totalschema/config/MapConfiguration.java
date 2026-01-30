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

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class MapConfiguration extends AbstractConfiguration {

    private final Map<String, String> map;

    public MapConfiguration(Map<String, String> map) {
        this.map = Collections.unmodifiableMap(map);
    }

    @Override
    public String toString() {
        return String.format("Configuration {%s}", map);
    }

    @Override
    public Optional<String> getString(String key) {
        return Optional.ofNullable(map.get(key));
    }

    @Override
    public Set<String> getKeys() {
        return map.keySet();
    }
}
