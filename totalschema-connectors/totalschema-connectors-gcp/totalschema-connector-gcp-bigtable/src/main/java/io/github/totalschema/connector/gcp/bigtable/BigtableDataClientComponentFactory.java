/*
 * totalschema: tool for managing database versioning and schema changes with ease.
 * Copyright (C) 2025-2026 totalschema development team
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

package io.github.totalschema.connector.gcp.bigtable;

import static io.github.totalschema.spi.factory.ArgumentSpecification.*;

import com.google.cloud.bigtable.data.v2.BigtableDataClient;
import com.google.cloud.bigtable.data.v2.BigtableDataSettings;
import io.github.totalschema.config.Configuration;
import io.github.totalschema.config.ConfigurationValueExtractor;
import io.github.totalschema.engine.api.Context;
import io.github.totalschema.spi.factory.ArgumentHandler;
import io.github.totalschema.spi.factory.ArgumentSpecification;
import io.github.totalschema.spi.factory.ComponentFactory;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Factory for creating {@link BigtableDataClient} instances.
 *
 * <p>This client is used for data-plane operations such as reading and writing data to Bigtable
 * tables.
 *
 * <p>Configuration requirements:
 *
 * <ul>
 *   <li>{@code projectId} - GCP project ID (required)
 *   <li>{@code instanceId} - Bigtable instance ID (required)
 * </ul>
 *
 * <p>Optional configuration:
 *
 * <ul>
 *   <li>{@code dataEndpoint} - Custom data endpoint for Private Service Connection or testing
 *       (default: bigtable.googleapis.com)
 *   <li>{@code appProfileId} - App profile ID for routing requests (default: "default")
 * </ul>
 */
public class BigtableDataClientComponentFactory extends ComponentFactory<BigtableDataClient> {

    private static final ArgumentSpecification<Configuration> CONFIGURATION =
            configuration("configuration");

    private static final ArgumentHandler ARGUMENTS =
            ArgumentHandler.getInstance(BigtableDataClientComponentFactory.class, CONFIGURATION);

    @Override
    public boolean isLazy() {
        return false;
    }

    @Override
    public Class<BigtableDataClient> getComponentType() {
        return BigtableDataClient.class;
    }

    @Override
    public Optional<String> getQualifier() {
        return Optional.empty();
    }

    @Override
    public List<Class<?>> getDependencies() {
        return List.of();
    }

    @Override
    public List<ArgumentSpecification<?>> getArgumentSpecifications() {
        return ARGUMENTS.getSpecifications();
    }

    @Override
    public BigtableDataClient createComponent(Context context, List<Object> arguments) {

        try {
            ARGUMENTS.validateStructure(arguments);

            Configuration configuration = ARGUMENTS.getArgument(CONFIGURATION, arguments);

            BigtableDataSettings.Builder settingBuilder = BigtableDataSettings.newBuilder();

            ConfigurationValueExtractor extractor = new ConfigurationValueExtractor(configuration);

            // Required configuration
            extractor.extractRequired("projectId", settingBuilder::setProjectId);
            extractor.extractRequired("instanceId", settingBuilder::setInstanceId);

            // Optional: app profile ID for routing requests
            // Default: "default"
            extractor.extractOptional("appProfileId", settingBuilder::setAppProfileId);

            // Optional: custom data endpoint for Private Service Connection or testing
            // Default: bigtable.googleapis.com
            extractor.extractOptional(
                    "dataEndpoint",
                    endpoint -> settingBuilder.stubSettings().setEndpoint(endpoint));

            return BigtableDataClient.create(settingBuilder.build());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
