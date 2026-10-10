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

package io.github.totalschema.connector.gcp.bigquery;

import static io.github.totalschema.spi.factory.ArgumentSpecification.*;

import com.google.cloud.bigquery.datatransfer.v1.DataTransferServiceClient;
import com.google.cloud.bigquery.datatransfer.v1.DataTransferServiceSettings;
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
 * Component factory for creating {@link DataTransferServiceClient} instances.
 *
 * <p>This client is used for managing BigQuery scheduled queries through the Data Transfer Service
 * API.
 *
 * <p>Configuration properties:
 *
 * <ul>
 *   <li>{@code dataTransferService.endpoint} - Custom Data Transfer Service API endpoint (optional)
 *   <li>{@code dataTransferService.apiKey} - API key for authentication (optional, alternative to
 *       credentials)
 *   <li>{@code dataTransferService.quotaProjectId} - Project ID for quota and billing (optional)
 *   <li>{@code dataTransferService.universeDomain} - Universe Domain to configure the resolved
 *       endpoint (optional)
 *   <li>{@code dataTransferService.gdchApiAudience} - GDC-H api audience (optional)
 * </ul>
 */
public final class DataTransferServiceClientComponentFactory
        extends ComponentFactory<DataTransferServiceClient> {

    private static final ArgumentSpecification<Configuration> CONFIGURATION =
            configuration("configuration");

    private static final ArgumentHandler ARGUMENTS =
            ArgumentHandler.getInstance(
                    DataTransferServiceClientComponentFactory.class, CONFIGURATION);

    @Override
    public boolean isLazy() {
        return false;
    }

    @Override
    public Class<DataTransferServiceClient> getComponentType() {
        return DataTransferServiceClient.class;
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
    public DataTransferServiceClient createComponent(Context context, List<Object> arguments) {

        ARGUMENTS.validateStructure(arguments);

        Configuration configuration = ARGUMENTS.getArgument(CONFIGURATION, arguments);

        try {
            // Note: DataTransferServiceClient.create() uses application default credentials
            // and does not require explicit projectId configuration unlike BigQueryClient.

            ConfigurationValueExtractor config = new ConfigurationValueExtractor(configuration);

            DataTransferServiceSettings.Builder settingsBuilder =
                    DataTransferServiceSettings.newBuilder();

            // Optional: endpoint (custom Data Transfer Service API endpoint)
            config.extractOptional("dataTransferService.endpoint", settingsBuilder::setEndpoint);

            // Optional: apiKey (API key authentication)
            config.extractOptional("dataTransferService.apiKey", settingsBuilder::setApiKey);

            // Optional: quotaProjectId (for quota and billing)
            config.extractOptional(
                    "dataTransferService.quotaProjectId", settingsBuilder::setQuotaProjectId);

            // Optional: universeDomain (Universe Domain to configure the resolved endpoint)
            config.extractOptional(
                    "dataTransferService.universeDomain", settingsBuilder::setUniverseDomain);

            // Optional: gdchApiAudience (GDC-H api audience)
            config.extractOptional(
                    "dataTransferService.gdchApiAudience", settingsBuilder::setGdchApiAudience);

            return DataTransferServiceClient.create(settingsBuilder.build());

        } catch (IOException e) {
            throw new BigQueryConnectorException("Failed to create DataTransferServiceClient", e);
        }
    }
}
