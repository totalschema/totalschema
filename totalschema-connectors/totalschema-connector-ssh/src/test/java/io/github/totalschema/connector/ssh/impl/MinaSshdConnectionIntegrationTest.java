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

package io.github.totalschema.connector.ssh.impl;

import static org.testng.Assert.*;

import io.github.totalschema.config.Configuration;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.sshd.scp.server.ScpCommandFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.shell.ProcessShellCommandFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Integration tests for {@link MinaSshdConnection} that spin up an in-process MINA SSHD server.
 *
 * <p>The server uses {@link ProcessShellCommandFactory} as the delegate so actual OS commands
 * (chmod, bash, rm, echo …) are executed on the local machine. {@link ScpCommandFactory} wraps it
 * to add SCP support for {@link MinaSshdConnection#uploadFile}.
 *
 * <p>{@code accept.all.server.keys=true} is set in the client configuration so that the dynamically
 * generated host key is accepted without a known-hosts file.
 */
public class MinaSshdConnectionIntegrationTest {

    private static final String TEST_USER = "testuser";
    private static final String TEST_PASSWORD = "testpass";

    private SshServer sshd;
    private int port;
    private Path remoteDir; // acts as the "remote" filesystem root for SCP uploads

    // -----------------------------------------------------------------------
    // Server lifecycle
    // -----------------------------------------------------------------------

    @BeforeClass
    public void startServer() throws IOException {
        remoteDir = Files.createTempDirectory("sshd-integration-remote-");
        Path hostKeyFile = remoteDir.resolve("hostkey.ser");

        sshd = SshServer.setUpDefaultServer();
        sshd.setPort(0); // let the OS pick a free port

        sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(hostKeyFile));

        // Accept any password for the test user
        sshd.setPasswordAuthenticator(
                (username, password, session) ->
                        TEST_USER.equals(username) && TEST_PASSWORD.equals(password));

        // ScpCommandFactory handles "scp …" commands; all other commands go to the OS shell
        ScpCommandFactory scpFactory = new ScpCommandFactory();
        scpFactory.setDelegateCommandFactory(ProcessShellCommandFactory.INSTANCE);
        sshd.setCommandFactory(scpFactory);

        sshd.start();
        port = sshd.getPort();
    }

    @AfterClass(alwaysRun = true)
    public void stopServer() throws IOException {
        if (sshd != null) {
            sshd.stop(true);
        }
        // Clean up any files that tests may have left on the "remote" side
        try (var stream = Files.walk(remoteDir)) {
            stream.sorted(java.util.Comparator.reverseOrder())
                    .forEach(
                            p -> {
                                try {
                                    Files.deleteIfExists(p);
                                } catch (IOException ignored) {
                                    // best-effort cleanup
                                }
                            });
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Builds a {@link Configuration} that points to the in-process SSH server. {@code
     * accept.all.server.keys} is always {@code true} so that the server's dynamically generated
     * host key is accepted without a known-hosts file.
     */
    private Configuration serverConfig() {
        return Configuration.builder()
                .set("host", "localhost")
                .set("port", String.valueOf(port))
                .set("user", TEST_USER)
                .set("password", TEST_PASSWORD)
                .set("strictHostKeyChecking", "false")
                .build();
    }

    // -----------------------------------------------------------------------
    // execute() — basic connectivity
    // -----------------------------------------------------------------------

    @Test
    public void testExecuteSimpleEchoCommandSucceeds() {
        MinaSshdConnection conn = new MinaSshdConnection("test-conn", serverConfig());

        // Should complete without any exception
        conn.execute("echo hello");

        conn.close();
    }

    @Test
    public void testExecuteMultipleCommandsReusesSameConnection() {
        MinaSshdConnection conn = new MinaSshdConnection("test-conn", serverConfig());

        conn.execute("echo first");
        conn.execute("echo second");
        conn.execute("echo third");

        conn.close();
    }

    @Test
    public void testExecuteCommandWithNonZeroExitCodeThrowsRuntimeException() {
        // 'false' is a standard Unix command that always exits with code 1
        try (MinaSshdConnection conn = new MinaSshdConnection("test-conn", serverConfig())) {
            conn.execute("false");
            fail("Expected RuntimeException for non-zero exit code");
        } catch (RuntimeException ex) {
            // expected — the SSH layer wraps the exit-code failure
        }
    }

    // -----------------------------------------------------------------------
    // uploadFile() — SCP transfer
    // -----------------------------------------------------------------------

    @Test
    public void testUploadFileArrivesAtRemotePath() throws Exception {
        Path localFile = Files.createTempFile("scp-test-local-", ".txt");
        Files.writeString(localFile, "hello from SCP\n");

        Path remoteDest = remoteDir.resolve("scp-test-upload.txt");

        try (MinaSshdConnection conn = new MinaSshdConnection("test-conn", serverConfig())) {
            conn.uploadFile(localFile, remoteDest.toString());

            assertTrue(Files.exists(remoteDest), "Uploaded file should exist on the remote path");
            assertEquals(
                    Files.readString(remoteDest),
                    "hello from SCP\n",
                    "Uploaded file content should match the original");
        } finally {
            Files.deleteIfExists(localFile);
            Files.deleteIfExists(remoteDest);
        }
    }

    @Test
    public void testUploadScriptThenExecuteItViaShell() throws Exception {
        Path localScript = Files.createTempFile("scp-script-", ".sh");
        Path remoteScript = remoteDir.resolve("scp-script-exec.sh");
        Path outputFile = remoteDir.resolve("scp-script-output.txt");

        Files.writeString(localScript, "#!/bin/bash\necho 'script ran' > " + outputFile + "\n");

        try (MinaSshdConnection conn = new MinaSshdConnection("test-conn", serverConfig())) {
            conn.uploadFile(localScript, remoteScript.toString());
            conn.execute("chmod +x " + remoteScript);
            conn.execute("/bin/bash " + remoteScript);

            assertTrue(
                    Files.exists(outputFile),
                    "Script should have created the output file on the remote side");
            assertEquals(Files.readString(outputFile).trim(), "script ran");
        } finally {
            Files.deleteIfExists(localScript);
            Files.deleteIfExists(remoteScript);
            Files.deleteIfExists(outputFile);
        }
    }

    // -----------------------------------------------------------------------
    // close() — connection teardown
    // -----------------------------------------------------------------------

    @Test
    public void testCloseSucceedsOnOpenConnection() {
        MinaSshdConnection conn = new MinaSshdConnection("test-conn", serverConfig());

        // Trigger a connection by executing a command
        conn.execute("echo open");

        // Should not throw
        conn.close();
    }

    @Test
    public void testCloseOnNeverConnectedIsNoop() {
        // Instantiate but never call execute() — close() should be safe
        MinaSshdConnection conn = new MinaSshdConnection("test-conn", serverConfig());
        conn.close(); // must not throw
    }
}
