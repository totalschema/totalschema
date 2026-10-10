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

import com.google.cloud.bigtable.admin.v2.BigtableInstanceAdminClient;
import com.google.cloud.bigtable.admin.v2.BigtableInstanceAdminSettings;
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
 * Factory for creating {@link BigtableInstanceAdminClient} instances.
 *
 * <p>This client is used for instance-level operations such as creating/deleting instances,
 * managing clusters, configuring replication, and autoscaling.
 *
 * <p>Configuration requirements:
 *
 * <ul>
 *   <li>{@code projectId} - GCP project ID (required)
 *   <li>{@code instanceId} - Not required for instance creation/deletion
 * </ul>
 */
public class BigtableInstanceAdminClientComponentFactory
        extends ComponentFactory<BigtableInstanceAdminClient> {

    private static final ArgumentSpecification<Configuration> CONFIGURATION =
            configuration("configuration");

    private static final ArgumentHandler ARGUMENTS =
            ArgumentHandler.getInstance(
                    BigtableInstanceAdminClientComponentFactory.class, CONFIGURATION);

    @Override
    public boolean isLazy() {
        return false;
    }

    @Override
    public Class<BigtableInstanceAdminClient> getComponentType() {
        return BigtableInstanceAdminClient.class;
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
    public BigtableInstanceAdminClient createComponent(Context context, List<Object> arguments) {

        try {
            ARGUMENTS.validateStructure(arguments);

            Configuration configuration = ARGUMENTS.getArgument(CONFIGURATION, arguments);

            BigtableInstanceAdminSettings.Builder settingBuilder =
                    BigtableInstanceAdminSettings.newBuilder();

            ConfigurationValueExtractor extractor = new ConfigurationValueExtractor(configuration);

            // Only projectId is required for instance admin operations
            extractor.extractRequired("projectId", settingBuilder::setProjectId);

            // Optional: custom admin endpoint for Private Service Connection or testing
            // Default: bigtableadmin.googleapis.com
            extractor.extractOptional(
                    "adminEndpoint",
                    endpoint -> settingBuilder.stubSettings().setEndpoint(endpoint));

            return BigtableInstanceAdminClient.create(settingBuilder.build());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
