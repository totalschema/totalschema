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

package io.github.totalschema.config;

import static org.testng.Assert.*;

import io.github.totalschema.ProjectConventions;
import java.util.Optional;
import java.util.Set;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class SystemPropertyConfigurationTest {

    private SystemPropertyConfiguration configuration;
    private static final String TEST_KEY = "test.key";
    private static final String TEST_VALUE = "test.value";
    private static final String FULL_PROPERTY_KEY =
            SystemPropertyConfiguration.JVM_SYSTEM_PROPERTY_PREFIX + TEST_KEY;

    @BeforeMethod
    public void setUp() {
        configuration = new SystemPropertyConfiguration();
        // Clean up any existing test properties
        System.clearProperty(FULL_PROPERTY_KEY);
    }

    @AfterMethod
    public void tearDown() {
        // Clean up test properties
        System.clearProperty(FULL_PROPERTY_KEY);
    }

    @Test
    public void testGetStringWithExistingProperty() {
        System.setProperty(FULL_PROPERTY_KEY, TEST_VALUE);

        Optional<String> result = configuration.getString(TEST_KEY);

        assertTrue(result.isPresent());
        assertEquals(result.get(), TEST_VALUE);
    }

    @Test
    public void testGetStringWithNonExistingProperty() {
        Optional<String> result = configuration.getString("nonexistent.key");

        assertFalse(result.isPresent());
    }

    @Test
    public void testGetKeys() {
        String key1 = "config.key1";
        String key2 = "config.key2";
        String fullKey1 = SystemPropertyConfiguration.JVM_SYSTEM_PROPERTY_PREFIX + key1;
        String fullKey2 = SystemPropertyConfiguration.JVM_SYSTEM_PROPERTY_PREFIX + key2;

        System.setProperty(fullKey1, "value1");
        System.setProperty(fullKey2, "value2");

        try {
            Set<String> keys = configuration.getKeys();

            assertTrue(keys.contains(key1));
            assertTrue(keys.contains(key2));
        } finally {
            System.clearProperty(fullKey1);
            System.clearProperty(fullKey2);
        }
    }

    @Test
    public void testGetKeysWithNoTotalschemaProperties() {
        // Ensure no totalschema properties exist
        System.getProperties().keySet().stream()
                .map(Object::toString)
                .filter(k -> k.startsWith(SystemPropertyConfiguration.JVM_SYSTEM_PROPERTY_PREFIX))
                .forEach(System::clearProperty);

        Set<String> keys = configuration.getKeys();

        assertNotNull(keys);
        assertTrue(keys.isEmpty());
    }

    @Test
    public void testGetKeysFiltersNonTotalschemaProperties() {
        String totalschemaKey = "mykey";
        String fullTotalschemaKey =
                SystemPropertyConfiguration.JVM_SYSTEM_PROPERTY_PREFIX + totalschemaKey;
        String nonTotalschemaKey = "some.other.property";

        System.setProperty(fullTotalschemaKey, "value1");
        System.setProperty(nonTotalschemaKey, "value2");

        try {
            Set<String> keys = configuration.getKeys();

            assertTrue(keys.contains(totalschemaKey));
            assertFalse(keys.contains(nonTotalschemaKey));
        } finally {
            System.clearProperty(fullTotalschemaKey);
            System.clearProperty(nonTotalschemaKey);
        }
    }

    @Test
    public void testSystemPropertyPrefixConstant() {
        String expectedPrefix = ProjectConventions.PROJECT_SYSTEM_NAME + ".";
        assertEquals(SystemPropertyConfiguration.JVM_SYSTEM_PROPERTY_PREFIX, expectedPrefix);
    }

    @Test
    public void testToString() {
        System.setProperty(FULL_PROPERTY_KEY, TEST_VALUE);

        try {
            String result = configuration.toString();

            assertNotNull(result);
            // toString should contain information about the configuration
            assertTrue(result.contains("Configuration") || result.contains("System"));
        } finally {
            System.clearProperty(FULL_PROPERTY_KEY);
        }
    }

    @Test
    public void testGetIntFromSystemProperty() {
        String intKey = "int.property";
        String fullIntKey = SystemPropertyConfiguration.JVM_SYSTEM_PROPERTY_PREFIX + intKey;
        System.setProperty(fullIntKey, "42");

        try {
            Optional<Integer> result = configuration.getInt(intKey);

            assertTrue(result.isPresent());
            assertEquals(result.get(), Integer.valueOf(42));
        } finally {
            System.clearProperty(fullIntKey);
        }
    }

    @Test
    public void testGetBooleanFromSystemProperty() {
        String boolKey = "bool.property";
        String fullBoolKey = SystemPropertyConfiguration.JVM_SYSTEM_PROPERTY_PREFIX + boolKey;
        System.setProperty(fullBoolKey, "true");

        try {
            Optional<Boolean> result = configuration.getBoolean(boolKey);

            assertTrue(result.isPresent());
            assertTrue(result.get());
        } finally {
            System.clearProperty(fullBoolKey);
        }
    }
}
