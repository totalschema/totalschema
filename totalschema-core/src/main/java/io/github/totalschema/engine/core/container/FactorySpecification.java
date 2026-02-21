package io.github.totalschema.engine.core.container;

import io.github.totalschema.spi.ComponentFactory;
import java.util.Objects;

public final class FactorySpecification {

    public static FactorySpecification from(ComponentFactory<?> factory) {
        return from(factory.getConstructedClass(), factory.getQualifier());
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
