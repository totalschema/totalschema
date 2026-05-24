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

package io.github.totalschema.connector.shell;

import static org.easymock.EasyMock.*;
import static org.testng.Assert.*;

import io.github.totalschema.config.Configuration;
import io.github.totalschema.connector.shell.spi.ShellScriptRunner;
import io.github.totalschema.connector.shell.spi.ShellScriptRunnerFactory;
import io.github.totalschema.engine.core.command.api.CommandContext;
import io.github.totalschema.model.ApplyFile;
import io.github.totalschema.model.ChangeFile;
import io.github.totalschema.model.ChangeType;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit tests for {@link ShellScriptConnector}.
 *
 * <p>All OS-process execution is eliminated by injecting a mock {@link ShellScriptRunnerFactory}
 * via the three-argument constructor. The mock factory returns a mock {@link ShellScriptRunner},
 * allowing every interaction to be verified without touching the file system or spawning a process.
 */
public class ShellScriptConnectorTest {

    private static final String CONNECTOR_NAME = "myshell";

    private ShellScriptRunnerFactory mockFactory;
    private ShellScriptRunner mockRunner;
    private Configuration configuration;
    private CommandContext context;
    private ShellScriptConnector connector;

    @BeforeMethod
    public void setUp() {
        mockFactory = createMock(ShellScriptRunnerFactory.class);
        mockRunner = createMock(ShellScriptRunner.class);
        configuration = Configuration.builder().build();
        context = new CommandContext();
        connector = new ShellScriptConnector(CONNECTOR_NAME, configuration, mockFactory);
    }

    // -------------------------------------------------------------------------
    // Constant & toString
    // -------------------------------------------------------------------------

    @Test
    public void testConnectorTypeConstantIsShell() {
        assertEquals(ShellScriptConnector.CONNECTOR_TYPE, "shell");
    }

    @Test
    public void testToStringContainsConnectorName() {
        assertTrue(connector.toString().contains(CONNECTOR_NAME));
    }

    @Test
    public void testToStringReflectsDifferentName() {
        ShellScriptConnector other =
                new ShellScriptConnector("anotherConnector", configuration, mockFactory);
        assertTrue(other.toString().contains("anotherConnector"));
        assertFalse(other.toString().contains(CONNECTOR_NAME));
    }

    // -------------------------------------------------------------------------
    // Factory argument passing
    // -------------------------------------------------------------------------

    @Test
    public void testExecutePassesConnectorNameToFactory() throws InterruptedException {
        Path changesDir = Paths.get("/changes");
        Path scriptFile = Paths.get("/changes/0001.setup.apply.myshell.sh");
        ApplyFile applyFile = makeApplyFile(changesDir, scriptFile, "0001", "setup", null, "sh");

        expect(mockFactory.getRunner(eq(CONNECTOR_NAME), eq(configuration), anyString()))
                .andReturn(mockRunner);
        mockRunner.execute(anyObject());
        mockRunner.close();
        replay(mockFactory, mockRunner);

        connector.execute(applyFile, context);

        verify(mockFactory, mockRunner);
    }

    @Test
    public void testExecutePassesConfigurationToFactory() throws InterruptedException {
        Configuration specificConfig =
                Configuration.builder().set("start.command", "bash,-c").build();
        ShellScriptConnector connectorWithConfig =
                new ShellScriptConnector(CONNECTOR_NAME, specificConfig, mockFactory);

        Path changesDir = Paths.get("/changes");
        Path scriptFile = Paths.get("/changes/0001.setup.apply.myshell.sh");
        ApplyFile applyFile = makeApplyFile(changesDir, scriptFile, "0001", "setup", null, "sh");

        expect(mockFactory.getRunner(anyString(), eq(specificConfig), anyString()))
                .andReturn(mockRunner);
        mockRunner.execute(anyObject());
        mockRunner.close();
        replay(mockFactory, mockRunner);

        connectorWithConfig.execute(applyFile, context);

        verify(mockFactory, mockRunner);
    }

