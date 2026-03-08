package io.github.totalschema.spi;

import io.github.totalschema.config.Configuration;

/** Specification for a factory argument. */
public class ArgumentSpecification<T> {
    private final Class<T> type;
    private final String name;

    /**
     * Creates an argument specification.
     *
     * @param type The expected type of the argument
     * @param name The name of the argument (for error messages)
     */
    public ArgumentSpecification(Class<T> type, String name) {
        this.type = type;
        this.name = name;
    }

    public static ArgumentSpecification<String> string(String name) {
        return new ArgumentSpecification<>(String.class, name);
    }

    public static ArgumentSpecification<Configuration> configuration(String name) {
        return new ArgumentSpecification<>(Configuration.class, name);
    }

    public Class<T> getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return type.getSimpleName() + " " + name;
    }
}
