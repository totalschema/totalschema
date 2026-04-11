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

package io.github.totalschema.connector.ssh;

import io.github.totalschema.config.Configuration;
import io.github.totalschema.connector.AbstractConnectorComponentFactory;
import io.github.totalschema.connector.Connector;
import java.util.Optional;

/**
 * ComponentFactory for creating SSH script connectors.
 *
 * <p>This factory creates {@link Connector} instances with qualifier "ssh-script" that can upload
 * and execute shell scripts on remote servers via SSH.
 *
 * <p>Usage: {@code context.get(Connector.class, "ssh-script", connectorName, configuration)}
 */
public final class SshScriptConnectorComponentFactory extends AbstractConnectorComponentFactory {

    @Override
    public Optional<String> getQualifier() {
        return Optional.of(SshScriptConnector.CONNECTOR_TYPE);
    }

    @Override
    protected Connector createConnector(String connectorName, Configuration configuration) {
        return new SshScriptConnector(connectorName, configuration);
    }
}
