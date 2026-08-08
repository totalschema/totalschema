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

package io.github.totalschema.engine.core.container;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class ObjectSpecification {

    public static ObjectSpecification from(FactorySpecification factorySpecification) {
        return from(
                factorySpecification.getConstructedClass(), factorySpecification.getQualifier());
    }

    public static ObjectSpecification from(Class<?> clazz) {
        return from(clazz, null, null);
    }

    public static ObjectSpecification from(Class<?> clazz, String qualifier) {
        return from(clazz, qualifier, null);
    }

    public static ObjectSpecification from(Class<?> clazz, String qualifier, Object[] arguments) {
        return new ObjectSpecification(clazz, qualifier, arguments);
    }

    private final Class<?> type;
    private final String qualifier;
    private final List<Object> arguments;

    private ObjectSpecification(Class<?> type, String qualifier, Object[] arguments) {
        this(type, qualifier, arrayToList(arguments));
    }

    private static List<Object> arrayToList(Object[] arguments) {
        List<Object> argumentsAsList;
        if (arguments == null || arguments.length == 0) {
            argumentsAsList = null;
        } else {
            argumentsAsList = List.of(arguments);
        }

        return argumentsAsList;
    }

    private ObjectSpecification(Class<?> type, String qualifier, List<Object> arguments) {
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.qualifier = qualifier; // qualifier can be null
        this.arguments = arguments;
    }

    public Class<?> getType() {
        return type;
    }

    public String getQualifier() {
        return qualifier;
    }

    public Optional<List<Object>> getArguments() {
        return Optional.ofNullable(arguments);
    }

    public ObjectSpecification withQualifier(String qualifier) {
        return new ObjectSpecification(this.type, qualifier, this.arguments);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ObjectSpecification that = (ObjectSpecification) o;
        return Objects.equals(type, that.type)
                && Objects.equals(qualifier, that.qualifier)
                && Objects.equals(arguments, that.arguments);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, qualifier, arguments);
    }

    @Override
    public String toString() {
        return "ObjectSpecification{"
                + "type="
                + type
                + ", qualifier='"
                + qualifier
                + '\''
                + ", arguments="
                + arguments
                + '}';
    }
}
