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

package io.github.totalschema.engine.internal.variables;

import io.github.totalschema.config.environment.Environment;
import java.util.Objects;

final class VariableCacheKey {

    private final Environment environment;

    VariableCacheKey(Environment environment) {
        this.environment = environment;
    }

    public Environment getEnvironment() {
        return environment;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        VariableCacheKey variableCacheKey = (VariableCacheKey) o;
        return Objects.equals(environment, variableCacheKey.environment);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(environment);
    }

    @Override
    public String toString() {
        return "VariableCacheKey{" + "environment=" + environment + '}';
    }
}
