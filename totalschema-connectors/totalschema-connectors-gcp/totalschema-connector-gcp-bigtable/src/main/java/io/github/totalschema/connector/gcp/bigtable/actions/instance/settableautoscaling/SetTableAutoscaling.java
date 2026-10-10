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

import com.google.bigtable.admin.v2.ClusterName;
import com.google.bigtable.admin.v2.PartialUpdateClusterRequest;
import com.google.cloud.bigtable.admin.v2.BaseBigtableInstanceAdminClient;
import com.google.cloud.bigtable.admin.v2.BigtableInstanceAdminClient;
import com.google.protobuf.FieldMask;
import io.github.totalschema.connector.gcp.bigtable.actions.instance.InstanceAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configures automatic node scaling for a BigTable cluster.
 *
 * <p><strong>Important Note:</strong> Despite the name "SetTableAutoscaling", autoscaling is
 * configured at the <strong>cluster level</strong>, not the table level. This means autoscaling
 * applies to all tables within the cluster. The "table" reference in the action name is for
 * consistency with the TotalSchema action naming convention and user workflows where table
 * deployment triggers cluster configuration.
 *
 * <p><strong>What This Action Does:</strong>
 *
 * <ul>
 *   <li>Enables cluster autoscaling with specified min/max node counts
 *   <li>Sets CPU target percentage for scaling decisions
 *   <li>Automatically adjusts cluster size based on workload
 *   <li>Optimizes cost for variable workloads
 * </ul>
 *
 * <p><strong>Use Cases:</strong>
 *
 * <ul>
 *   <li>Cost optimization for variable workloads (scale down during low traffic)
 *   <li>Performance guarantees during peak traffic (scale up automatically)
 *   <li>Development/testing environments (minimal cost when idle)
 *   <li>SaaS applications with unpredictable load patterns
 * </ul>
 *
 * <p>Example YAML configurations:
 *
 * <pre>
 * # Basic autoscaling for cost optimization
 * SetTableAutoscaling:
 *   instanceId: my-instance
 *   clusterId: my-cluster
 *   minNodes: 3
 *   maxNodes: 10
 *   cpuTargetPercent: 70
 *
 * # Aggressive autoscaling for dev environment
 * SetTableAutoscaling:
 *   instanceId: dev-instance
 *   clusterId: dev-cluster-us-central1
 *   minNodes: 1
 *   maxNodes: 5
 *   cpuTargetPercent: 80
 *
 * # Conservative autoscaling for production
 * SetTableAutoscaling:
 *   instanceId: prod-instance
 *   clusterId: prod-cluster-us-east1
 *   minNodes: 10
 *   maxNodes: 30
 *   cpuTargetPercent: 60
 * </pre>
 *
 * <p><strong>Configuration Guidelines:</strong>
 *
 * <ul>
 *   <li><strong>minNodes:</strong> Set based on baseline load (minimum 1, recommended 3+ for
 *       production)
 *   <li><strong>maxNodes:</strong> Set based on peak load expectations and budget
 *   <li><strong>cpuTargetPercent:</strong> Lower values (50-60) = more headroom, faster scaling;
 *       Higher values (70-80) = cost savings, slower response to spikes
 * </ul>
 *
 * <p><strong>Cost Implications:</strong> Each node costs approximately $0.65/hour (~$468/month) for
 * SSD storage. Autoscaling helps reduce costs during low-traffic periods while ensuring capacity
 * during peaks.
 *
 * <p><strong>Performance Considerations:</strong> Scaling operations can take 1-5 minutes. Set
 * minNodes high enough to handle sudden traffic spikes without waiting for scale-up.
 */
public class SetTableAutoscaling implements InstanceAction {

    private static final Logger logger = LoggerFactory.getLogger(SetTableAutoscaling.class);

    private String instanceId;
    private String clusterId;
    private Integer minNodes;
    private Integer maxNodes;
    private Integer cpuTargetPercent;

