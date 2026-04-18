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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.testng.annotations.Test;

public class YamlFileConfigurationTest {

    @Test
    public void testGetPrefixNamespaceWithNestedKeys() {
        // Supply YAML content from a string (no file required)
        String yamlContent =
                "lock:\n"
                        + "  type: database\n"
                        + "  database:\n"
                        + "    logSql: true\n"
                        + "    jdbc:\n"
                        + "      url: jdbc:h2:mem:test\n"
                        + "      username: sa\n"
                        + "    table:\n"
                        + "      name: lock_table\n";

        YamlFileConfiguration configuration =
                new YamlFileConfiguration(
                        () -> {
                            InputStream stream =
                                    new ByteArrayInputStream(
                                            yamlContent.getBytes(StandardCharsets.UTF_8));
                            return Optional.of(stream);
                        },
                        "In-memory YAML");

        Configuration prefixConfig = configuration.getPrefixNamespace("lock.database");

        // Should contain nested leaf keys
        assertTrue(prefixConfig.getKeys().contains("jdbc.url"));
        assertTrue(prefixConfig.getString("jdbc.url").isPresent());
        assertEquals(prefixConfig.getString("jdbc.url").orElse(null), "jdbc:h2:mem:test");

        assertTrue(prefixConfig.getKeys().contains("jdbc.username"));
        assertTrue(prefixConfig.getString("jdbc.username").isPresent());
        assertEquals(prefixConfig.getString("jdbc.username").orElse(null), "sa");

        assertTrue(prefixConfig.getKeys().contains("table.name"));
        assertTrue(prefixConfig.getString("table.name").isPresent());
        assertEquals(prefixConfig.getString("table.name").orElse(null), "lock_table");

        assertTrue(prefixConfig.getKeys().contains("logSql"));
        assertTrue(prefixConfig.getString("logSql").isPresent());
        assertEquals(prefixConfig.getString("logSql").orElse(null), "true");

        // Should NOT contain intermediate Map keys
        assertFalse(prefixConfig.getKeys().contains("jdbc"));
        assertFalse(prefixConfig.getString("jdbc").isPresent());

        assertFalse(prefixConfig.getKeys().contains("table"));
        assertFalse(prefixConfig.getString("table").isPresent());

        // Verify only leaf keys are present
        assertEquals(
                prefixConfig.getKeys(),
                Set.of("logSql", "jdbc.url", "jdbc.username", "table.name"));
    }

    @Test
    public void testFlatteningOfNestedStructure() {
        String yamlContent =
                "database:\n"
                        + "  connection:\n"
                        + "    host: localhost\n"
                        + "    port: 5432\n"
                        + "  pool:\n"
                        + "    maxSize: 10\n"
                        + "    minSize: 2\n"
                        + "simple: value\n";

        YamlFileConfiguration configuration =
                new YamlFileConfiguration(
                        () ->
                                Optional.of(
                                        new ByteArrayInputStream(
                                                yamlContent.getBytes(StandardCharsets.UTF_8))),
                        "Test YAML");

        // Verify all leaf keys are present
        assertTrue(configuration.getKeys().contains("database.connection.host"));
        assertTrue(configuration.getKeys().contains("database.connection.port"));
        assertTrue(configuration.getKeys().contains("database.pool.maxSize"));
        assertTrue(configuration.getKeys().contains("database.pool.minSize"));
        assertTrue(configuration.getKeys().contains("simple"));

        // Verify intermediate Map keys are NOT present
        assertFalse(configuration.getKeys().contains("database"));
        assertFalse(configuration.getKeys().contains("database.connection"));
        assertFalse(configuration.getKeys().contains("database.pool"));

        // Verify values
        assertEquals(configuration.getString("database.connection.host").orElse(null), "localhost");
        assertEquals(configuration.getString("database.connection.port").orElse(null), "5432");
        assertEquals(configuration.getString("database.pool.maxSize").orElse(null), "10");
        assertEquals(configuration.getString("simple").orElse(null), "value");

        // Verify intermediate keys return empty
        assertFalse(configuration.getString("database").isPresent());
        assertFalse(configuration.getString("database.connection").isPresent());
        assertFalse(configuration.getString("database.pool").isPresent());

        // Verify exact key count
        assertEquals(configuration.getKeys().size(), 5);
    }

    @Test
    public void testEmptyYaml() {
        YamlFileConfiguration configuration =
                new YamlFileConfiguration(
                        () ->
                                Optional.of(
                                        new ByteArrayInputStream(
                                                "".getBytes(StandardCharsets.UTF_8))),
                        "Empty YAML");

        assertTrue(configuration.getKeys().isEmpty());
        assertFalse(configuration.getString("anyKey").isPresent());
    }

