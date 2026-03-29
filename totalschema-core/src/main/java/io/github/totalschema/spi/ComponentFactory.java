package io.github.totalschema.spi;

import io.github.totalschema.engine.api.Context;
import io.github.totalschema.engine.core.container.FactorySpecification;
import io.github.totalschema.engine.core.container.ObjectSpecification;
import java.util.List;
import java.util.Map;

/**
 * Abstract factory for creating components within the TotalSchema IoC container.
 *
 * <p>This factory serves as a bridge between the container and component implementations, enabling
 * flexible component instantiation without reflection. Components can be customized and replaced by
 * users through the Service Provider Interface (SPI) mechanism.
 *
 * <p>Each factory is identified by:
 *
 * <ul>
 *   <li>The component type it creates ({@link #getComponentType()})
 *   <li>An optional qualifier for disambiguation ({@link #getQualifier()})
 * </ul>
 *
 * <p><b>Lifecycle:</b>
 *
 * <ol>
 *   <li>Factories are registered with the {@link
 *       io.github.totalschema.engine.core.container.ComponentContainerBuilder}
 *   <li>During container initialization, {@link #isEnabled(Map, Map)} is checked to determine if
 *       all required dependencies are available
 *   <li>Non-lazy factories without constructor arguments are instantiated immediately
 *   <li>Lazy factories or those requiring arguments are instantiated on-demand when requested
 * </ol>
 *
 * <p><b>Example Implementation:</b>
 *
 * <pre>{@code
 * public class CsvStateRepositoryFactory extends ComponentFactory<StateRepository> {
 *     @Override
 *     public boolean isLazy() { return true; }
 *
 *     @Override
 *     public Class<StateRepository> getComponentType() { return StateRepository.class; }
 *
 *     @Override
 *     public String getQualifier() { return "csv"; }
 *
 *     @Override
 *     public List<Class<?>> getRequiredContextTypes() { return List.of(Configuration.class); }
 *
 *     @Override
 *     public List<ArgumentSpecification<?>> getArgumentSpecifications() { return List.of(); }
 *
 *     @Override
 *     public StateRepository newComponent(Context context, Object... arguments) {
 *         Configuration config = context.get(Configuration.class);
 *         return new CsvStateRepository(config);
 *     }
 * }
 * }</pre>
 *
 * <p><b>Example with Arguments and Constraints:</b>
 *
 * <pre>{@code
 * public class ServerFactory extends ComponentFactory<Server> {
 *     private static final ArgumentSpecification<String> HOST_ARG = string("host", 1, 255);
 *     private static final ArgumentSpecification<Integer> PORT_ARG = integer("port", 1, 65535);
 *
 *     @Override
 *     public boolean isLazy() { return false; }
 *
 *     @Override
 *     public Class<Server> getComponentType() { return Server.class; }
 *
 *     @Override
 *     public String getQualifier() { return null; }
 *
 *     @Override
 *     public List<Class<?>> getRequiredContextTypes() { return List.of(Configuration.class); }
 *
 *     @Override
 *     public List<ArgumentSpecification<?>> getArgumentSpecifications() {
 *         return List.of(HOST_ARG, PORT_ARG);
 *     }
 *
 *     @Override
 *     public Server newComponent(Context context, Object... arguments) {
 *         validateArguments(arguments);  // Validate structure once
 *
 *         String host = getArgument(HOST_ARG, arguments, 0);  // Validates 1-255 length
 *         Integer port = getArgument(PORT_ARG, arguments, 1); // Validates 1-65535 range
 *
 *         Configuration config = context.get(Configuration.class);
 *         return new DefaultServer(config, host, port);
 *     }
 * }
 * }</pre>
 *
 * @param <T> The type of component this factory creates
 * @see io.github.totalschema.engine.core.container.ComponentContainer
 * @see io.github.totalschema.engine.core.container.ComponentContainerBuilder
 * @see ConditionalComponentFactory
 */
public abstract class ComponentFactory<T> {

    /**
     * Indicates whether the component should be created lazily (on-demand) or eagerly (at container
     * initialization).
     *
     * <p>Lazy components are created only when first requested via {@link Context#get(Class)} or
     * {@link Context#get(Class, String, Object...)}. Eager components are instantiated during
     * container initialization, but only if {@link #getArgumentSpecifications()} returns an empty
     * list.
     *
     * @return {@code true} if the component should be created lazily, {@code false} for eager
     *     initialization
     */
    public abstract boolean isLazy();

