package io.github.totalschema.spi.factory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Handles argument retrieval and validation for component factories.
 *
 * <p>This class encapsulates the logic for mapping {@link ArgumentSpecification}s to their
 * positions in an arguments array, performing type checking, and applying validation constraints.
 * It provides O(1) argument lookup using an internal index map.
 *
 * <p><b>Responsibilities:</b>
 *
 * <ul>
 *   <li>Building and caching the specification-to-index mapping
 *   <li>Validating argument structure (count, types, nulls)
 *   <li>Retrieving individual arguments with type safety
 *   <li>Applying constraint validation
 * </ul>
 *
 * <p><b>Thread Safety:</b> This class is thread-safe. The index map is immutable after
 * construction.
 *
 * <p><b>Example Usage:</b>
 *
 * <pre>{@code
 * // In a ComponentFactory
 * private static final ArgumentSpecification<String> HOST = string("host").withConstraint(notBlank());
 * private static final ArgumentSpecification<Integer> PORT = integer("port", 1, 65535);
 *
 * @Override
 * public MyComponent createComponent(Context context, List<Object> arguments) {
 *     // Create handler from specifications (pass factory class for error reporting)
 *     ArgumentHandler handler = new ArgumentHandler(getClass(), HOST, PORT);
 *
 *     // Validate structure (no need to pass factory name)
 *     handler.validateStructure(arguments);
 *
 *     // Retrieve arguments safely
 *     String host = handler.getArgument(HOST, arguments);
 *     Integer port = handler.getArgument(PORT, arguments);
 *
 *     return new MyComponentImpl(host, port);
 * }
 * }</pre>
 *
 * @see ArgumentSpecification
 * @see ArgumentValidator
 */
public class ArgumentHandler {

    private final List<ArgumentSpecification<?>> specifications;
    private final Map<ArgumentSpecification<?>, Integer> indexMap;
    private final Class<?> factoryClass;

    /**
     * Creates a new argument handler for the given specifications.
     *
     * <p>This static factory method is the recommended way to create ArgumentHandler instances. It
     * enforces that at least one specification must be provided at compile-time, eliminating the
     * possibility of creating a handler with no specifications.
     *
     * <p>The handler builds an internal index map for O(1) argument lookup and stores the factory
     * class for error reporting.
     *
     * <p><b>Typical Usage Pattern:</b>
     *
     * <pre>{@code
     * public class MyFactory extends ComponentFactory<MyComponent> {
     *
     *     // Define argument specifications as static constants
     *     private static final ArgumentSpecification<String> NAME = string("name").withConstraint(notBlank());
     *     private static final ArgumentSpecification<Integer> PORT = integer("port", 1, 65535);
     *
     *     // Create a single shared ArgumentHandler instance
     *     private static final ArgumentHandler ARGUMENTS = ArgumentHandler.getInstance(MyFactory.class, NAME, PORT);
     *
     *     @Override
     *     public List<ArgumentSpecification<?>> getArgumentSpecifications() {
     *         return ARGUMENTS.getSpecifications();
     *     }
     *
     *     @Override
     *     public MyComponent createComponent(Context context, List<Object> arguments) {
     *         // Validate structure
     *         ARGUMENTS.validateStructure(arguments);
     *
     *         // Retrieve arguments using static constants
     *         String name = ARGUMENTS.getArgument(NAME, arguments);
     *         Integer port = ARGUMENTS.getArgument(PORT, arguments);
     *
     *         return new MyComponentImpl(name, port);
     *     }
     * }
     * }</pre>
     *
     * <p><b>Key Benefits:</b>
     *
     * <ul>
     *   <li>No need for nested subclasses - just use static constants
     *   <li>Factory class is captured for error reporting
     *   <li>Single ArgumentHandler instance shared across all component creations
     *   <li>Type-safe argument retrieval using the specification constants
     * </ul>
     *
     * @param factoryClass The factory class that will use this handler (used in validation error
     *     messages). Must extend ComponentFactory.
     * @param first The first (required) argument specification
     * @param rest Additional argument specifications in the order they appear in the arguments list
     * @return A new ArgumentHandler instance configured for the specified arguments
     */
    public static ArgumentHandler getInstance(
            Class<? extends ComponentFactory<?>> factoryClass,
            ArgumentSpecification<?> first,
            ArgumentSpecification<?>... rest) {

        return new ArgumentHandler(factoryClass, first, rest);
    }

