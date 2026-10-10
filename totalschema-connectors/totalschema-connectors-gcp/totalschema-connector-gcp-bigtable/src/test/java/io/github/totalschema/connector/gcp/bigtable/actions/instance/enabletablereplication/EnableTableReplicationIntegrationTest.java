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

package io.github.totalschema.connector.gcp.bigtable.actions.instance.enabletablereplication;

import static org.testng.Assert.*;

import com.google.cloud.bigtable.admin.v2.BigtableTableAdminClient;
import com.google.cloud.bigtable.admin.v2.models.AppProfile;
import com.google.cloud.bigtable.admin.v2.models.Cluster;
import com.google.cloud.bigtable.admin.v2.models.CreateClusterRequest;
import com.google.cloud.bigtable.admin.v2.models.CreateInstanceRequest;
import com.google.cloud.bigtable.admin.v2.models.Instance;
import com.google.cloud.bigtable.admin.v2.models.StorageType;
import io.github.totalschema.connector.gcp.bigtable.AbstractBigtableInstanceAdminIntegrationTest;
import io.github.totalschema.connector.gcp.bigtable.EmulatorClientsFactory;
import io.github.totalschema.connector.gcp.bigtable.actions.table.createtable.CreateTable;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;

/**
 * Integration tests for {@link EnableTableReplication} using BigTable emulator in a TestContainer.
 *
 * <p>These tests verify that EnableTableReplication works correctly against the BigTable emulator.
 * The tests are skipped if Docker/Podman is not available.
 *
 * <p><strong>Note:</strong> The BigTable emulator has limited support for instance-level operations
 * like cluster management and app profiles. Some tests may need to be adapted or skipped based on
 * emulator capabilities.
 */
@Ignore(
        "Feature is not implemented in the BigTable emulator. Tests are skipped until emulator support is available.")
