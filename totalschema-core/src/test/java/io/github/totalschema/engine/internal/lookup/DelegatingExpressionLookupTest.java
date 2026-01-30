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

package io.github.totalschema.engine.internal.lookup;

import static org.testng.Assert.*;

import org.testng.annotations.Test;

public class DelegatingExpressionLookupTest {

    @Test
    public void testGetKey() {
        DelegatingExpressionLookup lookup =
                new DelegatingExpressionLookup("testKey", s -> s.toUpperCase(), "Test Description");

        assertEquals(lookup.getKey(), "testKey");
    }

    @Test
    public void testApply() {
        DelegatingExpressionLookup lookup =
                new DelegatingExpressionLookup("uppercase", s -> s.toUpperCase(), "To upper case");

        String result = lookup.apply("hello");

        assertEquals(result, "HELLO");
    }

    @Test
    public void testApplyWithNullValue() {
        DelegatingExpressionLookup lookup =
                new DelegatingExpressionLookup("identity", s -> s, "Identity function");

        String result = lookup.apply(null);

        assertNull(result);
    }

    @Test
    public void testApplyWithComplexTransformation() {
        DelegatingExpressionLookup lookup =
                new DelegatingExpressionLookup(
                        "reverse",
                        s -> new StringBuilder(s).reverse().toString(),
                        "Reverse string");

        String result = lookup.apply("hello");

        assertEquals(result, "olleh");
    }

    @Test
    public void testToString() {
        DelegatingExpressionLookup lookup =
                new DelegatingExpressionLookup("testKey", s -> s, "Test Description");

        String result = lookup.toString();

        assertEquals(result, "testKey-->Test Description");
    }

    @Test
    public void testMultipleApplications() {
        DelegatingExpressionLookup lookup =
                new DelegatingExpressionLookup("trim", String::trim, "Trim whitespace");

        assertEquals(lookup.apply("  hello  "), "hello");
        assertEquals(lookup.apply("world"), "world");
        assertEquals(lookup.apply("  test  "), "test");
    }
}