    /**
     * Returns the service interface or base class that clients use to lookup the component.
     *
     * <p><b>Critical:</b> If this factory provides an implementation for a service interface, you
     * <b>must</b> return the service interface type here, <b>not</b> the concrete implementation
     * class. This is required for the container to perform proper dependency wiring. The container
     * uses this type as the lookup key when components request dependencies via {@link
     * Context#get(Class)} or {@link Context#get(Class, String, Object...)}.
     *
     * <p>For example, if you have a {@code CsvStateRepository} class implementing the {@code
     * StateRepository} interface, return {@code StateRepository.class}, not {@code
     * CsvStateRepository.class}.
     *
     * <p>Multiple factories can produce the same type if they have different qualifiers (see {@link
     * #getQualifier()}).
     *
     * @return The service interface or base class type used for component lookup (never {@code
     *     null})
     */
    public abstract Class<T> getComponentType();

    /**
     * Determines whether this factory should be enabled based on the current container state.
     *
     * <p>This method is called during container initialization to check if all dependencies
     * required by this factory are available. The default implementation always returns {@code
     * true}. Subclasses can override this to implement conditional registration.
     *
     * <p><b>Important:</b> The provided maps are <b>unmodifiable, read-only views</b> of the
     * container state. They are supplied for inspection purposes only to allow this method to query
     * what components and factories are available. Any attempt to modify these maps will result in
     * an {@code UnsupportedOperationException}. The implementation must not mutate the container
     * state in any way.
     *
     * <p>For automatic dependency checking based on {@link #getRequiredContextTypes()}, consider
     * extending {@link ConditionalComponentFactory} instead of overriding this method directly.
     *
     * @param objects An unmodifiable view of object instances already registered in the container
     * @param factories An unmodifiable view of all factories registered in the container
     * @return {@code true} if this factory should be enabled, {@code false} to exclude it
     * @see ConditionalComponentFactory
     */
    public boolean isEnabled(
            Map<ObjectSpecification, Object> objects,
            Map<FactorySpecification, ComponentFactory<?>> factories) {
        return true;
    }

    /**
     * Returns the type qualifier for the component this factory creates.
     *
     * <p>Qualifiers are used to disambiguate between multiple factories that produce the same
     * component type. For example, multiple {@code StateRepository} implementations might have
     * qualifiers like "csv", "database", or "jdbc".
     *
     * <p>When retrieving components with qualifiers, use {@link Context#get(Class, String,
     * Object...)} with the appropriate qualifier string.
     *
     * @return The qualifier string (e.g., "jdbc", "csv", "database"), or {@code null} if no
     *     qualifier is needed
     */
    public abstract String getQualifier();

    /**
     * Returns the list of component types that must be available in the container before this
     * factory can create its component.
     *
     * <p>This is used by {@link ConditionalComponentFactory} to automatically determine if the
     * factory should be enabled. If any required type is missing, the factory will be disabled.
     *
     * <p>For base {@code ComponentFactory} implementations, this list is informational only unless
     * you override {@link #isEnabled(Map, Map)} to use it.
     *
     * @return An immutable list of required component types (never {@code null}, but may be empty)
     */
    public abstract List<Class<?>> getRequiredContextTypes();

    /**
     * Returns the specification of arguments expected by {@link #newComponent(Context, Object...)}.
     *
     * <p>This method declares what arguments the factory expects when creating components. Each
     * {@link ArgumentSpecification} defines the type and name of a required argument. If this
     * returns a non-empty list, the component will NOT be created automatically during container
     * initialization, even if {@link #isLazy()} returns {@code false}.
     *
     * <p>Arguments must be provided when retrieving the component via {@link Context#get(Class,
     * String, Object...)}. The arguments will be validated to ensure:
     *
     * <ul>
     *   <li>The exact number of arguments matches the specifications
     *   <li>Each argument is of the correct type (strict type checking)
     *   <li>No argument is null
     * </ul>
     *
     * <p><b>Example:</b>
     *
     * <pre>{@code
     * @Override
     * public List<ArgumentSpecification<?>> getArgumentSpecifications() {
     *     return List.of(
     *         new ArgumentSpecification<>(String.class, "name"),
     *         new ArgumentSpecification<>(Integer.class, "port")
     *     );
     * }
     * }</pre>
     *
     * @return An immutable list of {@link ArgumentSpecification}s (never {@code null}). Return an
     *     empty list if no arguments are required.
     * @see ArgumentSpecification
     * @see ArgumentValidator
     */
    public List<ArgumentSpecification<?>> getArgumentSpecifications() {
        return List.of();
    }