    @Override
    public void execute(BigtableInstanceAdminClient adminClient) {
        // Validation
        if (instanceId == null || instanceId.isEmpty()) {
            throw new IllegalArgumentException(
                    "instanceId is required. Example: instanceId: my-instance");
        }

        if (clusterId == null || clusterId.isEmpty()) {
            throw new IllegalArgumentException(
                    "clusterId is required. Example: clusterId: my-cluster");
        }

        if (minNodes == null || minNodes <= 0) {
            throw new IllegalArgumentException("minNodes must be > 0. Example: minNodes: 3");
        }

        if (maxNodes == null || maxNodes < minNodes) {
            throw new IllegalArgumentException(
                    "maxNodes must be >= minNodes. Example: maxNodes: 10 (with minNodes: 3)");
        }

        if (cpuTargetPercent == null || cpuTargetPercent <= 0 || cpuTargetPercent > 100) {
            throw new IllegalArgumentException(
                    "cpuTargetPercent must be between 1 and 100. Example: cpuTargetPercent: 70");
        }

        // Validate that the cluster exists
        try {
            adminClient.getCluster(instanceId, clusterId);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Failed to get cluster '"
                            + clusterId
                            + "' in instance '"
                            + instanceId
                            + "'. "
                            + "Ensure the cluster exists. Error: "
                            + e.getMessage(),
                    e);
        }

        // Update the cluster with autoscaling configuration using protobuf API
        try {
            String projectId = adminClient.getProjectId();
            ClusterName clusterName = ClusterName.of(projectId, instanceId, clusterId);

            // Build cluster with autoscaling configuration
            com.google.bigtable.admin.v2.Cluster cluster =
                    com.google.bigtable.admin.v2.Cluster.newBuilder()
                            .setName(clusterName.toString())
                            .setClusterConfig(
                                    com.google.bigtable.admin.v2.Cluster.ClusterConfig.newBuilder()
                                            .setClusterAutoscalingConfig(
                                                    com.google.bigtable.admin.v2.Cluster
                                                            .ClusterAutoscalingConfig.newBuilder()
                                                            .setAutoscalingLimits(
                                                                    com.google.bigtable.admin.v2
                                                                            .AutoscalingLimits
                                                                            .newBuilder()
                                                                            .setMinServeNodes(
                                                                                    minNodes)
                                                                            .setMaxServeNodes(
                                                                                    maxNodes)
                                                                            .build())
                                                            .setAutoscalingTargets(
                                                                    com.google.bigtable.admin.v2
                                                                            .AutoscalingTargets
                                                                            .newBuilder()
                                                                            .setCpuUtilizationPercent(
                                                                                    cpuTargetPercent)
                                                                            .build())
                                                            .build())
                                            .build())
                            .build();

            // Create update request with field mask for autoscaling
            PartialUpdateClusterRequest request =
                    PartialUpdateClusterRequest.newBuilder()
                            .setCluster(cluster)
                            .setUpdateMask(
                                    FieldMask.newBuilder()
                                            .addPaths("cluster_config.cluster_autoscaling_config")
                                            .build())
                            .build();

            // Use base client to perform the update
            BaseBigtableInstanceAdminClient baseClient = adminClient.getBaseClient();
            baseClient.partialUpdateClusterAsync(request).get();

            logger.info(
                    "Enabled autoscaling for cluster '{}' in instance '{}': minNodes={}, maxNodes={}, cpuTarget={}%",
                    clusterId, instanceId, minNodes, maxNodes, cpuTargetPercent);

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to enable autoscaling for cluster '"
                            + clusterId
                            + "' in instance '"
                            + instanceId
                            + "'. Error: "
                            + e.getMessage(),
                    e);
        }
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getClusterId() {
        return clusterId;
    }

    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }

    public Integer getMinNodes() {
        return minNodes;
    }

    public void setMinNodes(Integer minNodes) {
        this.minNodes = minNodes;
    }

    public Integer getMaxNodes() {
        return maxNodes;
    }

    public void setMaxNodes(Integer maxNodes) {
        this.maxNodes = maxNodes;
    }

    public Integer getCpuTargetPercent() {
        return cpuTargetPercent;
    }

    public void setCpuTargetPercent(Integer cpuTargetPercent) {
        this.cpuTargetPercent = cpuTargetPercent;
    }

    @Override
    public String toString() {
        return "SetTableAutoscaling{"
                + "instanceId='"
                + instanceId
                + '\''
                + ", clusterId='"
                + clusterId
                + '\''
                + ", minNodes="
                + minNodes
                + ", maxNodes="
                + maxNodes
                + ", cpuTargetPercent="
                + cpuTargetPercent
                + '}';
    }
}
