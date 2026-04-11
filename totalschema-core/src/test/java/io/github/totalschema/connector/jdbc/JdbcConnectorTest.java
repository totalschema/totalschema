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

package io.github.totalschema.connector.jdbc;

import static org.easymock.EasyMock.*;
import static org.testng.Assert.*;

import io.github.totalschema.config.Configuration;
import io.github.totalschema.engine.api.Context;
import io.github.totalschema.engine.core.command.api.CommandContext;
import io.github.totalschema.jdbc.JdbcDatabase;
import io.github.totalschema.model.ApplyFile;
import io.github.totalschema.model.ChangeFile;
import io.github.totalschema.model.ChangeType;
import io.github.totalschema.spi.script.ScriptExecutor;
import java.nio.file.Files;
import java.nio.file.Path;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit tests for {@link JdbcConnector}.
 *
 * <p>Variable substitution has moved to {@link
 * io.github.totalschema.engine.internal.script.SqlScriptExecutor}. The connector is now responsible
 * only for:
 *
 * <ol>
 *   <li>Reading the change file from disk.
 *   <li>Acquiring the {@link JdbcDatabase} from the IoC container and exposing it through a child
 *       context.
 *   <li>Retrieving the appropriate {@link ScriptExecutor} via the context.
 *   <li>Invoking the executor with the raw file content and the child context.
 * </ol>
 */
public class JdbcConnectorTest {

    private static final String CONNECTOR_NAME = "mydb";

    private ScriptExecutor mockScriptExecutor;
    private JdbcDatabase mockJdbcDatabase;
    private Context mockParentContext;
    private Path tempFile;

    @BeforeMethod
    public void setUp() {
        mockScriptExecutor = createMock(ScriptExecutor.class);
        mockJdbcDatabase = createMock(JdbcDatabase.class);
        mockParentContext = createMock(Context.class);
    }

    @AfterMethod
    public void tearDown() throws Exception {
        if (tempFile != null) {
            Files.deleteIfExists(tempFile);
        }
    }

    // -------------------------------------------------------------------------
    // toString / constant
    // -------------------------------------------------------------------------

    @Test
    public void testConnectorTypeConstantIsJdbc() {
        assertEquals(JdbcConnector.CONNECTOR_TYPE, "jdbc");
    }

    @Test
    public void testToStringContainsConnectorName() {
        JdbcConnector connector =
                new JdbcConnector(CONNECTOR_NAME, Configuration.builder().build());
        assertTrue(connector.toString().contains(CONNECTOR_NAME));
    }

    // -------------------------------------------------------------------------
    // Raw content forwarding — connector no longer does substitution
    // -------------------------------------------------------------------------

    @Test
    public void testRawFileContentIsForwardedToExecutor() throws Exception {
        Configuration connectorConfig = Configuration.builder().build();

        tempFile = Files.createTempFile("test-", ".sql");
        String rawContent = "CREATE TABLE foo (id INT);";
        Files.writeString(tempFile, rawContent);

        ApplyFile applyFile = makeApplyFile(tempFile, "sql");

        Capture<String> capturedScript = newCapture(CaptureType.FIRST);
        expectStandardMocks("sql", connectorConfig);
        mockScriptExecutor.execute(capture(capturedScript), isA(Context.class));
        replay(mockParentContext, mockScriptExecutor, mockJdbcDatabase);

        new JdbcConnector(CONNECTOR_NAME, connectorConfig)
                .execute(applyFile, new CommandContext(mockParentContext));

        verify(mockParentContext, mockScriptExecutor, mockJdbcDatabase);
        assertEquals(capturedScript.getValue(), rawContent);
    }

    @Test
    public void testPlaceholdersAreNotSubstitutedByConnector() throws Exception {
        // Substitution is now the executor's concern — the connector passes raw content.
        Configuration connectorConfig =
                Configuration.builder()
                        .set("scriptExecutors.sql.variableSubstitution", "true")
                        .build();

        tempFile = Files.createTempFile("test-", ".sql");
        String rawContent = "CREATE SCHEMA ${mySchema};";
        Files.writeString(tempFile, rawContent);

        ApplyFile applyFile = makeApplyFile(tempFile, "sql");

        Capture<String> capturedScript = newCapture(CaptureType.FIRST);
        expectStandardMocks("sql", connectorConfig);
        mockScriptExecutor.execute(capture(capturedScript), isA(Context.class));
        replay(mockParentContext, mockScriptExecutor, mockJdbcDatabase);

        new JdbcConnector(CONNECTOR_NAME, connectorConfig)
                .execute(applyFile, new CommandContext(mockParentContext));

        verify(mockParentContext, mockScriptExecutor, mockJdbcDatabase);
        // Content must be passed raw — no substitution at the connector level.
        assertEquals(capturedScript.getValue(), rawContent);
    }