    /**
     * Constructs a new component instance.
     *
     * <p>This method is called by the container when a component needs to be created. The factory
     * can retrieve dependencies from the provided context and use the supplied arguments to
     * configure the component.
     *
     * <p><b>WARNING - Varargs Pitfall:</b> When calling this method, pass arguments individually,
     * NOT as an array. Due to Java varargs behavior:
     *
     * <pre>{@code
     * // WRONG - creates nested array
     * Object[] myArgs = new Object[]{"name", config};
     * factory.newComponent(context, myArgs);
     *
     * // CORRECT - pass individually
     * factory.newComponent(context, "name", config);
     * }</pre>
     *
     * <p><b>Best Practice:</b> Call {@link #validateArguments(Object...)} once at the beginning of
     * this method to validate argument structure, then use {@link
     * #getArgument(ArgumentSpecification, Object[], int)} to retrieve and validate individual
     * arguments:
     *
     * <pre>{@code
     * @Override
     * public MyComponent newComponent(Context context, Object... arguments) {
     *     validateArguments(arguments);  // Validate structure once
     *
     *     String name = getArgument(NAME_ARG, arguments, 0);
     *     Integer port = getArgument(PORT_ARG, arguments, 1);
     *
     *     return new MyComponentImpl(name, port);
     * }
     * }</pre>
     *
     * @param context The IoC container context for retrieving dependencies (never {@code null})
     * @param arguments Runtime arguments passed from {@link Context#get(Class, String, Object...)}.
     *     The number and types should match what {@link #getArgumentSpecifications()} declares. May
     *     be empty if no arguments are required.
     * @return A new instance of the component (never {@code null})
     * @throws RuntimeException if component creation fails due to missing dependencies, invalid
     *     arguments, or other errors
     */
    public abstract T newComponent(Context context, Object... arguments);

    /**
     * Retrieves and validates a specific argument from the arguments array.
     *
     * <p>This method performs validation in two stages:
     *
     * <ol>
     *   <li><b>Type checking</b>: Ensures the argument is of the correct type as specified by the
     *       {@link ArgumentSpecification}
     *   <li><b>Constraint validation</b>: Applies any custom constraints defined in the
     *       specification (e.g., string length, numeric ranges, patterns)
     * </ol>
     *
     * <p><b>Important:</b> Callers should call {@link #validateArguments(Object...)} once at the
     * beginning of {@link #newComponent(Context, Object...)} to validate the argument count and
     * basic structure. This method then validates the specific argument's type and constraints.
     *
     * <p><b>Example Usage:</b>
     *
     * <pre>{@code
     * private static final ArgumentSpecification<String> NAME_ARG = string("name");
     * private static final ArgumentSpecification<Integer> PORT_ARG = integer("port", 1, 65535);
     *
     * @Override
     * public Server newComponent(Context context, Object... arguments) {
     *     validateArguments(arguments);
     *
     *     String name = getArgument(NAME_ARG, arguments, 0);  // Type + constraint validation
     *     Integer port = getArgument(PORT_ARG, arguments, 1); // Validates port is 1-65535
     *
     *     return new DefaultServer(name, port);
     * }
     * }</pre>
     *
     * @param spec The specification for this argument (defines type and constraints)
     * @param args The full arguments array
     * @param index The index of the argument to retrieve
     * @param <R> The expected type of the argument
     * @return The validated and type-cast argument value
     * @throws IllegalArgumentException if the argument index is out of bounds, the type is
     *     incorrect, or any constraint validation fails
     */
    protected <R> R getArgument(ArgumentSpecification<R> spec, Object[] args, int index) {
        // Bounds check
        if (index < 0 || index >= args.length) {
            throw new IllegalArgumentException(
                    "Argument index " + index + " out of bounds (array size: " + args.length + ")");
        }

        Object value = args[index];

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
     * Validates the provided arguments according to the specifications returned by {@link
     * #getArgumentSpecifications()}.
     *
     * <p>This method performs <b>structural validation only</b>:
     *
     * <ul>
     *   <li>The number of provided arguments matches the number of specifications
     *   <li>Each argument is of the correct type
     *   <li>No argument is null
     * </ul>
     *
     * <p><b>Does NOT validate custom constraints</b> such as string length, numeric ranges,
     * patterns, etc. Those constraints are validated per-argument when calling {@link
     * #getArgument(ArgumentSpecification, Object[], int)}.
     *
     * <p><b>Usage Pattern:</b> Call this method <b>once</b> at the beginning of {@link
     * #newComponent(Context, Object...)} to validate structure, then use {@link
     * #getArgument(ArgumentSpecification, Object[], int)} to retrieve and validate individual
     * arguments with their constraints.
     *
     * <p><b>Example:</b>
     *
     * <pre>{@code
     * @Override
     * public MyComponent newComponent(Context context, Object... arguments) {
     *     validateArguments(arguments);  // Validate structure once at the start
     *
     *     String name = getArgument(NAME_ARG, arguments, 0);
     *     Integer port = getArgument(PORT_ARG, arguments, 1);
     *
     *     return new MyComponentImpl(name, port);
     * }
     * }</pre>
     *
     * @param arguments The arguments to validate
     * @throws IllegalArgumentException if validation fails
     * @see ArgumentValidator
     */
    protected final void validateArguments(Object... arguments) {
        ArgumentValidator.validate(
                arguments, getArgumentSpecifications(), getClass().getSimpleName());
    }
}
