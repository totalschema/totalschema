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

import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.testng.annotations.Test;

public class ConfigurationBuilderTest {

    @Test
    public void testBuilderBasicUsage() {
        Configuration config =
                Configuration.builder()
                        .set("jdbc.url", "jdbc:h2:mem:testdb")
                        .set("username", "sa")
                        .set("password", "")
                        .build();

        assertEquals(config.getString("jdbc.url").orElse(null), "jdbc:h2:mem:testdb");
        assertEquals(config.getString("username").orElse(null), "sa");
        assertEquals(config.getString("password").orElse(null), "");
    }

    @Test
    public void testBuilderWithIntValue() {
        Configuration config = Configuration.builder().set("port", 5432).build();

        assertEquals(config.getInt("port").orElse(null), (Integer) 5432);
        assertEquals(config.getString("port").orElse(null), "5432");
    }

    @Test
    public void testBuilderWithLongValue() {
        Configuration config = Configuration.builder().set("timeout", 3000000000L).build();

        assertEquals(config.getLong("timeout").orElse(null), (Long) 3000000000L);
        assertEquals(config.getString("timeout").orElse(null), "3000000000");
    }

    @Test
    public void testBuilderWithBooleanValue() {
        Configuration config =
                Configuration.builder().set("autoCommit", true).set("logSql", false).build();

        assertTrue(config.getBoolean("autoCommit").orElse(null));
        assertFalse(config.getBoolean("logSql").orElse(null));
        assertEquals(config.getString("autoCommit").orElse(null), "true");
        assertEquals(config.getString("logSql").orElse(null), "false");
    }

    @Test
    public void testBuilderWithEnumValue() {
        Configuration config =
                Configuration.builder()
                        .set("timeUnit", TimeUnit.SECONDS)
                        .set("otherUnit", TimeUnit.MILLISECONDS)
                        .build();

        assertEquals(
                config.getEnumValue(TimeUnit.class, "timeUnit").orElse(null), TimeUnit.SECONDS);
        assertEquals(
                config.getEnumValue(TimeUnit.class, "otherUnit").orElse(null),
                TimeUnit.MILLISECONDS);
        assertEquals(config.getString("timeUnit").orElse(null), "SECONDS");
    }

    @Test
    public void testBuilderSetAll() {
        Map<String, String> entries = Map.of("key1", "value1", "key2", "value2", "key3", "value3");

        Configuration config = Configuration.builder().setAll(entries).build();

        assertEquals(config.getString("key1").orElse(null), "value1");
        assertEquals(config.getString("key2").orElse(null), "value2");
        assertEquals(config.getString("key3").orElse(null), "value3");
    }

    @Test
    public void testBuilderSetAllFromConfiguration() {
        Configuration original =
                Configuration.builder().set("key1", "value1").set("key2", "value2").build();

        Configuration copy = Configuration.builder().setAll(original).set("key3", "value3").build();

        assertEquals(copy.getString("key1").orElse(null), "value1");
        assertEquals(copy.getString("key2").orElse(null), "value2");
        assertEquals(copy.getString("key3").orElse(null), "value3");
    }

    @Test
    public void testBuilderOverwriteValue() {
        Configuration config =
                Configuration.builder().set("key", "original").set("key", "updated").build();

        assertEquals(config.getString("key").orElse(null), "updated");
    }

    @Test
    public void testBuilderRemove() {
        Configuration config =
                Configuration.builder()
                        .set("key1", "value1")
                        .set("key2", "value2")
                        .remove("key1")
                        .build();

        assertFalse(config.getString("key1").isPresent());
        assertEquals(config.getString("key2").orElse(null), "value2");
    }

    @Test
    public void testBuilderClear() {
        Configuration config =
                Configuration.builder()
                        .set("key1", "value1")
                        .set("key2", "value2")
                        .clear()
                        .set("key3", "value3")
                        .build();

        assertFalse(config.getString("key1").isPresent());
        assertFalse(config.getString("key2").isPresent());
        assertEquals(config.getString("key3").orElse(null), "value3");
    }

    @Test
    public void testBuilderEmptyConfiguration() {
        Configuration config = Configuration.builder().build();

        assertTrue(config.isEmpty());
        assertTrue(config.getKeys().isEmpty());
    }

