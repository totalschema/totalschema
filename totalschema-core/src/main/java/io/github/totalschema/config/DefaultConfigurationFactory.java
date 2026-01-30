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
import io.github.totalschema.config.environment.Environment;
import io.github.totalschema.spi.config.ConfigurationSupplier;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

public class DefaultConfigurationFactory extends ConfigurationFactory {

    @Override
    public Configuration getRawConfiguration(
            ConfigurationSupplier configurationSupplier, Environment environment) {

        Configuration fileConfiguration = configurationSupplier.getConfiguration();

        if (fileConfiguration.isEmpty()) {
            throw new RuntimeException(
                    "Configuration file is missing or empty: "
                            + ProjectConventions.YML_CONFIG_FILE);
        }

        Configuration configuration = fileConfiguration.addAll(new SystemPropertyConfiguration());

        Configuration appliedConfiguration;

        if (environment == null) {
            appliedConfiguration = configuration;
        } else {
            String environmentSpecificConfigPrefix =
                    String.format(
                            "%s.%s",
                            ProjectConventions.ConfigurationPropertyNames.ENVIRONMENTS,
                            environment.getName());

            Map<String, String> globalAndEnvironmentSpecificConfigMap =
                    configuration
                            .asMap()
                            .map(
                                    map ->
                                            map.entrySet().stream()
                                                    .filter(
                                                            entry ->
                                                                    !entry.getKey()
                                                                                    .startsWith(
                                                                                            ProjectConventions
                                                                                                    .ConfigurationPropertyNames
                                                                                                    .ENVIRONMENTS)
                                                                            || entry.getKey()
                                                                                    .startsWith(
                                                                                            environmentSpecificConfigPrefix))
                                                    .collect(
                                                            Collectors.toMap(
                                                                    Map.Entry::getKey,
                                                                    Map.Entry::getValue)))
                            .orElseGet(Collections::emptyMap);

            appliedConfiguration = new MapConfiguration(globalAndEnvironmentSpecificConfigMap);
        }

        return appliedConfiguration;
    }
}