    @Test
    public void testExecutePassesExtensionToFactory() throws InterruptedException {
        Path changesDir = Paths.get("/changes");
        Path scriptFile = Paths.get("/changes/0001.setup.apply.myshell.sh");
        ApplyFile applyFile = makeApplyFile(changesDir, scriptFile, "0001", "setup", null, "sh");

        expect(mockFactory.getRunner(CONNECTOR_NAME, configuration, "sh")).andReturn(mockRunner);
        mockRunner.execute(anyObject());
        mockRunner.close();
        replay(mockFactory, mockRunner);

        connector.execute(applyFile, context);

        verify(mockFactory, mockRunner);
    }

    @Test
    public void testExecutePassesExtensionNotFullFileName() throws InterruptedException {
        // File lives in a sub-directory; only the extension must reach the factory, not the path
        Path changesDir = Paths.get("/workspace/changes");
        Path scriptFile = Paths.get("/workspace/changes/v1/0002.install.apply.myshell.sh");
        ApplyFile applyFile = makeApplyFile(changesDir, scriptFile, "0002", "install", null, "sh");

        expect(mockFactory.getRunner(CONNECTOR_NAME, configuration, "sh")).andReturn(mockRunner);
        mockRunner.execute(anyObject());
        mockRunner.close();
        replay(mockFactory, mockRunner);

        connector.execute(applyFile, context);

        verify(mockFactory, mockRunner);
    }

    // -------------------------------------------------------------------------
    // Runner argument passing
    // -------------------------------------------------------------------------

    @Test
    public void testExecutePassesAbsoluteScriptPathToRunner() throws InterruptedException {
        Path changesDir = Paths.get("/changes");
        Path scriptFile = Paths.get("/changes/0001.setup.apply.myshell.sh");
        ApplyFile applyFile = makeApplyFile(changesDir, scriptFile, "0001", "setup", null, "sh");

        String expectedAbsolutePath = scriptFile.toAbsolutePath().toString();

        expect(mockFactory.getRunner(anyString(), anyObject(), anyString())).andReturn(mockRunner);
        mockRunner.execute(Collections.singletonList(expectedAbsolutePath));
        mockRunner.close();
        replay(mockFactory, mockRunner);

        connector.execute(applyFile, context);

        verify(mockFactory, mockRunner);
    }

    @Test
    public void testExecuteWrapsPathInSingletonList() throws InterruptedException {
        Path changesDir = Paths.get("/changes");
        Path scriptFile = Paths.get("/changes/0001.setup.apply.myshell.sh");
        ApplyFile applyFile = makeApplyFile(changesDir, scriptFile, "0001", "setup", null, "sh");

        expect(mockFactory.getRunner(anyString(), anyObject(), anyString())).andReturn(mockRunner);
        mockRunner.execute(Collections.singletonList(scriptFile.toAbsolutePath().toString()));
        mockRunner.close();
        replay(mockFactory, mockRunner);

        connector.execute(applyFile, context);

        verify(mockFactory, mockRunner);
    }

    // -------------------------------------------------------------------------
    // Runner lifecycle (close / try-with-resources)
    // -------------------------------------------------------------------------

    @Test
    public void testExecuteClosesRunnerAfterSuccessfulExecution() throws InterruptedException {
        Path changesDir = Paths.get("/changes");
        Path scriptFile = Paths.get("/changes/0001.setup.apply.myshell.sh");
        ApplyFile applyFile = makeApplyFile(changesDir, scriptFile, "0001", "setup", null, "sh");

        expect(mockFactory.getRunner(anyString(), anyObject(), anyString())).andReturn(mockRunner);
        mockRunner.execute(anyObject());
        mockRunner.close();
        expectLastCall().once();
        replay(mockFactory, mockRunner);

        connector.execute(applyFile, context);

        verify(mockFactory, mockRunner);
    }

