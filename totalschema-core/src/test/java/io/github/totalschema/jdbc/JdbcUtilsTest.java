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

package io.github.totalschema.jdbc;

import static org.testng.Assert.*;

import org.testng.annotations.Test;

public class JdbcUtilsTest {

    @Test
    public void testGetTableNameExpressionWithAllComponents() {
        String result =
                JdbcUtils.getTableNameExpression("my_catalog", "my_schema", "my_table", "\"");

        assertEquals(result, "\"my_catalog.my_schema.my_table\"");
    }

    @Test
    public void testGetTableNameExpressionWithoutCatalog() {
        String result = JdbcUtils.getTableNameExpression(null, "my_schema", "my_table", "\"");

        assertEquals(result, "\"my_schema.my_table\"");
    }

    @Test
    public void testGetTableNameExpressionWithoutSchema() {
        String result = JdbcUtils.getTableNameExpression("my_catalog", null, "my_table", "\"");

        assertEquals(result, "\"my_catalog.my_table\"");
    }

    @Test
    public void testGetTableNameExpressionWithTableOnly() {
        String result = JdbcUtils.getTableNameExpression(null, null, "my_table", "\"");

        assertEquals(result, "\"my_table\"");
    }

    @Test
    public void testGetTableNameExpressionWithoutQuotes() {
        String result =
                JdbcUtils.getTableNameExpression("my_catalog", "my_schema", "my_table", null);

        assertEquals(result, "my_catalog.my_schema.my_table");
    }

    @Test
    public void testGetTableNameExpressionWithBlankCatalog() {
        String result = JdbcUtils.getTableNameExpression("  ", "my_schema", "my_table", "\"");

        assertEquals(result, "\"my_schema.my_table\"");
    }

    @Test
    public void testGetTableNameExpressionWithBlankSchema() {
        String result = JdbcUtils.getTableNameExpression("my_catalog", "  ", "my_table", "\"");

        assertEquals(result, "\"my_catalog.my_table\"");
    }

    @Test
    public void testGetTableNameExpressionWithEmptyCatalog() {
        String result = JdbcUtils.getTableNameExpression("", "my_schema", "my_table", "`");

        assertEquals(result, "`my_schema.my_table`");
    }

    @Test
    public void testGetTableNameExpressionWithDifferentQuoteStyle() {
        String result = JdbcUtils.getTableNameExpression("catalog", "schema", "table", "`");

        assertEquals(result, "`catalog.schema.table`");
    }

    @Test
    public void testGetTableNameExpressionWithSquareBrackets() {
        String result = JdbcUtils.getTableNameExpression("catalog", "schema", "table", "[");

        assertEquals(result, "[catalog.schema.table[");
    }
}
