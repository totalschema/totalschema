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

package io.github.totalschema.cli.secret;

import io.github.totalschema.ProjectConventions;
import io.github.totalschema.cli.SecretManagerServiceCliCommand;
import io.github.totalschema.spi.secrets.SecretsManager;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import picocli.CommandLine;

@CommandLine.Command(
        name = "set",
        mixinStandardHelpOptions = true,
        description =
                "encrypts a value and it to the YAML configuration file with the specified key")
public class SecretsSetCliCommand extends SecretManagerServiceCliCommand {

    @CommandLine.Option(
            names = {"--key"},
            required = true,
            description =
                    "The key of the YAML entry to create (supports dot notation for nested keys, e.g., environments.DEV.variables.dbPassword)")
    protected String key;

    @CommandLine.Option(
            names = {"--value"},
            required = true,
            description = "The clear text value for secret manager to encrypt")
    protected String clearTextValue;

    @Override
    protected void run(SecretsManager secretsManager) {

        String cipherText = secretsManager.encode(clearTextValue);

        Path yamlFile = Paths.get(ProjectConventions.YML_CONFIG_FILE);

        // Read existing YAML file
        Map<String, Object> yamlData = new LinkedHashMap<>();

        if (Files.exists(yamlFile)) {
            try (InputStream is = Files.newInputStream(yamlFile)) {
                Yaml yaml = new Yaml();
                Object loaded = yaml.load(is);
                if (loaded instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> loadedMap = (Map<String, Object>) loaded;
                    yamlData.putAll(loadedMap);
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to read YAML file: " + yamlFile, e);
            }
        }

        String valueWithLookup = String.format("${secret:%s}", cipherText);

        setNestedValue(yamlData, key, valueWithLookup);

        // Write back to YAML file
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        Yaml yaml = new Yaml(options);

        try (Writer writer = Files.newBufferedWriter(yamlFile)) {
            yaml.dump(yamlData, writer);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write YAML file: " + yamlFile, e);
        }

        System.out.format(
                "Added new secret entry with key '%s' to %s%n", key, yamlFile.toAbsolutePath());
    }

    /**
     * Sets a nested value in the YAML data using dot notation. For example:
     * "environments.DEV.variables.dbPassword" will create the nested structure and set the value at
     * the appropriate location.
     */
    private void setNestedValue(Map<String, Object> data, String key, String value) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Key cannot be null or empty");
        }

        String[] parts = key.split("\\.");
        Map<String, Object> current = data;

        // Navigate/create the nested structure
        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];
            Object next = current.get(part);

            if (next instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nextMap = (Map<String, Object>) next;
                current = nextMap;
            } else {
                // Create a new nested map
                Map<String, Object> newMap = new LinkedHashMap<>();
                current.put(part, newMap);
                current = newMap;
            }
        }

        // Set the final value
        current.put(parts[parts.length - 1], value);
    }
}
