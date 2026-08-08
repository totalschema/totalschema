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

import io.github.totalschema.spi.factory.ComponentFactory;
import java.util.Objects;

public final class FactorySpecification {

    public static FactorySpecification from(ComponentFactory<?> factory) {
        return from(factory.getComponentType(), factory.getQualifier().orElse(null));
    }

    public static FactorySpecification from(ObjectSpecification objectSpecification) {
        return from(objectSpecification.getType(), objectSpecification.getQualifier());
    }

    public static FactorySpecification from(Class<?> clazz, String qualifier) {
        return new FactorySpecification(clazz, qualifier);
    }

    public FactorySpecification(Class<?> constructedClass, String qualifier) {
        this.constructedClass = constructedClass;
        this.qualifier = qualifier;
    }

    private final Class<?> constructedClass;
    private final String qualifier;

    public Class<?> getConstructedClass() {
        return constructedClass;
    }

    public String getQualifier() {
        return qualifier;
    }

    public FactorySpecification withQualifier(String qualifier) {
        return new FactorySpecification(this.constructedClass, qualifier);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        FactorySpecification that = (FactorySpecification) o;
        return Objects.equals(constructedClass, that.constructedClass)
                && Objects.equals(qualifier, that.qualifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(constructedClass, qualifier);
    }

    @Override
    public String toString() {
        return "FactorySpecification{"
                + "type="
                + constructedClass
                + ", qualifier='"
                + qualifier
                + '\''
                + '}';
    }
}
