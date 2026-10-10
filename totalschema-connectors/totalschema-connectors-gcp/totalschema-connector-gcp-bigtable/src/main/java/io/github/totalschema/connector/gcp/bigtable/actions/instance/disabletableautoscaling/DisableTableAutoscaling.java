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

import com.google.cloud.bigtable.admin.v2.BigtableInstanceAdminClient;
import io.github.totalschema.connector.gcp.bigtable.actions.instance.InstanceAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Disables autoscaling for a BigTable cluster and sets a fixed node count.
 *
 * <p><strong>Important Note:</strong> Despite the name "DisableTableAutoscaling", autoscaling is
 * configured at the <strong>cluster level</strong>, not the table level. This means the fixed node
 * count applies to all tables within the cluster. The "table" reference in the action name is for
 * consistency with the TotalSchema action naming convention and user workflows where table
 * deployment triggers cluster configuration.
 *
 * <p><strong>What This Action Does:</strong>
 *
 * <ul>
 *   <li>Disables cluster autoscaling
 *   <li>Sets a fixed node count for the cluster
 *   <li>Provides predictable performance and cost
 *   <li>Use for stable workloads with known capacity requirements
 * </ul>
 *
 * <p><strong>Use Cases:</strong>
 *
 * <ul>
 *   <li>Production workloads with predictable load patterns
 *   <li>Performance-critical applications requiring guaranteed capacity
 *   <li>Cost budgeting with fixed monthly expenses
 *   <li>Compliance requirements for consistent infrastructure
 *   <li>Simplifying operational monitoring (no dynamic scaling events)
 * </ul>
 *
 * <p>Example YAML configurations:
 *
 * <pre>
 * # Basic fixed capacity for production
 * DisableTableAutoscaling:
 *   instanceId: prod-instance
 *   clusterId: prod-cluster-us-east1
 *   fixedNodes: 10
 *
 * # Development environment with minimal cost
 * DisableTableAutoscaling:
 *   instanceId: dev-instance
 *   clusterId: dev-cluster-us-central1
 *   fixedNodes: 1
 *
 * # High-capacity production cluster
 * DisableTableAutoscaling:
 *   instanceId: prod-instance
 *   clusterId: prod-cluster-europe-west1
 *   fixedNodes: 25
 * </pre>
 *
 * <p><strong>Capacity Planning Guidelines:</strong>
 *
 * <ul>
 *   <li><strong>Development:</strong> 1-3 nodes (sufficient for testing)
 *   <li><strong>Staging:</strong> 3-5 nodes (production-like capacity)
 *   <li><strong>Production:</strong> 10+ nodes (based on load testing and growth projections)
 * </ul>
 *
 * <p><strong>Cost Implications:</strong> Each node costs approximately $0.65/hour (~$468/month) for
 * SSD storage. Fixed node count means predictable monthly costs but no automatic cost savings
 * during low-traffic periods.
 *
 * <p><strong>Performance Considerations:</strong> Choose node count based on:
 *
 * <ul>
 *   <li>Peak concurrent requests per second (RPS)
 *   <li>Data size and read/write patterns
 *   <li>Desired p99 latency targets
 *   <li>Growth projections (add 20-30% headroom)
 * </ul>
 */
public class DisableTableAutoscaling implements InstanceAction {

    private static final Logger logger = LoggerFactory.getLogger(DisableTableAutoscaling.class);

    private String instanceId;
    private String clusterId;
    private Integer fixedNodes;

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

        if (fixedNodes == null || fixedNodes <= 0) {
            throw new IllegalArgumentException("fixedNodes must be > 0. Example: fixedNodes: 5");
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

        // Update the cluster with fixed node count (disables autoscaling)
        try {
            adminClient.resizeCluster(instanceId, clusterId, fixedNodes);

            logger.info(
                    "Disabled autoscaling for cluster '{}' in instance '{}': fixed to {} nodes",
                    clusterId,
                    instanceId,
                    fixedNodes);

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to disable autoscaling for cluster '"
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

    public Integer getFixedNodes() {
        return fixedNodes;
    }

    public void setFixedNodes(Integer fixedNodes) {
        this.fixedNodes = fixedNodes;
    }

    @Override
    public String toString() {
        return "DisableTableAutoscaling{"
                + "instanceId='"
                + instanceId
                + '\''
                + ", clusterId='"
                + clusterId
                + '\''
                + ", fixedNodes="
                + fixedNodes
                + '}';
    }
}
