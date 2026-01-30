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

package io.github.totalschema.engine.internal.shell.direct.ssh.impl;

import io.github.totalschema.concurrent.LockTemplate;
import io.github.totalschema.config.Configuration;
import io.github.totalschema.config.MissingConfigurationKeyException;
import io.github.totalschema.engine.internal.shell.AbstractTerminalSession;
import io.github.totalschema.engine.internal.shell.direct.ssh.spi.SshConnection;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ChannelExec;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.PropertyResolverUtils;
import org.apache.sshd.common.config.keys.FilePasswordProvider;
import org.apache.sshd.common.keyprovider.FileKeyPairProvider;
import org.apache.sshd.scp.client.ScpClient;
import org.apache.sshd.scp.client.ScpClientCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SSH connection implementation using Apache MINA SSHD.
 *
 * <p><b>Authentication Methods:</b>
 *
 * <ul>
 *   <li><b>Password:</b> Configure {@code password} in connector configuration
 *   <li><b>Private Key:</b> Configure {@code privateKey.path} (and optionally {@code
 *       privateKey.passphrase})
 * </ul>
 *
 * <p><b>Example Configuration (Password):</b>
 *
 * <pre>
 * connectors:
 *   myserver:
 *     type: ssh
 *     host: server.example.com
 *     user: deployuser
 *     password: ${secret:...}
 * </pre>
 *
 * <p><b>Example Configuration (SSH Key):</b>
 *
 * <pre>
 * connectors:
 *   myserver:
 *     type: ssh
 *     host: server.example.com
 *     user: deployuser
 *     privateKey:
 *       path: /path/to/id_rsa
 *       passphrase: ${secret:...}  # Optional, if key is encrypted
 * </pre>
 */
