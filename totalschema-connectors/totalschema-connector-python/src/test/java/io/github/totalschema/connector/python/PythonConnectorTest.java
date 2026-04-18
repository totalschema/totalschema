/*
 * totalschema: tool for managing database versioning and schema changes with ease.
 * Copyright (C) 2026 totalschema development team
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

package io.github.totalschema.connector.python;

import static org.easymock.EasyMock.*;
import static org.testng.Assert.*;

import io.github.totalschema.config.Configuration;
import io.github.totalschema.engine.core.command.api.CommandContext;
import io.github.totalschema.model.ApplyFile;
import io.github.totalschema.model.ChangeFile;
import io.github.totalschema.model.ChangeType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit tests for {@link PythonConnector}.
 *
 * <p>All OS-process execution is eliminated by injecting a mock {@link PythonProcessRunner} via the
 * package-private three-argument constructor. The mock runner captures the exact arguments that
 * would be passed to the OS, allowing every interaction to be verified without spawning real
 * processes or requiring Python to be installed.
 */
public class PythonConnectorTest {

    private static final String CONNECTOR_NAME = "myetl";
    private static final String SCRIPT_CONTENT = "print('hello from totalschema')";

    private PythonProcessRunner mockRunner;
    private CommandContext context;
    private Path tempScriptFile;

    @BeforeMethod
    public void setUp() throws IOException {
        mockRunner = createMock(PythonProcessRunner.class);
        context = new CommandContext();
        tempScriptFile = Files.createTempFile("ts_test_script_", ".py");
        Files.writeString(tempScriptFile, SCRIPT_CONTENT);
    }

    @AfterMethod
    public void tearDown() throws IOException {
        Files.deleteIfExists(tempScriptFile);
    }

    // -------------------------------------------------------------------------
    // Constant & toString
    // -------------------------------------------------------------------------

    @Test
    public void testConnectorTypeConstantIsPython() {
        assertEquals(PythonConnector.CONNECTOR_TYPE, "python");
    }

    @Test
    public void testToStringContainsConnectorName() {
        PythonConnector connector = connector(Configuration.builder().build());
        assertTrue(connector.toString().contains(CONNECTOR_NAME));
    }

    @Test
    public void testToStringReflectsDifferentName() {
        PythonConnector other =
                new PythonConnector("other", Configuration.builder().build(), mockRunner);
        assertTrue(other.toString().contains("other"));
        assertFalse(other.toString().contains(CONNECTOR_NAME));
    }

    // -------------------------------------------------------------------------
    // Default executable
    // -------------------------------------------------------------------------

    @Test
    public void testDefaultExecutableIsPython3() throws Exception {
        Capture<List<String>> capturedCmd = newCapture(CaptureType.ALL);
        mockRunner.run(capture(capturedCmd), anyObject(Path.class), anyObject());
        expectLastCall().once();
        replay(mockRunner);

        connector(Configuration.builder().build()).execute(applyFile(), context);

        verify(mockRunner);
        List<String> pythonCmd = capturedCmd.getValues().get(0);
        assertEquals(pythonCmd.get(0), "python3");
    }

    @Test
    public void testCustomExecutableIsUsedInCommand() throws Exception {
        Configuration config = Configuration.builder().set("executable", "python3.11").build();
        Capture<List<String>> capturedCmd = newCapture(CaptureType.ALL);
        mockRunner.run(capture(capturedCmd), anyObject(Path.class), anyObject());
        expectLastCall().once();
        replay(mockRunner);

        new PythonConnector(CONNECTOR_NAME, config, mockRunner).execute(applyFile(), context);

        verify(mockRunner);
        assertEquals(capturedCmd.getValues().get(0).get(0), "python3.11");
    }

    // -------------------------------------------------------------------------
    // Script invocation
    // -------------------------------------------------------------------------

    @Test
    public void testExecutePassesOriginalScriptPathToRunner() throws Exception {
        Capture<List<String>> capturedCmd = newCapture(CaptureType.ALL);
        mockRunner.run(capture(capturedCmd), anyObject(Path.class), anyObject());
        expectLastCall().once();
        replay(mockRunner);

        connector(Configuration.builder().build()).execute(applyFile(), context);

        verify(mockRunner);
        List<String> cmd = capturedCmd.getValues().get(0);
        assertEquals(cmd.size(), 2);
        assertEquals(
                Path.of(cmd.get(1)).toAbsolutePath(),
                tempScriptFile.toAbsolutePath(),
                "Runner must receive the original script path, not a copy");
    }

    // -------------------------------------------------------------------------
    // Working directory
    // -------------------------------------------------------------------------

    @Test
    public void testWorkingDirectoryDefaultsToChangeFileParent() throws Exception {
        Capture<Path> capturedWorkDir = newCapture(CaptureType.ALL);
        mockRunner.run(anyObject(), capture(capturedWorkDir), anyObject());
        expectLastCall().once();
        replay(mockRunner);

        connector(Configuration.builder().build()).execute(applyFile(), context);

        verify(mockRunner);
        Path expectedWorkDir = tempScriptFile.toAbsolutePath().getParent();
        assertEquals(capturedWorkDir.getValues().get(0), expectedWorkDir);
    }

    @Test
    public void testWorkingDirectoryCanBeOverridden() throws Exception {
        Path overrideDir = Path.of(System.getProperty("java.io.tmpdir"));
        Configuration config =
                Configuration.builder().set("workingDirectory", overrideDir.toString()).build();

        Capture<Path> capturedWorkDir = newCapture(CaptureType.ALL);
        mockRunner.run(anyObject(), capture(capturedWorkDir), anyObject());
        expectLastCall().once();
        replay(mockRunner);

        new PythonConnector(CONNECTOR_NAME, config, mockRunner).execute(applyFile(), context);

        verify(mockRunner);
        assertEquals(capturedWorkDir.getValues().get(0), overrideDir);
    }

    // -------------------------------------------------------------------------
    // Null guard
    // -------------------------------------------------------------------------

    @Test(
            expectedExceptions = NullPointerException.class,
            expectedExceptionsMessageRegExp = "file is null")
    public void testNullChangeFilePathThrowsNpe() throws InterruptedException {
        ChangeFile mockChangeFile = createMock(ChangeFile.class);
        expect(mockChangeFile.getFile()).andReturn(null);
        replay(mockChangeFile, mockRunner);

        connector(Configuration.builder().build()).execute(mockChangeFile, context);
    }

    // -------------------------------------------------------------------------
    // Default (public) constructor smoke-test
    // -------------------------------------------------------------------------

    @Test
    public void testDefaultConstructorCreatesConnector() {
        PythonConnector c = new PythonConnector(CONNECTOR_NAME, Configuration.builder().build());
        assertNotNull(c);
    }

    @Test
    public void testDefaultConstructorConnectorNameIsReflectedInToString() {
        PythonConnector c = new PythonConnector("namedConnector", Configuration.builder().build());
        assertTrue(c.toString().contains("namedConnector"));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private PythonConnector connector(Configuration configuration) {
        return new PythonConnector(CONNECTOR_NAME, configuration, mockRunner);
    }

    private ApplyFile applyFile() {
        ChangeFile.Id id =
                new ChangeFile.Id(
                        "", "0001", "run_pipeline", null, ChangeType.APPLY, CONNECTOR_NAME, "py");
        return new ApplyFile(tempScriptFile.getParent(), tempScriptFile, id);
    }
}
