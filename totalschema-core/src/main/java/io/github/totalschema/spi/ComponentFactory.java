package io.github.totalschema.spi;

import io.github.totalschema.engine.api.Context;
import java.util.List;

public interface ComponentFactory<T> {

    boolean isLazy();

    /** Returns the base class or interface of the component being constructed. */
    Class<T> getConstructedClass();

    /**
     * Returns the type qualifier for the component this factory creates (e.g., "jdbc", "foobar").
     * This is used to select the correct factory based on configuration.
     */
    String getQualifier();

    List<Class<?>> getRequiredContextTypes();

    List<Class<?>> getArgumentTypes();

    /**
     * Constructs a new component instance.
     *
     * @param arguments the arguments to be passed to the factory for constructing the component.
     *     The specific
     * @return A new instance of the component.
     */
    T newComponent(Context context, Object... arguments);
}
