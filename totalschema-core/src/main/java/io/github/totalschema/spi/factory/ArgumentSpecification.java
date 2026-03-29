package io.github.totalschema.spi.factory;

import io.github.totalschema.config.Configuration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Specification for a factory argument, including type information and optional validation
 * constraints.
 *
 * <p>ArgumentSpecification supports two levels of validation:
 *
 * <ul>
 *   <li><b>Type validation</b>: Ensures the argument is of the correct Java type
 *   <li><b>Constraint validation</b>: Applies custom validation rules such as string length,
 *       numeric ranges, patterns, etc.
 * </ul>
 *
 * <p><b>Example Usage:</b>
 *
 * <pre>{@code
 * // Simple argument (no constraints)
 * ArgumentSpecification<String> NAME_ARG = string("name");
 *
 * // String with length constraints
 * ArgumentSpecification<String> HOST_ARG = string("host", 1, 255);
 *
 * // Integer with range constraint
 * ArgumentSpecification<Integer> PORT_ARG = integer("port", 1, 65535);
 *
 * // String with pattern constraint
 * ArgumentSpecification<String> EMAIL_ARG =
 *     string("email").withConstraint(pattern("^[^@]+@[^@]+$"));
 *
 * // Custom constraint
 * ArgumentSpecification<Integer> EVEN_ARG =
 *     integer("retryCount", 0, 10)
 *         .withConstraint(custom(n -> n % 2 == 0, "must be even"));
 * }</pre>
 */
public class ArgumentSpecification<T> {
    private final Class<T> type;
    private final String name;
    private final List<Constraint<T>> constraints;

    /**
     * Creates an argument specification without constraints.
     *
     * @param type The expected type of the argument
     * @param name The name of the argument (for error messages)
     */
    public ArgumentSpecification(Class<T> type, String name) {
        this(type, name, List.of());
    }

    private ArgumentSpecification(Class<T> type, String name, List<Constraint<T>> constraints) {
        this.type = type;
        this.name = name;
        this.constraints = constraints;
    }

    /**
     * Validates a value against this specification's constraints.
     *
     * <p>This method should be called after type checking to validate custom constraints. The value
     * must already be confirmed to be of the correct type.
     *
     * @param value The value to validate (must be non-null and type-checked)
     * @throws IllegalArgumentException if any constraint validation fails
     */
    public void validateValue(T value) {
        for (Constraint<T> constraint : constraints) {
            if (!constraint.test(value)) {
                throw new IllegalArgumentException(
                        "Argument '"
                                + name
                                + "' validation failed: "
                                + constraint.getErrorMessage(value));
            }
        }
    }

    /**
     * Returns a new ArgumentSpecification with an additional constraint.
     *
     * <p>This method uses a fluent API pattern, allowing multiple constraints to be chained:
     *
     * <pre>{@code
     * ArgumentSpecification<String> spec = string("email")
     *     .withConstraint(minLength(5))
     *     .withConstraint(maxLength(320))
     *     .withConstraint(pattern("@"));
     * }</pre>
     *
     * @param constraint The constraint to add
     * @return A new ArgumentSpecification with the added constraint
     */
    public ArgumentSpecification<T> withConstraint(Constraint<T> constraint) {
        List<Constraint<T>> newConstraints = new ArrayList<>(this.constraints);
        newConstraints.add(constraint);
        return new ArgumentSpecification<>(type, name, newConstraints);
    }

    // ========== Factory Methods ==========

    /**
     * Creates a String argument specification without constraints.
     *
     * @param name The argument name
     * @return A String argument specification
     */
    public static ArgumentSpecification<String> string(String name) {
        return new ArgumentSpecification<>(String.class, name);
    }

    /**
     * Creates a String argument specification with length constraints.
     *
     * @param name The argument name
     * @param minLength Minimum allowed length (inclusive)
     * @param maxLength Maximum allowed length (inclusive)
     * @return A String argument specification with length validation
     */
    public static ArgumentSpecification<String> string(String name, int minLength, int maxLength) {
        return string(name)
                .withConstraint(minLength(minLength))
                .withConstraint(maxLength(maxLength));
    }