public final class MinaSshdConnection extends AbstractTerminalSession<String>
        implements SshConnection {

    private static final Logger log = LoggerFactory.getLogger(MinaSshdConnection.class);

    private final String name;

    private static final class DefaultValues {
        private static final int SSH_PORT = 22;
        private static final int TIMEOUT = 30;
        private static final TimeUnit TIMEOUT_UNIT = TimeUnit.SECONDS;
        private static final long COMMAND_TIMEOUT_MS = 300000; // 5 minutes
    }

    private final String host;
    private final int port;
    private final String user;
    private final String password;
    private final String privateKeyPath;
    private final String privateKeyPassphrase;
    private final Properties sshProperties;

    private final Integer lockTimeout;
    private final TimeUnit lockTimeoutUnit;
    private final long commandTimeoutMs;

    private final LockTemplate lockTemplate;

    private SshClient client = null;
    private ClientSession session = null;

    public MinaSshdConnection(String name, Configuration configuration) {
        this.name = name;

        this.host =
                configuration
                        .getString("host")
                        .orElseThrow(() -> new MissingConfigurationKeyException("host"));
        this.port = configuration.getInt("port").orElse(DefaultValues.SSH_PORT);
        this.user = configuration.getString("user").orElse(null);
        this.password = configuration.getString("password").orElse(null);

        // SSH key-based authentication support
        this.privateKeyPath = configuration.getString("privateKey", "path").orElse(null);
        this.privateKeyPassphrase =
                configuration.getString("privateKey", "passphrase").orElse(null);

        this.lockTimeout = configuration.getInt("lock", "timeout").orElse(DefaultValues.TIMEOUT);

        this.lockTimeoutUnit =
                configuration
                        .getEnumValue(TimeUnit.class, "lock", "timeoutUnit")
                        .orElse(DefaultValues.TIMEOUT_UNIT);

        this.commandTimeoutMs =
                configuration
                        .getLong("command", "timeoutMs")
                        .orElse(DefaultValues.COMMAND_TIMEOUT_MS);

        this.lockTemplate = new LockTemplate(lockTimeout, lockTimeoutUnit, new ReentrantLock());

        this.sshProperties =
                configuration.getPrefixNamespace("ssh", "properties").asProperties().orElse(null);
    }

    @Override
    public void execute(String command) {
        try {
            executeWithLockHeld(() -> executeSSHCommand(command));
        } catch (IOException e) {
            throw new RuntimeException("SSH execution failed", e);
        }
    }

    private void executeWithLockHeld(SshAction action) throws IOException {
        lockTemplate.withTryLock(
                () -> {
                    try {
                        action.execute();
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("interrupted", e);
                    }
                });
    }

    private void executeSSHCommand(String command) throws IOException, InterruptedException {
        ensureConnected();

        log.info("Executing command: {}", command);

        try (ChannelExec channel = session.createExecChannel(command)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ByteArrayOutputStream err = new ByteArrayOutputStream();

            channel.setOut(out);
            channel.setErr(err);

            channel.open().verify(commandTimeoutMs);

            // Wait for command to complete
            channel.waitFor(java.util.EnumSet.of(ClientChannelEvent.CLOSED), commandTimeoutMs);

            Integer exitStatus = channel.getExitStatus();

            // Process output - convert ByteArrayOutputStream to InputStream for processing
            java.io.ByteArrayInputStream outStream =
                    new java.io.ByteArrayInputStream(out.toByteArray());
            java.io.ByteArrayInputStream errStream =
                    new java.io.ByteArrayInputStream(err.toByteArray());

            Future<?> outReader =
                    submitReaderTask(outStream, (line) -> System.out.format("[SSH] %s%n", line));
            Future<?> errorReader =
                    submitReaderTask(errStream, (line) -> System.err.format("[SSH] %s%n", line));

            try {
                outReader.get();
                errorReader.get();
            } catch (ExecutionException executionException) {
                Throwable cause = executionException.getCause();
                if (cause instanceof RuntimeException) {
                    throw (RuntimeException) cause;
                }
                if (cause instanceof Exception) {
                    throw new RuntimeException(cause);
                }
                if (cause instanceof Error) {
                    throw (Error) cause;
                } else {
                    throw new RuntimeException(cause);
                }
            }

            if (exitStatus != null && exitStatus != 0) {
                throw new RuntimeException(
                        "Exit code " + exitStatus + " received for command: " + command);
            }

        } catch (IOException e) {
            log.error("SSH command execution failed", e);
            throw e;
        }
    }

    private void ensureConnected() throws IOException, InterruptedException {
        if (session == null || session.isClosed() || !session.isOpen()) {
            connect();
        }
    }

    private void connect() throws IOException, InterruptedException {
        if (client == null) {
            client = SshClient.setUpDefaultClient();

            // Apply custom SSH properties if provided
            if (sshProperties != null) {
                sshProperties.forEach(
                        (key, value) ->
                                PropertyResolverUtils.updateProperty(
                                        client, key.toString(), value));
            }

            client.start();
        }

        log.info("Connecting to SSH server: {}@{}:{}", user, host, port);

        session =
                client.connect(user, host, port)
                        .verify(lockTimeoutUnit.toMillis(lockTimeout))
                        .getSession();

        // Configure authentication methods
        if (privateKeyPath != null) {
            // Key-based authentication
            log.debug("Using SSH key-based authentication with key: {}", privateKeyPath);

            Path keyPath = Paths.get(privateKeyPath);
            if (!Files.exists(keyPath)) {
                throw new IOException("Private key file not found: " + privateKeyPath);
            }

            // Create key pair provider
            FileKeyPairProvider keyPairProvider = new FileKeyPairProvider(keyPath);

            // Set password provider if passphrase is provided
            if (privateKeyPassphrase != null) {
                keyPairProvider.setPasswordFinder(FilePasswordProvider.of(privateKeyPassphrase));
            }

            try {
                // Load key pairs
                Iterable<KeyPair> keyPairs = keyPairProvider.loadKeys(session);
                for (KeyPair keyPair : keyPairs) {
                    session.addPublicKeyIdentity(keyPair);
                }
            } catch (Exception e) {
                throw new IOException("Failed to load SSH private key from " + privateKeyPath, e);
            }

        } else if (password != null) {
            // Password-based authentication
            log.debug("Using SSH password-based authentication");
            session.addPasswordIdentity(password);
        } else {
            throw new IllegalStateException(
                    "No authentication method configured. Provide either password or privateKey.path");
        }

        session.auth().verify(lockTimeoutUnit.toMillis(lockTimeout));

        log.info("Successfully connected to SSH server: {}@{}:{}", user, host, port);
    }

    /**
     * Uploads a file to the remote server using SCP.
     *
     * <p>This method uses Apache MINA SSHD's built-in SCP client to securely transfer files to the
     * remote server. It's more efficient and secure than base64 encoding or other workarounds.
     *
     * <p>This method is thread-safe and uses the same locking mechanism as {@link
     * #execute(String)}.
     *
     * @param localFile the local file to upload
     * @param remotePath the destination path on the remote server
     * @throws IOException if the file transfer fails
     * @throws InterruptedException if the operation is interrupted
     */
    @Override
    public void uploadFile(Path localFile, String remotePath)
            throws IOException, InterruptedException {
        try {
            executeWithLockHeld(() -> uploadFileInternal(localFile, remotePath));
        } catch (IOException e) {
            throw new RuntimeException("SCP file upload failed", e);
        }
    }

    private void uploadFileInternal(Path localFile, String remotePath)
            throws IOException, InterruptedException {
        ensureConnected();

        log.info("Uploading file {} to remote path: {}", localFile.getFileName(), remotePath);

        try {
            // Create SCP client from the session
            ScpClientCreator creator = ScpClientCreator.instance();
            ScpClient scpClient = creator.createScpClient(session);

            // Upload the file - MINA SSHD handles all the SCP protocol details
            scpClient.upload(localFile, remotePath);

            log.info("Successfully uploaded file to: {}", remotePath);

        } catch (IOException e) {
            log.error("Failed to upload file {} to {}", localFile, remotePath, e);
            throw new IOException("SCP upload failed for " + localFile + " to " + remotePath, e);
        }
    }

    @Override
    public void close() {
        try {
            executeWithLockHeld(this::disconnect);
        } catch (IOException e) {
            log.warn("Error closing SSH connection", e);
        }
    }

    private void disconnect() {
        if (session != null && session.isOpen()) {
            try {
                session.close();
            } catch (IOException e) {
                log.warn("Error closing SSH session", e);
            } finally {
                session = null;
            }
        }

        if (client != null && client.isStarted()) {
            try {
                client.stop();
            } catch (RuntimeException e) {
                log.warn("Error stopping SSH client", e);
            } finally {
                client = null;
            }
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("MinaSshdConnection{");
        sb.append("name='").append(name).append('\'');
        sb.append(", host='").append(host).append('\'');
        sb.append(", port=").append(port);
        sb.append(", user='").append(user).append('\'');
        sb.append(", password='").append(password != null ? "***" : "null").append('\'');
        sb.append(", sshProperties=").append(sshProperties);
        sb.append(", lockTimeout=").append(lockTimeout);
        sb.append(", lockTimeoutUnit=").append(lockTimeoutUnit);
        sb.append(", commandTimeoutMs=").append(commandTimeoutMs);
        sb.append(", connected=").append(session != null && session.isOpen());
        sb.append('}');
        return sb.toString();
    }

    @FunctionalInterface
    interface SshAction {
        void execute() throws IOException, InterruptedException;
    }
}
