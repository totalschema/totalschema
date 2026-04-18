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

package io.github.totalschema.connector.python;

import io.github.totalschema.config.Configuration;
import io.github.totalschema.connector.AbstractConnectorComponentFactory;
import io.github.totalschema.connector.Connector;
import java.util.Optional;

/**
 * {@link io.github.totalschema.spi.factory.ComponentFactory} for {@link PythonConnector}.
 *
 * <p>Registered via Java {@link java.util.ServiceLoader} in {@code
 * META-INF/services/io.github.totalschema.spi.factory.ComponentFactory}. The IoC container calls
 * this factory whenever a connector of type {@code "python"} is requested.
 *
 * <p>Inherits all argument handling and configuration-merging logic from {@link
 * io.github.totalschema.connector.AbstractConnectorComponentFactory}: exactly two arguments are
 * expected — the connector name ({@code String}) and the merged connector configuration ({@link
 * io.github.totalschema.config.Configuration}).
 */
public final class PythonConnectorComponentFactory extends AbstractConnectorComponentFactory {

    /** Creates a new {@code PythonConnectorComponentFactory}. */
    public PythonConnectorComponentFactory() {}

    /**
     * Returns the qualifier that binds this factory to the {@code "python"} connector type.
     *
     * @return an {@link Optional} containing {@value PythonConnector#CONNECTOR_TYPE}
     */
    @Override
    public Optional<String> getQualifier() {
        return Optional.of(PythonConnector.CONNECTOR_TYPE);
    }

    @Override
    protected Connector createConnector(String name, Configuration configuration) {
        return new PythonConnector(name, configuration);
    }
}
