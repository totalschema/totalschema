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

package io.github.totalschema.engine.internal.variables;

import static org.easymock.EasyMock.*;
import static org.testng.Assert.*;

import io.github.totalschema.config.Configuration;
import io.github.totalschema.config.environment.Environment;
import io.github.totalschema.spi.expression.evaluator.ExpressionEvaluator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class DefaultVariableServiceTest {

    private Configuration mockConfiguration;
    private ExpressionEvaluator mockEvaluator;
    private DefaultVariableService service;

    @BeforeMethod
    public void setUp() {
        mockConfiguration = createMock(Configuration.class);
        mockEvaluator = createMock(ExpressionEvaluator.class);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testConstructorWithNullConfiguration() {
        new DefaultVariableService(null, mockEvaluator);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testConstructorWithNullEvaluator() {
        new DefaultVariableService(mockConfiguration, null);
    }

    @Test
    public void testGetVariablesEmpty() {
        Configuration emptyConfig = createMock(Configuration.class);

        expect(mockConfiguration.getPrefixNamespace("variables")).andReturn(emptyConfig);
        expect(emptyConfig.asMap()).andReturn(Optional.empty());

        replay(mockConfiguration, emptyConfig);

        service = new DefaultVariableService(mockConfiguration, mockEvaluator);
        Map<String, String> result = service.getVariables();

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(mockConfiguration, emptyConfig);
    }

    @Test
    public void testGetVariablesWithValues() {
        Configuration variablesConfig = createMock(Configuration.class);

        Map<String, String> varMap = new HashMap<>();
        varMap.put("var1", "expression1");
        varMap.put("var2", "expression2");

        expect(mockConfiguration.getPrefixNamespace("variables")).andReturn(variablesConfig);
        expect(variablesConfig.asMap()).andReturn(Optional.of(varMap));
        expect(mockEvaluator.evaluate("expression1", varMap)).andReturn("value1");
        expect(mockEvaluator.evaluate("expression2", varMap)).andReturn("value2");

        replay(mockConfiguration, variablesConfig, mockEvaluator);

        service = new DefaultVariableService(mockConfiguration, mockEvaluator);
        Map<String, String> result = service.getVariables();

        assertNotNull(result);
        assertEquals(result.size(), 2);
        assertEquals(result.get("var1"), "value1");
        assertEquals(result.get("var2"), "value2");

        verify(mockConfiguration, variablesConfig, mockEvaluator);
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testGetVariablesWithMissingExpression() {
        Configuration variablesConfig = createMock(Configuration.class);

        // A map with an explicit null value triggers the null-check in evaluateVariable()
        Map<String, String> varMapWithNull = new HashMap<>();
        varMapWithNull.put("var1", null);

        expect(mockConfiguration.getPrefixNamespace("variables")).andReturn(variablesConfig);
        expect(variablesConfig.asMap()).andReturn(Optional.of(varMapWithNull));

        replay(mockConfiguration, variablesConfig);

        service = new DefaultVariableService(mockConfiguration, mockEvaluator);
        service.getVariables();
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testGetVariablesWithEvaluationError() {
        Configuration variablesConfig = createMock(Configuration.class);

        Map<String, String> varMap = Map.of("var1", "bad-expression");

        expect(mockConfiguration.getPrefixNamespace("variables")).andReturn(variablesConfig);
        expect(variablesConfig.asMap()).andReturn(Optional.of(varMap));
        expect(mockEvaluator.evaluate("bad-expression", varMap))
                .andThrow(new RuntimeException("Evaluation failed"));

        replay(mockConfiguration, variablesConfig, mockEvaluator);

        service = new DefaultVariableService(mockConfiguration, mockEvaluator);
        service.getVariables();
    }

    @Test
    public void testGetVariablesInEnvironment() {
        Environment environment = new Environment("DEV");
        Configuration globalConfig = createMock(Configuration.class);
        Configuration envConfig = createMock(Configuration.class);
        Configuration mergedConfig = createMock(Configuration.class);
        Configuration finalConfig = createMock(Configuration.class);

        Map<String, String> finalVarMap = Map.of("var1", "env-expression");

        expect(mockConfiguration.getPrefixNamespace("variables")).andReturn(globalConfig);
        expect(mockConfiguration.getPrefixNamespace("environments", "DEV", "variables"))
                .andReturn(envConfig);
        expect(envConfig.addAll(globalConfig)).andReturn(mergedConfig);
        expect(mergedConfig.withEntry("environment", "DEV")).andReturn(finalConfig);
        expect(finalConfig.asMap()).andReturn(Optional.of(finalVarMap));
        expect(mockEvaluator.evaluate("env-expression", finalVarMap)).andReturn("env-value");

        replay(
                mockConfiguration,
                globalConfig,
                envConfig,
                mergedConfig,
                finalConfig,
                mockEvaluator);

        service = new DefaultVariableService(mockConfiguration, mockEvaluator);
        Map<String, String> result = service.getVariablesInEnvironment(environment);

        assertNotNull(result);
        assertEquals(result.size(), 1);
        assertEquals(result.get("var1"), "env-value");

        verify(
                mockConfiguration,
                globalConfig,
                envConfig,
                mergedConfig,
                finalConfig,
                mockEvaluator);
    }

    @Test
    public void testGetVariablesCachesResult() {
        Configuration variablesConfig = createMock(Configuration.class);

        Map<String, String> varMap = Map.of("var1", "expression1");

        // Expectations are set up for exactly one invocation each.
        // If the cache is bypassed on the second call, EasyMock will throw
        // "Unexpected method call" and the test will fail.
        expect(mockConfiguration.getPrefixNamespace("variables")).andReturn(variablesConfig);
        expect(variablesConfig.asMap()).andReturn(Optional.of(varMap));
        expect(mockEvaluator.evaluate("expression1", varMap)).andReturn("value1");

        replay(mockConfiguration, variablesConfig, mockEvaluator);

        service = new DefaultVariableService(mockConfiguration, mockEvaluator);
        Map<String, String> first = service.getVariables();
        Map<String, String> second = service.getVariables();

        assertSame(first, second, "Expected the same cached instance on repeated calls");
        verify(mockConfiguration, variablesConfig, mockEvaluator);
    }

    @Test
    public void testGetVariablesInEnvironmentCachesResult() {
        Environment environment = new Environment("DEV");
        Configuration globalConfig = createMock(Configuration.class);
        Configuration envConfig = createMock(Configuration.class);
        Configuration mergedConfig = createMock(Configuration.class);
        Configuration finalConfig = createMock(Configuration.class);

        Map<String, String> finalVarMap = Map.of("var1", "env-expression");

        // Expectations are set up for exactly one invocation each.
        // If the cache is bypassed on the second call, EasyMock will throw
        // "Unexpected method call" and the test will fail.
        expect(mockConfiguration.getPrefixNamespace("variables")).andReturn(globalConfig);
        expect(mockConfiguration.getPrefixNamespace("environments", "DEV", "variables"))
                .andReturn(envConfig);
        expect(envConfig.addAll(globalConfig)).andReturn(mergedConfig);
        expect(mergedConfig.withEntry("environment", "DEV")).andReturn(finalConfig);
        expect(finalConfig.asMap()).andReturn(Optional.of(finalVarMap));
        expect(mockEvaluator.evaluate("env-expression", finalVarMap)).andReturn("env-value");

        replay(
                mockConfiguration,
                globalConfig,
                envConfig,
                mergedConfig,
                finalConfig,
                mockEvaluator);

        service = new DefaultVariableService(mockConfiguration, mockEvaluator);
        Map<String, String> first = service.getVariablesInEnvironment(environment);
        Map<String, String> second = service.getVariablesInEnvironment(environment);

        assertSame(first, second, "Expected the same cached instance on repeated calls");
        verify(
                mockConfiguration,
                globalConfig,
                envConfig,
                mergedConfig,
                finalConfig,
                mockEvaluator);
    }

    @Test
    public void testDefaultVariableServiceFactory() {
        DefaultVariableServiceFactory factory = new DefaultVariableServiceFactory();

        replay(mockConfiguration);

        DefaultVariableService result =
                (DefaultVariableService)
                        factory.getVariableService(mockConfiguration, mockEvaluator);

        assertNotNull(result);

        verify(mockConfiguration);
    }
}
