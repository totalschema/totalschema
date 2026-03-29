package io.github.totalschema.spi;

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
