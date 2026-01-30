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

package io.github.totalschema.connector;

import io.github.totalschema.engine.core.command.api.CommandContext;
import io.github.totalschema.spi.ServiceLoaderFactory;

/**
 * Manager for creating and caching connector instances.
 *
 * <p>Connectors are cached and reused within the same execution context.
 *
 * <p>This manager uses the {@link ConnectorFactory} SPI to discover available connector types.
 * Different connection types use different factory implementations, which can be:
 *
 * <ul>
 *   <li>Built-in factories (jdbc, ssh-script, ssh-commands, shell)
 *   <li>User-defined factories loaded via Java ServiceLoader API
 * </ul>
 *
 * <p>To create a custom connector type:
 *
 * <ol>
 *   <li>Implement the {@link Connector} interface
 *   <li>Implement the {@link ConnectorFactory} interface
 *   <li>Register your factory in {@code
 *       META-INF/services/io.github.totalschema.connector.ConnectorFactory}
 * </ol>
 *
 * <p>Alternatively, you can replace the entire manager by implementing this interface and
 * registering it via {@code META-INF/services/io.github.totalschema.connector.ConnectorManager}.
 */
public interface ConnectorManager {

    static ConnectorManager getInstance() {
        return ServiceLoaderFactory.getSingleService(ConnectorManager.class)
                .orElseGet(DefaultConnectorManager::new);
    }

    /**
     * Get or create a connector by name.
     *
     * @param name the connector name from configuration
     * @param context the command context
     * @return the connector instance
     */
    Connector getConnectorByName(String name, CommandContext context);
}
