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

package io.github.totalschema.engine.internal.script;

import static org.easymock.EasyMock.*;
import static org.testng.Assert.*;

import io.github.totalschema.config.Configuration;
import io.github.totalschema.config.MapConfiguration;
import io.github.totalschema.config.environment.Environment;
import io.github.totalschema.engine.api.Context;
import io.github.totalschema.engine.core.command.api.ObjectsContext;
import io.github.totalschema.engine.internal.expression.evaluator.DefaultExpressionEvaluatorFactory;
import io.github.totalschema.engine.internal.secrets.DefaultSecretManagerFactory;
import io.github.totalschema.engine.internal.variables.DefaultVariableServiceFactory;
import io.github.totalschema.jdbc.JdbcDatabase;
import io.github.totalschema.spi.expression.evaluator.ExpressionEvaluator;
import io.github.totalschema.spi.variables.VariableService;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.easymock.EasyMock;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit tests for {@link SqlChangeExecutor}.
 *
 * <p>Covers variable substitution (on/off), statement splitting, blank-statement filtering, and
 * error propagation. Each test provides the {@link JdbcDatabase} via the context for SQL statement
 * execution (matching the runtime setup performed by {@link
 * io.github.totalschema.connector.jdbc.JdbcConnector}). The bindings map is used for script-visible
 * variables (empty for SQL executor since SQL doesn't support script variables). The context is
 * also used for dependency injection of engine services like {@link ExpressionEvaluator} and {@link
 * VariableService}.
 */
public class SqlChangeExecutorTest {

    private JdbcDatabase mockDatabase;
    private ExpressionEvaluator expressionEvaluator;
    private Path tempDir;

    @BeforeMethod
    public void setUp() throws IOException {
        mockDatabase = createMock(JdbcDatabase.class);
        expressionEvaluator =
                new DefaultExpressionEvaluatorFactory()
                        .getExpressionEvaluator(
                                new DefaultSecretManagerFactory().getSecretsManager(null, null));
        tempDir = Files.createTempDirectory("sql-change-executor-test");
    }

    @AfterMethod
    public void tearDown() throws IOException {
        // Clean up temp directory
        try (var paths = Files.walk(tempDir)) {
            paths.sorted(java.util.Comparator.reverseOrder())
                    .forEach(
                            path -> {
                                try {
                                    Files.deleteIfExists(path);
                                } catch (IOException e) {
                                    // ignore
                                }
                            });
        }
    }

    // -------------------------------------------------------------------------
    // Variable substitution — enabled
    // -------------------------------------------------------------------------

    @Test
    public void testVariableSubstitutionAppliedWhenEnabled() throws Exception {
        Configuration connectorConfig =
                Configuration.builder()
                        .set("scriptExecutors.sql.variableSubstitution", "true")
                        .build();
        Configuration fullConfig = new MapConfiguration(Map.of("variables.schema", "public"));

        mockDatabase.execute("CREATE SCHEMA public");
        replay(mockDatabase);

        Context context = buildContext(fullConfig, null, mockDatabase);
        String script = "CREATE SCHEMA ${schema}";
        Map<String, Object> bindings = Map.of();
        executeScript(new SqlChangeExecutor(connectorConfig, context), script, context);

        verify(mockDatabase);
    }

    @Test
    public void testVariableSubstitutionAppliedWithEnvironmentOverride() throws Exception {
        Configuration connectorConfig =
                Configuration.builder()
                        .set("scriptExecutors.sql.variableSubstitution", "true")
                        .build();
        Configuration fullConfig =
                new MapConfiguration(Map.of("environments.DEV.variables.schema", "dev_schema"));

        mockDatabase.execute("CREATE SCHEMA dev_schema");
        replay(mockDatabase);

        Context context = buildContext(fullConfig, new Environment("DEV"), mockDatabase);
        String changeFile = "CREATE SCHEMA ${schema}";
        Map<String, Object> bindings = Map.of();
        executeScript(new SqlChangeExecutor(connectorConfig, context), changeFile, context);

        verify(mockDatabase);
    }

    @Test
    public void testMultipleVariablesSubstituted() throws Exception {
        Configuration connectorConfig =
                Configuration.builder()
                        .set("scriptExecutors.sql.variableSubstitution", "true")
                        .build();
        Configuration fullConfig =
                new MapConfiguration(Map.of("variables.user", "svc", "variables.schema", "app"));

        mockDatabase.execute("CREATE USER svc IN SCHEMA app");
        replay(mockDatabase);

        Context context = buildContext(fullConfig, null, mockDatabase);
        String changeFile = "CREATE USER ${user} IN SCHEMA ${schema}";
        Map<String, Object> bindings = Map.of();
        executeScript(new SqlChangeExecutor(connectorConfig, context), changeFile, context);

        verify(mockDatabase);
    }

    // -------------------------------------------------------------------------
    // Variable substitution — disabled
    // -------------------------------------------------------------------------

    @Test
    public void testVariableSubstitutionSkippedWhenNotConfigured() throws Exception {
        Configuration connectorConfig = Configuration.builder().build(); // no extensions key

        mockDatabase.execute("CREATE SCHEMA ${schema}");
        replay(mockDatabase);

        Context context = buildContext(new MapConfiguration(Map.of()), null, mockDatabase);
        String changeFile = "CREATE SCHEMA ${schema}";
        Map<String, Object> bindings = Map.of();
        executeScript(new SqlChangeExecutor(connectorConfig, context), changeFile, context);

        verify(mockDatabase);
    }

    @Test
    public void testVariableSubstitutionSkippedWhenFlagExplicitlyFalse() throws Exception {
        Configuration connectorConfig =
                Configuration.builder()
                        .set("scriptExecutors.sql.variableSubstitution", "false")
                        .build();

        mockDatabase.execute("CREATE SCHEMA ${schema}");
        replay(mockDatabase);

        Context context = buildContext(new MapConfiguration(Map.of()), null, mockDatabase);
        String changeFile = "CREATE SCHEMA ${schema}";
        Map<String, Object> bindings = Map.of();
        executeScript(new SqlChangeExecutor(connectorConfig, context), changeFile, context);

        verify(mockDatabase);
    }

    // -------------------------------------------------------------------------
    // Statement splitting
    // -------------------------------------------------------------------------

    @Test
    public void testMultipleStatementsAreSplitAndExecuted() throws Exception {
        Configuration connectorConfig = Configuration.builder().build();

        List<String> executed = new ArrayList<>();
        mockDatabase.execute(EasyMock.anyString());
        expectLastCall()
                .andAnswer(
                        () -> {
                            executed.add((String) EasyMock.getCurrentArguments()[0]);
                            return null;
                        })
                .times(2);
        replay(mockDatabase);

        Context context = buildContext(new MapConfiguration(Map.of()), null, mockDatabase);
        String changeFile = "SELECT 1; SELECT 2";
        Map<String, Object> bindings = Map.of();
        executeScript(new SqlChangeExecutor(connectorConfig, context), changeFile, context);

        verify(mockDatabase);
        assertEquals(executed, List.of("SELECT 1", "SELECT 2"));
    }

    @Test
    public void testBlankStatementsAreFiltered() throws Exception {
        Configuration connectorConfig = Configuration.builder().build();

        mockDatabase.execute("SELECT 1");
        replay(mockDatabase);

        Context context = buildContext(new MapConfiguration(Map.of()), null, mockDatabase);
        // trailing semicolon produces a blank trailing token — must be filtered
        String changeFile = "SELECT 1;";
        Map<String, Object> bindings = Map.of();
        executeScript(new SqlChangeExecutor(connectorConfig, context), changeFile, context);

        verify(mockDatabase);
    }

    @Test
    public void testCustomStatementSeparator() throws Exception {
        Configuration connectorConfig =
                Configuration.builder().set("statementSeparator", ";;").build();

        List<String> executed = new ArrayList<>();
        mockDatabase.execute(EasyMock.anyString());
        expectLastCall()
                .andAnswer(
                        () -> {
                            executed.add((String) EasyMock.getCurrentArguments()[0]);
                            return null;
                        })
                .times(2);
        replay(mockDatabase);

        Context context = buildContext(new MapConfiguration(Map.of()), null, mockDatabase);
        String changeFile = "SELECT 1;; SELECT 2";
        Map<String, Object> bindings = Map.of();
        executeScript(new SqlChangeExecutor(connectorConfig, context), changeFile, context);

        verify(mockDatabase);
        assertEquals(executed, List.of("SELECT 1", "SELECT 2"));
    }

    @Test
    public void testNoSeparatorExecutesScriptAsOneStatement() throws Exception {
        Configuration connectorConfig =
                Configuration.builder().set("disableStatementSeparator", "true").build();

        String script = "SELECT 1; SELECT 2";
        mockDatabase.execute(script);
        replay(mockDatabase);

        Context context = buildContext(new MapConfiguration(Map.of()), null, mockDatabase);

        Map<String, Object> bindings = Map.of();
        executeScript(new SqlChangeExecutor(connectorConfig, context), script, context);

        verify(mockDatabase);
    }

    // -------------------------------------------------------------------------
    // Error propagation
    // -------------------------------------------------------------------------

    @Test(
            expectedExceptions = RuntimeException.class,
            expectedExceptionsMessageRegExp = "Statement failed:.*")
    public void testSqlExceptionIsWrappedWithStatementText() throws Exception {
        Configuration connectorConfig = Configuration.builder().build();

        mockDatabase.execute("BAD SQL");
        expectLastCall().andThrow(new SQLException("syntax error"));
        replay(mockDatabase);

        Context context = buildContext(new MapConfiguration(Map.of()), null, mockDatabase);
        String changeFile = "BAD SQL";
        Map<String, Object> bindings = Map.of();
        executeScript(new SqlChangeExecutor(connectorConfig, context), changeFile, context);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Helper method to execute a SQL script string through SqlChangeExecutor.
     *
     * <p>Converts the script string to an InputStream and calls the protected execute method.
     */
    private void executeScript(SqlChangeExecutor executor, String script, Context context)
            throws InterruptedException {
        InputStream inputStream = new ByteArrayInputStream(script.getBytes(StandardCharsets.UTF_8));
        executor.execute(inputStream, context);
    }

    private ObjectsContext buildContext(
            Configuration fullConfig, Environment environment, JdbcDatabase jdbcDatabase) {
        VariableService variableService =
                new DefaultVariableServiceFactory()
                        .getVariableService(fullConfig, expressionEvaluator);

        ObjectsContext context = new ObjectsContext();
        context.setValue(ExpressionEvaluator.class, expressionEvaluator);
        context.setValue(VariableService.class, variableService);
        context.setValue(Configuration.class, fullConfig);
        if (environment != null) {
            context.setValue(Environment.class, environment);
        }
        if (jdbcDatabase != null) {
            context.setValue(JdbcDatabase.class, jdbcDatabase);
        }
        return context;
    }
}
