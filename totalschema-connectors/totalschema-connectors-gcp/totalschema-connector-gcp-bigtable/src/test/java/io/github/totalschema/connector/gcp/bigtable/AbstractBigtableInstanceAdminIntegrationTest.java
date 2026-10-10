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

import com.google.cloud.bigtable.admin.v2.BigtableInstanceAdminClient;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * Abstract base class for BigTable instance-level integration tests.
 *
 * <p>This class extends {@link EmulatorIntegrationTestBase} and creates an instance admin client
 * for instance-level operations (cluster management, app profile configuration, etc.).
 *
 * <p>Subclasses inherit a fully configured instance admin client for testing against the BigTable
 * emulator.
 *
 * <p><strong>Note:</strong> The BigTable emulator has limitations with cluster-level operations.
 * Some instance-level actions may not fully replicate production behavior in the emulator.
 */
public abstract class AbstractBigtableInstanceAdminIntegrationTest
        extends EmulatorIntegrationTestBase {

    protected BigtableInstanceAdminClient instanceAdminClient;

    /**
     * Creates instance admin client for instance-level operations.
     *
     * @param projectId the test project ID
     * @param instanceId the test instance ID
     * @param emulatorHost the emulator host:port endpoint
     * @throws IOException if client creation fails
     */
    @Override
    protected void createClients(String projectId, String instanceId, String emulatorHost)
            throws IOException {
        instanceAdminClient =
                EmulatorClientsFactory.createInstanceAdminClient(projectId, emulatorHost);
    }

    /**
     * Returns the list of resources to tear down in the cleanup phase.
     *
     * <p>This implementation returns the instance admin client and the container.
     *
     * @return list of AutoCloseable resources to close during tearDown
     */
    @Override
    protected LinkedList<AutoCloseable> getResourcesToTearDown() {
        return new LinkedList<>(List.of(instanceAdminClient, bigtableContainer));
    }
}
