package io.github.totalschema.spi;

import java.util.List;

/** Validates arguments against their specifications. */
public final class ArgumentValidator {

    private ArgumentValidator() {
        // Utility class
    }

    /**
     * Validates the provided arguments according to the specifications.
     *
     * <p>This method validates:
     *
     * <ul>
     *   <li>The number of provided arguments matches the number of specifications
     *   <li>Each argument is of the correct type (strict type checking, no conversion)
     *   <li>No argument is null
     * </ul>
     *
     * @param arguments The arguments to validate
     * @param specs The argument specifications
     * @param factoryName The name of the factory (for error messages)
     * @return The validated arguments array (same as input if valid)
     * @throws IllegalArgumentException if validation fails
     */
    public static Object[] validate(
            Object[] arguments, List<ArgumentSpecification<?>> specs, String factoryName) {

        if (specs == null || specs.isEmpty()) {
            if (arguments != null && arguments.length > 0) {
                throw new IllegalArgumentException(
                        "Factory "
                                + factoryName
                                + " expects no arguments, but "
                                + arguments.length
                                + " were provided");
            }
            return new Object[0];
        }

        int expectedCount = specs.size();
        int providedCount = (arguments == null) ? 0 : arguments.length;

        if (providedCount != expectedCount) {
            throw new IllegalArgumentException(
                    "Factory "
                            + factoryName
                            + " expects exactly "
                            + expectedCount
                            + " argument(s), but "
                            + providedCount
                            + " were provided. "
                            + "Expected: "
                            + formatSpecs(specs));
        }

        // Validate each argument type
        for (int i = 0; i < expectedCount; i++) {
            ArgumentSpecification<?> spec = specs.get(i);
            Object value = arguments[i];

            validateSingleArgument(value, spec, i);
        }

        return arguments;
    }

    /**
     * Validates a single argument against its specification.
     *
     * @param value The argument value
     * @param spec The argument specification
     * @param index The argument index (for error messages)
     * @throws IllegalArgumentException if validation fails
     */
    private static void validateSingleArgument(
            Object value, ArgumentSpecification<?> spec, int index) {

        if (value == null) {
            throw new IllegalArgumentException(
                    "Argument " + index + " ('" + spec.getName() + "') cannot be null");
        }

        if (!spec.getType().isInstance(value)) {
            throw new IllegalArgumentException(
                    "Argument "
                            + index
                            + " ('"
                            + spec.getName()
                            + "') has incorrect type. "
                            + "Expected: "
                            + spec.getType().getName()
                            + ", "
                            + "but got: "
                            + value.getClass().getName());
        }
    }

    /**
     * Formats argument specifications for error messages.
     *
     * @param specs The argument specifications
     * @return A formatted string describing the expected arguments
     */
    private static String formatSpecs(List<ArgumentSpecification<?>> specs) {
        if (specs.isEmpty()) {
            return "(none)";
        }

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < specs.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(specs.get(i).toString());
        }
        sb.append("]");
        return sb.toString();
    }
}
