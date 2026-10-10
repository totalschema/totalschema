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

package io.github.totalschema.connector.gcp.bigquery.actions;

import com.google.cloud.bigquery.dataexchange.v1beta1.AnalyticsHubServiceClient;
import io.github.totalschema.config.Configuration;

/**
 * Actions that manipulate BigQuery assets via Analytics Hubs APIs.
 *
 * <p>Examples: CreateListing, etc.
 *
 * <p>These actions use the BigQuery Analytics Hub API to manage data sharing and discovery through
 * data exchanges and listings.
 *
 * <p>Analytics Hub listing operations typically include:
 *
 * <ul>
 *   <li>Creating listings to share datasets via data exchanges
 *   <li>Updating listing metadata (description, categories, documentation)
 *   <li>Deleting listings
 *   <li>Managing subscriber access and restrictions
 * </ul>
 *
 * <p><strong>Note:</strong> Analytics Hub is a separate API from BigQuery's core API. These actions
 * manage the data sharing and discovery layer, enabling organizations to publish and subscribe to
 * curated datasets.
 *
 * <p><strong>Use Case:</strong> Analytics Hub listings are typically used for:
 *
 * <ul>
 *   <li>Sharing datasets across GCP projects within an organization
 *   <li>Publishing public or commercial datasets to external consumers
 *   <li>Creating data marketplaces and data exchange ecosystems
 *   <li>Programmatic data product management in SaaS/multi-tenant environments
 * </ul>
 */
public interface AnalyticsHubServiceClientAction extends Action<AnalyticsHubServiceClient> {
    /**
     * Executes the Analytics Hub listing action with type-safe client.
     *
     * @param client the Analytics Hub Service client
     * @param connectorConfiguration the connector-level configuration providing defaults
     */
    void execute(AnalyticsHubServiceClient client, Configuration connectorConfiguration);
}
