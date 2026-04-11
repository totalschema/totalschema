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

import static org.easymock.EasyMock.*;
import static org.testng.Assert.*;

import io.github.totalschema.config.Configuration;
import io.github.totalschema.connector.ssh.spi.SshConnection;
import io.github.totalschema.engine.core.command.api.CommandContext;
import io.github.totalschema.model.ChangeFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class SshScriptConnectorTest {

    // -----------------------------------------------------------------------
    // Shared mock infrastructure
    // -----------------------------------------------------------------------

    private static final String CONNECTOR_NAME = "my-ssh-script";

    private SshConnection mockConnection;
    private SshScriptConnector connector;
    private CommandContext context;
    private Path tempScript;

    @BeforeMethod
    public void setUp() throws IOException {
        mockConnection = createMock(SshConnection.class);
        connector =
                new SshScriptConnector(
                        CONNECTOR_NAME, mockConnection, Configuration.builder().build());
        context = new CommandContext();
        tempScript = Files.createTempFile("ssh-script-test-", ".sh");
        Files.writeString(tempScript, "#!/bin/bash\necho hello\n");
    }

    @AfterMethod
    public void tearDown() throws IOException {
        Files.deleteIfExists(tempScript);
    }

    /** Returns a mock ChangeFile whose {@code getFile()} returns the shared temp script. */
    private ChangeFile changeFile() {
        ChangeFile mock = createMock(ChangeFile.class);
        expect(mock.getFile()).andReturn(tempScript);
        replay(mock);
        return mock;
    }

    // -----------------------------------------------------------------------
    // Constant & toString
    // -----------------------------------------------------------------------

    @Test
    public void testConnectorTypeConstant() {
        assertEquals(SshScriptConnector.CONNECTOR_TYPE, "ssh-script");
    }

    @Test
    public void testToStringContainsNameDefaultRemoteDirAndDefaultShell() {
        replay(mockConnection);

        String str = connector.toString();

        assertTrue(str.contains(CONNECTOR_NAME));
        assertTrue(str.contains("/tmp"));
        assertTrue(str.contains("/bin/bash"));
        verify(mockConnection);
    }

    @Test
    public void testToStringReflectsCustomRemoteDirAndShell() {
        Configuration config =
                Configuration.builder()
                        .set("remote.temp.dir", "/opt/tmp")
                        .set("shell", "/bin/sh")
                        .build();
        SshScriptConnector customConnector =
                new SshScriptConnector(CONNECTOR_NAME, mockConnection, config);
        replay(mockConnection);

        String str = customConnector.toString();

        assertTrue(str.contains(CONNECTOR_NAME));
        assertTrue(str.contains("/opt/tmp"));
        assertTrue(str.contains("/bin/sh"));
        verify(mockConnection);
    }

    // -----------------------------------------------------------------------
    // execute() — happy path with defaults
    // -----------------------------------------------------------------------

    @Test
    public void testHappyPathUploadChmodExecuteCleanupAreCalledInOrder() throws Exception {
        Capture<String> remotePath = newCapture(CaptureType.FIRST);

        mockConnection.uploadFile(eq(tempScript), capture(remotePath));
        mockConnection.execute(anyString()); // chmod +x <path>
        mockConnection.execute(anyString()); // /bin/bash <path>
        mockConnection.execute(anyString()); // rm -f <path>
        replay(mockConnection);

        connector.execute(changeFile(), context);

        verify(mockConnection);
        assertTrue(remotePath.getValue().startsWith("/tmp/totalschema-script-"));
        assertTrue(remotePath.getValue().endsWith(".sh"));
    }

    @Test
    public void testDefaultShellAndRemoteDirAreUsedInCommands() throws Exception {
        Capture<String> chmodArg = newCapture(CaptureType.FIRST);
        Capture<String> executeArg = newCapture(CaptureType.FIRST);
        Capture<String> rmArg = newCapture(CaptureType.FIRST);

        mockConnection.uploadFile(eq(tempScript), anyString());
        mockConnection.execute(capture(chmodArg));
        mockConnection.execute(capture(executeArg));
        mockConnection.execute(capture(rmArg));
        replay(mockConnection);

        connector.execute(changeFile(), context);

        verify(mockConnection);
        assertTrue(chmodArg.getValue().startsWith("chmod +x /tmp/totalschema-script-"));
        assertTrue(executeArg.getValue().startsWith("/bin/bash /tmp/totalschema-script-"));
        assertTrue(rmArg.getValue().startsWith("rm -f /tmp/totalschema-script-"));
    }

    @Test
    public void testAllThreeCommandsShareTheSameRemotePath() throws Exception {
        Capture<String> uploadedPath = newCapture(CaptureType.FIRST);
        Capture<String> chmodArg = newCapture(CaptureType.FIRST);
        Capture<String> executeArg = newCapture(CaptureType.FIRST);
        Capture<String> rmArg = newCapture(CaptureType.FIRST);

        mockConnection.uploadFile(eq(tempScript), capture(uploadedPath));
        mockConnection.execute(capture(chmodArg));
        mockConnection.execute(capture(executeArg));
        mockConnection.execute(capture(rmArg));
        replay(mockConnection);

        connector.execute(changeFile(), context);

        verify(mockConnection);
        String path = uploadedPath.getValue();
        assertEquals(chmodArg.getValue(), "chmod +x " + path);
        assertEquals(executeArg.getValue(), "/bin/bash " + path);
        assertEquals(rmArg.getValue(), "rm -f " + path);
    }

    // -----------------------------------------------------------------------
    // execute() — custom configuration
    // -----------------------------------------------------------------------

    @Test
    public void testCustomRemoteTempDirIsUsed() throws Exception {
        Configuration config =
                Configuration.builder().set("remote.temp.dir", "/opt/deploy").build();
        SshScriptConnector customConnector =
                new SshScriptConnector(CONNECTOR_NAME, mockConnection, config);
        Capture<String> remotePath = newCapture(CaptureType.FIRST);

        mockConnection.uploadFile(eq(tempScript), capture(remotePath));
        mockConnection.execute(anyString());
        mockConnection.execute(anyString());
        mockConnection.execute(anyString());
        replay(mockConnection);

        customConnector.execute(changeFile(), context);

        verify(mockConnection);
        assertTrue(remotePath.getValue().startsWith("/opt/deploy/totalschema-script-"));
    }

    @Test
    public void testCustomShellIsUsedInExecuteCommand() throws Exception {
        Configuration config = Configuration.builder().set("shell", "/bin/sh").build();
        SshScriptConnector customConnector =
                new SshScriptConnector(CONNECTOR_NAME, mockConnection, config);
        Capture<String> chmodArg = newCapture(CaptureType.FIRST);
        Capture<String> executeArg = newCapture(CaptureType.FIRST);
        Capture<String> rmArg = newCapture(CaptureType.FIRST);

        mockConnection.uploadFile(eq(tempScript), anyString());
        mockConnection.execute(capture(chmodArg)); // chmod +x
        mockConnection.execute(capture(executeArg)); // run script
        mockConnection.execute(capture(rmArg)); // rm -f
        replay(mockConnection);

        customConnector.execute(changeFile(), context);

        verify(mockConnection);
        assertTrue(chmodArg.getValue().startsWith("chmod +x "));
        assertTrue(executeArg.getValue().startsWith("/bin/sh "));
        assertTrue(rmArg.getValue().startsWith("rm -f "));
    }

    // -----------------------------------------------------------------------
    // execute() — cleanup in finally block
    // -----------------------------------------------------------------------

    @Test
    public void testCleanupIsCalledEvenWhenChmodThrows() throws Exception {
        mockConnection.uploadFile(eq(tempScript), anyString());
        mockConnection.execute(anyString()); // chmod +x — throws
        expectLastCall().andThrow(new InterruptedException("simulated"));
        mockConnection.execute(anyString()); // rm -f still runs in finally
        replay(mockConnection);

        try {
            connector.execute(changeFile(), context);
            fail("Expected RuntimeException");
        } catch (RuntimeException ex) {
            assertEquals(ex.getMessage(), "interrupted");
        }

        // Clear the interrupted flag so it does not bleed into other tests
        boolean wasInterrupted = Thread.interrupted();
        assertTrue(wasInterrupted);
        verify(mockConnection);
    }

    @Test
    public void testCleanupIsCalledEvenWhenScriptExecutionThrows() throws Exception {
        mockConnection.uploadFile(eq(tempScript), anyString());
        mockConnection.execute(anyString()); // chmod +x — ok
        mockConnection.execute(anyString()); // run script — throws
        expectLastCall().andThrow(new InterruptedException("simulated"));
        mockConnection.execute(anyString()); // rm -f still runs in finally
        replay(mockConnection);

        try {
            connector.execute(changeFile(), context);
            fail("Expected RuntimeException");
        } catch (RuntimeException ex) {
            assertEquals(ex.getMessage(), "interrupted");
        }

        boolean wasInterrupted = Thread.interrupted();
        assertTrue(wasInterrupted);
        verify(mockConnection);
    }

    // -----------------------------------------------------------------------
    // execute() — exception handling
    // -----------------------------------------------------------------------

    @Test
    public void testIOExceptionFromUploadIsWrapped() throws Exception {
        mockConnection.uploadFile(eq(tempScript), anyString());
        expectLastCall().andThrow(new IOException("upload failed"));
        // remoteScriptPath is assigned before uploadFile() is called, so finally always runs rm -f
        mockConnection.execute(anyString());
        replay(mockConnection);

        try {
            connector.execute(changeFile(), context);
            fail("Expected RuntimeException");
        } catch (RuntimeException ex) {
            assertTrue(ex.getCause() instanceof IOException);
            assertTrue(ex.getMessage().contains("Failure processing script file"));
        }

        verify(mockConnection);
    }

    @Test
    public void testInterruptedExceptionFromUploadIsWrappedAndInterruptFlagIsSet()
            throws Exception {
        mockConnection.uploadFile(eq(tempScript), anyString());
        expectLastCall().andThrow(new InterruptedException("interrupted"));
        mockConnection.execute(anyString()); // rm -f still runs in finally
        replay(mockConnection);

        try {
            connector.execute(changeFile(), context);
            fail("Expected RuntimeException");
        } catch (RuntimeException ex) {
            assertEquals(ex.getMessage(), "interrupted");
            assertTrue(ex.getCause() instanceof InterruptedException);
            assertTrue(Thread.currentThread().isInterrupted());
        }

        // Clear the interrupted flag so it does not bleed into other tests
        boolean wasInterrupted = Thread.interrupted();
        assertTrue(wasInterrupted);
        verify(mockConnection);
    }

    // -----------------------------------------------------------------------
    // close()
    // -----------------------------------------------------------------------

    @Test
    public void testCloseCallsSessionClose() throws IOException {
        mockConnection.close();
        replay(mockConnection);

        connector.close();

        verify(mockConnection);
    }
}