    @Test(expectedExceptions = InterruptedException.class)
    public void testExecutePropagatesInterruptedException() throws InterruptedException {
        Path changesDir = Paths.get("/changes");
        Path scriptFile = Paths.get("/changes/0001.setup.apply.myshell.sh");
        ApplyFile applyFile = makeApplyFile(changesDir, scriptFile, "0001", "setup", null, "sh");

        expect(mockFactory.getRunner(anyString(), anyObject(), anyString())).andReturn(mockRunner);
        mockRunner.execute(anyObject());
        expectLastCall().andThrow(new InterruptedException("simulated interruption"));
        mockRunner.close();
        replay(mockFactory, mockRunner);

        connector.execute(applyFile, context);
    }

    @Test
    public void testExecuteClosesRunnerEvenWhenInterrupted() throws InterruptedException {
        Path changesDir = Paths.get("/changes");
        Path scriptFile = Paths.get("/changes/0001.setup.apply.myshell.sh");
        ApplyFile applyFile = makeApplyFile(changesDir, scriptFile, "0001", "setup", null, "sh");

        expect(mockFactory.getRunner(anyString(), anyObject(), anyString())).andReturn(mockRunner);
        mockRunner.execute(anyObject());
        expectLastCall().andThrow(new InterruptedException("simulated interruption"));
        mockRunner.close();
        expectLastCall().once();
        replay(mockFactory, mockRunner);

        try {
            connector.execute(applyFile, context);
        } catch (InterruptedException ignored) {
            // exception is ignored
        }

        verify(mockFactory, mockRunner);
    }

    @Test
    public void testRunnerIsCreatedFreshForEachExecution() throws InterruptedException {
        ShellScriptRunner mockRunner2 = createMock(ShellScriptRunner.class);

        Path changesDir = Paths.get("/changes");
        Path script1 = Paths.get("/changes/0001.setup.apply.myshell.sh");
        Path script2 = Paths.get("/changes/0002.migrate.apply.myshell.sh");
        ApplyFile applyFile1 = makeApplyFile(changesDir, script1, "0001", "setup", null, "sh");
        ApplyFile applyFile2 = makeApplyFile(changesDir, script2, "0002", "migrate", null, "sh");

        expect(mockFactory.getRunner(CONNECTOR_NAME, configuration, "sh")).andReturn(mockRunner);
        mockRunner.execute(anyObject());
        mockRunner.close();

        expect(mockFactory.getRunner(CONNECTOR_NAME, configuration, "sh")).andReturn(mockRunner2);
        mockRunner2.execute(anyObject());
        mockRunner2.close();

        replay(mockFactory, mockRunner, mockRunner2);

        connector.execute(applyFile1, context);
        connector.execute(applyFile2, context);

        verify(mockFactory, mockRunner, mockRunner2);
    }

    // -------------------------------------------------------------------------
    // Different script extensions
    // -------------------------------------------------------------------------

    @Test
    public void testExecuteWithBatExtension() throws InterruptedException {
        Path changesDir = Paths.get("/changes");
        Path scriptFile = Paths.get("/changes/0002.install.apply.myshell.bat");
        ApplyFile applyFile = makeApplyFile(changesDir, scriptFile, "0002", "install", null, "bat");

        expect(mockFactory.getRunner(CONNECTOR_NAME, configuration, "bat")).andReturn(mockRunner);
        mockRunner.execute(anyObject());
        mockRunner.close();
        replay(mockFactory, mockRunner);

        connector.execute(applyFile, context);

        verify(mockFactory, mockRunner);
    }

    @Test
    public void testExecuteWithPs1Extension() throws InterruptedException {
        Path changesDir = Paths.get("/changes");
        Path scriptFile = Paths.get("/changes/0003.configure.apply.myshell.ps1");
        ApplyFile applyFile =
                makeApplyFile(changesDir, scriptFile, "0003", "configure", null, "ps1");

        expect(mockFactory.getRunner(CONNECTOR_NAME, configuration, "ps1")).andReturn(mockRunner);
        mockRunner.execute(anyObject());
        mockRunner.close();
        replay(mockFactory, mockRunner);

        connector.execute(applyFile, context);

        verify(mockFactory, mockRunner);
    }

    // -------------------------------------------------------------------------
    // checkConnection — interpreter probing per distinct extension
    // -------------------------------------------------------------------------

