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
import io.github.totalschema.config.MapConfiguration;
import io.github.totalschema.config.environment.Environment;
import io.github.totalschema.engine.api.Context;
import io.github.totalschema.engine.core.command.api.CommandContext;
import io.github.totalschema.engine.internal.expression.evaluator.DefaultExpressionEvaluatorFactory;
import io.github.totalschema.engine.internal.secrets.DefaultSecretManagerFactory;
import io.github.totalschema.engine.internal.variables.DefaultVariableServiceFactory;
import io.github.totalschema.model.ApplyFile;
import io.github.totalschema.model.ChangeFile;
import io.github.totalschema.model.ChangeType;
import io.github.totalschema.spi.expression.evaluator.ExpressionEvaluator;
import io.github.totalschema.spi.script.ScriptExecutor;
import io.github.totalschema.spi.variables.VariableService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit tests for {@link JdbcConnector}, focusing on the variable-substitution opt-in behaviour.
 *
 * <p>The {@link ScriptExecutor} lookup is satisfied by injecting a mock {@link Context} as the
 * parent of the {@link CommandContext}. Real {@link ExpressionEvaluator} and {@link Configuration}
 * instances are placed directly into the {@code CommandContext} so that substitution logic can be
 * exercised end-to-end without touching any database.
 */
public class JdbcConnectorTest {

    private static final String CONNECTOR_NAME = "mydb";

    private ExpressionEvaluator expressionEvaluator;
    private ScriptExecutor mockScriptExecutor;
    private Context mockParentContext;
    private Path tempFile;

