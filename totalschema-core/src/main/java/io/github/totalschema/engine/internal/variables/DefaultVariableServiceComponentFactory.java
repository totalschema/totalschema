/*
 * totalschema: tool for managing database versioning and schema changes with ease.
 * Copyright (C) 2026 totalschema development team
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

package io.github.totalschema.engine.internal.variables;

import io.github.totalschema.config.Configuration;
import io.github.totalschema.engine.api.Context;
import io.github.totalschema.spi.expression.evaluator.ExpressionEvaluator;
import io.github.totalschema.spi.factory.ArgumentSpecification;
import io.github.totalschema.spi.factory.ComponentFactory;
import io.github.totalschema.spi.variables.VariableService;
import java.util.List;
import java.util.Optional;

/**
 * ComponentFactory that registers a {@link VariableService} instance in the IoC container.
 *
 * <p>The service is created eagerly at container initialisation time from the {@link Configuration}
 * and {@link ExpressionEvaluator} already present in the container. Once registered, any component
 * that needs variable resolution can retrieve it via
 *
 * <pre>{@code
 * context.get(VariableService.class)
 * }</pre>
 *
 * instead of constructing one manually.
 */
public final class DefaultVariableServiceComponentFactory
        extends ComponentFactory<VariableService> {

    @Override
    public boolean isLazy() {
        return false;
    }

    @Override
    public Class<VariableService> getComponentType() {
        return VariableService.class;
    }

    @Override
    public Optional<String> getQualifier() {
        return Optional.empty();
    }

    @Override
    public List<Class<?>> getDependencies() {
        return List.of(Configuration.class, ExpressionEvaluator.class);
    }

    @Override
    public List<ArgumentSpecification<?>> getArgumentSpecifications() {
        return List.of();
    }

    @Override
    public VariableService createComponent(Context context, List<Object> arguments) {
        Configuration configuration = context.get(Configuration.class);
        ExpressionEvaluator expressionEvaluator = context.get(ExpressionEvaluator.class);
        return new DefaultVariableService(configuration, expressionEvaluator);
    }
}