    @Test
    public void testBuilderImmutability() {
        ConfigurationBuilder builder = Configuration.builder();
        Configuration config1 = builder.set("key", "value1").build();
        Configuration config2 = builder.set("key", "value2").build();

        // First config should still have original value
        assertEquals(config1.getString("key").orElse(null), "value1");
        // Second config should have new value
        assertEquals(config2.getString("key").orElse(null), "value2");
    }

    @Test
    public void testBuilderMultipleBuildCalls() {
        ConfigurationBuilder builder =
                Configuration.builder().set("key1", "value1").set("key2", "value2");

        Configuration config1 = builder.build();
        Configuration config2 = builder.build();

        // Both should have same values
        assertEquals(config1.getString("key1").orElse(null), "value1");
        assertEquals(config2.getString("key1").orElse(null), "value1");

        // But should be different instances
        assertNotSame(config1, config2);
    }

    @Test
    public void testBuilderNullKeyThrowsException() {
        ConfigurationBuilder builder = Configuration.builder();

        assertThrows(NullPointerException.class, () -> builder.set(null, "value"));
    }

    @Test
    public void testBuilderNullStringValueThrowsException() {
        ConfigurationBuilder builder = Configuration.builder();

        assertThrows(NullPointerException.class, () -> builder.set("key", (String) null));
    }

    @Test
    public void testBuilderNullEnumValueThrowsException() {
        ConfigurationBuilder builder = Configuration.builder();

        assertThrows(NullPointerException.class, () -> builder.set("key", (Enum<?>) null));
    }

    @Test
    public void testBuilderNullMapThrowsException() {
        ConfigurationBuilder builder = Configuration.builder();

        assertThrows(NullPointerException.class, () -> builder.setAll((Map<String, String>) null));
    }

    @Test
    public void testBuilderNullConfigurationThrowsException() {
        ConfigurationBuilder builder = Configuration.builder();

        assertThrows(NullPointerException.class, () -> builder.setAll((Configuration) null));
    }

    @Test
    public void testBuilderNullRemoveKeyThrowsException() {
        ConfigurationBuilder builder = Configuration.builder();

        assertThrows(NullPointerException.class, () -> builder.remove(null));
    }

    @Test
    public void testBuilderChaining() {
        // Test that all methods support chaining
        Configuration config =
                Configuration.builder()
                        .set("key1", "value1")
                        .set("key2", 42)
                        .set("key3", true)
                        .set("key4", TimeUnit.SECONDS)
                        .setAll(Map.of("key5", "value5"))
                        .remove("key2")
                        .build();

        assertEquals(config.getString("key1").orElse(null), "value1");
        assertFalse(config.getString("key2").isPresent());
        assertTrue(config.getBoolean("key3").orElse(null));
        assertEquals(config.getEnumValue(TimeUnit.class, "key4").orElse(null), TimeUnit.SECONDS);
        assertEquals(config.getString("key5").orElse(null), "value5");
    }

    @Test
    public void testBuilderIntegrationWithExistingFeatures() {
        Configuration config =
                Configuration.builder()
                        .set("jdbc.url", "jdbc:h2:mem:testdb")
                        .set("username", "sa")
                        .set("password", "secret")
                        .set("autoCommit", true)
                        .set("pool.maximumPoolSize", 10)
                        .set("connectionTest.timeout", 30)
                        .set("connectionTest.timeoutUnit", TimeUnit.SECONDS)
                        .build();

        // Test getString
        assertEquals(config.getString("jdbc.url").orElse(null), "jdbc:h2:mem:testdb");

        // Test getBoolean
        assertTrue(config.getBoolean("autoCommit").orElse(null));

        // Test getInt
        assertEquals(config.getInt("pool.maximumPoolSize").orElse(null), (Integer) 10);
        assertEquals(config.getInt("connectionTest.timeout").orElse(null), (Integer) 30);

        // Test getEnumValue
        assertEquals(
                config.getEnumValue(TimeUnit.class, "connectionTest.timeoutUnit").orElse(null),
                TimeUnit.SECONDS);

        // Test addAll
        Configuration extended = config.withEntry("newKey", "newValue");
        assertEquals(extended.getString("newKey").orElse(null), "newValue");
        assertEquals(extended.getString("jdbc.url").orElse(null), "jdbc:h2:mem:testdb");
    }