    @BeforeMethod
    public void setUp() {
        expressionEvaluator =
                new DefaultExpressionEvaluatorFactory()
                        .getExpressionEvaluator(
                                new DefaultSecretManagerFactory().getSecretsManager(null, null));

        mockScriptExecutor = createMock(ScriptExecutor.class);
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
    // Variable substitution — enabled for the matching extension
    // -------------------------------------------------------------------------

    @Test
    public void testVariableSubstitutionAppliedWhenExtensionIsListed() throws Exception {
        Configuration connectorConfig =
                Configuration.builder().set("variableSubstitution.extensions", "sql").build();
        Configuration fullConfig = new MapConfiguration(Map.of("variables.mySchema", "public"));

        tempFile = Files.createTempFile("test-", ".sql");
        Files.writeString(tempFile, "CREATE SCHEMA ${mySchema};");

        ApplyFile applyFile = makeApplyFile(tempFile, "sql");

        Capture<String> capturedScript = newCapture(CaptureType.FIRST);
        mockScriptExecutor.execute(capture(capturedScript), anyObject());
        expect(mockParentContext.has(Environment.class)).andReturn(false);
        expect(
                        mockParentContext.get(
                                eq(ScriptExecutor.class),
                                eq("sql"),
                                eq(CONNECTOR_NAME),
                                eq(connectorConfig)))
                .andReturn(mockScriptExecutor);
        replay(mockParentContext, mockScriptExecutor);

        CommandContext context = buildContext(mockParentContext, fullConfig, null);

        new JdbcConnector(CONNECTOR_NAME, connectorConfig).execute(applyFile, context);

        verify(mockParentContext, mockScriptExecutor);
        assertEquals(capturedScript.getValue(), "CREATE SCHEMA public;");
    }

    @Test
    public void testVariableSubstitutionAppliedWithEnvironmentOverride() throws Exception {
        Configuration connectorConfig =
                Configuration.builder().set("variableSubstitution.extensions", "sql").build();
        // Use an environment-only variable (no global counterpart) to avoid the known
        // DefaultVariableService ordering issue where global values currently take
        // precedence over environment-specific ones when both keys are identical.
        Configuration fullConfig =
                new MapConfiguration(Map.of("environments.DEV.variables.devSchema", "dev_schema"));

        tempFile = Files.createTempFile("test-", ".sql");
        Files.writeString(tempFile, "CREATE SCHEMA ${devSchema};");

        ApplyFile applyFile = makeApplyFile(tempFile, "sql");

        Capture<String> capturedScript = newCapture(CaptureType.FIRST);
        mockScriptExecutor.execute(capture(capturedScript), anyObject());
        expect(
                        mockParentContext.get(
                                eq(ScriptExecutor.class),
                                eq("sql"),
                                eq(CONNECTOR_NAME),
                                eq(connectorConfig)))
                .andReturn(mockScriptExecutor);
        replay(mockParentContext, mockScriptExecutor);

        Environment devEnv = new Environment("DEV");
        CommandContext context = buildContext(mockParentContext, fullConfig, devEnv);

        new JdbcConnector(CONNECTOR_NAME, connectorConfig).execute(applyFile, context);

        verify(mockParentContext, mockScriptExecutor);
        assertEquals(capturedScript.getValue(), "CREATE SCHEMA dev_schema;");
    }

    @Test
    public void testMultipleVariablesAreSubstituted() throws Exception {
        Configuration connectorConfig =
                Configuration.builder().set("variableSubstitution.extensions", "sql").build();
        Configuration fullConfig =
                new MapConfiguration(
                        Map.of("variables.schemaName", "app", "variables.userName", "svc_user"));

        tempFile = Files.createTempFile("test-", ".sql");
        Files.writeString(tempFile, "CREATE USER ${userName} IN SCHEMA ${schemaName};");

        ApplyFile applyFile = makeApplyFile(tempFile, "sql");

        Capture<String> capturedScript = newCapture(CaptureType.FIRST);
        mockScriptExecutor.execute(capture(capturedScript), anyObject());
        expect(mockParentContext.has(Environment.class)).andReturn(false);
        expect(
                        mockParentContext.get(
                                eq(ScriptExecutor.class),
                                eq("sql"),
                                eq(CONNECTOR_NAME),
                                eq(connectorConfig)))
                .andReturn(mockScriptExecutor);
        replay(mockParentContext, mockScriptExecutor);

        CommandContext context = buildContext(mockParentContext, fullConfig, null);

        new JdbcConnector(CONNECTOR_NAME, connectorConfig).execute(applyFile, context);

        verify(mockParentContext, mockScriptExecutor);
        assertEquals(capturedScript.getValue(), "CREATE USER svc_user IN SCHEMA app;");
    }

    // -------------------------------------------------------------------------
    // Variable substitution — not applied when extension is not listed
    // -------------------------------------------------------------------------

    @Test
    public void testVariableSubstitutionNotAppliedForUnlistedExtension() throws Exception {
        // groovy is not in the extensions list
        Configuration connectorConfig =
                Configuration.builder().set("variableSubstitution.extensions", "sql").build();
        Configuration fullConfig = new MapConfiguration(Map.of("variables.mySchema", "public"));

        tempFile = Files.createTempFile("test-", ".groovy");
        String originalContent = "sql.execute(\"CREATE SCHEMA ${mySchema}\")";
        Files.writeString(tempFile, originalContent);

        ApplyFile applyFile = makeApplyFile(tempFile, "groovy");

        Capture<String> capturedScript = newCapture(CaptureType.FIRST);
        mockScriptExecutor.execute(capture(capturedScript), anyObject());
        expect(
                        mockParentContext.get(
                                eq(ScriptExecutor.class),
                                eq("groovy"),
                                eq(CONNECTOR_NAME),
                                eq(connectorConfig)))
                .andReturn(mockScriptExecutor);
        replay(mockParentContext, mockScriptExecutor);

        CommandContext context = buildContext(mockParentContext, fullConfig, null);

        new JdbcConnector(CONNECTOR_NAME, connectorConfig).execute(applyFile, context);

        verify(mockParentContext, mockScriptExecutor);
        // Content must be passed through unchanged — no substitution on Groovy
        assertEquals(capturedScript.getValue(), originalContent);
    }

    @Test
    public void testVariableSubstitutionNotAppliedWhenNotConfigured() throws Exception {
        // No variableSubstitution.extensions key at all
        Configuration connectorConfig = Configuration.builder().build();
        Configuration fullConfig = new MapConfiguration(Map.of("variables.mySchema", "public"));

        tempFile = Files.createTempFile("test-", ".sql");
        String originalContent = "CREATE SCHEMA ${mySchema};";
        Files.writeString(tempFile, originalContent);

        ApplyFile applyFile = makeApplyFile(tempFile, "sql");

        Capture<String> capturedScript = newCapture(CaptureType.FIRST);
        mockScriptExecutor.execute(capture(capturedScript), anyObject());
        expect(
                        mockParentContext.get(
                                eq(ScriptExecutor.class),
                                eq("sql"),
                                eq(CONNECTOR_NAME),
                                eq(connectorConfig)))
                .andReturn(mockScriptExecutor);
        replay(mockParentContext, mockScriptExecutor);

        CommandContext context = buildContext(mockParentContext, fullConfig, null);

        new JdbcConnector(CONNECTOR_NAME, connectorConfig).execute(applyFile, context);

        verify(mockParentContext, mockScriptExecutor);
        // Default behaviour: no substitution, original content passes through
        assertEquals(capturedScript.getValue(), originalContent);
    }

    // -------------------------------------------------------------------------
    // Variable substitution — multiple extensions in a comma-separated list
    // -------------------------------------------------------------------------

    @Test
    public void testVariableSubstitutionAppliedForAllListedExtensions() throws Exception {
        Configuration connectorConfig =
                Configuration.builder().set("variableSubstitution.extensions", "sql,xml").build();
        Configuration fullConfig = new MapConfiguration(Map.of("variables.tableName", "orders"));

        // Test .xml extension
        tempFile = Files.createTempFile("test-", ".xml");
        Files.writeString(tempFile, "<table>${tableName}</table>");

        ApplyFile applyFile = makeApplyFile(tempFile, "xml");

        Capture<String> capturedScript = newCapture(CaptureType.FIRST);
        mockScriptExecutor.execute(capture(capturedScript), anyObject());
        expect(mockParentContext.has(Environment.class)).andReturn(false);
        expect(
                        mockParentContext.get(
                                eq(ScriptExecutor.class),
                                eq("xml"),
                                eq(CONNECTOR_NAME),
                                eq(connectorConfig)))
                .andReturn(mockScriptExecutor);
        replay(mockParentContext, mockScriptExecutor);

        CommandContext context = buildContext(mockParentContext, fullConfig, null);

        new JdbcConnector(CONNECTOR_NAME, connectorConfig).execute(applyFile, context);

        verify(mockParentContext, mockScriptExecutor);
        assertEquals(capturedScript.getValue(), "<table>orders</table>");
    }

    @Test
    public void testExtensionNotInCommaListIsNotSubstituted() throws Exception {
        Configuration connectorConfig =
                Configuration.builder().set("variableSubstitution.extensions", "sql,xml").build();
        Configuration fullConfig = new MapConfiguration(Map.of("variables.tableName", "orders"));

        tempFile = Files.createTempFile("test-", ".groovy");
        String originalContent = "// groovy: ${tableName}";
        Files.writeString(tempFile, originalContent);

        ApplyFile applyFile = makeApplyFile(tempFile, "groovy");

        Capture<String> capturedScript = newCapture(CaptureType.FIRST);
        mockScriptExecutor.execute(capture(capturedScript), anyObject());
        expect(
                        mockParentContext.get(
                                eq(ScriptExecutor.class),
                                eq("groovy"),
                                eq(CONNECTOR_NAME),
                                eq(connectorConfig)))
                .andReturn(mockScriptExecutor);
        replay(mockParentContext, mockScriptExecutor);

        CommandContext context = buildContext(mockParentContext, fullConfig, null);

        new JdbcConnector(CONNECTOR_NAME, connectorConfig).execute(applyFile, context);

        verify(mockParentContext, mockScriptExecutor);
        assertEquals(capturedScript.getValue(), originalContent);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Builds a {@link CommandContext} backed by the given mock parent. The real {@link
     * ExpressionEvaluator} and a {@link VariableService} built from {@code fullConfig} are always
     * set. An optional {@link Environment} is injected when provided.
     */
    private CommandContext buildContext(
            Context parent, Configuration fullConfig, Environment environment) {
        CommandContext context = new CommandContext(parent);
        context.setValue(ExpressionEvaluator.class, expressionEvaluator);
        context.setValue(Configuration.class, fullConfig);
        VariableService variableService =
                new DefaultVariableServiceFactory()
                        .getVariableService(fullConfig, expressionEvaluator);
        context.setValue(VariableService.class, variableService);
        if (environment != null) {
            context.setValue(Environment.class, environment);
        }
        return context;
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
