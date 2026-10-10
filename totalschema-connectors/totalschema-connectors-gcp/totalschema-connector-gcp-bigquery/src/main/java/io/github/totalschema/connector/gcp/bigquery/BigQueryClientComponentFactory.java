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

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.http.HttpTransportOptions;
import io.github.totalschema.config.Configuration;
import io.github.totalschema.config.ConfigurationValueExtractor;
import io.github.totalschema.engine.api.Context;
import io.github.totalschema.spi.factory.ArgumentHandler;
import io.github.totalschema.spi.factory.ArgumentSpecification;
import io.github.totalschema.spi.factory.ComponentFactory;
import java.util.List;
import java.util.Optional;

/**
 * Component factory for creating {@link BigQuery} client instances.
 *
 * <p>Configuration properties:
 *
 * <ul>
 *   <li>{@code projectId} - GCP project ID (required, from root config)
 *   <li>{@code bigquery.location} - Default location for datasets and queries (optional, default:
 *       US)
 *   <li>{@code bigquery.endpoint} - Custom BigQuery API endpoint (optional, default:
 *       bigquery.googleapis.com)
 *   <li>{@code bigquery.apiKey} - API key for authentication (optional, alternative to credentials)
 *   <li>{@code bigquery.quotaProjectId} - Project ID for quota and billing (optional)
 *   <li>{@code bigquery.universeDomain} - Universe Domain to configure the resolved endpoint
 *       (optional)
 *   <li>{@code bigquery.gdchApiAudience} - GDC-H api audience (optional)
 *   <li>{@code bigquery.connectTimeout} - HTTP connection timeout in milliseconds (optional)
 *   <li>{@code bigquery.readTimeout} - HTTP read timeout in milliseconds (optional)
 * </ul>
 */
public final class BigQueryClientComponentFactory extends ComponentFactory<BigQuery> {

    private static final ArgumentSpecification<Configuration> CONFIGURATION =
            configuration("configuration");

    private static final ArgumentHandler ARGUMENTS =
            ArgumentHandler.getInstance(BigQueryClientComponentFactory.class, CONFIGURATION);

    @Override
    public boolean isLazy() {
        return false;
    }

    @Override
    public Class<BigQuery> getComponentType() {
        return BigQuery.class;
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
    public BigQuery createComponent(Context context, List<Object> arguments) {

        ARGUMENTS.validateStructure(arguments);

        Configuration configuration = ARGUMENTS.getArgument(CONFIGURATION, arguments);

        BigQueryOptions.Builder optionsBuilder = BigQueryOptions.newBuilder();

        ConfigurationValueExtractor config = new ConfigurationValueExtractor(configuration);

        // Required: projectId from root config
        config.extractRequired("projectId", optionsBuilder::setProjectId);

        // Optional: location (default location for datasets and queries)
        config.extractOptional("bigquery.location", optionsBuilder::setLocation);

        // Optional: endpoint (custom endpoint for emulator or private API)
        config.extractOptional("bigquery.endpoint", optionsBuilder::setHost);

        // Optional: quotaProjectId (for quota and billing)
        config.extractOptional("bigquery.quotaProjectId", optionsBuilder::setQuotaProjectId);

        // Optional: universeDomain (Universe Domain to configure the resolved endpoint)
        config.extractOptional("bigquery.universeDomain", optionsBuilder::setUniverseDomain);

        // Optional: connectTimeout and readTimeout
        // Build transport options if either timeout is specified
        if (config.hasKey("bigquery.connectTimeout") || config.hasKey("bigquery.readTimeout")) {
            HttpTransportOptions.Builder transportBuilder = HttpTransportOptions.newBuilder();

            config.extractOptionalInt(
                    "bigquery.connectTimeout", transportBuilder::setConnectTimeout);
            config.extractOptionalInt("bigquery.readTimeout", transportBuilder::setReadTimeout);

            optionsBuilder.setTransportOptions(transportBuilder.build());
        }

        return optionsBuilder.build().getService();
    }
}
