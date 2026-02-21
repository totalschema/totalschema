package io.github.totalschema.engine.core.container;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class ObjectSpecification {

    public static ObjectSpecification from(FactorySpecification factorySpecification) {
        return from(
                factorySpecification.getConstructedClass(), factorySpecification.getQualifier());
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