    @Test
    public void testGroovyContentIsForwardedUnchanged() throws Exception {
        Configuration connectorConfig = Configuration.builder().build();

        tempFile = Files.createTempFile("test-", ".groovy");
        String rawContent = "sql.execute(\"SELECT 1\")";
        Files.writeString(tempFile, rawContent);

        ApplyFile applyFile = makeApplyFile(tempFile, "groovy");

        Capture<String> capturedScript = newCapture(CaptureType.FIRST);
        expectStandardMocks("groovy", connectorConfig);
        mockScriptExecutor.execute(capture(capturedScript), isA(Context.class));
        replay(mockParentContext, mockScriptExecutor, mockJdbcDatabase);

        new JdbcConnector(CONNECTOR_NAME, connectorConfig)
                .execute(applyFile, new CommandContext(mockParentContext));

        verify(mockParentContext, mockScriptExecutor, mockJdbcDatabase);
        assertEquals(capturedScript.getValue(), rawContent);
    }

    // -------------------------------------------------------------------------
    // JdbcDatabase injection into the child context
    // -------------------------------------------------------------------------

    @Test
    public void testJdbcDatabaseIsAvailableInContextReceivedByExecutor() throws Exception {
        Configuration connectorConfig = Configuration.builder().build();

        tempFile = Files.createTempFile("test-", ".sql");
        Files.writeString(tempFile, "SELECT 1;");

        ApplyFile applyFile = makeApplyFile(tempFile, "sql");

        Capture<Context> capturedContext = newCapture(CaptureType.FIRST);
        expectStandardMocks("sql", connectorConfig);
        mockScriptExecutor.execute(anyString(), capture(capturedContext));
        replay(mockParentContext, mockScriptExecutor, mockJdbcDatabase);

        new JdbcConnector(CONNECTOR_NAME, connectorConfig)
                .execute(applyFile, new CommandContext(mockParentContext));

        verify(mockParentContext, mockScriptExecutor, mockJdbcDatabase);

        // The executor receives a child context that exposes the JdbcDatabase directly.
        assertSame(capturedContext.getValue().get(JdbcDatabase.class), mockJdbcDatabase);
    }

    @Test
    public void testExtensionDeterminesScriptExecutorQualifier() throws Exception {
        // Verify that the connector uses the file extension as the qualifier when looking
        // up the ScriptExecutor.
        Configuration connectorConfig = Configuration.builder().build();

        tempFile = Files.createTempFile("test-", ".groovy");
        Files.writeString(tempFile, "// groovy script");

        ApplyFile applyFile = makeApplyFile(tempFile, "groovy");

        expectStandardMocks("groovy", connectorConfig);
        mockScriptExecutor.execute(anyString(), isA(Context.class));
        replay(mockParentContext, mockScriptExecutor, mockJdbcDatabase);

        new JdbcConnector(CONNECTOR_NAME, connectorConfig)
                .execute(applyFile, new CommandContext(mockParentContext));

        // verify() confirms the correct "groovy" qualifier was used when getting the executor.
        verify(mockParentContext, mockScriptExecutor, mockJdbcDatabase);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Sets up the two mandatory mock expectations every test shares:
     *
     * <ol>
     *   <li>JdbcDatabase lookup from the parent context.
     *   <li>ScriptExecutor lookup with the given extension qualifier.
     * </ol>
     */
    private void expectStandardMocks(String extension, Configuration connectorConfig) {
        expect(mockParentContext.get(JdbcDatabase.class, null, CONNECTOR_NAME, connectorConfig))
                .andReturn(mockJdbcDatabase);
        expect(mockParentContext.get(eq(ScriptExecutor.class), eq(extension), eq(connectorConfig)))
                .andReturn(mockScriptExecutor);
    }

    private ApplyFile makeApplyFile(Path file, String extension) {
        ChangeFile.Id id =
                new ChangeFile.Id(
                        "",
                        "0001",
                        "test_change",
                        null,
                        ChangeType.APPLY,
                        CONNECTOR_NAME,
                        extension);
        return new ApplyFile(file.getParent(), file, id);
    }
}
