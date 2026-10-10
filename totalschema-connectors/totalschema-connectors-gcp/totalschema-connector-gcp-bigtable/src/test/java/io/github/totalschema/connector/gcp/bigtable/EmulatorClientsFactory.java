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

import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.InstantiatingGrpcChannelProvider;
import com.google.cloud.bigtable.admin.v2.BigtableInstanceAdminClient;
import com.google.cloud.bigtable.admin.v2.BigtableInstanceAdminSettings;
import com.google.cloud.bigtable.admin.v2.BigtableTableAdminClient;
import com.google.cloud.bigtable.admin.v2.BigtableTableAdminSettings;
import com.google.cloud.bigtable.data.v2.BigtableDataClient;
import com.google.cloud.bigtable.data.v2.BigtableDataSettings;
import java.io.IOException;

/**
 * Factory class for creating BigTable client instances configured for the BigTable emulator.
 *
 * <p>This factory encapsulates the common configuration logic required to create BigTable clients
 * that connect to the emulator instead of production GCP. All clients created by this factory are
 * configured with:
 *
 * <ul>
 *   <li>No credentials (using {@link NoCredentialsProvider})
 *   <li>Plain text gRPC connections (not TLS)
 *   <li>Custom emulator endpoint
 * </ul>
 *
 * <p><strong>Usage Example:</strong>
 *
 * <pre>{@code
 * // Create a table admin client for managing tables
 * BigtableTableAdminClient tableAdmin = EmulatorClientsFactory.createTableAdminClient(
 *     "test-project",
 *     "test-instance",
 *     "localhost:9060"
 * );
 *
 * // Create an instance admin client for managing clusters and instances
 * BigtableInstanceAdminClient instanceAdmin = EmulatorClientsFactory.createInstanceAdminClient(
 *     "test-project",
 *     "localhost:9060"
 * );
 *
 * // Create a data client for reading/writing data
 * BigtableDataClient dataClient = EmulatorClientsFactory.createDataClient(
 *     "test-project",
 *     "test-instance",
 *     "localhost:9060"
 * );
 * }</pre>
 *
 * <p><strong>Thread Safety:</strong> This class is stateless and thread-safe. All methods are
 * static and can be called concurrently from multiple threads.
 *
 * <p><strong>Note:</strong> This factory is intended for testing purposes only. Production code
 * should use properly configured clients with authentication and TLS.
 *
 * @see BigtableTableAdminClient
 * @see BigtableDataClient
 * @see BigtableInstanceAdminClient
 */
public final class EmulatorClientsFactory {

    /** Private constructor to prevent instantiation of this utility class. */
    private EmulatorClientsFactory() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    /**
     * Creates a {@link BigtableTableAdminClient} configured to connect to the BigTable emulator.
     *
     * <p>The table admin client is used for table-level administrative operations such as:
     *
     * <ul>
     *   <li>Creating and deleting tables
     *   <li>Managing column families
     *   <li>Modifying table schemas
     *   <li>Managing backups
     *   <li>Configuring garbage collection policies
     * </ul>
     *
     * <p>The returned client is configured with no authentication and uses plain text (non-TLS)
     * connections, suitable for connecting to the BigTable emulator running in a test container.
     *
     * <p><strong>Important:</strong> The caller is responsible for closing the returned client when
     * done to free up resources. Consider using try-with-resources:
     *
     * <pre>{@code
     * try (BigtableTableAdminClient client = EmulatorClientsFactory.createTableAdminClient(...)) {
     *     // Use the client
     * }
     * }</pre>
     *
     * @param projectId the GCP project ID (can be any string for emulator testing)
     * @param instanceId the BigTable instance ID (must match the instance in the emulator)
     * @param emulatorHost the emulator endpoint in "host:port" format (e.g., "localhost:9060")
     * @return a fully configured {@link BigtableTableAdminClient} ready to use
     * @throws IOException if the client cannot be created due to configuration errors
     * @see BigtableTableAdminClient
     */
    public static BigtableTableAdminClient createTableAdminClient(
            String projectId, String instanceId, String emulatorHost) throws IOException {
        BigtableTableAdminSettings.Builder adminSettingsBuilder =
                BigtableTableAdminSettings.newBuilder()
                        .setProjectId(projectId)
                        .setInstanceId(instanceId)
                        .setCredentialsProvider(NoCredentialsProvider.create());

        adminSettingsBuilder
                .stubSettings()
                .setEndpoint(emulatorHost)
                .setTransportChannelProvider(
                        InstantiatingGrpcChannelProvider.newBuilder()
                                .setEndpoint(emulatorHost)
                                .setChannelConfigurator(io.grpc.ManagedChannelBuilder::usePlaintext)
                                .build());

        BigtableTableAdminSettings adminSettings = adminSettingsBuilder.build();
        return BigtableTableAdminClient.create(adminSettings);
    }

