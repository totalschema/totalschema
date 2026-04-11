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
import io.github.totalschema.connector.ssh.spi.SshConnection;
import io.github.totalschema.connector.ssh.spi.SshConnectionFactory;
import io.github.totalschema.engine.core.command.api.CommandContext;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * SSH connector that executes commands from a file line-by-line.
 *
 * <p>Each non-blank line in the file is executed as an independent SSH command. Commands do NOT
 * share shell context - variables, functions, or state from one line will not be available in
 * subsequent lines.
 *
 * <p><b>Comments:</b> Lines whose first character is {@code #} are treated as comments and ignored
 * entirely. Comment lines inside a multi-line continuation sequence are also skipped without
 * interrupting the continuation.
 *
 * <p><b>Multi-line commands:</b> A line ending with a backslash ({@code \}) is treated as a
 * continuation of the following line. The backslash and the line boundary are removed and the lines
 * are joined before execution. This mirrors the standard shell line-continuation convention.
 *
 * <p>Example file content:
 *
 * <pre>
 * # Create working directory
 * mkdir -p /tmp/myapp
 *
 * # Long command split across lines
 * echo "This is a very long argument that \
 * spans two lines"
 *
 * ls -la  # inline comment – NOT stripped; sent to the shell as-is
 * </pre>
 *
 * <p>For executing shell scripts with shared context, use {@link SshScriptConnector} instead.
 *
 * @see SshScriptConnector
 */
final class SshCommandListConnector extends AbstractTerminalConnector<SshConnection> {

    public static final String CONNECTOR_TYPE = "ssh-commands";

    private final String name;

    public SshCommandListConnector(String name, Configuration connectorConfiguration) {
        this(
                name,
                SshConnectionFactory.getInstance().getSshConnection(name, connectorConfiguration));
    }

    public SshCommandListConnector(String name, SshConnection connection) {
        super(connection);
        this.name = name;
    }

    @Override
    protected void execute(Path commandListFile, CommandContext context) {
        try {
            List<String> commands = parseCommands(Files.readAllLines(commandListFile));
            for (String command : commands) {
                session.execute(command);
            }
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failure processing command list file: " + commandListFile, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("interrupted", e);
        }
    }

    /**
     * Parses raw lines from a command-list file into a list of commands ready to execute.
     *
     * <p>Lines whose first character is {@code #} are treated as comments and silently dropped,
     * even when they appear inside a multi-line continuation sequence.
     *
     * <p>Lines ending with {@code \} are continuation lines: the backslash is stripped and the next
     * non-comment line is appended directly, producing a single logical command. Blank logical
     * commands (after joining) are silently skipped.
     *
     * @param lines the raw lines read from the file
     * @return ordered list of commands to execute; never {@code null}
     */
    static List<String> parseCommands(List<String> lines) {
        List<String> commands = new ArrayList<>();
        StringBuilder pending = null;

        for (String line : lines) {
            if (line.startsWith("#")) {
                continue;
            }
            if (line.endsWith("\\")) {
                String part = line.substring(0, line.length() - 1);
                if (pending == null) {
                    pending = new StringBuilder(part);
                } else {
                    pending.append(part);
                }
            } else {
                String command;
                if (pending != null) {
                    pending.append(line);
                    command = pending.toString();
                    pending = null;
                } else {
                    command = line;
                }
                if (!command.isBlank()) {
                    commands.add(command);
                }
            }
        }

        // Handle a trailing continuation at end of file (missing final line)
        if (pending != null) {
            String command = pending.toString();
            if (!command.isBlank()) {
                commands.add(command);
            }
        }

        return commands;
    }

    @Override
    public String toString() {
        return "SSH Command List Connector named '" + name + "'{session='" + session + '\'' + '}';
    }
}
