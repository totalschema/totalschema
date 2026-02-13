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

package io.github.totalschema.engine.services.sql;

import static org.testng.Assert.*;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class DefaultSqlDialectTest {

    private DefaultSqlDialect dialect;

    @BeforeMethod
    public void setUp() {
        dialect = new DefaultSqlDialect();
    }

    @Test
    public void testVariableCharacterColumnExpressionWithLength50() {
        String result = dialect.variableCharacterColumnExpression(50);
        assertEquals(result, "VARCHAR(50)");
    }

    @Test
    public void testVariableCharacterColumnExpressionWithLength255() {
        String result = dialect.variableCharacterColumnExpression(255);
        assertEquals(result, "VARCHAR(255)");
    }

    @Test
    public void testVariableCharacterColumnExpressionWithLength1() {
        String result = dialect.variableCharacterColumnExpression(1);
        assertEquals(result, "VARCHAR(1)");
    }

    @Test
    public void testVariableCharacterColumnExpressionWithLargeLength() {
        String result = dialect.variableCharacterColumnExpression(4000);
        assertEquals(result, "VARCHAR(4000)");
    }

    @Test
    public void testTimestampColumnExpression() {
        String result = dialect.timestampColumnExpression();
        assertEquals(result, "TIMESTAMP");
    }

    @Test
    public void testDefaultSqlDialectFactory() {
        DefaultSqlDialectFactory factory = new DefaultSqlDialectFactory();
        DefaultSqlDialect createdDialect = (DefaultSqlDialect) factory.getSqlDialect();

        assertNotNull(createdDialect);
        assertEquals(createdDialect.timestampColumnExpression(), "TIMESTAMP");
        assertEquals(createdDialect.variableCharacterColumnExpression(100), "VARCHAR(100)");
    }
}
