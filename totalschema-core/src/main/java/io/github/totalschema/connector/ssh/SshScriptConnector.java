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
import io.github.totalschema.engine.internal.shell.direct.ssh.spi.SshConnection;
import io.github.totalschema.engine.internal.shell.direct.ssh.spi.SshConnectionFactory;
import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SSH connector that executes shell scripts on a remote host.
 *
 * <p>The script file is transferred to the remote host via SCP (Secure Copy Protocol), made
 * executable, executed as a single shell script (preserving shell context), and then cleaned up.
 *
 * <p>Unlike {@link SshCommandListConnector}, this connector executes the entire script in a single
 * shell session, so variables, functions, and state are preserved across lines.
 *
 * <p>Example script:
 *
 * <pre>
 * #!/bin/bash
 * MY_VAR="hello"
 * echo $MY_VAR
 *
 * my_function() {
 *   echo "Function called"
 * }
 * my_function
 * </pre>
 *
 * <p>Configuration options:
 *
 * <ul>
 *   <li>{@code remote.temp.dir} - Remote directory for temporary scripts (default: /tmp)
 *   <li>{@code shell} - Shell to use for execution (default: /bin/bash)
 * </ul>
 *
 * <p><b>File Transfer:</b> This connector uses Apache MINA SSHD's built-in SCP (Secure Copy
 * Protocol) client for efficient and secure file transfer, replacing the previous base64 encoding
 * approach.
 *
 * @see SshCommandListConnector
 */
public class SshScriptConnector extends AbstractTerminalConnector<String> {

    private static final Logger log = LoggerFactory.getLogger(SshScriptConnector.class);

    public static final String CONNECTOR_TYPE = "ssh-script";

    private final String name;
    private final String remoteTempDir;
    private final String shell;
    private final SshConnection sshConnection;

    public SshScriptConnector(String name, Configuration connectorConfiguration) {
        super(SshConnectionFactory.getInstance().getSshConnection(name, connectorConfiguration));

        this.name = name;
        this.sshConnection =
                SshConnectionFactory.getInstance().getSshConnection(name, connectorConfiguration);
        this.remoteTempDir =
                connectorConfiguration.getString("remote", "temp", "dir").orElse("/tmp");
        this.shell = connectorConfiguration.getString("shell").orElse("/bin/bash");
    }

    @Override
    protected final void execute(Path scriptFile, CommandContext context) {
        String remoteScriptPath = null;
        try {
            // Generate unique remote script path
            String scriptName = "totalschema-script-" + UUID.randomUUID() + ".sh";
            remoteScriptPath = remoteTempDir + "/" + scriptName;

            log.info(
                    "Uploading script {} to remote host at {}",
                    scriptFile.getFileName(),
                    remoteScriptPath);

            // Upload file using Apache MINA SSHD's SCP client
            sshConnection.uploadFile(scriptFile, remoteScriptPath);

            // Make script executable
            session.execute("chmod +x " + remoteScriptPath);

            log.info("Executing remote script: {}", remoteScriptPath);

            // Execute the script
            session.execute(shell + " " + remoteScriptPath);

            log.info("Successfully executed script: {}", scriptFile.getFileName());

        } catch (IOException e) {
            throw new RuntimeException("Failure processing script file: " + scriptFile, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("interrupted", e);
        } finally {
            // Clean up remote script file
            if (remoteScriptPath != null) {
                try {
                    log.debug("Cleaning up remote script: {}", remoteScriptPath);
                    session.execute("rm -f " + remoteScriptPath);
                } catch (Exception e) {
                    log.warn(
                            "Failed to clean up remote script {}: {}",
                            remoteScriptPath,
                            e.getMessage());
                }
            }
        }
    }

    @Override
    public String toString() {
        return "SSH Script Connector named '"
                + name
                + "'{session='"
                + session
                + '\''
                + ", remoteTempDir='"
                + remoteTempDir
                + '\''
                + ", shell='"
                + shell
                + '\''
                + '}';
    }
}
