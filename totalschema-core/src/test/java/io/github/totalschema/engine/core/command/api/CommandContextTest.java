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

package io.github.totalschema.engine.core.command.api;

import static org.testng.Assert.*;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class CommandContextTest {

    private CommandContext context;

    @BeforeMethod
    public void setUp() {
        context = new CommandContext();
    }

    @Test
    public void testSetValueAndGet() {
        String value = "test-value";
        context.setValue(String.class, value);

        String result = context.get(String.class);
        assertEquals(result, value);
    }

    @Test
    public void testHasReturnsTrueAfterSetValue() {
        context.setValue(String.class, "value");

        assertTrue(context.has(String.class));
    }

    @Test
    public void testHasReturnsFalseForMissingValue() {
        assertFalse(context.has(String.class));
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testSetValueWithNullClass() {
        context.setValue(null, "value");
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testSetValueWithNullValue() {
        context.setValue(String.class, null);
    }

    @Test(
            expectedExceptions = IllegalStateException.class,
            expectedExceptionsMessageRegExp = "Value for type.*")
    public void testSetValueTwiceThrowsException() {
        context.setValue(String.class, "first");
        context.setValue(String.class, "second");
    }

    @Test(
            expectedExceptions = IllegalStateException.class,
            expectedExceptionsMessageRegExp = "No Context value found.*")
    public void testGetWithMissingValueThrowsException() {
        context.get(String.class);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testGetWithNullClass() {
        context.get(null);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testHasWithNullClass() {
        context.has(null);
    }

    @Test
    public void testMultipleTypesInContext() {
        String stringValue = "text";
        Integer intValue = 42;
        Boolean boolValue = true;

        context.setValue(String.class, stringValue);
        context.setValue(Integer.class, intValue);
        context.setValue(Boolean.class, boolValue);

        assertEquals(context.get(String.class), stringValue);
        assertEquals(context.get(Integer.class), intValue);
        assertEquals(context.get(Boolean.class), boolValue);
    }

    @Test
    public void testHasForMultipleTypes() {
        context.setValue(String.class, "value");
        context.setValue(Integer.class, 123);

        assertTrue(context.has(String.class));
        assertTrue(context.has(Integer.class));
        assertFalse(context.has(Boolean.class));
    }

    @Test
    public void testSetValueWithSuperclass() {
        // Can store a String using CharSequence class
        String value = "test";
        context.setValue(CharSequence.class, value);

        CharSequence result = context.get(CharSequence.class);
        assertEquals(result, value);
    }

    @Test
    public void testToString() {
        context.setValue(String.class, "value");
        String str = context.toString();

        assertNotNull(str);
        assertTrue(str.contains("CommandContext"));
        assertTrue(str.contains("values"));
    }

    @Test
    public void testEmptyContextToString() {
        String str = context.toString();

        assertNotNull(str);
        assertTrue(str.contains("CommandContext"));
    }

    @Test
    public void testContextWithCustomClass() {
        CustomTestClass customValue = new CustomTestClass("test");
        context.setValue(CustomTestClass.class, customValue);

        CustomTestClass result = context.get(CustomTestClass.class);
        assertEquals(result, customValue);
    }

    @Test
    public void testIndependentContextInstances() {
        CommandContext context1 = new CommandContext();
        CommandContext context2 = new CommandContext();

        context1.setValue(String.class, "value1");
        context2.setValue(String.class, "value2");

        assertEquals(context1.get(String.class), "value1");
        assertEquals(context2.get(String.class), "value2");
    }

    // Helper class for testing
    private static class CustomTestClass {
        private final String data;

        CustomTestClass(String data) {
            this.data = data;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CustomTestClass that = (CustomTestClass) o;
            return data.equals(that.data);
        }

        @Override
        public int hashCode() {
            return data.hashCode();
        }
    }
}
