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
import io.github.totalschema.connector.Connector;
import io.github.totalschema.connector.ConnectorFactory;

/** Factory for SSH command list connectors. */
public class SshCommandListConnectorFactory implements ConnectorFactory {

    @Override
    public String getConnectorType() {
        return SshCommandListConnector.CONNECTOR_TYPE;
    }

    @Override
    public Connector createConnector(String name, Configuration configuration) {
        return new SshCommandListConnector(name, configuration);
    }
}