public class EnableTableReplicationIntegrationTest
        extends AbstractBigtableInstanceAdminIntegrationTest {

    private BigtableTableAdminClient adminClient;

    /**
     * Creates both instance admin client (for replication) and table admin client (for creating
     * test tables).
     *
     * @param projectId the test project ID
     * @param instanceId the test instance ID
     * @param emulatorHost the emulator host:port endpoint
     * @throws IOException if client creation fails
     */
    @Override
    protected void createClients(String projectId, String instanceId, String emulatorHost)
            throws IOException {
        // First, create the instance admin client via parent
        super.createClients(projectId, instanceId, emulatorHost);

        adminClient =
                EmulatorClientsFactory.createTableAdminClient(projectId, instanceId, emulatorHost);
    }

    @Override
    protected LinkedList<AutoCloseable> getResourcesToTearDown() {

        LinkedList<AutoCloseable> resourcesToTearDown = super.getResourcesToTearDown();

        resourcesToTearDown.addFirst(adminClient);

        return resourcesToTearDown;
    }

    /**
     * Set up test instance with multiple clusters for replication testing.
     *
     * @throws IOException if setup fails
     * @throws InterruptedException if setup is interrupted
     */
    @BeforeClass
    @Override
    public void setUp() throws IOException, InterruptedException {
        // Call parent setup to initialize clients
        super.setUp();

        // Set up test instance with multiple clusters if needed
        setupTestInstanceWithClusters();
    }

    /**
     * Set up test instance with multiple clusters for replication testing.
     *
     * <p><strong>Note:</strong> This may be limited by emulator capabilities. The emulator may
     * already have a default instance set up, or may not support multiple clusters.
     */
    private void setupTestInstanceWithClusters() {
        try {
            // Try to get the existing instance (emulator may have created it)
            Instance existingInstance = instanceAdminClient.getInstance(TEST_INSTANCE_ID);
            if (existingInstance != null) {
                // Instance already exists, try to add additional clusters if needed
                List<Cluster> existingClusters = instanceAdminClient.listClusters(TEST_INSTANCE_ID);

                // Check if we need to add more clusters
                if (existingClusters.size() < 2) {
                    // Try to add a second cluster for replication testing
                    // Note: This may not be supported by the emulator
                    try {
                        @SuppressWarnings("deprecation")
                        CreateClusterRequest clusterRequest =
                                CreateClusterRequest.of(TEST_INSTANCE_ID, "test-cluster-2")
                                        .setZone("us-central1-b")
                                        .setServeNodes(1)
                                        .setStorageType(StorageType.SSD);
                        instanceAdminClient.createCluster(clusterRequest);
                    } catch (Exception e) {
                        // Emulator may not support cluster creation
                        // Tests will need to handle single-cluster scenarios
                    }
                }
            }
        } catch (Exception e) {
            // Instance might not exist, or emulator has limitations
            // Try to create an instance with multiple clusters
            try {
                CreateInstanceRequest instanceRequest =
                        CreateInstanceRequest.of(TEST_INSTANCE_ID)
                                .addCluster("test-cluster-1", "us-central1-a", 1, StorageType.SSD)
                                .addCluster("test-cluster-2", "us-central1-b", 1, StorageType.SSD)
                                .setDisplayName("Test Instance")
                                .setType(Instance.Type.DEVELOPMENT);

                instanceAdminClient.createInstance(instanceRequest);
            } catch (Exception createException) {
                // Emulator may have limitations, tests will adapt
            }
        }
    }

    @Test(
            description =
                    "Enable multi-cluster replication with two clusters and eventual consistency")
    public void testEnableReplicationWithTwoClustersEventual() {
        // Given: a table exists
        String tableId = "test-replication-eventual";
        createEmptyTable(tableId);

        // And: we have at least 2 clusters
        List<Cluster> clusters = instanceAdminClient.listClusters(TEST_INSTANCE_ID);
        if (clusters.size() < 2) {
            // Skip if emulator doesn't support multiple clusters
            throw new org.testng.SkipException(
                    "Emulator does not support multiple clusters (found: " + clusters.size() + ")");
        }

        // And: an EnableTableReplication action with eventual consistency
        EnableTableReplication action = new EnableTableReplication();
        action.setInstanceId(TEST_INSTANCE_ID);
        action.setTableId(tableId);
        action.setClusters(
                List.of(
                        clusters.get(0).getId(),
                        clusters.get(1).getId())); // Use actual cluster IDs
        action.setConsistency("EVENTUAL");

        // When: executing the action
        action.execute(instanceAdminClient);

        // Then: an app profile should be created
        String expectedAppProfileId = tableId + "-replication";
        AppProfile appProfile =
                instanceAdminClient.getAppProfile(TEST_INSTANCE_ID, expectedAppProfileId);
        assertNotNull(appProfile, "App profile should be created");
        assertEquals(appProfile.getId(), expectedAppProfileId);
    }

    @Test(description = "Enable multi-cluster replication with two clusters and strong consistency")
    public void testEnableReplicationWithTwoClustersStrong() {
        // Given: a table exists
        String tableId = "test-replication-strong";
        createEmptyTable(tableId);

        // And: we have at least 2 clusters
        List<Cluster> clusters = instanceAdminClient.listClusters(TEST_INSTANCE_ID);
        if (clusters.size() < 2) {
            throw new org.testng.SkipException(
                    "Emulator does not support multiple clusters (found: " + clusters.size() + ")");
        }

        // And: an EnableTableReplication action with strong consistency
        EnableTableReplication action = new EnableTableReplication();
        action.setInstanceId(TEST_INSTANCE_ID);
        action.setTableId(tableId);
        action.setClusters(List.of(clusters.get(0).getId(), clusters.get(1).getId()));
        action.setConsistency("STRONG");

        // When: executing the action
        action.execute(instanceAdminClient);

        // Then: an app profile should be created
        String expectedAppProfileId = tableId + "-replication";
        AppProfile appProfile =
                instanceAdminClient.getAppProfile(TEST_INSTANCE_ID, expectedAppProfileId);
        assertNotNull(appProfile, "App profile should be created");
    }

    @Test(description = "Enable replication with custom app profile ID")
    public void testEnableReplicationWithCustomAppProfileId() {
        // Given: a table exists
        String tableId = "test-replication-custom";
        createEmptyTable(tableId);

        // And: we have at least 2 clusters
        List<Cluster> clusters = instanceAdminClient.listClusters(TEST_INSTANCE_ID);
        if (clusters.size() < 2) {
            throw new org.testng.SkipException(
                    "Emulator does not support multiple clusters (found: " + clusters.size() + ")");
        }

        // And: an EnableTableReplication action with custom app profile ID
        EnableTableReplication action = new EnableTableReplication();
        action.setInstanceId(TEST_INSTANCE_ID);
        action.setTableId(tableId);
        action.setClusters(List.of(clusters.get(0).getId(), clusters.get(1).getId()));
        action.setAppProfileId("my-custom-profile");
        action.setDescription("Custom description for testing");

        // When: executing the action
        action.execute(instanceAdminClient);

        // Then: app profile should exist with custom ID
        AppProfile appProfile =
                instanceAdminClient.getAppProfile(TEST_INSTANCE_ID, "my-custom-profile");
        assertNotNull(appProfile, "App profile with custom ID should be created");
        assertEquals(appProfile.getId(), "my-custom-profile");
    }

    @Test(description = "Enable replication for multiple tables in the same instance")
    public void testEnableReplicationForMultipleTables() {
        // Given: multiple tables exist
        String tableId1 = "test-multi-rep-1";
        String tableId2 = "test-multi-rep-2";
        createEmptyTable(tableId1);
        createEmptyTable(tableId2);

        // And: we have at least 2 clusters
        List<Cluster> clusters = instanceAdminClient.listClusters(TEST_INSTANCE_ID);
        if (clusters.size() < 2) {
            throw new org.testng.SkipException(
                    "Emulator does not support multiple clusters (found: " + clusters.size() + ")");
        }

        // And: EnableTableReplication actions for both tables
        EnableTableReplication action1 = new EnableTableReplication();
        action1.setInstanceId(TEST_INSTANCE_ID);
        action1.setTableId(tableId1);
        action1.setClusters(List.of(clusters.get(0).getId(), clusters.get(1).getId()));

        EnableTableReplication action2 = new EnableTableReplication();
        action2.setInstanceId(TEST_INSTANCE_ID);
        action2.setTableId(tableId2);
        action2.setClusters(List.of(clusters.get(0).getId(), clusters.get(1).getId()));

        // When: executing both actions
        action1.execute(instanceAdminClient);
        action2.execute(instanceAdminClient);

        // Then: both app profiles should exist
        AppProfile appProfile1 =
                instanceAdminClient.getAppProfile(TEST_INSTANCE_ID, tableId1 + "-replication");
        AppProfile appProfile2 =
                instanceAdminClient.getAppProfile(TEST_INSTANCE_ID, tableId2 + "-replication");

        assertNotNull(appProfile1, "App profile for table 1 should exist");
        assertNotNull(appProfile2, "App profile for table 2 should exist");
    }

    @Test(
            description = "Verify enabling replication with non-existent clusters fails",
            expectedExceptions = IllegalArgumentException.class)
    public void testEnableReplicationWithNonExistentClustersFails() {
        // Given: a table exists
        String tableId = "test-invalid-clusters";
        createEmptyTable(tableId);

        // And: an EnableTableReplication action with non-existent clusters
        EnableTableReplication action = new EnableTableReplication();
        action.setInstanceId(TEST_INSTANCE_ID);
        action.setTableId(tableId);
        action.setClusters(List.of("non-existent-cluster-1", "non-existent-cluster-2"));

        // When/Then: should throw IllegalArgumentException
        action.execute(instanceAdminClient);
    }

    @Test(
            description = "Verify enabling replication with only one cluster fails",
            expectedExceptions = IllegalArgumentException.class)
    public void testEnableReplicationWithSingleClusterFails() {
        // Given: a table exists
        String tableId = "test-single-cluster";
        createEmptyTable(tableId);

        // And: we have at least one cluster
        List<Cluster> clusters = instanceAdminClient.listClusters(TEST_INSTANCE_ID);
        if (clusters.isEmpty()) {
            throw new org.testng.SkipException("No clusters available in test instance");
        }

        // And: an EnableTableReplication action with only one cluster
        EnableTableReplication action = new EnableTableReplication();
        action.setInstanceId(TEST_INSTANCE_ID);
        action.setTableId(tableId);
        action.setClusters(List.of(clusters.get(0).getId())); // Only one cluster

        // When/Then: should throw IllegalArgumentException
        action.execute(instanceAdminClient);
    }

    @Test(
            description = "Verify enabling replication without instanceId fails",
            expectedExceptions = IllegalArgumentException.class)
    public void testEnableReplicationWithoutInstanceIdFails() {
        // Given: an action without instanceId
        EnableTableReplication action = new EnableTableReplication();
        action.setInstanceId(null);
        action.setTableId("test-table");
        action.setClusters(List.of("cluster1", "cluster2"));

        // When/Then: should throw IllegalArgumentException
        action.execute(instanceAdminClient);
    }

    @Test(
            description = "Verify enabling replication without tableId fails",
            expectedExceptions = IllegalArgumentException.class)
    public void testEnableReplicationWithoutTableIdFails() {
        // Given: an action without tableId
        EnableTableReplication action = new EnableTableReplication();
        action.setInstanceId(TEST_INSTANCE_ID);
        action.setTableId(null);
        action.setClusters(List.of("cluster1", "cluster2"));

        // When/Then: should throw IllegalArgumentException
        action.execute(instanceAdminClient);
    }

    @Test(
            description = "Verify enabling replication without clusters fails",
            expectedExceptions = IllegalArgumentException.class)
    public void testEnableReplicationWithoutClustersFails() {
        // Given: an action without clusters
        EnableTableReplication action = new EnableTableReplication();
        action.setInstanceId(TEST_INSTANCE_ID);
        action.setTableId("test-table");
        action.setClusters(null);

        // When/Then: should throw IllegalArgumentException
        action.execute(instanceAdminClient);
    }

    @Test(
            description = "Verify enabling replication with invalid consistency fails",
            expectedExceptions = IllegalArgumentException.class)
    public void testEnableReplicationWithInvalidConsistencyFails() {
        // Given: a table exists
        String tableId = "test-invalid-consistency";
        createEmptyTable(tableId);

        // And: we have at least 2 clusters
        List<Cluster> clusters = instanceAdminClient.listClusters(TEST_INSTANCE_ID);
        if (clusters.size() < 2) {
            throw new org.testng.SkipException(
                    "Emulator does not support multiple clusters (found: " + clusters.size() + ")");
        }

        // And: an EnableTableReplication action with invalid consistency
        EnableTableReplication action = new EnableTableReplication();
        action.setInstanceId(TEST_INSTANCE_ID);
        action.setTableId(tableId);
        action.setClusters(List.of(clusters.get(0).getId(), clusters.get(1).getId()));
        action.setConsistency("INVALID_CONSISTENCY");

        // When/Then: should throw IllegalArgumentException
        action.execute(instanceAdminClient);
    }

    @Test(description = "Verify re-enabling replication logs info but does not fail")
    public void testReEnableReplicationOnExistingAppProfile() {
        // Given: a table exists with replication already enabled
        String tableId = "test-re-enable";
        createEmptyTable(tableId);

        List<Cluster> clusters = instanceAdminClient.listClusters(TEST_INSTANCE_ID);
        if (clusters.size() < 2) {
            throw new org.testng.SkipException(
                    "Emulator does not support multiple clusters (found: " + clusters.size() + ")");
        }

        EnableTableReplication action1 = new EnableTableReplication();
        action1.setInstanceId(TEST_INSTANCE_ID);
        action1.setTableId(tableId);
        action1.setClusters(List.of(clusters.get(0).getId(), clusters.get(1).getId()));
        action1.execute(instanceAdminClient);

        // And: the same action is executed again
        EnableTableReplication action2 = new EnableTableReplication();
        action2.setInstanceId(TEST_INSTANCE_ID);
        action2.setTableId(tableId);
        action2.setClusters(List.of(clusters.get(0).getId(), clusters.get(1).getId()));

        // When: executing the action again
        // Then: should not throw exception, just log info
        action2.execute(instanceAdminClient);

        // Verify the app profile still exists
        AppProfile appProfile =
                instanceAdminClient.getAppProfile(TEST_INSTANCE_ID, tableId + "-replication");
        assertNotNull(appProfile);
    }

    /**
     * Helper method to create an empty table for testing.
     *
     * @param tableId the ID of the table to create
     */
    private void createEmptyTable(String tableId) {
        CreateTable createTable = new CreateTable();
        createTable.setTableId(tableId);
        createTable.execute(adminClient);
    }
}
