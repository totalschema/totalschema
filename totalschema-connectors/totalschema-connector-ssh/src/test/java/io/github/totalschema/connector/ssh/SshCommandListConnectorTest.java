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

import io.github.totalschema.connector.ssh.spi.SshConnection;
import io.github.totalschema.engine.core.command.api.CommandContext;
import io.github.totalschema.model.ChangeFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class SshCommandListConnectorTest {

    // -----------------------------------------------------------------------
    // Shared mock infrastructure
    // -----------------------------------------------------------------------

    private static final String CONNECTOR_NAME = "my-ssh";

    private SshConnection mockConnection;
    private SshCommandListConnector connector;
    private CommandContext context;
    private Path tempFile;

    @BeforeMethod
    public void setUp() throws IOException {
        mockConnection = createMock(SshConnection.class);
        connector = new SshCommandListConnector(CONNECTOR_NAME, mockConnection);
        context = new CommandContext();
        tempFile = Files.createTempFile("ssh-cmd-test-", ".txt");
    }

    @AfterMethod
    public void tearDown() throws IOException {
        Files.deleteIfExists(tempFile);
    }

    /** Writes lines joined by {@code \n} to the shared temp file and returns a mock ChangeFile. */
    private ChangeFile fileWith(String content) throws IOException {
        Files.writeString(tempFile, content);
        ChangeFile mockChangeFile = createMock(ChangeFile.class);
        expect(mockChangeFile.getFile()).andReturn(tempFile);
        replay(mockChangeFile);
        return mockChangeFile;
    }

    // -----------------------------------------------------------------------
    // Constant & toString
    // -----------------------------------------------------------------------

    @Test
    public void testConnectorTypeConstant() {
        assertEquals(SshCommandListConnector.CONNECTOR_TYPE, "ssh-commands");
    }

    @Test
    public void testToStringContainsName() {
        replay(mockConnection);
        assertTrue(connector.toString().contains(CONNECTOR_NAME));
        verify(mockConnection);
    }

    // -----------------------------------------------------------------------
    // execute() — via mocked SshConnection
    // -----------------------------------------------------------------------

    @Test
    public void testEmptyFileExecutesNoCommands() throws Exception {
        ChangeFile changeFile = fileWith("");
        replay(mockConnection); // no execute() calls expected

        connector.execute(changeFile, context);

        verify(mockConnection);
    }

    @Test
    public void testBlankLinesAndCommentsExecuteNoCommands() throws Exception {
        ChangeFile changeFile = fileWith("# comment\n\n   \n# another\n");
        replay(mockConnection);

        connector.execute(changeFile, context);

        verify(mockConnection);
    }

    @Test
    public void testSingleCommandIsExecuted() throws Exception {
        ChangeFile changeFile = fileWith("echo hello\n");
        mockConnection.execute("echo hello");
        replay(mockConnection);

        connector.execute(changeFile, context);

        verify(mockConnection);
    }

    @Test
    public void testMultipleCommandsAreExecutedInOrder() throws Exception {
        ChangeFile changeFile = fileWith("mkdir -p /tmp/app\nls -la\nwhoami\n");
        mockConnection.execute("mkdir -p /tmp/app");
        mockConnection.execute("ls -la");
        mockConnection.execute("whoami");
        replay(mockConnection);

        connector.execute(changeFile, context);

        verify(mockConnection);
    }

    @Test
    public void testCommentsAndBlanksAmongCommandsAreSkipped() throws Exception {
        ChangeFile changeFile = fileWith("# setup\nmkdir -p /tmp/app\n\n# list\nls -la\n");
        mockConnection.execute("mkdir -p /tmp/app");
        mockConnection.execute("ls -la");
        replay(mockConnection);

        connector.execute(changeFile, context);

        verify(mockConnection);
    }

    @Test
    public void testContinuationLinesAreJoinedIntoSingleExecuteCall() throws Exception {
        ChangeFile changeFile = fileWith("echo \\\nfoo \\\nbar\n");
        mockConnection.execute("echo foo bar");
        replay(mockConnection);

        connector.execute(changeFile, context);

        verify(mockConnection);
    }

    @Test
    public void testCommentInsideContinuationIsSkipped() throws Exception {
        ChangeFile changeFile = fileWith("echo \\\n# mid comment\nworld\n");
        mockConnection.execute("echo world");
        replay(mockConnection);

        connector.execute(changeFile, context);

        verify(mockConnection);
    }

    @Test
    public void testInterruptedExceptionIsWrappedAndInterruptedFlagIsSet() throws Exception {
        ChangeFile changeFile = fileWith("echo hello\n");
        mockConnection.execute("echo hello");
        expectLastCall().andThrow(new InterruptedException("simulated"));
        replay(mockConnection);

        try {
            connector.execute(changeFile, context);
            fail("Expected RuntimeException");
        } catch (RuntimeException ex) {
            assertEquals(ex.getMessage(), "interrupted");
            assertTrue(ex.getCause() instanceof InterruptedException);
            assertTrue(Thread.currentThread().isInterrupted());
        }

        // Clear the interrupted flag so it does not bleed into other tests
        Thread.interrupted();
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

    // -----------------------------------------------------------------------
    // parseCommands() — static unit tests (no SSH, no I/O)
    // -----------------------------------------------------------------------

    @Test
    public void testEmptyFileProducesNoCommands() {
        List<String> result = SshCommandListConnector.parseCommands(List.of());
        assertTrue(result.isEmpty());
    }

    @Test
    public void testBlankLinesAreSkipped() {
        List<String> result =
                SshCommandListConnector.parseCommands(List.of("", "   ", "\t", "echo hello", ""));
        assertEquals(result, List.of("echo hello"));
    }

    // -----------------------------------------------------------------------
    // Comment lines (start with #)
    // -----------------------------------------------------------------------

    @Test
    public void testCommentOnlyFileProducesNoCommands() {
        List<String> result =
                SshCommandListConnector.parseCommands(List.of("# header", "# another comment"));
        assertTrue(result.isEmpty());
    }

    @Test
    public void testCommentLinesAreSkipped() {
        List<String> result =
                SshCommandListConnector.parseCommands(
                        List.of("# create dir", "mkdir -p /tmp/app", "# list", "ls -la"));
        assertEquals(result, List.of("mkdir -p /tmp/app", "ls -la"));
    }

    @Test
    public void testInlineHashIsNotAComment() {
        // Only a line whose FIRST character is '#' is a comment
        List<String> result =
                SshCommandListConnector.parseCommands(List.of("ls -la  # not a comment"));
        assertEquals(result, List.of("ls -la  # not a comment"));
    }

    @Test
    public void testIndentedHashIsNotAComment() {
        // Leading whitespace means the '#' is not at position 0
        List<String> result = SshCommandListConnector.parseCommands(List.of("  # not a comment"));
        assertEquals(result, List.of("  # not a comment"));
    }

    @Test
    public void testCommentInsideContinuationSequenceIsSkipped() {
        // The comment is dropped; the continuation is not interrupted
        List<String> result =
                SshCommandListConnector.parseCommands(
                        List.of("echo foo \\", "# comment mid-continuation", "bar"));
        assertEquals(result, List.of("echo foo bar"));
    }

    @Test
    public void testSingleCommandNoNewline() {
        List<String> result = SshCommandListConnector.parseCommands(List.of("echo hello"));
        assertEquals(result, List.of("echo hello"));
    }

    @Test
    public void testMultipleSimpleCommands() {
        List<String> result =
                SshCommandListConnector.parseCommands(
                        List.of("mkdir -p /tmp/myapp", "ls -la", "whoami"));
        assertEquals(result, List.of("mkdir -p /tmp/myapp", "ls -la", "whoami"));
    }

    // -----------------------------------------------------------------------
    // parseCommands() — basic / no continuation
    // -----------------------------------------------------------------------

    @Test
    public void testTwoLineContinuationIsJoined() {
        // "echo foo \" + "bar"  →  "echo foo bar"
        List<String> result = SshCommandListConnector.parseCommands(List.of("echo foo \\", "bar"));
        assertEquals(result, List.of("echo foo bar"));
    }

    @Test
    public void testThreeLineContinuationIsJoined() {
        List<String> result =
                SshCommandListConnector.parseCommands(List.of("echo \\", "foo \\", "bar"));
        assertEquals(result, List.of("echo foo bar"));
    }

    @Test
    public void testContinuationBetweenNormalCommands() {
        List<String> result =
                SshCommandListConnector.parseCommands(
                        List.of("first", "second \\", "continued", "third"));
        assertEquals(result, List.of("first", "second continued", "third"));
    }

    @Test
    public void testContinuationWithLeadingWhitespaceOnNextLine() {
        // Joining is literal (no extra space added): caller controls spacing
        List<String> result = SshCommandListConnector.parseCommands(List.of("echo \\", "  world"));
        assertEquals(result, List.of("echo   world"));
    }

    // -----------------------------------------------------------------------
    // Edge cases
    // -----------------------------------------------------------------------

    @Test
    public void testTrailingContinuationAtEndOfFileIsKept() {
        // Last line is a continuation with no following line
        List<String> result = SshCommandListConnector.parseCommands(List.of("echo \\"));
        assertEquals(result, List.of("echo "));
    }

    @Test
    public void testTrailingContinuationThatIsBlankIsDiscarded() {
        // A lone backslash at EOF produces an empty (blank) pending → discarded
        List<String> result = SshCommandListConnector.parseCommands(List.of("\\"));
        assertTrue(result.isEmpty());
    }

    @Test
    public void testBlankLineAfterContinuationProducesNoCommand() {
        // "  \" + ""  →  joined = "  "  →  isBlank() → skipped
        List<String> result = SshCommandListConnector.parseCommands(List.of("  \\", ""));
        assertTrue(result.isEmpty());
    }

    @Test
    public void testMultipleContinuationsInterleavedWithNormalLines() {
        List<String> result =
                SshCommandListConnector.parseCommands(
                        List.of(
                                "cmd1 \\",
                                "--flag1 \\",
                                "--flag2",
                                "",
                                "cmd2",
                                "cmd3 \\",
                                "--opt"));
        assertEquals(result, List.of("cmd1 --flag1 --flag2", "cmd2", "cmd3 --opt"));
    }
}
