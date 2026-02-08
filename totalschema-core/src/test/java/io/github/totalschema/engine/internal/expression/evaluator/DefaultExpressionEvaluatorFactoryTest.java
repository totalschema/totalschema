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

import static org.easymock.EasyMock.*;
import static org.testng.Assert.*;

import io.github.totalschema.spi.expression.evaluator.ExpressionEvaluator;
import io.github.totalschema.spi.secrets.SecretsManager;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class DefaultExpressionEvaluatorFactoryTest {

    private DefaultExpressionEvaluatorFactory factory;
    private SecretsManager secretsManager;

    @BeforeMethod
    public void setUp() {
        factory = new DefaultExpressionEvaluatorFactory();
        secretsManager = createMock(SecretsManager.class);
    }

    @Test
    public void testGetExpressionEvaluator() {
        ExpressionEvaluator evaluator = factory.getExpressionEvaluator(secretsManager);

        assertNotNull(evaluator);
    }

    @Test
    public void testGetExpressionEvaluatorReturnsDefaultExpressionEvaluator() {
        ExpressionEvaluator evaluator = factory.getExpressionEvaluator(secretsManager);

        assertTrue(evaluator instanceof DefaultExpressionEvaluator);
    }

    @Test
    public void testGetExpressionEvaluatorCanEvaluateSimpleExpression() {
        ExpressionEvaluator evaluator = factory.getExpressionEvaluator(secretsManager);

        String result = evaluator.evaluate("Hello ${name}", java.util.Map.of("name", "World"));

        assertEquals(result, "Hello World");
    }
}