    @Test
    public void testToBuilder() {
        Configuration original =
                Configuration.builder()
                        .set("key1", "value1")
                        .set("key2", "value2")
                        .set("key3", "value3")
                        .build();

        Configuration modified = original.toBuilder().set("key2", "newValue2").build();

        // Original should be unchanged
        assertEquals(original.getString("key1").orElse(null), "value1");
        assertEquals(original.getString("key2").orElse(null), "value2");
        assertEquals(original.getString("key3").orElse(null), "value3");

        // Modified should have updated value
        assertEquals(modified.getString("key1").orElse(null), "value1");
        assertEquals(modified.getString("key2").orElse(null), "newValue2");
        assertEquals(modified.getString("key3").orElse(null), "value3");
    }

    @Test
    public void testToBuilderAddNewValues() {
        Configuration original =
                Configuration.builder().set("key1", "value1").set("key2", "value2").build();

        Configuration modified =
                original.toBuilder().set("key3", "value3").set("key4", "value4").build();

        // Original should be unchanged
        assertEquals(original.getString("key1").orElse(null), "value1");
        assertEquals(original.getString("key2").orElse(null), "value2");
        assertFalse(original.getString("key3").isPresent());
        assertFalse(original.getString("key4").isPresent());

        // Modified should have all values
        assertEquals(modified.getString("key1").orElse(null), "value1");
        assertEquals(modified.getString("key2").orElse(null), "value2");
        assertEquals(modified.getString("key3").orElse(null), "value3");
        assertEquals(modified.getString("key4").orElse(null), "value4");
    }

    @Test
    public void testToBuilderRemoveValues() {
        Configuration original =
                Configuration.builder()
                        .set("key1", "value1")
                        .set("key2", "value2")
                        .set("key3", "value3")
                        .build();

        Configuration modified = original.toBuilder().remove("key2").build();

        // Original should be unchanged
        assertEquals(original.getString("key1").orElse(null), "value1");
        assertEquals(original.getString("key2").orElse(null), "value2");
        assertEquals(original.getString("key3").orElse(null), "value3");

        // Modified should not have key2
        assertEquals(modified.getString("key1").orElse(null), "value1");
        assertFalse(modified.getString("key2").isPresent());
        assertEquals(modified.getString("key3").orElse(null), "value3");
    }

    @Test
    public void testToBuilderWithEmptyConfiguration() {
        Configuration empty = Configuration.builder().build();

        Configuration modified = empty.toBuilder().set("key1", "value1").build();

        // Empty should remain empty
        assertTrue(empty.isEmpty());

        // Modified should have the new value
        assertEquals(modified.getString("key1").orElse(null), "value1");
    }

    @Test
    public void testToBuilderChaining() {
        Configuration config1 =
                Configuration.builder()
                        .set("jdbc.url", "jdbc:h2:mem:testdb")
                        .set("username", "sa")
                        .build();

        Configuration config2 =
                config1.toBuilder()
                        .set("password", "secret")
                        .set("autoCommit", true)
                        .set("pool.size", 10)
                        .build();

        Configuration config3 =
                config2.toBuilder()
                        .set("username", "admin") // Override
                        .remove("pool.size") // Remove
                        .set("timeout", 5000L)
                        .build();

        // Verify config1
        assertEquals(config1.getString("jdbc.url").orElse(null), "jdbc:h2:mem:testdb");
        assertEquals(config1.getString("username").orElse(null), "sa");
        assertFalse(config1.getString("password").isPresent());

        // Verify config2
        assertEquals(config2.getString("jdbc.url").orElse(null), "jdbc:h2:mem:testdb");
        assertEquals(config2.getString("username").orElse(null), "sa");
        assertEquals(config2.getString("password").orElse(null), "secret");
        assertTrue(config2.getBoolean("autoCommit").orElse(null));

        // Verify config3
        assertEquals(config3.getString("jdbc.url").orElse(null), "jdbc:h2:mem:testdb");
        assertEquals(config3.getString("username").orElse(null), "admin");
        assertEquals(config3.getString("password").orElse(null), "secret");
        assertTrue(config3.getBoolean("autoCommit").orElse(null));
        assertFalse(config3.getString("pool.size").isPresent());
        assertEquals(config3.getLong("timeout").orElse(null), (Long) 5000L);
    }

    @Test
    public void testSetIfAbsentString() {
        Configuration config =
                Configuration.builder()
                        .set("key1", "value1")
                        .setIfAbsent("key1", "newValue1") // Should not override
                        .setIfAbsent("key2", "value2") // Should add
                        .build();

        assertEquals(config.getString("key1").orElse(null), "value1");
        assertEquals(config.getString("key2").orElse(null), "value2");
    }

