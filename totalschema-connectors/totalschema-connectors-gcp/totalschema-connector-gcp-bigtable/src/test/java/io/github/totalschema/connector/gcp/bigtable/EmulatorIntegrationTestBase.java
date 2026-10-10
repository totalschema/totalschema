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

import java.io.IOException;
import java.util.List;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

/**
 * Abstract base class for BigTable integration tests using the BigTable emulator in a
 * TestContainer.
 *
 * <p>This class is responsible for bootstrapping and managing the BigTable emulator container.
 * Subclasses must implement template methods to create their specific clients and declare which
 * resources need to be cleaned up.
 *
 * <p>Tests are automatically skipped if Docker/Podman is not available.
 */
public abstract class EmulatorIntegrationTestBase {

    private static final int BIGTABLE_EMULATOR_PORT = 9060;
    private static final String BIGTABLE_EMULATOR_IMAGE = "google/cloud-sdk:emulators";
    protected static final String TEST_PROJECT_ID = "test-project";
    protected static final String TEST_INSTANCE_ID = "test-instance";

    protected GenericContainer<?> bigtableContainer;

    /**
     * Set up the BigTable emulator container and create clients.
     *
     * @throws IOException if client creation fails
     * @throws InterruptedException if the setup is interrupted
     */
    @BeforeClass
    public void setUp() throws IOException, InterruptedException {
        if (!isContainerSupportAvailable()) {
            throw new SkipException("Docker/Podman is not available");
        }

        bigtableContainer =
                new GenericContainer<>(DockerImageName.parse(BIGTABLE_EMULATOR_IMAGE))
                        .withCommand(
                                "gcloud",
                                "beta",
                                "emulators",
                                "bigtable",
                                "start",
                                "--host-port=0.0.0.0:9060")
                        .withExposedPorts(BIGTABLE_EMULATOR_PORT)
                        .withStartupAttempts(3);

        bigtableContainer.start();

        String host = bigtableContainer.getHost();
        int port = bigtableContainer.getMappedPort(BIGTABLE_EMULATOR_PORT);

        String emulatorHost = host + ":" + port;

        // Wait for emulator to be ready
        Thread.sleep(2000);

        // Template method: let subclasses create their specific clients
        createClients(TEST_PROJECT_ID, TEST_INSTANCE_ID, emulatorHost);
    }

    /**
     * Check if Docker/Podman container support is available on the system.
     *
     * @return true if Docker/Podman is available, false otherwise
     */
    private static boolean isContainerSupportAvailable() {
        try {
            return tryRunningCommand("docker", "version");
        } catch (Exception e) {
            try {
                return tryRunningCommand("podman", "version");
            } catch (Exception ignored) {
                return false;
            }
        }
    }

    private static boolean tryRunningCommand(String... command)
            throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        int exitCode = process.waitFor();
        return exitCode == 0;
    }

    /**
     * Template method for subclasses to create the specific clients they need.
     *
     * <p>This method is called after the emulator container is started and ready. Subclasses should
     * create their required clients (table admin, data, instance admin, etc.) using the provided
     * connection parameters.
     *
     * @param projectId the test project ID
     * @param instanceId the test instance ID
     * @param emulatorHost the emulator host:port endpoint
     * @throws IOException if client creation fails
     */
    protected abstract void createClients(String projectId, String instanceId, String emulatorHost)
            throws IOException;

    /**
     * Clean up all clients and stop the BigTable emulator container.
     *
     * <p>This method ensures all cleanup operations are attempted even if some fail. Any exceptions
     * are collected and thrown as suppressed exceptions.
     *
     * @throws Exception if any cleanup operation fails
     */
    @AfterClass
    public final void tearDown() throws Exception {
        Exception primaryException = null;

        // Get resources to close from subclass
        List<AutoCloseable> resources = getResourcesToTearDown();

        for (AutoCloseable resource : resources) {
            if (resource != null) {
                try {
                    resource.close();
                } catch (Exception e) {
                    if (primaryException == null) {
                        primaryException = e;
                    } else {
                        primaryException.addSuppressed(e);
                    }
                }
            }
        }

        // If any exception occurred, throw it
        if (primaryException != null) {
            throw primaryException;
        }
    }

    /**
     * Template method for subclasses to declare which resources need to be closed.
     *
     * <p>Subclasses should return a list containing all AutoCloseable resources they created,
     * including the container. Resources will be closed in the order they appear in the list.
     *
     * @return list of AutoCloseable resources to close during tearDown
     */
    protected abstract List<AutoCloseable> getResourcesToTearDown();
}
