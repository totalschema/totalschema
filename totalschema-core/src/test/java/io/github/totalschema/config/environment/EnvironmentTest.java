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

package io.github.totalschema.config.environment;

import static org.testng.Assert.*;

import org.testng.annotations.Test;

public class EnvironmentTest {

    @Test
    public void testConstructorWithValidName() {
        Environment env = new Environment("production");
        assertNotNull(env);
        assertEquals(env.getName(), "production");
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testConstructorWithNullName() {
        new Environment(null);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*whitespace-only.*")
    public void testConstructorWithEmptyName() {
        new Environment("");
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*whitespace-only.*")
    public void testConstructorWithBlankName() {
        new Environment("   ");
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*whitespace-only.*")
    public void testConstructorWithTabsAndSpaces() {
        new Environment("\t\n  ");
    }

    @Test
    public void testGetName() {
        String expectedName = "development";
        Environment env = new Environment(expectedName);

        assertEquals(env.getName(), expectedName);
    }

    @Test
    public void testEqualsWithSameInstance() {
        Environment env = new Environment("test");
        assertEquals(env, env);
    }

    @Test
    public void testEqualsWithEqualEnvironments() {
        Environment env1 = new Environment("staging");
        Environment env2 = new Environment("staging");

        assertEquals(env1, env2);
    }

    @Test
    public void testEqualsWithDifferentEnvironments() {
        Environment env1 = new Environment("dev");
        Environment env2 = new Environment("prod");

        assertNotEquals(env1, env2);
    }

    @Test
    public void testEqualsWithNull() {
        Environment env = new Environment("test");
        assertNotEquals(env, null);
    }

    @Test
    public void testEqualsWithDifferentClass() {
        Environment env = new Environment("test");
        String str = "test";

        assertNotEquals(env, str);
    }

    @Test
    public void testHashCodeConsistency() {
        Environment env = new Environment("test");
        int hash1 = env.hashCode();
        int hash2 = env.hashCode();

        assertEquals(hash1, hash2);
    }

    @Test
    public void testHashCodeEqualityForEqualObjects() {
        Environment env1 = new Environment("production");
        Environment env2 = new Environment("production");

        assertEquals(env1.hashCode(), env2.hashCode());
    }

    @Test
    public void testHashCodeDifferenceForDifferentObjects() {
        Environment env1 = new Environment("dev");
        Environment env2 = new Environment("prod");

        // While not guaranteed, hash codes should typically be different
        assertNotEquals(env1.hashCode(), env2.hashCode());
    }

    @Test
    public void testToString() {
        Environment env = new Environment("myenv");
        String str = env.toString();

        assertNotNull(str);
        assertTrue(str.contains("Environment"));
        assertTrue(str.contains("myenv"));
    }

    @Test
    public void testToStringFormat() {
        Environment env = new Environment("test-env");
        String str = env.toString();

        assertEquals(str, "Environment{name='test-env'}");
    }

    @Test
    public void testEnvironmentWithSpecialCharacters() {
        Environment env = new Environment("env-name_123");
        assertEquals(env.getName(), "env-name_123");
    }

    @Test
    public void testEnvironmentWithNumericName() {
        Environment env = new Environment("123");
        assertEquals(env.getName(), "123");
    }
}
