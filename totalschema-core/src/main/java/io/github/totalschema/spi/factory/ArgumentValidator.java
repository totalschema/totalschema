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

package io.github.totalschema.spi.factory;

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
     * @return The validated arguments list (same as input if valid)
     * @throws IllegalArgumentException if validation fails
     */
    public static List<Object> validate(
            List<Object> arguments, List<ArgumentSpecification<?>> specs, String factoryName) {

        if (specs == null || specs.isEmpty()) {
            if (arguments != null && !arguments.isEmpty()) {
                throw new IllegalArgumentException(
                        "Factory "
                                + factoryName
                                + " expects no arguments, but "
                                + arguments.size()
                                + " were provided");
            }
            return List.of();
        }

        int expectedCount = specs.size();
        int providedCount = (arguments == null) ? 0 : arguments.size();

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
                            + formatList(specs)
                            + ", Was: "
                            + formatList(arguments));
        }

        // Validate each argument type
        for (int i = 0; i < expectedCount; i++) {
            ArgumentSpecification<?> spec = specs.get(i);
            Object value = arguments.get(i);

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
     * @param elements The argument specifications
     * @return A formatted string describing the expected arguments
     */
    private static String formatList(List<?> elements) {
        if (elements == null || elements.isEmpty()) {
            return "(none)";
        }

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < elements.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(elements.get(i).toString());
        }
        sb.append("]");
        return sb.toString();
    }
}
