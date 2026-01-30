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

package io.github.totalschema.spi.change;

import io.github.totalschema.engine.core.command.api.CommandContext;
import io.github.totalschema.engine.internal.change.DefaultChangeServiceFactory;
import io.github.totalschema.spi.ServiceLoaderFactory;

/**
 * Factory interface for creating ChangeService instances. Implementations can be provided via
 * ServiceLoader or the default factory is used.
 */
public interface ChangeServiceFactory {

    /**
     * Returns a ChangeServiceFactory instance, either from ServiceLoader or the default
     * implementation.
     *
     * @return ChangeServiceFactory instance
     */
    static ChangeServiceFactory getInstance() {
        return ServiceLoaderFactory.getSingleService(ChangeServiceFactory.class)
                .orElseGet(DefaultChangeServiceFactory::new);
    }

    /**
     * Creates a ChangeService instance for the given context.
     *
     * @param context the command context containing environment and configuration
     * @return ChangeService instance
     */
    ChangeService getChangeService(CommandContext context);
}
