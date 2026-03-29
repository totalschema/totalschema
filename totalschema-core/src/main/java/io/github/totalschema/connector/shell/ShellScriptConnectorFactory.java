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
 * ComponentFactory for creating shell script connectors.
 *
 * <p>This factory creates {@link Connector} instances with qualifier "shell" that execute shell
 * scripts on the local machine.
 *
 * <p>Usage: {@code context.get(Connector.class, "shell", connectorName, configuration)}
 */
public final class ShellScriptConnectorFactory extends AbstractConnectorComponentFactory {

    @Override
    public Optional<String> getQualifier() {
        return Optional.of(ShellScriptConnector.CONNECTOR_TYPE);
    }

    @Override
    protected Connector createConnector(String connectorName, Configuration configuration) {
        return new ShellScriptConnector(connectorName, configuration);
    }
}
