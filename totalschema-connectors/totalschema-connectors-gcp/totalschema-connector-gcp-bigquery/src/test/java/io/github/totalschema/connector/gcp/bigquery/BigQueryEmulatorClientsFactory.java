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

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.http.HttpTransportOptions;

/**
 * Factory class for creating BigQuery client instances configured for the BigQuery emulator.
 *
 * <p>This factory encapsulates the common configuration logic required to create BigQuery clients
 * that connect to the emulator instead of production GCP. All clients created by this factory are
 * configured with:
 *
 * <ul>
 *   <li>Uses a dummy credential because the BigQuery client validates credentials even when
 *       connecting to the emulator
 *   <li>HTTP connections (not HTTPS)
 *   <li>Custom emulator endpoint
 * </ul>
 *
 * <p><strong>Usage Example:</strong>
 *
 * <pre>{@code
 * // Create a BigQuery client for the emulator
 * BigQuery bigQuery = BigQueryEmulatorClientsFactory.createBigQueryClient(
 *     "test-project",
 *     "http://localhost:9050"
 * );
 * }</pre>
 *
 * <p><strong>Thread Safety:</strong> This class is stateless and thread-safe. All methods are
 * static and can be called concurrently from multiple threads.
 *
 * <p><strong>Note:</strong> This factory is intended for testing purposes only. Production code
 * should use properly configured clients with authentication and HTTPS.
 *
 * @see BigQuery
 */
public final class BigQueryEmulatorClientsFactory {

    /** Private constructor to prevent instantiation of this utility class. */
    private BigQueryEmulatorClientsFactory() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    /**
     * Creates a {@link BigQuery} client configured to connect to the BigQuery emulator.
     *
     * <p>The BigQuery client can be used for:
     *
     * <ul>
     *   <li>Creating and deleting datasets
     *   <li>Creating and deleting tables
     *   <li>Executing queries
     *   <li>Loading and exporting data
     *   <li>Managing jobs
     * </ul>
     *
     * <p>The returned client uses a dummy credential and HTTP (non-HTTPS) connections, suitable for
     * connecting to the BigQuery emulator running in a test container.
     *
     * <p><strong>Important:</strong> The caller is responsible for closing the returned client when
     * done to free up resources.
     *
     * @param projectId the GCP project ID (can be any string for emulator testing)
     * @param emulatorEndpoint the emulator HTTP endpoint (e.g., "http://localhost:9050")
     * @return a fully configured {@link BigQuery} client ready to use
     * @see BigQuery
     */
    public static BigQuery createBigQueryClient(String projectId, String emulatorEndpoint) {
        // Configure HTTP transport options
        HttpTransportOptions transportOptions =
                HttpTransportOptions.newBuilder()
                        .setConnectTimeout(60000)
                        .setReadTimeout(60000)
                        .build();

        // Build BigQuery options pointing to emulator
        BigQueryOptions options =
                BigQueryOptions.newBuilder()
                        .setProjectId(projectId)
                        .setHost(emulatorEndpoint)
                        .setTransportOptions(transportOptions)
                        .setCredentials(GoogleCredentials.create(new AccessToken("unused", null)))
                        .build();

        return options.getService();
    }
}