    @Test
    public void testMissingFile() {
        YamlFileConfiguration configuration =
                new YamlFileConfiguration(Optional::empty, "Missing file");

        assertTrue(configuration.getKeys().isEmpty());
        assertFalse(configuration.getString("anyKey").isPresent());
    }

    @Test
    public void testNullValuesAreIgnored() {
        String yamlContent = "key1: value1\n" + "key2: null\n" + "key3: value3\n";

        YamlFileConfiguration configuration =
                new YamlFileConfiguration(
                        () ->
                                Optional.of(
                                        new ByteArrayInputStream(
                                                yamlContent.getBytes(StandardCharsets.UTF_8))),
                        "YAML with nulls");

        // key2 with null value should not be included
        assertEquals(configuration.getKeys(), Set.of("key1", "key3"));
        assertEquals(configuration.getString("key1").orElse(null), "value1");
        assertFalse(configuration.getString("key2").isPresent());
        assertEquals(configuration.getString("key3").orElse(null), "value3");
    }

    @Test
    public void testListValuesConvertedToIndexedKeys() {
        String yamlContent = "items:\n" + "  - item1\n" + "  - item2\n" + "simple: value\n";

        YamlFileConfiguration configuration =
                new YamlFileConfiguration(
                        () ->
                                Optional.of(
                                        new ByteArrayInputStream(
                                                yamlContent.getBytes(StandardCharsets.UTF_8))),
                        "YAML with list");

        // List entries are flattened to indexed keys: items.0, items.1
        assertTrue(configuration.getKeys().contains("items.0"));
        assertTrue(configuration.getKeys().contains("items.1"));
        assertEquals(configuration.getString("items.0").orElse(null), "item1");
        assertEquals(configuration.getString("items.1").orElse(null), "item2");

        // The parent key itself is NOT a leaf
        assertFalse(configuration.getString("items").isPresent());

        assertEquals(configuration.getString("simple").orElse(null), "value");
    }

    @Test
    public void testGetListFromYamlList() {
        String yamlContent =
                "initCommands:\n"
                        + "  - pip install -r requirements.txt\n"
                        + "  - pip install pandas\n";

        YamlFileConfiguration configuration =
                new YamlFileConfiguration(
                        () ->
                                Optional.of(
                                        new ByteArrayInputStream(
                                                yamlContent.getBytes(StandardCharsets.UTF_8))),
                        "YAML with initCommands list");

        Optional<List<String>> result = configuration.getList("initCommands");
        assertTrue(result.isPresent());
        assertEquals(
                result.get(), List.of("pip install -r requirements.txt", "pip install pandas"));
    }

    @Test
    public void testGetListFromCommaSeparatedString() {
        String yamlContent = "initCommands: pip install -r requirements.txt, pip install pandas\n";

        YamlFileConfiguration configuration =
                new YamlFileConfiguration(
                        () ->
                                Optional.of(
                                        new ByteArrayInputStream(
                                                yamlContent.getBytes(StandardCharsets.UTF_8))),
                        "YAML with comma-separated initCommands");

        Optional<List<String>> result = configuration.getList("initCommands");
        assertTrue(result.isPresent());
        assertEquals(
                result.get(), List.of("pip install -r requirements.txt", "pip install pandas"));
    }

    @Test
    public void testDeepNesting() {
        String yamlContent =
                "level1:\n"
                        + "  level2:\n"
                        + "    level3:\n"
                        + "      level4:\n"
                        + "        level5: deepValue\n";

        YamlFileConfiguration configuration =
                new YamlFileConfiguration(
                        () ->
                                Optional.of(
                                        new ByteArrayInputStream(
                                                yamlContent.getBytes(StandardCharsets.UTF_8))),
                        "Deeply nested YAML");

        // Should handle deep nesting without recursion
        assertTrue(configuration.getKeys().contains("level1.level2.level3.level4.level5"));
        assertEquals(
                configuration.getString("level1.level2.level3.level4.level5").orElse(null),
                "deepValue");

        // Intermediate keys should not be present
        assertFalse(configuration.getKeys().contains("level1"));
        assertFalse(configuration.getKeys().contains("level1.level2"));
        assertFalse(configuration.getKeys().contains("level1.level2.level3"));
        assertFalse(configuration.getKeys().contains("level1.level2.level3.level4"));

        // Should have exactly one key
        assertEquals(configuration.getKeys().size(), 1);
    }
}
