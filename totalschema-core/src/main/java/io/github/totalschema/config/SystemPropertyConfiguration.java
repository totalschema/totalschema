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

import io.github.totalschema.ProjectConventions;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class SystemPropertyConfiguration extends AbstractConfiguration {

    public static final String JVM_SYSTEM_PROPERTY_PREFIX =
            String.format("%s.", ProjectConventions.PROJECT_SYSTEM_NAME);

    @Override
    public Optional<String> getString(String key) {

        String systemPropertyKey = String.format("%s%s", JVM_SYSTEM_PROPERTY_PREFIX, key);

        return Optional.ofNullable(System.getProperty(systemPropertyKey));
    }

    @Override
    public Set<String> getKeys() {
        return System.getProperties().keySet().stream()
                .map(Objects::toString)
                .filter(it -> it.startsWith(JVM_SYSTEM_PROPERTY_PREFIX))
                .map(it -> it.substring(JVM_SYSTEM_PROPERTY_PREFIX.length()))
                .collect(Collectors.toSet());
    }

    @Override
    public String toString() {
        return String.format("JVM System properties with '%s' prefix", JVM_SYSTEM_PROPERTY_PREFIX);
    }
}
