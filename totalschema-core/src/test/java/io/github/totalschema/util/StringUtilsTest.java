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

package io.github.totalschema.util;

import static org.testng.Assert.*;

import org.testng.annotations.Test;

public class StringUtilsTest {

    @Test
    public void testEmptyToNullWithNonEmptyString() {
        String result = StringUtils.emptyToNull("hello");
        assertEquals(result, "hello");
    }

    @Test
    public void testEmptyToNullWithEmptyString() {
        String result = StringUtils.emptyToNull("");
        assertNull(result);
    }

    @Test
    public void testEmptyToNullWithBlankString() {
        String result = StringUtils.emptyToNull("   ");
        assertNull(result);
    }

    @Test
    public void testEmptyToNullWithTabsAndSpaces() {
        String result = StringUtils.emptyToNull("\t\n  ");
        assertNull(result);
    }

    @Test
    public void testEmptyToNullWithStringContainingWhitespace() {
        String result = StringUtils.emptyToNull("  hello  ");
        assertEquals(result, "  hello  ");
    }

    @Test
    public void testMaskPasswordWithNullPassword() {
        String result = StringUtils.maskPassword(null);
        assertEquals(result, "null");
    }

    @Test
    public void testMaskPasswordWithEmptyPassword() {
        String result = StringUtils.maskPassword("");
        assertEquals(result, "");
    }

    @Test
    public void testMaskPasswordWithShortPassword() {
        String result = StringUtils.maskPassword("abc");
        assertEquals(result, "***");
    }

    @Test
    public void testMaskPasswordWithLongPassword() {
        String result = StringUtils.maskPassword("password123");
        assertEquals(result, "***********");
        assertEquals(result.length(), "password123".length());
    }

    @Test
    public void testMaskPasswordWithSingleCharacter() {
        String result = StringUtils.maskPassword("x");
        assertEquals(result, "*");
    }

    @Test
    public void testMaskPasswordPreservesLength() {
        String password = "mySecretPassword123!@#";
        String masked = StringUtils.maskPassword(password);

        assertEquals(masked.length(), password.length());
        assertTrue(masked.chars().allMatch(c -> c == '*'));
    }
}