    /**
     * Creates a new argument handler for the given specifications.
     *
     * <p>The handler builds an internal index map for O(1) argument lookup.
     *
     * <p>This constructor enforces that at least one specification must be provided at
     * compile-time, eliminating the possibility of creating a handler with no specifications.
     *
     * @param factoryClass The factory class for error reporting (used in validation messages)
     * @param first The first (required) argument specification
     * @param rest Additional argument specifications in the order they appear in the arguments list
     */
    private ArgumentHandler(
            Class<?> factoryClass,
            ArgumentSpecification<?> first,
            ArgumentSpecification<?>... rest) {
        this.factoryClass = factoryClass;
        this.specifications = List.copyOf(buildSpecificationsList(first, rest));
        this.indexMap = buildIndexMap(this.specifications);
    }

    /**
     * Combines the first specification with the rest into a single immutable list.
     *
     * @param first The first specification
     * @param rest The remaining specifications
     * @return An immutable list containing all specifications
     */
    private static List<ArgumentSpecification<?>> buildSpecificationsList(
            ArgumentSpecification<?> first, ArgumentSpecification<?>... rest) {
        if (rest == null || rest.length == 0) {
            return List.of(first);
        }

        LinkedList<ArgumentSpecification<?>> all = new LinkedList<>();
        all.add(first);
        all.addAll(List.of(rest));

        return all;
    }

    /**
     * Builds an immutable map from argument specifications to their indices.
     *
     * @param specs The list of specifications
     * @return A map from specification to index position
     */
    private static Map<ArgumentSpecification<?>, Integer> buildIndexMap(
            List<ArgumentSpecification<?>> specs) {
        Map<ArgumentSpecification<?>, Integer> map = new HashMap<>(specs.size());
        for (int i = 0; i < specs.size(); i++) {
            map.put(specs.get(i), i);
        }
        return map;
    }

    /**
     * Validates the structural correctness of the provided arguments.
     *
     * <p>This method performs structural validation only:
     *
     * <ul>
     *   <li>The number of provided arguments matches the number of specifications
     *   <li>Each argument is of the correct type
     *   <li>No argument is null
     * </ul>
     *
     * <p><b>Does NOT</b> validate custom constraints such as string length, numeric ranges, or
     * patterns. Those are validated when retrieving individual arguments via {@link
     * #getArgument(ArgumentSpecification, List)}.
     *
     * @param arguments The arguments list to validate
     * @throws IllegalArgumentException if validation fails
     */
    public void validateStructure(List<Object> arguments) {
        ArgumentValidator.validate(arguments, specifications, factoryClass.getSimpleName());
    }

    /**
     * Retrieves and validates a specific argument from the arguments list.
     *
     * <p>This method automatically determines the argument's position from the specifications list
     * provided at construction time. It performs:
     *
     * <ol>
     *   <li><b>Index lookup</b>: Finds the argument position using the cached index map (O(1))
     *   <li><b>Bounds checking</b>: Ensures the index is within the arguments list
     *   <li><b>Type checking</b>: Ensures the argument is of the correct type
     *   <li><b>Constraint validation</b>: Applies any custom constraints defined in the
     *       specification
     * </ol>
     *
     * @param spec The specification for the argument to retrieve (defines type and constraints)
     * @param args The full arguments list
     * @param <R> The expected type of the argument
     * @return The validated and type-cast argument value
     * @throws IllegalArgumentException if the specification is not in the specifications list, the
     *     index is out of bounds, the type is incorrect, or any constraint validation fails
     */
    public <R> R getArgument(ArgumentSpecification<R> spec, List<Object> args) {
        // Look up the index for this specification
        Integer index = indexMap.get(spec);
        if (index == null) {
            throw new IllegalArgumentException(
                    "ArgumentSpecification '"
                            + spec.getName()
                            + "' is not in the specifications list. "
                            + "Ensure the specification is included in the list provided to ArgumentHandler.");
        }

        // Bounds check
        if (index >= args.size()) {
            throw new IllegalArgumentException(
                    "Argument index "
                            + index
                            + " for specification '"
                            + spec.getName()
                            + "' is out of bounds (list size: "
                            + args.size()
                            + ")");
        }

        Object value = args.get(index);

        // Type check with clear error message
        if (!spec.getType().isInstance(value)) {
            throw new IllegalArgumentException(
                    "Argument at index "
                            + index
                            + " ('"
                            + spec.getName()
                            + "') has incorrect type. "
                            + "Expected: "
                            + spec.getType().getName()
                            + ", but got: "
                            + (value == null ? "null" : value.getClass().getName()));
        }

        // Safe cast using Class.cast()
        R typedValue = spec.getType().cast(value);

        // Apply custom constraints (e.g., string length, numeric ranges, etc.)
        spec.validateValue(typedValue);

        return typedValue;
    }

    /**
     * Returns the list of argument specifications managed by this handler.
     *
     * @return An immutable view of the specifications list
     */
    public List<ArgumentSpecification<?>> getSpecifications() {
        return specifications;
    }
}