    /**
     * Creates a {@link BigtableDataClient} configured to connect to the BigTable emulator.
     *
     * <p>The data client is used for reading and writing data operations such as:
     *
     * <ul>
     *   <li>Reading rows with filters
     *   <li>Writing single rows or batches
     *   <li>Scanning ranges of rows
     *   <li>Performing mutations
     *   <li>Checking row existence
     * </ul>
     *
     * <p>The returned client is configured with no authentication and uses plain text (non-TLS)
     * connections, suitable for connecting to the BigTable emulator running in a test container.
     *
     * <p><strong>Important:</strong> The caller is responsible for closing the returned client when
     * done to free up resources. Consider using try-with-resources:
     *
     * <pre>{@code
     * try (BigtableDataClient client = EmulatorClientsFactory.createDataClient(...)) {
     *     // Use the client
     * }
     * }</pre>
     *
     * @param projectId the GCP project ID (can be any string for emulator testing)
     * @param instanceId the BigTable instance ID (must match the instance in the emulator)
     * @param emulatorHost the emulator endpoint in "host:port" format (e.g., "localhost:9060")
     * @return a fully configured {@link BigtableDataClient} ready to use
     * @throws IOException if the client cannot be created due to configuration errors
     * @see BigtableDataClient
     */
    public static BigtableDataClient createDataClient(
            String projectId, String instanceId, String emulatorHost) throws IOException {
        BigtableDataSettings.Builder dataSettingsBuilder =
                BigtableDataSettings.newBuilder()
                        .setProjectId(projectId)
                        .setInstanceId(instanceId)
                        .setCredentialsProvider(NoCredentialsProvider.create());

        dataSettingsBuilder
                .stubSettings()
                .setEndpoint(emulatorHost)
                .setTransportChannelProvider(
                        InstantiatingGrpcChannelProvider.newBuilder()
                                .setEndpoint(emulatorHost)
                                .setChannelConfigurator(io.grpc.ManagedChannelBuilder::usePlaintext)
                                .build());

        BigtableDataSettings dataSettings = dataSettingsBuilder.build();
        return BigtableDataClient.create(dataSettings);
    }

    /**
     * Creates a {@link BigtableInstanceAdminClient} configured to connect to the BigTable emulator.
     *
     * <p>The instance admin client is used for instance-level administrative operations such as:
     *
     * <ul>
     *   <li>Creating and deleting instances
     *   <li>Managing clusters within instances
     *   <li>Configuring app profiles
     *   <li>Managing IAM policies
     *   <li>Configuring autoscaling and replication
     * </ul>
     *
     * <p>The returned client is configured with no authentication and uses plain text (non-TLS)
     * connections, suitable for connecting to the BigTable emulator running in a test container.
     *
     * <p><strong>Note:</strong> This method does not require an {@code instanceId} parameter
     * because instance admin operations can span multiple instances within a project.
     *
     * <p><strong>Important:</strong> The caller is responsible for closing the returned client when
     * done to free up resources. Consider using try-with-resources:
     *
     * <pre>{@code
     * try (BigtableInstanceAdminClient client = EmulatorClientsFactory.createInstanceAdminClient(...)) {
     *     // Use the client
     * }
     * }</pre>
     *
     * @param projectId the GCP project ID (can be any string for emulator testing)
     * @param emulatorHost the emulator endpoint in "host:port" format (e.g., "localhost:9060")
     * @return a fully configured {@link BigtableInstanceAdminClient} ready to use
     * @throws IOException if the client cannot be created due to configuration errors
     * @see BigtableInstanceAdminClient
     */
    public static BigtableInstanceAdminClient createInstanceAdminClient(
            String projectId, String emulatorHost) throws IOException {
        BigtableInstanceAdminSettings.Builder instanceAdminSettingsBuilder =
                BigtableInstanceAdminSettings.newBuilder()
                        .setProjectId(projectId)
                        .setCredentialsProvider(NoCredentialsProvider.create());

        instanceAdminSettingsBuilder
                .stubSettings()
                .setEndpoint(emulatorHost)
                .setTransportChannelProvider(
                        InstantiatingGrpcChannelProvider.newBuilder()
                                .setEndpoint(emulatorHost)
                                .setChannelConfigurator(io.grpc.ManagedChannelBuilder::usePlaintext)
                                .build());

        BigtableInstanceAdminSettings instanceAdminSettings = instanceAdminSettingsBuilder.build();
        return BigtableInstanceAdminClient.create(instanceAdminSettings);
    }
}
