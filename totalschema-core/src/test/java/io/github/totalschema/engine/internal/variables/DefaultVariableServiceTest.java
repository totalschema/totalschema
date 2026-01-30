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
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
        expect(emptyConfig.getKeys()).andReturn(Set.of());

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

        expect(mockConfiguration.getPrefixNamespace("variables")).andReturn(variablesConfig);
        expect(variablesConfig.getKeys()).andReturn(Set.of("var1", "var2"));
        expect(variablesConfig.getString("var1")).andReturn(Optional.of("expression1"));
        expect(variablesConfig.getString("var2")).andReturn(Optional.of("expression2"));
        expect(mockEvaluator.evaluate("expression1", variablesConfig)).andReturn("value1");
        expect(mockEvaluator.evaluate("expression2", variablesConfig)).andReturn("value2");

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

        expect(mockConfiguration.getPrefixNamespace("variables")).andReturn(variablesConfig);
        expect(variablesConfig.getKeys()).andReturn(Set.of("var1"));
        expect(variablesConfig.getString("var1")).andReturn(Optional.empty());

        replay(mockConfiguration, variablesConfig);

        service = new DefaultVariableService(mockConfiguration, mockEvaluator);
        service.getVariables();
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testGetVariablesWithEvaluationError() {
        Configuration variablesConfig = createMock(Configuration.class);

        expect(mockConfiguration.getPrefixNamespace("variables")).andReturn(variablesConfig);
        expect(variablesConfig.getKeys()).andReturn(Set.of("var1"));
        expect(variablesConfig.getString("var1")).andReturn(Optional.of("bad-expression"));
        expect(mockEvaluator.evaluate("bad-expression", variablesConfig))
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

        expect(mockConfiguration.getPrefixNamespace("variables")).andReturn(globalConfig);
        expect(mockConfiguration.getPrefixNamespace("environments", "DEV", "variables"))
                .andReturn(envConfig);
        expect(envConfig.addAll(globalConfig)).andReturn(mergedConfig);
        expect(mergedConfig.withEntry("environment", "DEV")).andReturn(finalConfig);
        expect(finalConfig.getKeys()).andReturn(Set.of("var1"));
        expect(finalConfig.getString("var1")).andReturn(Optional.of("env-expression"));
        expect(mockEvaluator.evaluate("env-expression", finalConfig)).andReturn("env-value");

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
    public void testDefaultVariableServiceFactory() {
        DefaultVariableServiceFactory factory = new DefaultVariableServiceFactory();

        expect(mockConfiguration.getPrefixNamespace("variables"))
                .andReturn(mockConfiguration)
                .anyTimes();
        expect(mockConfiguration.getKeys()).andReturn(Set.of()).anyTimes();

        replay(mockConfiguration);

        DefaultVariableService result =
                (DefaultVariableService)
                        factory.getVariableService(mockConfiguration, mockEvaluator);

        assertNotNull(result);

        verify(mockConfiguration);
    }
}