    @Test
    public void testCheckConnectionProbesRunnerForEachDistinctExtension()
            throws InterruptedException {
        ShellScriptRunner mockRunner2 = createMock(ShellScriptRunner.class);

        ChangeFile.Id idSh = makeId("0001", "setup", null, "sh");
        ChangeFile.Id idPs1 = makeId("0002", "configure", null, "ps1");
        ChangeFile.Id idSh2 = makeId("0003", "migrate", null, "sh"); // same ext as idSh

        expect(mockFactory.getRunner(CONNECTOR_NAME, configuration, "sh")).andReturn(mockRunner);
        mockRunner.checkReady();
        mockRunner.close();

        expect(mockFactory.getRunner(CONNECTOR_NAME, configuration, "ps1")).andReturn(mockRunner2);
        mockRunner2.checkReady();
        mockRunner2.close();

        replay(mockFactory, mockRunner, mockRunner2);

        connector.checkConnection(context, List.of(idSh, idPs1, idSh2));

        verify(mockFactory, mockRunner, mockRunner2);
    }

    @Test
    public void testCheckConnectionWithEmptyListDoesNothing() throws InterruptedException {
        replay(mockFactory, mockRunner);

        connector.checkConnection(context, Collections.emptyList());

        verify(mockFactory, mockRunner); // no interactions expected
    }

    @Test
    public void testCheckConnectionClosesRunnerAfterCheckReady() throws InterruptedException {
        ChangeFile.Id id = makeId("0001", "setup", null, "sh");

        expect(mockFactory.getRunner(CONNECTOR_NAME, configuration, "sh")).andReturn(mockRunner);
        mockRunner.checkReady();
        mockRunner.close();
        expectLastCall().once();
        replay(mockFactory, mockRunner);

        connector.checkConnection(context, List.of(id));

        verify(mockFactory, mockRunner);
    }

    @Test(expectedExceptions = InterruptedException.class)
    public void testCheckConnectionPropagatesInterruptedException() throws InterruptedException {
        ChangeFile.Id id = makeId("0001", "setup", null, "sh");

        expect(mockFactory.getRunner(CONNECTOR_NAME, configuration, "sh")).andReturn(mockRunner);
        mockRunner.checkReady();
        expectLastCall().andThrow(new InterruptedException("simulated"));
        mockRunner.close();
        replay(mockFactory, mockRunner);

        connector.checkConnection(context, List.of(id));
    }

    // -------------------------------------------------------------------------
    // Null-safety guards
    // -------------------------------------------------------------------------

    @Test(expectedExceptions = NullPointerException.class)
    public void testExecuteWithNullChangeFileThrowsNpe() throws InterruptedException {
        connector.execute(null, context);
    }

    @Test(
            expectedExceptions = NullPointerException.class,
            expectedExceptionsMessageRegExp = "file is null")
    public void testExecuteWithNullFilePathThrowsNpe() throws InterruptedException {
        ChangeFile mockChangeFile = createMock(ChangeFile.class);
        expect(mockChangeFile.getFile()).andReturn(null);
        replay(mockChangeFile);

        connector.execute(mockChangeFile, context);
    }

    // -------------------------------------------------------------------------
    // Default-factory constructor
    // -------------------------------------------------------------------------

    @Test
    public void testDefaultConstructorCreatesConnector() {
        ShellScriptConnector c = new ShellScriptConnector(CONNECTOR_NAME, configuration);
        assertNotNull(c);
    }

    @Test
    public void testDefaultConstructorConnectorNameIsReflectedInToString() {
        ShellScriptConnector c = new ShellScriptConnector("namedConnector", configuration);
        assertTrue(c.toString().contains("namedConnector"));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static ApplyFile makeApplyFile(
            Path changesDir,
            Path file,
            String order,
            String description,
            String environment,
            String extension) {
        return new ApplyFile(changesDir, file, makeId(order, description, environment, extension));
    }

    private static ChangeFile.Id makeId(
            String order, String description, String environment, String extension) {
        return new ChangeFile.Id(
                "", order, description, environment, ChangeType.APPLY, CONNECTOR_NAME, extension);
    }
}