    /**
     * Creates a Configuration argument specification.
     *
     * @param name The argument name
     * @return A Configuration argument specification
     */
    public static ArgumentSpecification<Configuration> configuration(String name) {
        return new ArgumentSpecification<>(Configuration.class, name);
    }

    /**
     * Creates an Integer argument specification with range constraints.
     *
     * @param name The argument name
     * @param min Minimum allowed value (inclusive)
     * @param max Maximum allowed value (inclusive)
     * @return An Integer argument specification with range validation
     */
    public static ArgumentSpecification<Integer> integer(String name, int min, int max) {
        return new ArgumentSpecification<>(Integer.class, name).withConstraint(range(min, max));
    }

    // ========== Common Constraint Factories ==========

    /**
     * Creates a constraint that validates a value is within a range.
     *
     * @param min The minimum allowed value (inclusive)
     * @param max The maximum allowed value (inclusive)
     * @param <T> The type of value (must be Comparable)
     * @return A range constraint
     */
    public static <T extends Comparable<T>> Constraint<T> range(T min, T max) {
        return new Constraint<T>() {
            @Override
            public boolean test(T value) {
                return value.compareTo(min) >= 0 && value.compareTo(max) <= 0;
            }

            @Override
            public String getErrorMessage(T value) {
                return "must be between " + min + " and " + max + " (got " + value + ")";
            }
        };
    }

    /**
     * Creates a constraint that validates a string has a minimum length.
     *
     * @param min The minimum allowed length
     * @return A minimum length constraint
     */
    public static Constraint<String> minLength(int min) {
        return new Constraint<String>() {
            @Override
            public boolean test(String value) {
                return value.length() >= min;
            }

            @Override
            public String getErrorMessage(String value) {
                return "must be at least " + min + " characters (got " + value.length() + ")";
            }
        };
    }

    /**
     * Creates a constraint that validates a string has a maximum length.
     *
     * @param max The maximum allowed length
     * @return A maximum length constraint
     */
    public static Constraint<String> maxLength(int max) {
        return new Constraint<String>() {
            @Override
            public boolean test(String value) {
                return value.length() <= max;
            }

            @Override
            public String getErrorMessage(String value) {
                return "must be at most " + max + " characters (got " + value.length() + ")";
            }
        };
    }

    /**
     * Creates a constraint that validates a string matches a regular expression pattern.
     *
     * @param regex The regular expression pattern
     * @return A pattern matching constraint
     */
    public static Constraint<String> pattern(String regex) {
        Pattern compiled = Pattern.compile(regex);
        return new Constraint<String>() {
            @Override
            public boolean test(String value) {
                return compiled.matcher(value).matches();
            }

            @Override
            public String getErrorMessage(String value) {
                return "must match pattern '" + regex + "'";
            }
        };
    }

    /**
     * Creates a constraint that validates a string is not blank (not empty and not
     * whitespace-only).
     *
     * @return A not-blank constraint
     */
    public static Constraint<String> notBlank() {
        return new Constraint<String>() {
            @Override
            public boolean test(String value) {
                return value != null && !value.trim().isEmpty();
            }

            @Override
            public String getErrorMessage(String value) {
                if (value == null) {
                    return "cannot be null";
                } else if (value.isEmpty()) {
                    return "cannot be empty";
                } else {
                    return "cannot be blank (whitespace-only)";
                }
            }
        };
    }

    /**
     * Creates a custom constraint with a predicate and error message.
     *
     * @param test The predicate to test values
     * @param errorMessage The error message when validation fails
     * @param <T> The type of value
     * @return A custom constraint
     */
    public static <T> Constraint<T> custom(Predicate<T> test, String errorMessage) {
        return new Constraint<T>() {
            @Override
            public boolean test(T value) {
                return test.test(value);
            }

            @Override
            public String getErrorMessage(T value) {
                return errorMessage;
            }
        };
    }

    // ========== Accessors ==========

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
