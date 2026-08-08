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

package io.github.totalschema.spi.change.executor;

import static io.github.totalschema.spi.factory.ArgumentSpecification.configuration;

import io.github.totalschema.config.Configuration;
import io.github.totalschema.engine.api.Context;
import io.github.totalschema.spi.factory.ArgumentHandler;
import io.github.totalschema.spi.factory.ArgumentSpecification;
import io.github.totalschema.spi.factory.ComponentFactory;
import java.util.List;
import java.util.Optional;

/**
 * Base class for {@link ChangeExecutor} component factories.
 *
 * <p>All change-executor factories share the same argument contract — a single {@link
 * Configuration} — and the same lifecycle properties (lazy, no pre-declared dependencies).
 * Subclasses supply the file-extension qualifier and the concrete executor instance via {@link
 * #createExecutor(Configuration, Context)}.
 *
 * <p>Usage: {@code context.get(ChangeExecutor.class, "<extension>", configuration)}
 */
public abstract class AbstractChangeExecutorComponentFactory
        extends ComponentFactory<ChangeExecutor> {

    private static final ArgumentSpecification<Configuration> CONFIGURATION =
            configuration("configuration");

    private static final ArgumentHandler ARGUMENTS =
            ArgumentHandler.getInstance(
                    AbstractChangeExecutorComponentFactory.class, CONFIGURATION);

    private final String extension;

    /**
     * @param extension the file-extension qualifier that identifies this executor (e.g. {@code
     *     "sql"}, {@code "groovy"})
     */
    protected AbstractChangeExecutorComponentFactory(String extension) {
        this.extension = extension;
    }

    @Override
    public final boolean isLazy() {
        return true;
    }

    @Override
    public final Class<ChangeExecutor> getComponentType() {
        return ChangeExecutor.class;
    }

    @Override
    public final Optional<String> getQualifier() {
        return Optional.of(extension);
    }

    @Override
    public final List<Class<?>> getDependencies() {
        return List.of();
    }

    @Override
    public final List<ArgumentSpecification<?>> getArgumentSpecifications() {
        return ARGUMENTS.getSpecifications();
    }

    @Override
    public final ChangeExecutor createComponent(Context context, List<Object> arguments) {
        ARGUMENTS.validateStructure(arguments);
        Configuration configuration = ARGUMENTS.getArgument(CONFIGURATION, arguments);
        return createExecutor(configuration, context);
    }

    /**
     * Creates the concrete {@link ChangeExecutor} for this extension.
     *
     * @param configuration the connector configuration passed by the caller
     * @param context the container context
     * @return a new executor instance
     */
    protected abstract ChangeExecutor createExecutor(Configuration configuration, Context context);
}
