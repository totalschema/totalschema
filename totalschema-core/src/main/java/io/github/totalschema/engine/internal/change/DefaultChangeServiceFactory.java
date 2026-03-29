/*
 * totalschema: tool for managing database versioning and schema changes with ease.
 * Copyright (C) 2025 totalschema development team
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

package io.github.totalschema.engine.internal.change;

import io.github.totalschema.config.environment.Environment;
import io.github.totalschema.connector.ConnectorManager;
import io.github.totalschema.engine.api.Context;
import io.github.totalschema.spi.change.ChangeService;
import io.github.totalschema.spi.factory.ArgumentSpecification;
import io.github.totalschema.spi.factory.ConditionalComponentFactory;
import java.util.List;
import java.util.Optional;

public final class DefaultChangeServiceFactory extends ConditionalComponentFactory<ChangeService> {

    @Override
    public boolean isLazy() {
        return false;
    }

    @Override
    public Class<ChangeService> getComponentType() {
        return ChangeService.class;
    }

    @Override
    public Optional<String> getQualifier() {
        return Optional.empty();
    }

    @Override
    public List<Class<?>> getRequiredContextTypes() {
        return List.of(ConnectorManager.class, Environment.class);
    }

    @Override
    public List<ArgumentSpecification<?>> getArgumentSpecifications() {
        return List.of();
    }

    @Override
    public ChangeService createComponent(Context context, List<Object> arguments) {

        ConnectorManager connectorManager = context.get(ConnectorManager.class);
        Environment environment = context.get(Environment.class);

        return new DefaultChangeService(connectorManager, environment);
    }
}
