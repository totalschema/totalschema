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

package io.github.totalschema.connector.gcp.bigtable.actions.instance.disabletableautoscaling;

import static org.testng.Assert.*;

import com.google.cloud.bigtable.admin.v2.models.Cluster;
import io.github.totalschema.connector.gcp.bigtable.AbstractBigtableInstanceAdminIntegrationTest;
import org.testng.SkipException;
import org.testng.annotations.Test;

/**
 * Integration tests for {@link DisableTableAutoscaling} using BigTable emulator in a TestContainer.
 *
 * <p>These tests verify that DisableTableAutoscaling works correctly against the BigTable emulator.
 * The tests are skipped if Docker/Podman is not available.
 *
 * <p><strong>Important Note:</strong> The BigTable emulator has limited support for cluster-level
 * operations. Some tests may be skipped or have reduced verification due to emulator limitations.
 * These tests primarily verify that the action doesn't crash and follows the proper execution flow.
 */
public class DisableTableAutoscalingIntegrationTest
        extends AbstractBigtableInstanceAdminIntegrationTest {

    @Test(description = "Verify DisableTableAutoscaling validation catches missing instanceId")
    public void testDisableTableAutoscalingMissingInstanceId() {
        // Given: a DisableTableAutoscaling action without instanceId
        DisableTableAutoscaling action = new DisableTableAutoscaling();
        action.setClusterId("test-cluster");
        action.setFixedNodes(5);

        // When/Then: should throw IllegalArgumentException
        try {
            action.execute(instanceAdminClient);
            fail("Expected IllegalArgumentException for missing instanceId");
        } catch (IllegalArgumentException e) {
            assertTrue(
                    e.getMessage().contains("instanceId is required"),
                    "Error message should mention instanceId is required");
        }
    }

    @Test(description = "Verify DisableTableAutoscaling validation catches missing clusterId")
    public void testDisableTableAutoscalingMissingClusterId() {
        // Given: a DisableTableAutoscaling action without clusterId
        DisableTableAutoscaling action = new DisableTableAutoscaling();
        action.setInstanceId(TEST_INSTANCE_ID);
        action.setFixedNodes(5);

        // When/Then: should throw IllegalArgumentException
        try {
            action.execute(instanceAdminClient);
            fail("Expected IllegalArgumentException for missing clusterId");
        } catch (IllegalArgumentException e) {
            assertTrue(
                    e.getMessage().contains("clusterId is required"),
                    "Error message should mention clusterId is required");
        }
    }

    @Test(description = "Verify DisableTableAutoscaling validation catches missing fixedNodes")
    public void testDisableTableAutoscalingMissingFixedNodes() {
        // Given: a DisableTableAutoscaling action without fixedNodes
        DisableTableAutoscaling action = new DisableTableAutoscaling();
        action.setInstanceId(TEST_INSTANCE_ID);
        action.setClusterId("test-cluster");

        // When/Then: should throw IllegalArgumentException
        try {
            action.execute(instanceAdminClient);
            fail("Expected IllegalArgumentException for missing fixedNodes");
        } catch (IllegalArgumentException e) {
            assertTrue(
                    e.getMessage().contains("fixedNodes must be > 0"),
                    "Error message should mention fixedNodes must be > 0");
        }
    }

    @Test(description = "Verify DisableTableAutoscaling validation catches zero fixedNodes")
    public void testDisableTableAutoscalingZeroFixedNodes() {
        // Given: a DisableTableAutoscaling action with zero fixedNodes
        DisableTableAutoscaling action = new DisableTableAutoscaling();
        action.setInstanceId(TEST_INSTANCE_ID);
        action.setClusterId("test-cluster");
        action.setFixedNodes(0);

        // When/Then: should throw IllegalArgumentException
        try {
            action.execute(instanceAdminClient);
            fail("Expected IllegalArgumentException for zero fixedNodes");
        } catch (IllegalArgumentException e) {
            assertTrue(
                    e.getMessage().contains("fixedNodes must be > 0"),
                    "Error message should mention fixedNodes must be > 0");
        }
    }

    @Test(description = "Verify DisableTableAutoscaling validation catches negative fixedNodes")
    public void testDisableTableAutoscalingNegativeFixedNodes() {
        // Given: a DisableTableAutoscaling action with negative fixedNodes
        DisableTableAutoscaling action = new DisableTableAutoscaling();
        action.setInstanceId(TEST_INSTANCE_ID);
        action.setClusterId("test-cluster");
        action.setFixedNodes(-5);

        // When/Then: should throw IllegalArgumentException
        try {
            action.execute(instanceAdminClient);
            fail("Expected IllegalArgumentException for negative fixedNodes");
        } catch (IllegalArgumentException e) {
            assertTrue(
                    e.getMessage().contains("fixedNodes must be > 0"),
                    "Error message should mention fixedNodes must be > 0");
        }
    }

    @Test(description = "Verify DisableTableAutoscaling handles non-existent cluster")
    public void testDisableTableAutoscalingNonExistentCluster() {
        // Given: a DisableTableAutoscaling action with non-existent cluster
        DisableTableAutoscaling action = new DisableTableAutoscaling();
        action.setInstanceId(TEST_INSTANCE_ID);
        action.setClusterId("non-existent-cluster");
        action.setFixedNodes(5);

        // When/Then: should throw IllegalArgumentException for cluster not found
        try {
            action.execute(instanceAdminClient);
            fail("Expected IllegalArgumentException for non-existent cluster");
        } catch (IllegalArgumentException e) {
            assertTrue(
                    e.getMessage().contains("Failed to get cluster"),
                    "Error message should mention failed to get cluster");
        } catch (Exception e) {
            // Emulator might throw different exceptions; accept this too
            assertTrue(
                    e.getMessage().contains("not found")
                            || e.getMessage().contains("does not exist")
                            || e.getMessage().contains("Failed to get cluster"),
                    "Error message should indicate cluster not found");
        }
    }

    @Test(description = "Attempt to disable autoscaling with emulator (may have limitations)")
    public void testDisableTableAutoscalingWithEmulator() {
        // Note: The BigTable emulator may not fully support cluster operations.
        // This test verifies the action executes without crashing, but actual
        // cluster resizing may not be verifiable in the emulator.

        // Given: try to list existing clusters
        try {
            Iterable<Cluster> clusters = instanceAdminClient.listClusters(TEST_INSTANCE_ID);

            // Check if any clusters exist
            boolean hasCluster = clusters.iterator().hasNext();

            if (!hasCluster) {
                throw new SkipException(
                        "No clusters available in emulator. Cluster-level operations may not be "
                                + "supported by the emulator.");
            }

            // Get the first cluster
            Cluster cluster = clusters.iterator().next();
            String clusterId = cluster.getId();

            // Given: a valid DisableTableAutoscaling action
            DisableTableAutoscaling action = new DisableTableAutoscaling();
            action.setInstanceId(TEST_INSTANCE_ID);
            action.setClusterId(clusterId);
            action.setFixedNodes(3);

            // When: executing the action
            action.execute(instanceAdminClient);

            // Then: verify the action completes without throwing an exception
            // Note: The emulator may accept the call but not actually resize the cluster
            // Full verification would require checking cluster.getServeNodes() which may
            // not be supported in the emulator

        } catch (UnsupportedOperationException e) {
            throw new SkipException(
                    "Cluster operations not supported by emulator: " + e.getMessage());
        } catch (Exception e) {
            // Check if this is an emulator limitation
            if (e.getMessage() != null
                    && (e.getMessage().contains("Unimplemented")
                            || e.getMessage().contains("not supported")
                            || e.getMessage().contains("UNIMPLEMENTED"))) {
                throw new SkipException("Emulator does not support cluster operations: " + e);
            }
            // Otherwise, this is a real failure
            throw e;
        }
    }

    @Test(
            description = "Verify minimal node count (1 node) is accepted",
            dependsOnMethods = {"testDisableTableAutoscalingWithEmulator"})
    public void testDisableTableAutoscalingMinimalNodeCount() {
        // Given: try to get a cluster
        try {
            Iterable<Cluster> clusters = instanceAdminClient.listClusters(TEST_INSTANCE_ID);
            boolean hasCluster = clusters.iterator().hasNext();

            if (!hasCluster) {
                throw new SkipException("No clusters available in emulator");
            }

            Cluster cluster = clusters.iterator().next();
            String clusterId = cluster.getId();

            // Given: a DisableTableAutoscaling action with minimal node count
            DisableTableAutoscaling action = new DisableTableAutoscaling();
            action.setInstanceId(TEST_INSTANCE_ID);
            action.setClusterId(clusterId);
            action.setFixedNodes(1); // Minimal valid value

            // When: executing the action
            action.execute(instanceAdminClient);

            // Then: action completes successfully (emulator limitations apply)

        } catch (UnsupportedOperationException e) {
            throw new SkipException("Cluster operations not supported by emulator");
        } catch (Exception e) {
            if (e.getMessage() != null
                    && (e.getMessage().contains("Unimplemented")
                            || e.getMessage().contains("UNIMPLEMENTED"))) {
                throw new SkipException("Emulator limitation: " + e);
            }
            throw e;
        }
    }

    @Test(
            description = "Verify large node count is accepted",
            dependsOnMethods = {"testDisableTableAutoscalingWithEmulator"})
    public void testDisableTableAutoscalingLargeNodeCount() {
        // Given: try to get a cluster
        try {
            Iterable<Cluster> clusters = instanceAdminClient.listClusters(TEST_INSTANCE_ID);
            boolean hasCluster = clusters.iterator().hasNext();

            if (!hasCluster) {
                throw new SkipException("No clusters available in emulator");
            }

            Cluster cluster = clusters.iterator().next();
            String clusterId = cluster.getId();

            // Given: a DisableTableAutoscaling action with large node count
            DisableTableAutoscaling action = new DisableTableAutoscaling();
            action.setInstanceId(TEST_INSTANCE_ID);
            action.setClusterId(clusterId);
            action.setFixedNodes(50); // Large value for production scenarios

            // When: executing the action
            action.execute(instanceAdminClient);

            // Then: action completes successfully (emulator limitations apply)

        } catch (UnsupportedOperationException e) {
            throw new SkipException("Cluster operations not supported by emulator");
        } catch (Exception e) {
            if (e.getMessage() != null
                    && (e.getMessage().contains("Unimplemented")
                            || e.getMessage().contains("UNIMPLEMENTED"))) {
                throw new SkipException("Emulator limitation: " + e);
            }
            throw e;
        }
    }
}
