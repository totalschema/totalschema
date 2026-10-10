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

package io.github.totalschema.connector.gcp.bigtable.actions.instance.settableautoscaling;

import static org.testng.Assert.*;

import com.google.cloud.bigtable.admin.v2.models.Cluster;
import io.github.totalschema.connector.gcp.bigtable.AbstractBigtableInstanceAdminIntegrationTest;
import org.testng.SkipException;
import org.testng.annotations.Test;

/**
 * Integration tests for {@link SetTableAutoscaling} using BigTable emulator in a TestContainer.
 *
 * <p>These tests verify that SetTableAutoscaling works correctly against the BigTable emulator. The
 * tests are skipped if Docker/Podman is not available.
 *
 * <p><strong>Important Note:</strong> The BigTable emulator has limited support for cluster-level
 * operations, particularly autoscaling configuration. Some tests may be skipped or have reduced
 * verification due to emulator limitations. These tests primarily verify that the action doesn't
 * crash and follows the proper execution flow.
 */
public class SetTableAutoscalingIntegrationTest
        extends AbstractBigtableInstanceAdminIntegrationTest {

    @Test(description = "Verify SetTableAutoscaling validation catches missing instanceId")
    public void testSetTableAutoscalingMissingInstanceId() {
        // Given: a SetTableAutoscaling action without instanceId
        SetTableAutoscaling action = new SetTableAutoscaling();
        action.setClusterId("test-cluster");
        action.setMinNodes(3);
        action.setMaxNodes(10);
        action.setCpuTargetPercent(70);

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

    @Test(description = "Verify SetTableAutoscaling validation catches empty instanceId")
    public void testSetTableAutoscalingEmptyInstanceId() {
        // Given: a SetTableAutoscaling action with empty instanceId
        SetTableAutoscaling action = new SetTableAutoscaling();
        action.setInstanceId("");
        action.setClusterId("test-cluster");
        action.setMinNodes(3);
        action.setMaxNodes(10);
        action.setCpuTargetPercent(70);

        // When/Then: should throw IllegalArgumentException
        try {
            action.execute(instanceAdminClient);
            fail("Expected IllegalArgumentException for empty instanceId");
        } catch (IllegalArgumentException e) {
            assertTrue(
                    e.getMessage().contains("instanceId is required"),
                    "Error message should mention instanceId is required");
        }
    }

    @Test(description = "Verify SetTableAutoscaling validation catches missing clusterId")
    public void testSetTableAutoscalingMissingClusterId() {
        // Given: a SetTableAutoscaling action without clusterId
        SetTableAutoscaling action = new SetTableAutoscaling();
        action.setInstanceId(TEST_INSTANCE_ID);
        action.setMinNodes(3);
        action.setMaxNodes(10);
        action.setCpuTargetPercent(70);

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

    @Test(description = "Verify SetTableAutoscaling validation catches empty clusterId")
    public void testSetTableAutoscalingEmptyClusterId() {
        // Given: a SetTableAutoscaling action with empty clusterId
        SetTableAutoscaling action = new SetTableAutoscaling();
        action.setInstanceId(TEST_INSTANCE_ID);
        action.setClusterId("");
        action.setMinNodes(3);
        action.setMaxNodes(10);
        action.setCpuTargetPercent(70);

        // When/Then: should throw IllegalArgumentException
        try {
            action.execute(instanceAdminClient);
            fail("Expected IllegalArgumentException for empty clusterId");
        } catch (IllegalArgumentException e) {
            assertTrue(
                    e.getMessage().contains("clusterId is required"),
                    "Error message should mention clusterId is required");
        }
    }

    @Test(description = "Verify SetTableAutoscaling validation catches missing minNodes")
    public void testSetTableAutoscalingMissingMinNodes() {
        // Given: a SetTableAutoscaling action without minNodes
        SetTableAutoscaling action = new SetTableAutoscaling();
        action.setInstanceId(TEST_INSTANCE_ID);
        action.setClusterId("test-cluster");
        action.setMaxNodes(10);
        action.setCpuTargetPercent(70);

        // When/Then: should throw IllegalArgumentException
        try {
            action.execute(instanceAdminClient);
            fail("Expected IllegalArgumentException for missing minNodes");
        } catch (IllegalArgumentException e) {
            assertTrue(
                    e.getMessage().contains("minNodes must be > 0"),
                    "Error message should mention minNodes must be > 0");
        }
    }

    @Test(description = "Verify SetTableAutoscaling validation catches zero minNodes")
    public void testSetTableAutoscalingZeroMinNodes() {
        // Given: a SetTableAutoscaling action with zero minNodes
        SetTableAutoscaling action = new SetTableAutoscaling();
        action.setInstanceId(TEST_INSTANCE_ID);
        action.setClusterId("test-cluster");
        action.setMinNodes(0);
        action.setMaxNodes(10);
        action.setCpuTargetPercent(70);

        // When/Then: should throw IllegalArgumentException
        try {
            action.execute(instanceAdminClient);
            fail("Expected IllegalArgumentException for zero minNodes");
        } catch (IllegalArgumentException e) {
            assertTrue(
                    e.getMessage().contains("minNodes must be > 0"),
                    "Error message should mention minNodes must be > 0");
        }
    }

    @Test(description = "Verify SetTableAutoscaling validation catches negative minNodes")
    public void testSetTableAutoscalingNegativeMinNodes() {
        // Given: a SetTableAutoscaling action with negative minNodes
        SetTableAutoscaling action = new SetTableAutoscaling();
        action.setInstanceId(TEST_INSTANCE_ID);
        action.setClusterId("test-cluster");
        action.setMinNodes(-1);
        action.setMaxNodes(10);
        action.setCpuTargetPercent(70);

        // When/Then: should throw IllegalArgumentException
        try {
            action.execute(instanceAdminClient);
            fail("Expected IllegalArgumentException for negative minNodes");
        } catch (IllegalArgumentException e) {
            assertTrue(
                    e.getMessage().contains("minNodes must be > 0"),
                    "Error message should mention minNodes must be > 0");
        }
    }

    @Test(description = "Verify SetTableAutoscaling validation catches missing maxNodes")
    public void testSetTableAutoscalingMissingMaxNodes() {
        // Given: a SetTableAutoscaling action without maxNodes
        SetTableAutoscaling action = new SetTableAutoscaling();
        action.setInstanceId(TEST_INSTANCE_ID);
        action.setClusterId("test-cluster");
        action.setMinNodes(3);
        action.setCpuTargetPercent(70);

        // When/Then: should throw IllegalArgumentException
        try {
            action.execute(instanceAdminClient);
            fail("Expected IllegalArgumentException for missing maxNodes");
        } catch (IllegalArgumentException e) {
            assertTrue(
                    e.getMessage().contains("maxNodes must be >= minNodes"),
                    "Error message should mention maxNodes must be >= minNodes");
        }
    }

    @Test(description = "Verify SetTableAutoscaling validation catches maxNodes < minNodes")
    public void testSetTableAutoscalingMaxNodesLessThanMinNodes() {
        // Given: a SetTableAutoscaling action with maxNodes < minNodes
        SetTableAutoscaling action = new SetTableAutoscaling();
        action.setInstanceId(TEST_INSTANCE_ID);
        action.setClusterId("test-cluster");
        action.setMinNodes(10);
        action.setMaxNodes(5);
        action.setCpuTargetPercent(70);

        // When/Then: should throw IllegalArgumentException
        try {
            action.execute(instanceAdminClient);
            fail("Expected IllegalArgumentException for maxNodes < minNodes");
        } catch (IllegalArgumentException e) {
            assertTrue(
                    e.getMessage().contains("maxNodes must be >= minNodes"),
                    "Error message should mention maxNodes must be >= minNodes");
        }
    }

    @Test(description = "Verify SetTableAutoscaling validation catches missing cpuTargetPercent")
    public void testSetTableAutoscalingMissingCpuTargetPercent() {
        // Given: a SetTableAutoscaling action without cpuTargetPercent
        SetTableAutoscaling action = new SetTableAutoscaling();
        action.setInstanceId(TEST_INSTANCE_ID);
        action.setClusterId("test-cluster");
        action.setMinNodes(3);
        action.setMaxNodes(10);

        // When/Then: should throw IllegalArgumentException
        try {
            action.execute(instanceAdminClient);
            fail("Expected IllegalArgumentException for missing cpuTargetPercent");
        } catch (IllegalArgumentException e) {
            assertTrue(
                    e.getMessage().contains("cpuTargetPercent must be between 1 and 100"),
                    "Error message should mention cpuTargetPercent must be between 1 and 100");
        }
    }

    @Test(description = "Verify SetTableAutoscaling validation catches zero cpuTargetPercent")
    public void testSetTableAutoscalingZeroCpuTargetPercent() {
        // Given: a SetTableAutoscaling action with zero cpuTargetPercent
        SetTableAutoscaling action = new SetTableAutoscaling();
        action.setInstanceId(TEST_INSTANCE_ID);
        action.setClusterId("test-cluster");
        action.setMinNodes(3);
        action.setMaxNodes(10);
        action.setCpuTargetPercent(0);

        // When/Then: should throw IllegalArgumentException
        try {
            action.execute(instanceAdminClient);
            fail("Expected IllegalArgumentException for zero cpuTargetPercent");
        } catch (IllegalArgumentException e) {
            assertTrue(
                    e.getMessage().contains("cpuTargetPercent must be between 1 and 100"),
                    "Error message should mention cpuTargetPercent must be between 1 and 100");
        }
    }

    @Test(description = "Verify SetTableAutoscaling validation catches cpuTargetPercent > 100")
    public void testSetTableAutoscalingCpuTargetPercentOver100() {
        // Given: a SetTableAutoscaling action with cpuTargetPercent > 100
        SetTableAutoscaling action = new SetTableAutoscaling();
        action.setInstanceId(TEST_INSTANCE_ID);
        action.setClusterId("test-cluster");
        action.setMinNodes(3);
        action.setMaxNodes(10);
        action.setCpuTargetPercent(101);

        // When/Then: should throw IllegalArgumentException
        try {
            action.execute(instanceAdminClient);
            fail("Expected IllegalArgumentException for cpuTargetPercent > 100");
        } catch (IllegalArgumentException e) {
            assertTrue(
                    e.getMessage().contains("cpuTargetPercent must be between 1 and 100"),
                    "Error message should mention cpuTargetPercent must be between 1 and 100");
        }
    }

    @Test(description = "Verify SetTableAutoscaling handles non-existent cluster")
    public void testSetTableAutoscalingNonExistentCluster() {
        // Given: a SetTableAutoscaling action with non-existent cluster
        SetTableAutoscaling action = new SetTableAutoscaling();
        action.setInstanceId(TEST_INSTANCE_ID);
        action.setClusterId("non-existent-cluster");
        action.setMinNodes(3);
        action.setMaxNodes(10);
        action.setCpuTargetPercent(70);

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

    @Test(description = "Attempt to enable autoscaling with emulator (may have limitations)")
    public void testSetTableAutoscalingWithEmulator() {
        // Note: The BigTable emulator may not fully support cluster autoscaling operations.
        // This test verifies the action executes without crashing, but actual
        // autoscaling configuration may not be verifiable in the emulator.

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

            // Given: a valid SetTableAutoscaling action
            SetTableAutoscaling action = new SetTableAutoscaling();
            action.setInstanceId(TEST_INSTANCE_ID);
            action.setClusterId(clusterId);
            action.setMinNodes(3);
            action.setMaxNodes(10);
            action.setCpuTargetPercent(70);

            // When: executing the action
            action.execute(instanceAdminClient);

            // Then: verify the action completes without throwing an exception
            // Note: The emulator may accept the call but not actually configure autoscaling
            // Full verification would require checking cluster autoscaling config which may
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
            description = "Verify minimal autoscaling configuration (1 node min/max)",
            dependsOnMethods = {"testSetTableAutoscalingWithEmulator"})
    public void testSetTableAutoscalingMinimalConfiguration() {
        // Given: try to get a cluster
        try {
            Iterable<Cluster> clusters = instanceAdminClient.listClusters(TEST_INSTANCE_ID);
            boolean hasCluster = clusters.iterator().hasNext();

            if (!hasCluster) {
                throw new SkipException("No clusters available in emulator");
            }

            Cluster cluster = clusters.iterator().next();
            String clusterId = cluster.getId();

            // Given: a SetTableAutoscaling action with minimal configuration
            SetTableAutoscaling action = new SetTableAutoscaling();
            action.setInstanceId(TEST_INSTANCE_ID);
            action.setClusterId(clusterId);
            action.setMinNodes(1); // Minimal valid value
            action.setMaxNodes(1); // Same as min (no actual autoscaling)
            action.setCpuTargetPercent(80);

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
            description = "Verify conservative autoscaling for production (low CPU target)",
            dependsOnMethods = {"testSetTableAutoscalingWithEmulator"})
    public void testSetTableAutoscalingConservativeConfiguration() {
        // Given: try to get a cluster
        try {
            Iterable<Cluster> clusters = instanceAdminClient.listClusters(TEST_INSTANCE_ID);
            boolean hasCluster = clusters.iterator().hasNext();

            if (!hasCluster) {
                throw new SkipException("No clusters available in emulator");
            }

            Cluster cluster = clusters.iterator().next();
            String clusterId = cluster.getId();

            // Given: a SetTableAutoscaling action with conservative settings
            SetTableAutoscaling action = new SetTableAutoscaling();
            action.setInstanceId(TEST_INSTANCE_ID);
            action.setClusterId(clusterId);
            action.setMinNodes(10); // Higher baseline for production
            action.setMaxNodes(30); // Room for growth
            action.setCpuTargetPercent(50); // Low target = more headroom

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
            description = "Verify aggressive autoscaling for cost optimization",
            dependsOnMethods = {"testSetTableAutoscalingWithEmulator"})
    public void testSetTableAutoscalingAggressiveConfiguration() {
        // Given: try to get a cluster
        try {
            Iterable<Cluster> clusters = instanceAdminClient.listClusters(TEST_INSTANCE_ID);
            boolean hasCluster = clusters.iterator().hasNext();

            if (!hasCluster) {
                throw new SkipException("No clusters available in emulator");
            }

            Cluster cluster = clusters.iterator().next();
            String clusterId = cluster.getId();

            // Given: a SetTableAutoscaling action with aggressive cost-saving settings
            SetTableAutoscaling action = new SetTableAutoscaling();
            action.setInstanceId(TEST_INSTANCE_ID);
            action.setClusterId(clusterId);
            action.setMinNodes(1); // Minimal baseline for dev/test
            action.setMaxNodes(5); // Limited max for budget control
            action.setCpuTargetPercent(80); // High target = cost savings

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
            description = "Verify large-scale autoscaling configuration",
            dependsOnMethods = {"testSetTableAutoscalingWithEmulator"})
    public void testSetTableAutoscalingLargeScale() {
        // Given: try to get a cluster
        try {
            Iterable<Cluster> clusters = instanceAdminClient.listClusters(TEST_INSTANCE_ID);
            boolean hasCluster = clusters.iterator().hasNext();

            if (!hasCluster) {
                throw new SkipException("No clusters available in emulator");
            }

            Cluster cluster = clusters.iterator().next();
            String clusterId = cluster.getId();

            // Given: a SetTableAutoscaling action for large production workload
            SetTableAutoscaling action = new SetTableAutoscaling();
            action.setInstanceId(TEST_INSTANCE_ID);
            action.setClusterId(clusterId);
            action.setMinNodes(50); // Large baseline
            action.setMaxNodes(100); // High capacity
            action.setCpuTargetPercent(60); // Balanced target

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
            description = "Verify autoscaling with maxNodes equal to minNodes",
            dependsOnMethods = {"testSetTableAutoscalingWithEmulator"})
    public void testSetTableAutoscalingEqualMinMax() {
        // Given: try to get a cluster
        try {
            Iterable<Cluster> clusters = instanceAdminClient.listClusters(TEST_INSTANCE_ID);
            boolean hasCluster = clusters.iterator().hasNext();

            if (!hasCluster) {
                throw new SkipException("No clusters available in emulator");
            }

            Cluster cluster = clusters.iterator().next();
            String clusterId = cluster.getId();

            // Given: a SetTableAutoscaling action with equal min/max (effectively fixed size)
            SetTableAutoscaling action = new SetTableAutoscaling();
            action.setInstanceId(TEST_INSTANCE_ID);
            action.setClusterId(clusterId);
            action.setMinNodes(5);
            action.setMaxNodes(5); // Same as min
            action.setCpuTargetPercent(70);

            // When: executing the action
            action.execute(instanceAdminClient);

            // Then: action completes successfully (emulator limitations apply)
            // Note: This effectively creates a fixed-size cluster with autoscaling disabled

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
            description = "Verify autoscaling with boundary CPU target values",
            dependsOnMethods = {"testSetTableAutoscalingWithEmulator"})
    public void testSetTableAutoscalingBoundaryCpuTargets() {
        // Given: try to get a cluster
        try {
            Iterable<Cluster> clusters = instanceAdminClient.listClusters(TEST_INSTANCE_ID);
            boolean hasCluster = clusters.iterator().hasNext();

            if (!hasCluster) {
                throw new SkipException("No clusters available in emulator");
            }

            Cluster cluster = clusters.iterator().next();
            String clusterId = cluster.getId();

            // Test with minimum CPU target (1%)
            SetTableAutoscaling action1 = new SetTableAutoscaling();
            action1.setInstanceId(TEST_INSTANCE_ID);
            action1.setClusterId(clusterId);
            action1.setMinNodes(3);
            action1.setMaxNodes(10);
            action1.setCpuTargetPercent(1); // Minimum valid value

            action1.execute(instanceAdminClient);

            // Test with maximum CPU target (100%)
            SetTableAutoscaling action2 = new SetTableAutoscaling();
            action2.setInstanceId(TEST_INSTANCE_ID);
            action2.setClusterId(clusterId);
            action2.setMinNodes(3);
            action2.setMaxNodes(10);
            action2.setCpuTargetPercent(100); // Maximum valid value

            action2.execute(instanceAdminClient);

            // Then: both actions complete successfully (emulator limitations apply)

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

    @Test(description = "Verify SetTableAutoscaling toString contains all fields")
    public void testSetTableAutoscalingToString() {
        // Given: a SetTableAutoscaling action with all fields set
        SetTableAutoscaling action = new SetTableAutoscaling();
        action.setInstanceId("test-instance");
        action.setClusterId("test-cluster");
        action.setMinNodes(3);
        action.setMaxNodes(10);
        action.setCpuTargetPercent(70);

        // When: calling toString
        String result = action.toString();

        // Then: it should contain all fields
        assertTrue(result.contains("test-instance"), "toString should contain instanceId");
        assertTrue(result.contains("test-cluster"), "toString should contain clusterId");
        assertTrue(result.contains("3"), "toString should contain minNodes");
        assertTrue(result.contains("10"), "toString should contain maxNodes");
        assertTrue(result.contains("70"), "toString should contain cpuTargetPercent");
        assertTrue(result.contains("SetTableAutoscaling"), "toString should contain class name");
    }

    @Test(description = "Verify SetTableAutoscaling getters and setters")
    public void testSetTableAutoscalingGettersSetters() {
        // Given: a SetTableAutoscaling action
        SetTableAutoscaling action = new SetTableAutoscaling();

        // When: setting values
        action.setInstanceId("instance-id");
        action.setClusterId("cluster-id");
        action.setMinNodes(5);
        action.setMaxNodes(20);
        action.setCpuTargetPercent(65);

        // Then: getters should return the values
        assertEquals(action.getInstanceId(), "instance-id", "instanceId should match");
        assertEquals(action.getClusterId(), "cluster-id", "clusterId should match");
        assertEquals(action.getMinNodes(), Integer.valueOf(5), "minNodes should match");
        assertEquals(action.getMaxNodes(), Integer.valueOf(20), "maxNodes should match");
        assertEquals(
                action.getCpuTargetPercent(), Integer.valueOf(65), "cpuTargetPercent should match");
    }
}
