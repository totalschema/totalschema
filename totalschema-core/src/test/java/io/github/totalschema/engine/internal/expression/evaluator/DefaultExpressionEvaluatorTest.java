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

package io.github.totalschema.engine.internal.expression.evaluator;

import static org.testng.Assert.*;

import io.github.totalschema.config.Configuration;
import io.github.totalschema.config.MapConfiguration;
import io.github.totalschema.engine.internal.lookup.DelegatingExpressionLookup;
import io.github.totalschema.spi.lookup.ExpressionLookup;
import java.util.List;
import java.util.Map;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class DefaultExpressionEvaluatorTest {

    private DefaultExpressionEvaluator evaluator;

    @BeforeMethod
    public void setUp() {
        ExpressionLookup uppercaseLookup =
                new DelegatingExpressionLookup(
                        "upper", String::toUpperCase, "Converts to uppercase");
        ExpressionLookup lowercaseLookup =
                new DelegatingExpressionLookup(
                        "lower", String::toLowerCase, "Converts to lowercase");

        evaluator = new DefaultExpressionEvaluator(List.of(uppercaseLookup, lowercaseLookup));
    }

    @Test
    public void testEvaluateWithSimpleVariable() {
        String result = evaluator.evaluate("Hello ${name}", Map.of("name", "World"));

        assertEquals(result, "Hello World");
    }

    @Test
    public void testEvaluateWithMultipleVariables() {
        String result =
                evaluator.evaluate(
                        "${greeting} ${name}!", Map.of("greeting", "Hello", "name", "World"));

        assertEquals(result, "Hello World!");
    }

    @Test
    public void testEvaluateWithNoVariables() {
        String result = evaluator.evaluate("Static text", Map.of());

        assertEquals(result, "Static text");
    }

    @Test
    public void testEvaluateWithLookupFunction() {
        String result = evaluator.evaluate("${upper:hello}", Map.of());

        assertEquals(result, "HELLO");
    }

    @Test
    public void testEvaluateWithLowerLookupFunction() {
        String result = evaluator.evaluate("${lower:WORLD}", Map.of());

        assertEquals(result, "world");
    }

    @Test
    public void testEvaluateWithVariableAndLookup() {
        String result = evaluator.evaluate("${upper:${name}}", Map.of("name", "hello"));

        assertEquals(result, "HELLO");
    }

    @Test
    public void testEvaluateWithNestedVariables() {
        String result =
                evaluator.evaluate("${outer}", Map.of("outer", "${inner}", "inner", "value"));

        assertEquals(result, "value");
    }

    @Test
    public void testEvaluateWithConfiguration() {
        Configuration config = new MapConfiguration(Map.of("key1", "value1", "key2", "value2"));

        String result = evaluator.evaluate("${key1} and ${key2}", config);

        assertEquals(result, "value1 and value2");
    }

    @Test
    public void testEvaluateWithEmptyExpression() {
        String result = evaluator.evaluate("", Map.of("key", "value"));

        assertEquals(result, "");
    }

    @Test
    public void testEvaluateWithNullVariableValue() {
        String result = evaluator.evaluate("Value: ${key}", Map.of("key", "null"));

        assertEquals(result, "Value: null");
    }

    @Test
    public void testEvaluateWithComplexExpression() {
        String result =
                evaluator.evaluate(
                        "${upper:${greeting}} ${name}!",
                        Map.of("greeting", "hello", "name", "world"));

        assertEquals(result, "HELLO world!");
    }
}
