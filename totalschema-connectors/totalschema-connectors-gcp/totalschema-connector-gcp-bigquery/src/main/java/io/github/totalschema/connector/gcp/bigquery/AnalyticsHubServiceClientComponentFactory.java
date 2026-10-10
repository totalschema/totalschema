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

import com.google.cloud.bigquery.dataexchange.v1beta1.AnalyticsHubServiceClient;
import com.google.cloud.bigquery.dataexchange.v1beta1.AnalyticsHubServiceSettings;
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
 * Component factory for creating {@link AnalyticsHubServiceClient} instances.
 *
 * <p>This client is used for managing BigQuery Analytics Hub listings and data exchanges through
 * the Analytics Hub API.
 *
 * <p>Configuration properties:
 *
 * <ul>
 *   <li>{@code analyticsHub.endpoint} - Custom Analytics Hub API endpoint (optional)
 *   <li>{@code analyticsHub.apiKey} - API key for authentication (optional, alternative to
 *       credentials)
 *   <li>{@code analyticsHub.quotaProjectId} - Project ID for quota and billing (optional)
 *   <li>{@code analyticsHub.universeDomain} - Universe Domain to configure the resolved endpoint
 *       (optional)
 *   <li>{@code analyticsHub.gdchApiAudience} - GDC-H api audience (optional)
 * </ul>
 */
public final class AnalyticsHubServiceClientComponentFactory
        extends ComponentFactory<AnalyticsHubServiceClient> {

    private static final ArgumentSpecification<Configuration> CONFIGURATION =
            configuration("configuration");

    private static final ArgumentHandler ARGUMENTS =
            ArgumentHandler.getInstance(
                    AnalyticsHubServiceClientComponentFactory.class, CONFIGURATION);

    @Override
    public boolean isLazy() {
        return false;
    }

    @Override
    public Class<AnalyticsHubServiceClient> getComponentType() {
        return AnalyticsHubServiceClient.class;
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
    public AnalyticsHubServiceClient createComponent(Context context, List<Object> arguments) {

        ARGUMENTS.validateStructure(arguments);

        Configuration configuration = ARGUMENTS.getArgument(CONFIGURATION, arguments);

        try {
            ConfigurationValueExtractor config = new ConfigurationValueExtractor(configuration);

            AnalyticsHubServiceSettings.Builder settingsBuilder =
                    AnalyticsHubServiceSettings.newBuilder();

            // Optional: endpoint (custom Analytics Hub API endpoint)
            config.extractOptional("analyticsHub.endpoint", settingsBuilder::setEndpoint);

            // Optional: apiKey (API key authentication)
            config.extractOptional("analyticsHub.apiKey", settingsBuilder::setApiKey);

            // Optional: quotaProjectId (for quota and billing)
            config.extractOptional(
                    "analyticsHub.quotaProjectId", settingsBuilder::setQuotaProjectId);

            // Optional: universeDomain (Universe Domain to configure the resolved endpoint)
            config.extractOptional(
                    "analyticsHub.universeDomain", settingsBuilder::setUniverseDomain);

            // Optional: gdchApiAudience (GDC-H api audience)
            config.extractOptional(
                    "analyticsHub.gdchApiAudience", settingsBuilder::setGdchApiAudience);

            return AnalyticsHubServiceClient.create(settingsBuilder.build());

        } catch (IOException e) {
            throw new BigQueryConnectorException("Failed to create AnalyticsHubServiceClient", e);
        }
    }
}
