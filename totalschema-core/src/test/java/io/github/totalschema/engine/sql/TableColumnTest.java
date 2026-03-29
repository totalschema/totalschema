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

package io.github.totalschema.engine.sql;

import static org.testng.Assert.*;

import io.github.totalschema.engine.internal.sql.TableColumn;
import org.testng.annotations.Test;

public class TableColumnTest {

    @Test
    public void testValidConstruction() {
        // When
        TableColumn column = new TableColumn("id", "VARCHAR(255)");

        // Then
        assertEquals(column.getName(), "id");
        assertEquals(column.getTypeExpression(), "VARCHAR(255)");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNullName() {
        new TableColumn(null, "VARCHAR(255)");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testBlankName() {
        new TableColumn("  ", "VARCHAR(255)");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNullTypeExpression() {
        new TableColumn("id", null);
    }

    @Test
    public void testEquality() {
        // Given
        TableColumn column1 = new TableColumn("id", "VARCHAR(255)");
        TableColumn column2 = new TableColumn("id", "VARCHAR(255)");
        TableColumn column3 = new TableColumn("name", "VARCHAR(255)");
        TableColumn column4 = new TableColumn("id", "BIGINT");

        // Then
        assertEquals(column1, column2);
        assertNotEquals(column1, column3);
        assertNotEquals(column1, column4);
        assertNotEquals(column1, null);
        assertNotEquals(column1, "string");
    }

    @Test
    public void testHashCode() {
        // Given
        TableColumn column1 = new TableColumn("id", "VARCHAR(255)");
        TableColumn column2 = new TableColumn("id", "VARCHAR(255)");

        // Then
        assertEquals(column1.hashCode(), column2.hashCode());
    }

    @Test
    public void testToString() {
        // Given
        TableColumn column = new TableColumn("id", "VARCHAR(255)");

        // Then
        String str = column.toString();
        assertTrue(str.contains("id"));
        assertTrue(str.contains("VARCHAR(255)"));
    }
}
