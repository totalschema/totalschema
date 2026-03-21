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
}
