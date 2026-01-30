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

package io.github.totalschema.connector.jdbc;

import io.github.totalschema.config.Configuration;
import io.github.totalschema.connector.Connector;
import io.github.totalschema.engine.core.command.api.CommandContext;
import io.github.totalschema.model.ChangeFile;
import io.github.totalschema.spi.script.ScriptExecutor;
import io.github.totalschema.spi.script.ScriptExecutorFactory;
import io.github.totalschema.spi.script.ScriptExecutorManager;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * JDBC connector for executing SQL and other database scripts.
 *
 * <p>Supports any JDBC-compliant database including PostgreSQL, MySQL, Oracle, H2, SQL Server,
 * BigQuery, and others.
 */
public class JdbcConnector extends Connector {

    public static final String CONNECTOR_TYPE = "jdbc";

    private final String name;
    private final Configuration connectorConfiguration;

    public JdbcConnector(String name, Configuration connectorConfiguration) {
        this.name = name;
        this.connectorConfiguration = connectorConfiguration;
    }

    @Override
    public String toString() {
        return "JDBC Connector named '"
                + name
                + "'{"
                + " configuration='"
                + connectorConfiguration
                + '\''
                + '}';
    }

    @Override
    public void execute(ChangeFile changeFile, CommandContext context) throws InterruptedException {
        Path file = changeFile.getFile();

        try {
            String fileContent = Files.readString(file);

            ScriptExecutorManager scriptExecutorManager = context.get(ScriptExecutorManager.class);

            String extension = changeFile.getId().getExtension();

            ScriptExecutorFactory scriptExecutorFactory =
                    scriptExecutorManager.getScriptExecutorFactoryByExtension(extension);

            ScriptExecutor scriptExecutor =
                    scriptExecutorFactory.getScriptExecutor(name, connectorConfiguration, context);

            scriptExecutor.execute(fileContent, context);

        } catch (IOException e) {
            throw new RuntimeException("Failure reading: " + file);
        }
    }

    @Override
    public void close() throws IOException {
        // no-op
    }
}
