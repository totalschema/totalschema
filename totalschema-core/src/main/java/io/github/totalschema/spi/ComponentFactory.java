package io.github.totalschema.spi;

import io.github.totalschema.engine.api.Context;
import io.github.totalschema.engine.core.container.FactorySpecification;
import io.github.totalschema.engine.core.container.ObjectSpecification;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
 *     public Optional<String> getQualifier() { return Optional.of("csv"); }
 *
 *     @Override
 *     public List<Class<?>> getRequiredContextTypes() { return List.of(Configuration.class); }
 *
 *     @Override
 *     public List<ArgumentSpecification<?>> getArgumentSpecifications() { return List.of(); }
 *
 *     @Override
 *     public StateRepository createComponent(Context context, List<Object> arguments) {
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
 *     // ArgumentHandler subclass encapsulates all argument specifications
 *     static class Arguments extends ArgumentHandler {
 *         private static final ArgumentSpecification<String> HOST = string("host", 1, 255);
 *         private static final ArgumentSpecification<Integer> PORT = integer("port", 1, 65535);
 *
 *         public Arguments() {
 *             super(HOST, PORT);
 *         }
 *
 *         public String getHost(List<Object> args) {
 *             return getArgument(HOST, args);
 *         }
 *
 *         public Integer getPort(List<Object> args) {
 *             return getArgument(PORT, args);
 *         }
 *     }
 *
 *     @Override
 *     public boolean isLazy() { return false; }
 *
 * @Override
 * public Class<Server> getComponentType() { return Server.class; }
 *
 *     @Override
 *     public Optional<String> getQualifier() { return Optional.empty(); }
 *
 *     @Override
 *     public List<Class<?>> getRequiredContextTypes() { return List.of(Configuration.class); }
 *
 *     @Override
 *     public List<ArgumentSpecification<?>> getArgumentSpecifications() {
 *         return new Arguments().getSpecifications();
 *     }
 *
 *     @Override
 *     public Server createComponent(Context context, List<Object> arguments) {
 *         Arguments args = new Arguments();
 *         args.validateStructure(arguments, getClass().getSimpleName());
 *
 *         String host = args.getHost(arguments);
 *         Integer port = args.getPort(arguments);
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
     * @return An Optional containing the qualifier string (e.g., "jdbc", "csv", "database"), or
     *     {@code Optional.empty()} if no qualifier is needed
     */
    public abstract Optional<String> getQualifier();

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
     * Returns the specification of arguments expected by {@link #createComponent(Context, List)}.
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
    public abstract List<ArgumentSpecification<?>> getArgumentSpecifications();

    public final T createComponent(Context context) {
        return createComponent(context, List.of());
    }

    /**
     * Constructs a new component instance.
     *
     * <p>This method is called by the container when a component needs to be created. The factory
     * can retrieve dependencies from the provided context and use the supplied arguments to
     * configure the component.
     *
     * <p><b>Best Practice:</b> Use the {@link ArgumentHandler} pattern for type-safe argument
     * access:
     *
     * <pre>{@code
     * ArgumentHandler handler = new ArgumentHandler(getArgumentSpecifications());
     * handler.validateStructure(arguments, getClass().getSimpleName());
     * String name = handler.getArgument(NAME_ARG, arguments);
     * Integer port = handler.getArgument(PORT_ARG, arguments);
     * }</pre>
     *
     * @param context The IoC container context for retrieving dependencies (never {@code null})
     * @param arguments Runtime arguments passed from {@link Context#get(Class, String, Object...)}.
     *     The number and types should match what {@link #getArgumentSpecifications()} declares.
     *     Will be an empty list if no arguments are required.
     * @return A new instance of the component (never {@code null})
     * @throws RuntimeException if component creation fails due to missing dependencies, invalid
     *     arguments, or other errors
     */
    public abstract T createComponent(Context context, List<Object> arguments);
}
