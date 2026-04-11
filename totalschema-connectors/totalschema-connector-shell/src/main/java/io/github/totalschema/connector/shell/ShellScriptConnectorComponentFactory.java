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

package io.github.totalschema.connector.shell;

import io.github.totalschema.config.Configuration;
import io.github.totalschema.connector.AbstractConnectorComponentFactory;
import io.github.totalschema.connector.Connector;
import java.util.Optional;

/**
 * {@link io.github.totalschema.spi.factory.ComponentFactory} for {@link ShellScriptConnector}.
 *
 * <p>Registered via Java {@link java.util.ServiceLoader} in {@code
 * META-INF/services/io.github.totalschema.spi.factory.ComponentFactory}. The IoC container calls
 * this factory whenever a connector of type {@code "shell"} is requested.
 *
 * <p>Inherits all argument handling and configuration-merging logic from {@link
 * io.github.totalschema.connector.AbstractConnectorComponentFactory}: exactly two arguments are
 * expected — the connector name ({@code String}) and the merged connector configuration ({@link
 * io.github.totalschema.config.Configuration}).
 */
public final class ShellScriptConnectorComponentFactory extends AbstractConnectorComponentFactory {

    /** Creates a new {@code ShellScriptConnectorComponentFactory}. */
    public ShellScriptConnectorComponentFactory() {}

    /**
     * Returns the qualifier that binds this factory to the {@code "shell"} connector type.
     *
     * <p>The returned value must match the {@code type} field in the connector's {@code
     * totalschema.yml} block (e.g. {@code type: shell}).
     *
     * @return an {@link java.util.Optional} containing {@value ShellScriptConnector#CONNECTOR_TYPE}
     */
    @Override
    public Optional<String> getQualifier() {
        return Optional.of(ShellScriptConnector.CONNECTOR_TYPE);
    }

    /**
     * Creates a new {@link ShellScriptConnector} for the given connector name and configuration.
     *
     * @param connectorName the name of the connector as declared in {@code totalschema.yml}
     * @param configuration the merged (global + environment-specific) connector configuration
     * @return a fully initialised {@link ShellScriptConnector}
     */
    @Override
    protected Connector createConnector(String connectorName, Configuration configuration) {
        return new ShellScriptConnector(connectorName, configuration);
    }
}
