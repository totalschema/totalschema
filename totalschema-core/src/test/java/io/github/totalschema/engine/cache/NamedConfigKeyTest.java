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

package io.github.totalschema.engine.cache;

import static org.testng.Assert.*;

import io.github.totalschema.config.Configuration;
import io.github.totalschema.config.MapConfiguration;
import java.util.Map;
import org.testng.annotations.Test;

public class NamedConfigKeyTest {

    @Test
    public void testConstructorWithConfiguration() {
        Configuration config = new MapConfiguration(Map.of("key1", "value1", "key2", "value2"));
        NamedConfigKey cacheKey = new NamedConfigKey("testName", config);

        assertNotNull(cacheKey);
    }

    @Test
    public void testConstructorWithMap() {
        Map<String, String> configMap = Map.of("key1", "value1", "key2", "value2");
        NamedConfigKey cacheKey = new NamedConfigKey("testName", configMap);

        assertNotNull(cacheKey);
    }

    @Test
    public void testEqualsWithSameValues() {
        Map<String, String> configMap = Map.of("key1", "value1", "key2", "value2");
        NamedConfigKey key1 = new NamedConfigKey("testName", configMap);
        NamedConfigKey key2 = new NamedConfigKey("testName", configMap);

        assertEquals(key1, key2);
    }

    @Test
    public void testEqualsWithDifferentNames() {
        Map<String, String> configMap = Map.of("key1", "value1");
        NamedConfigKey key1 = new NamedConfigKey("name1", configMap);
        NamedConfigKey key2 = new NamedConfigKey("name2", configMap);

        assertNotEquals(key1, key2);
    }

    @Test
    public void testEqualsWithDifferentConfig() {
        NamedConfigKey key1 = new NamedConfigKey("testName", Map.of("key1", "value1"));
        NamedConfigKey key2 = new NamedConfigKey("testName", Map.of("key1", "value2"));

        assertNotEquals(key1, key2);
    }

    @Test
    public void testEqualsWithSameInstance() {
        NamedConfigKey key = new NamedConfigKey("testName", Map.of("key1", "value1"));

        assertEquals(key, key);
    }

    @Test
    public void testEqualsWithNull() {
        NamedConfigKey key = new NamedConfigKey("testName", Map.of("key1", "value1"));

        assertNotEquals(key, null);
    }

    @Test
    public void testEqualsWithDifferentClass() {
        NamedConfigKey key = new NamedConfigKey("testName", Map.of("key1", "value1"));

        assertNotEquals(key, "string");
    }

    @Test
    public void testHashCodeConsistency() {
        Map<String, String> configMap = Map.of("key1", "value1", "key2", "value2");
        NamedConfigKey key1 = new NamedConfigKey("testName", configMap);
        NamedConfigKey key2 = new NamedConfigKey("testName", configMap);

        assertEquals(key1.hashCode(), key2.hashCode());
    }

    @Test
    public void testHashCodeDifferentForDifferentObjects() {
        NamedConfigKey key1 = new NamedConfigKey("name1", Map.of("key1", "value1"));
        NamedConfigKey key2 = new NamedConfigKey("name2", Map.of("key1", "value1"));

        assertNotEquals(key1.hashCode(), key2.hashCode());
    }
}
