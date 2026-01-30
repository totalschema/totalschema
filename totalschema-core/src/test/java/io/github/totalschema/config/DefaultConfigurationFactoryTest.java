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

import static org.easymock.EasyMock.*;
import static org.testng.Assert.*;

import io.github.totalschema.ProjectConventions;
import io.github.totalschema.config.environment.Environment;
import io.github.totalschema.spi.config.ConfigurationSupplier;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class DefaultConfigurationFactoryTest {

    private DefaultConfigurationFactory factory;
    private ConfigurationSupplier mockConfigurationSupplier;
    private Configuration mockFileConfiguration;

    @BeforeMethod
    public void setUp() {
        factory = new DefaultConfigurationFactory();
        mockConfigurationSupplier = createMock(ConfigurationSupplier.class);
        mockFileConfiguration = createMock(Configuration.class);
    }

    @Test
    public void testGetRawConfigurationWithEmptyFile() {
        expect(mockConfigurationSupplier.getConfiguration()).andReturn(mockFileConfiguration);
        expect(mockFileConfiguration.isEmpty()).andReturn(true);

        replay(mockConfigurationSupplier, mockFileConfiguration);

        RuntimeException exception =
                expectThrows(
                        RuntimeException.class,
                        () -> factory.getRawConfiguration(mockConfigurationSupplier, null));

        assertEquals(
                exception.getMessage(),
                "Configuration file is missing or empty: " + ProjectConventions.YML_CONFIG_FILE);

        verify(mockConfigurationSupplier, mockFileConfiguration);
    }

    @Test
    public void testGetRawConfigurationWithoutEnvironment() {
        Configuration mockCombinedConfig = createMock(Configuration.class);

        expect(mockConfigurationSupplier.getConfiguration()).andReturn(mockFileConfiguration);
        expect(mockFileConfiguration.isEmpty()).andReturn(false);
        expect(mockFileConfiguration.addAll(anyObject(SystemPropertyConfiguration.class)))
                .andReturn(mockCombinedConfig);

        replay(mockConfigurationSupplier, mockFileConfiguration, mockCombinedConfig);

        Configuration result = factory.getRawConfiguration(mockConfigurationSupplier, null);

        assertNotNull(result);
        assertEquals(result, mockCombinedConfig);

        verify(mockConfigurationSupplier, mockFileConfiguration, mockCombinedConfig);
    }

    @Test
    public void testGetRawConfigurationWithEnvironment() {
        Environment environment = new Environment("dev");
        Configuration mockCombinedConfig = createMock(Configuration.class);

        Map<String, String> configMap = new HashMap<>();
        configMap.put("database.url", "jdbc:postgresql://localhost/db");
        configMap.put("database.user", "user");
        configMap.put("environments.dev.database.url", "jdbc:postgresql://devhost/db");
        configMap.put("environments.dev.database.user", "devuser");
        configMap.put("environments.prod.database.url", "jdbc:postgresql://prodhost/db");
        configMap.put("environments.prod.database.user", "produser");

        expect(mockConfigurationSupplier.getConfiguration()).andReturn(mockFileConfiguration);
        expect(mockFileConfiguration.isEmpty()).andReturn(false);
        expect(mockFileConfiguration.addAll(anyObject(SystemPropertyConfiguration.class)))
                .andReturn(mockCombinedConfig);
        expect(mockCombinedConfig.asMap()).andReturn(Optional.of(configMap));

        replay(mockConfigurationSupplier, mockFileConfiguration, mockCombinedConfig);

        Configuration result = factory.getRawConfiguration(mockConfigurationSupplier, environment);

        assertNotNull(result);
        assertTrue(result instanceof MapConfiguration);

        // Verify that only global and dev environment properties are included
        Optional<Map<String, String>> resultMapOpt = result.asMap();
        assertTrue(resultMapOpt.isPresent());

        Map<String, String> resultMap = resultMapOpt.get();
        assertTrue(resultMap.containsKey("database.url"));
        assertTrue(resultMap.containsKey("database.user"));
        assertTrue(resultMap.containsKey("environments.dev.database.url"));
        assertTrue(resultMap.containsKey("environments.dev.database.user"));
        assertFalse(
                resultMap.containsKey("environments.prod.database.url"),
                "Should not contain prod environment properties");
        assertFalse(
                resultMap.containsKey("environments.prod.database.user"),
                "Should not contain prod environment properties");

        verify(mockConfigurationSupplier, mockFileConfiguration, mockCombinedConfig);
    }

    @Test
    public void testGetRawConfigurationWithEnvironmentNoEnvironmentSpecificConfig() {
        Environment environment = new Environment("staging");
        Configuration mockCombinedConfig = createMock(Configuration.class);

        Map<String, String> configMap = new HashMap<>();
        configMap.put("database.url", "jdbc:postgresql://localhost/db");
        configMap.put("database.user", "user");

        expect(mockConfigurationSupplier.getConfiguration()).andReturn(mockFileConfiguration);
        expect(mockFileConfiguration.isEmpty()).andReturn(false);
        expect(mockFileConfiguration.addAll(anyObject(SystemPropertyConfiguration.class)))
                .andReturn(mockCombinedConfig);
        expect(mockCombinedConfig.asMap()).andReturn(Optional.of(configMap));

        replay(mockConfigurationSupplier, mockFileConfiguration, mockCombinedConfig);

        Configuration result = factory.getRawConfiguration(mockConfigurationSupplier, environment);

        assertNotNull(result);
        assertTrue(result instanceof MapConfiguration);

        // Verify that only global properties are included
        Optional<Map<String, String>> resultMapOpt = result.asMap();
        assertTrue(resultMapOpt.isPresent());

        Map<String, String> resultMap = resultMapOpt.get();
        assertEquals(resultMap.size(), 2);
        assertTrue(resultMap.containsKey("database.url"));
        assertTrue(resultMap.containsKey("database.user"));

        verify(mockConfigurationSupplier, mockFileConfiguration, mockCombinedConfig);
    }

    @Test
    public void testGetRawConfigurationWithEnvironmentEmptyMap() {
        Environment environment = new Environment("dev");
        Configuration mockCombinedConfig = createMock(Configuration.class);

        expect(mockConfigurationSupplier.getConfiguration()).andReturn(mockFileConfiguration);
        expect(mockFileConfiguration.isEmpty()).andReturn(false);
        expect(mockFileConfiguration.addAll(anyObject(SystemPropertyConfiguration.class)))
                .andReturn(mockCombinedConfig);
        expect(mockCombinedConfig.asMap()).andReturn(Optional.empty());

        replay(mockConfigurationSupplier, mockFileConfiguration, mockCombinedConfig);

        Configuration result = factory.getRawConfiguration(mockConfigurationSupplier, environment);

        assertNotNull(result);
        assertTrue(result instanceof MapConfiguration);

        // Verify that the result is empty when asMap returns empty
        assertTrue(result.isEmpty());

        verify(mockConfigurationSupplier, mockFileConfiguration, mockCombinedConfig);
    }

    @Test
    public void testGetRawConfigurationFiltersMultipleEnvironments() {
        Environment environment = new Environment("test");
        Configuration mockCombinedConfig = createMock(Configuration.class);

        Map<String, String> configMap = new HashMap<>();
        configMap.put("app.name", "myapp");
        configMap.put("environments.dev.database.url", "jdbc:dev");
        configMap.put("environments.test.database.url", "jdbc:test");
        configMap.put("environments.test.database.port", "5432");
        configMap.put("environments.prod.database.url", "jdbc:prod");
        configMap.put("environments.staging.database.url", "jdbc:staging");

        expect(mockConfigurationSupplier.getConfiguration()).andReturn(mockFileConfiguration);
        expect(mockFileConfiguration.isEmpty()).andReturn(false);
        expect(mockFileConfiguration.addAll(anyObject(SystemPropertyConfiguration.class)))
                .andReturn(mockCombinedConfig);
        expect(mockCombinedConfig.asMap()).andReturn(Optional.of(configMap));

        replay(mockConfigurationSupplier, mockFileConfiguration, mockCombinedConfig);

        Configuration result = factory.getRawConfiguration(mockConfigurationSupplier, environment);

        assertNotNull(result);
        Optional<Map<String, String>> resultMapOpt = result.asMap();
        assertTrue(resultMapOpt.isPresent());

        Map<String, String> resultMap = resultMapOpt.get();

        // Should contain global and test environment only
        assertTrue(resultMap.containsKey("app.name"));
        assertTrue(resultMap.containsKey("environments.test.database.url"));
        assertTrue(resultMap.containsKey("environments.test.database.port"));

        // Should NOT contain other environments
        assertFalse(resultMap.containsKey("environments.dev.database.url"));
        assertFalse(resultMap.containsKey("environments.prod.database.url"));
        assertFalse(resultMap.containsKey("environments.staging.database.url"));

        assertEquals(resultMap.size(), 3);

        verify(mockConfigurationSupplier, mockFileConfiguration, mockCombinedConfig);
    }
}
