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

import io.github.totalschema.config.Configuration;

/**
 * Factory interface for creating connector instances.
 *
 * <p>Implementations of this interface can be registered via the Java ServiceLoader API to provide
 * custom connector types. This allows users to extend TotalSchema with their own connector
 * implementations without modifying the core codebase.
 *
 * <p>To register a custom factory, create a file named: {@code
 * META-INF/services/io.github.totalschema.connector.ConnectorFactory} containing the fully
 * qualified class name of your implementation.
 *
 * <p>Example implementation:
 *
 * <pre>
 * public class MyConnectorFactory implements ConnectorFactory {
 *     &#64;Override
 *     public String getConnectorType() {
 *         return "my-custom-type";
 *     }
 *
 *     &#64;Override
 *     public Connector createConnector(String name, Configuration configuration) {
 *         return new MyCustomConnector(name, configuration);
 *     }
 * }
 * </pre>
 */
public interface ConnectorFactory {

    /**
     * Returns the connector type identifier that this factory handles.
     *
     * <p>This should match the "type" value in the connector configuration. For example: "jdbc",
     * "ssh-script", "my-custom-type", etc.
     *
     * @return the connector type identifier
     */
    String getConnectorType();

    /**
     * Creates a new connector instance for the given name and configuration.
     *
     * @param name the connector name from configuration
     * @param configuration the connector-specific configuration
     * @return a new connector instance
     */
    Connector createConnector(String name, Configuration configuration);
}
