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

import com.google.cloud.bigtable.admin.v2.BigtableTableAdminClient;
import com.google.cloud.bigtable.data.v2.BigtableDataClient;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * Abstract base class for BigTable table-level integration tests.
 *
 * <p>This class extends {@link EmulatorIntegrationTestBase} and creates table admin and data
 * clients for table-level operations (creating tables, column families, reading/writing data,
 * etc.).
 *
 * <p>Subclasses inherit fully configured table admin and data clients for testing against the
 * BigTable emulator.
 */
public abstract class AbstractBigtableAdminIntegrationTest extends EmulatorIntegrationTestBase {

    protected BigtableTableAdminClient adminClient;
    protected BigtableDataClient dataClient;

    /**
     * Creates table admin and data clients for table-level operations.
     *
     * @param projectId the test project ID
     * @param instanceId the test instance ID
     * @param emulatorHost the emulator host:port endpoint
     * @throws IOException if client creation fails
     */
    @Override
    protected void createClients(String projectId, String instanceId, String emulatorHost)
            throws IOException {

        adminClient =
                EmulatorClientsFactory.createTableAdminClient(projectId, instanceId, emulatorHost);

        dataClient = EmulatorClientsFactory.createDataClient(projectId, instanceId, emulatorHost);
    }

    /**
     * Returns the list of resources to tear down in the cleanup phase.
     *
     * <p>This implementation returns the table admin client, data client, and the container.
     *
     * @return list of AutoCloseable resources to close during tearDown
     */
    @Override
    protected List<AutoCloseable> getResourcesToTearDown() {
        return new LinkedList<>(List.of(adminClient, dataClient, bigtableContainer));
    }
}
