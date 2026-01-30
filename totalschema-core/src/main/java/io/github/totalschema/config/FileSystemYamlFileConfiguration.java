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

import io.github.totalschema.ProjectConventions;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.function.Supplier;

/** Factory class for creating YamlFileConfiguration instances that read from the filesystem. */
public final class FileSystemYamlFileConfiguration {

    private FileSystemYamlFileConfiguration() {
        // Utility class - prevent instantiation
    }

    public static YamlFileConfiguration create() {
        return create(ProjectConventions.YML_CONFIG_FILE);
    }

    /**
     * Creates a YamlFileConfiguration that reads from a file on the filesystem.
     *
     * @param yamlFileName the path to the YAML file
     * @return a YamlFileConfiguration configured to read from the filesystem
     */
    public static YamlFileConfiguration create(String yamlFileName) {
        Supplier<Optional<java.io.InputStream>> provider =
                () -> {
                    Path path = Paths.get(yamlFileName);
                    try {
                        if (Files.exists(path)) {
                            return Optional.of(Files.newInputStream(path));
                        } else {
                            return Optional.empty();
                        }
                    } catch (java.io.IOException e) {
                        throw new RuntimeException("Failed to open file: " + yamlFileName, e);
                    }
                };

        String description = String.format("YAML file: %s", yamlFileName);
        return new YamlFileConfiguration(provider, description);
    }
}
