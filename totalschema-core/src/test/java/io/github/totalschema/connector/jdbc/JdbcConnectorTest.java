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
import io.github.totalschema.config.environment.Environment;
import io.github.totalschema.engine.api.Context;
import io.github.totalschema.engine.core.command.api.ObjectsContext;
import io.github.totalschema.engine.core.container.ObjectSpecification;
import io.github.totalschema.jdbc.JdbcDatabase;
import io.github.totalschema.model.ApplyFile;
import io.github.totalschema.model.ChangeFile;
import io.github.totalschema.model.ChangeType;
import io.github.totalschema.spi.change.executor.ChangeExecutor;
import io.github.totalschema.spi.change.executor.ObjectBindingChangeExecutor;
import io.github.totalschema.spi.change.executor.SimpleChangeExecutor;
import io.github.totalschema.spi.database.sql.SQLDatabase;
import io.github.totalschema.spi.storage.StorageService;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Map;
import javax.sql.DataSource;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit tests for {@link JdbcConnector}.
 *
 * <p>The connector is responsible for:
 *
 * <ol>
 *   <li>Acquiring the {@link JdbcDatabase} from the IoC container.
 *   <li>Retrieving the appropriate {@link ChangeExecutor} via the context using file extension as
 *       qualifier.
 *   <li>Obtaining a DataSource via {@link JdbcDatabase#getDataSource()}.
 *   <li>Building a script bindings map containing jdbcDatabase, dataSource, and configuration.
 *   <li>Invoking the appropriate execute method based on executor type (ObjectBindingChangeExecutor
 *       vs SimpleChangeExecutor).
 * </ol>
 */
public class JdbcConnectorTest {

    private static final String CONNECTOR_NAME = "mydb";

    private SimpleChangeExecutor testSqlExecutor;
    private ObjectBindingChangeExecutor testScriptExecutor;
    private JdbcDatabase mockJdbcDatabase;
    private Context mockParentContext;
    private DataSource mockDataSource;
    private StorageService mockStorageService;
    private Path tempFile;

    private Capture<Context> capturedSqlContext;
    private Capture<Context> capturedScriptContext;
    private Capture<Map<String, Object>> capturedScriptBindings;

    @BeforeMethod
    public void setUp() throws Exception {
        mockJdbcDatabase = createMock(JdbcDatabase.class);
        mockParentContext = createMock(Context.class);
        Context mockExecutorContext = createMock(Context.class);
        mockDataSource = createMock(DataSource.class);
        mockStorageService = createMock(StorageService.class);

        // Set up executor context to provide StorageService for openChangeFileStream
        expect(mockExecutorContext.get(eq(StorageService.class)))
                .andAnswer(() -> mockStorageService)
                .anyTimes();

        // Make StorageService return file input streams
        expect(mockStorageService.openInputStream(anyObject(ChangeFile.class)))
                .andAnswer(
                        () -> {
                            ChangeFile cf = (ChangeFile) getCurrentArguments()[0];
                            return Files.newInputStream(cf.getFile());
                        })
                .anyTimes();

        replay(mockExecutorContext, mockStorageService);

        // Create real test executors with proper context
        capturedSqlContext = newCapture(CaptureType.ALL);
        capturedScriptContext = newCapture(CaptureType.ALL);
        capturedScriptBindings = newCapture(CaptureType.ALL);

        testSqlExecutor =
                new SimpleChangeExecutor(mockExecutorContext) {
                    @Override
                    public void execute(InputStream inputStream, Context context)
                            throws InterruptedException {
                        capturedSqlContext.setValue(context);
                        // Consume the stream to simulate execution
                        try {
                            inputStream.readAllBytes();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                };

        testScriptExecutor =
                new ObjectBindingChangeExecutor(mockExecutorContext) {
                    @Override
                    protected void execute(
                            InputStream inputStream, Context context, Map<String, Object> bindings)
                            throws InterruptedException {
                        capturedScriptContext.setValue(context);
                        capturedScriptBindings.setValue(bindings);
                        // Consume the stream to simulate execution
                        try {
                            inputStream.readAllBytes();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                };
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
    // ChangeFile and bindings forwarding
    // -------------------------------------------------------------------------

    @Test
    public void testChangeFileIsForwardedToExecutor() throws Exception {
        Configuration connectorConfig = Configuration.builder().build();

        tempFile = Files.createTempFile("test-", ".sql");
        String rawContent = "CREATE TABLE foo (id INT);";
        Files.writeString(tempFile, rawContent);

        ApplyFile applyFile = makeApplyFile(tempFile, "sql");

        expectSqlExecutorMocks("sql", connectorConfig);
        replay(mockParentContext, mockJdbcDatabase, mockDataSource);

        new JdbcConnector(CONNECTOR_NAME, connectorConfig)
                .execute(applyFile, new ObjectsContext(mockParentContext));

        verify(mockParentContext, mockJdbcDatabase, mockDataSource);

        // Verify JdbcDatabase is available in the execution context
        Context executionContext = capturedSqlContext.getValue();
        assertNotNull(executionContext);
        assertSame(executionContext.get(JdbcDatabase.class), mockJdbcDatabase);
        assertSame(executionContext.get(SQLDatabase.class), mockJdbcDatabase);
    }

    @Test
    public void testJdbcDatabaseAndDataSourceArePassedInBindings() throws Exception {
        Configuration connectorConfig = Configuration.builder().build();

        tempFile = Files.createTempFile("test-", ".groovy");
        Files.writeString(tempFile, "sql.execute('SELECT 1')");

        ApplyFile applyFile = makeApplyFile(tempFile, "groovy");

        expectScriptExecutorMocks("groovy", connectorConfig);
        replay(mockParentContext, mockJdbcDatabase, mockDataSource);

        new JdbcConnector(CONNECTOR_NAME, connectorConfig)
                .execute(applyFile, new ObjectsContext(mockParentContext));

        verify(mockParentContext, mockJdbcDatabase, mockDataSource);

        // Verify the jdbcDatabase, dataSource, and configuration are available in bindings
        Map<String, Object> bindings = capturedScriptBindings.getValue();
        assertSame(bindings.get("jdbcDatabase"), mockJdbcDatabase);
        assertSame(bindings.get("dataSource"), mockDataSource);
        assertSame(bindings.get("configuration"), connectorConfig);
    }

    @Test
    public void testGroovyExecutorIsSelectedByExtension() throws Exception {
        Configuration connectorConfig = Configuration.builder().build();

        tempFile = Files.createTempFile("test-", ".groovy");
        String rawContent = "sql.execute(\"SELECT 1\")";
        Files.writeString(tempFile, rawContent);

        ApplyFile applyFile = makeApplyFile(tempFile, "groovy");

        expectScriptExecutorMocks("groovy", connectorConfig);
        replay(mockParentContext, mockJdbcDatabase, mockDataSource);

        new JdbcConnector(CONNECTOR_NAME, connectorConfig)
                .execute(applyFile, new ObjectsContext(mockParentContext));

        verify(mockParentContext, mockJdbcDatabase, mockDataSource);
    }

    // -------------------------------------------------------------------------
    // Connection lifecycle
    // -------------------------------------------------------------------------

    @Test
    public void testDataSourceIsObtainedViaGetDataSource() throws Exception {
        Configuration connectorConfig = Configuration.builder().build();

        tempFile = Files.createTempFile("test-", ".sql");
        Files.writeString(tempFile, "SELECT 1;");

        ApplyFile applyFile = makeApplyFile(tempFile, "sql");

        expectSqlExecutorMocks("sql", connectorConfig);
        replay(mockParentContext, mockJdbcDatabase, mockDataSource);

        new JdbcConnector(CONNECTOR_NAME, connectorConfig)
                .execute(applyFile, new ObjectsContext(mockParentContext));

        verify(mockParentContext, mockJdbcDatabase, mockDataSource);
    }

    @Test
    public void testExtensionDeterminesChangeExecutorQualifier() throws Exception {
        // Verify that the connector uses the file extension as the qualifier when looking
        // up the ChangeExecutor.
        Configuration connectorConfig = Configuration.builder().build();

        tempFile = Files.createTempFile("test-", ".groovy");
        Files.writeString(tempFile, "// groovy script");

        ApplyFile applyFile = makeApplyFile(tempFile, "groovy");

        expectScriptExecutorMocks("groovy", connectorConfig);
        replay(mockParentContext, mockJdbcDatabase, mockDataSource);

        new JdbcConnector(CONNECTOR_NAME, connectorConfig)
                .execute(applyFile, new ObjectsContext(mockParentContext));

        // verify() confirms the correct "groovy" qualifier was used when getting the executor.
        verify(mockParentContext, mockJdbcDatabase, mockDataSource);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Sets up mock expectations for SQL executors (SimpleChangeExecutor):
     *
     * <ol>
     *   <li>SQLDatabase lookup from the parent context with "jdbc" qualifier.
     *   <li>ChangeExecutor lookup with the given extension qualifier (returns testSqlExecutor).
     *   <li>JdbcDatabase.getDataSource() invocation that returns mockDataSource.
     * </ol>
     */
    private void expectSqlExecutorMocks(String extension, Configuration connectorConfig)
            throws SQLException, InterruptedException {
        // Expect SQLDatabase lookup with ObjectSpecification and "jdbc" qualifier
        ObjectSpecification sqlDatabaseSpec =
                ObjectSpecification.from(
                        SQLDatabase.class,
                        JdbcConnector.CONNECTOR_TYPE,
                        new Object[] {CONNECTOR_NAME, connectorConfig});
        expect(mockParentContext.get(eq(sqlDatabaseSpec))).andReturn(mockJdbcDatabase);

        // Expect ChangeExecutor lookup with extension as qualifier
        ObjectSpecification changeExecutorSpec =
                ObjectSpecification.from(
                        ChangeExecutor.class, extension, new Object[] {connectorConfig});
        expect(mockParentContext.get(eq(changeExecutorSpec))).andReturn(testSqlExecutor);

        // Expect getDataSource() to return mockDataSource
        expect(mockJdbcDatabase.getDataSource()).andReturn(mockDataSource);
    }

    /**
     * Sets up mock expectations for script executors (ObjectBindingChangeExecutor):
     *
     * <ol>
     *   <li>SQLDatabase lookup from the parent context with "jdbc" qualifier (called twice: once in
     *       getExecutionContext, once in getBindings).
     *   <li>ChangeExecutor lookup with the given extension qualifier (returns testScriptExecutor).
     *   <li>Environment.class presence check (for script bindings).
     *   <li>JdbcDatabase.getDataSource() invocation that returns mockDataSource (called twice).
     * </ol>
     */
    private void expectScriptExecutorMocks(String extension, Configuration connectorConfig)
            throws SQLException, InterruptedException {
        // Expect SQLDatabase lookup with ObjectSpecification and "jdbc" qualifier (called twice)
        ObjectSpecification sqlDatabaseSpec =
                ObjectSpecification.from(
                        SQLDatabase.class,
                        JdbcConnector.CONNECTOR_TYPE,
                        new Object[] {CONNECTOR_NAME, connectorConfig});
        expect(mockParentContext.get(eq(sqlDatabaseSpec))).andReturn(mockJdbcDatabase).times(2);

        // Expect ChangeExecutor lookup with extension as qualifier
        ObjectSpecification changeExecutorSpec =
                ObjectSpecification.from(
                        ChangeExecutor.class, extension, new Object[] {connectorConfig});
        expect(mockParentContext.get(eq(changeExecutorSpec))).andReturn(testScriptExecutor);

        // Expect Environment.class check in getScriptBindings
        expect(mockParentContext.has(eq(Environment.class))).andReturn(false);

        // Expect getDataSource() to return mockDataSource (called twice)
        expect(mockJdbcDatabase.getDataSource()).andReturn(mockDataSource).times(2);
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
