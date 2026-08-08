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

/**
 * A validation constraint that can be applied to factory arguments.
 *
 * <p>Constraints enable custom validation logic beyond basic type checking, such as string length
 * validation, numeric range checks, pattern matching, and custom business rules.
 *
 * <p><b>Example Usage:</b>
 *
 * <pre>{@code
 * // Range constraint for integers
 * Constraint<Integer> portRange = new Constraint<Integer>() {
 *     @Override
 *     public boolean test(Integer value) {
 *         return value >= 1 && value <= 65535;
 *     }
 *
 *     @Override
 *     public String getErrorMessage(Integer value) {
 *         return "port must be between 1 and 65535 (got " + value + ")";
 *     }
 * };
 *
 * // Using with ArgumentSpecification
 * ArgumentSpecification<Integer> PORT_ARG =
 *     new ArgumentSpecification<>(Integer.class, "port")
 *         .withConstraint(portRange);
 * }</pre>
 *
 * @param <T> The type of value this constraint validates
 * @see ArgumentSpecification#withConstraint(Constraint)
 * @see ArgumentSpecification#validateValue(Object)
 */
@FunctionalInterface
public interface Constraint<T> {

    /**
     * Tests if the value satisfies this constraint.
     *
     * @param value The value to test (never {@code null}, already type-checked)
     * @return {@code true} if the constraint is satisfied, {@code false} otherwise
     */
    boolean test(T value);

    /**
     * Returns a descriptive error message when the constraint fails.
     *
     * <p>This method is called only when {@link #test(Object)} returns {@code false}. The message
     * should clearly describe what constraint was violated and include the actual value for
     * debugging purposes.
     *
     * @param value The value that failed validation
     * @return A human-readable error message describing the constraint violation
     */
    default String getErrorMessage(T value) {
        return "constraint violation for value: " + value;
    }
}
