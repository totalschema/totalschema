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
import io.github.totalschema.connector.AbstractTerminalConnector;
import io.github.totalschema.engine.core.command.api.CommandContext;
import io.github.totalschema.engine.internal.shell.direct.ssh.spi.SshConnectionFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * SSH connector that executes commands from a file line-by-line.
 *
 * <p>Each non-blank line in the file is executed as an independent SSH command. Commands do NOT
 * share shell context - variables, functions, or state from one line will not be available in
 * subsequent lines.
 *
 * <p>Example file content:
 *
 * <pre>
 * mkdir -p /tmp/myapp
 * cd /tmp/myapp
 * ls -la  # This will NOT be in /tmp/myapp directory!
 * </pre>
 *
 * <p>For executing shell scripts with shared context, use {@link SshScriptConnector} instead.
 *
 * @see SshScriptConnector
 */
public class SshCommandListConnector extends AbstractTerminalConnector<String> {

    public static final String CONNECTOR_TYPE = "ssh-commands";

    private final String name;

    public SshCommandListConnector(String name, Configuration connectorConfiguration) {
        super(SshConnectionFactory.getInstance().getSshConnection(name, connectorConfiguration));

        this.name = name;
    }

    @Override
    protected final void execute(Path commandListFile, CommandContext context) {
        try {
            for (String line : Files.readAllLines(commandListFile)) {
                if (!line.isBlank()) {
                    session.execute(line);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failure processing command list file: " + commandListFile, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("interrupted", e);
        }
    }

    @Override
    public String toString() {
        return "SSH Command List Connector named '" + name + "'{session='" + session + '\'' + '}';
    }
}