    @Test
    public void testSetIfAbsentInt() {
        Configuration config =
                Configuration.builder()
                        .set("port", 8080)
                        .setIfAbsent("port", 9090) // Should not override
                        .setIfAbsent("maxConnections", 100) // Should add
                        .build();

        assertEquals(config.getInt("port").orElse(null), (Integer) 8080);
        assertEquals(config.getInt("maxConnections").orElse(null), (Integer) 100);
    }

    @Test
    public void testSetIfAbsentLong() {
        Configuration config =
                Configuration.builder()
                        .set("timeout", 5000L)
                        .setIfAbsent("timeout", 10000L) // Should not override
                        .setIfAbsent("maxFileSize", 1048576L) // Should add
                        .build();

        assertEquals(config.getLong("timeout").orElse(null), (Long) 5000L);
        assertEquals(config.getLong("maxFileSize").orElse(null), (Long) 1048576L);
    }

    @Test
    public void testSetIfAbsentBoolean() {
        Configuration config =
                Configuration.builder()
                        .set("enabled", true)
                        .setIfAbsent("enabled", false) // Should not override
                        .setIfAbsent("debug", false) // Should add
                        .build();

        assertTrue(config.getBoolean("enabled").orElse(null));
        assertFalse(config.getBoolean("debug").orElse(null));
    }

    @Test
    public void testSetIfAbsentEnum() {
        Configuration config =
                Configuration.builder()
                        .set("unit", TimeUnit.SECONDS)
                        .setIfAbsent("unit", TimeUnit.MILLISECONDS) // Should not override
                        .setIfAbsent("defaultUnit", TimeUnit.MINUTES) // Should add
                        .build();

        assertEquals(config.getEnumValue(TimeUnit.class, "unit").orElse(null), TimeUnit.SECONDS);
        assertEquals(
                config.getEnumValue(TimeUnit.class, "defaultUnit").orElse(null), TimeUnit.MINUTES);
    }

    @Test
    public void testSetIfAbsentWithDefaults() {
        // Use case: providing default values that can be overridden
        Configuration config =
                Configuration.builder()
                        // Set user-provided values
                        .set("username", "admin")
                        .set("port", 8080)
                        // Set defaults (only applied if not already set)
                        .setIfAbsent("username", "guest")
                        .setIfAbsent("password", "defaultPassword")
                        .setIfAbsent("port", 3000)
                        .setIfAbsent("debug", false)
                        .build();

        // User-provided values should remain
        assertEquals(config.getString("username").orElse(null), "admin");
        assertEquals(config.getInt("port").orElse(null), (Integer) 8080);

        // Defaults should be applied where no value was set
        assertEquals(config.getString("password").orElse(null), "defaultPassword");
        assertFalse(config.getBoolean("debug").orElse(null));
    }

    @Test
    public void testSetIfAbsentNullKeyThrowsException() {
        ConfigurationBuilder builder = Configuration.builder();

        assertThrows(NullPointerException.class, () -> builder.setIfAbsent(null, "value"));
    }

    @Test
    public void testSetIfAbsentNullStringValueThrowsException() {
        ConfigurationBuilder builder = Configuration.builder();

        assertThrows(NullPointerException.class, () -> builder.setIfAbsent("key", (String) null));
    }

    @Test
    public void testSetIfAbsentNullEnumValueThrowsException() {
        ConfigurationBuilder builder = Configuration.builder();

        assertThrows(NullPointerException.class, () -> builder.setIfAbsent("key", (Enum<?>) null));
    }

    @Test
    public void testSetIfAbsentChaining() {
        Configuration config =
                Configuration.builder()
                        .setIfAbsent("key1", "value1")
                        .setIfAbsent("key2", 42)
                        .setIfAbsent("key3", true)
                        .setIfAbsent("key4", TimeUnit.SECONDS)
                        .set("key2", 100) // Override
                        .setIfAbsent("key2", 200) // Should not override
                        .build();

        assertEquals(config.getString("key1").orElse(null), "value1");
        assertEquals(config.getInt("key2").orElse(null), (Integer) 100);
        assertTrue(config.getBoolean("key3").orElse(null));
        assertEquals(config.getEnumValue(TimeUnit.class, "key4").orElse(null), TimeUnit.SECONDS);
    }
}
