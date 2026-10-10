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

package io.github.totalschema.connector.gcp.bigtable.actions.instance.createinstance;

import com.google.cloud.bigtable.admin.v2.BigtableInstanceAdminClient;
import com.google.cloud.bigtable.admin.v2.models.CreateInstanceRequest;
import com.google.cloud.bigtable.admin.v2.models.Instance;
import com.google.cloud.bigtable.admin.v2.models.StorageType;
import io.github.totalschema.connector.gcp.bigtable.actions.instance.InstanceAction;
import java.util.List;
import java.util.Map;

/**
 * Creates a BigTable instance with cluster configuration.
 *
 * <p><strong>Use Case Philosophy:</strong> Instance creation is typically handled by
 * infrastructure-as-code tools (Terraform, Pulumi) for production environments. This action is
 * designed for:
 *
 * <ul>
 *   <li>Development/testing environments - Ephemeral instances per feature branch
 *   <li>Automated testing pipelines - Create/destroy instances for integration tests
 *   <li>Multi-tenant SaaS - Programmatic instance provisioning per customer
 *   <li>Disaster recovery automation - Scripted instance recreation
 * </ul>
 *
 * <p><strong>Cost Alert:</strong> BigTable instances incur hourly charges based on node count and
 * storage type. A single-node SSD cluster costs ~$0.65/hour (~$468/month). Always delete unused
 * instances to avoid unexpected bills.
 *
 * <p>Example YAML configurations:
 *
 * <pre>
 * # Simple development instance with single cluster
 * CreateInstance:
 *   instanceId: dev-instance
 *   displayName: "Development Instance"
 *   clusters:
 *     - clusterId: dev-cluster-us-central1
 *       zone: us-central1-b
 *       nodes: 3
 *       storageType: SSD  # or HDD
 *   instanceType: DEVELOPMENT  # or PRODUCTION
 *   labels:
 *     environment: dev
 *     team: engineering
 *
 * # Production instance with multiple clusters (replication) and autoscaling
 * CreateInstance:
 *   instanceId: prod-instance
 *   displayName: "Production Instance"
 *   clusters:
 *     - clusterId: prod-cluster-us-east1
 *       zone: us-east1-b
 *       nodes: 10
 *       storageType: SSD
 *       autoscaling:
 *         minNodes: 5
 *         maxNodes: 20
 *         cpuTargetPercent: 70
 *     - clusterId: prod-cluster-europe-west1
 *       zone: europe-west1-c
 *       nodes: 10
 *       storageType: SSD
 *   instanceType: PRODUCTION
 *   labels:
 *     environment: production
 *     criticality: high
 * </pre>
 */
public class CreateInstance implements InstanceAction {

    private String instanceId;
    private String displayName;
    private List<ClusterConfig> clusters;
    private String instanceType; // "PRODUCTION" or "DEVELOPMENT"
    private Map<String, String> labels;
    private Boolean dryRun;

    @Override
    public void execute(BigtableInstanceAdminClient adminClient) {
        // Validation
        if (instanceId == null || instanceId.isEmpty()) {
            throw new IllegalArgumentException("instanceId is required");
        }

        if (clusters == null || clusters.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one cluster configuration is required. "
                            + "Example: clusters: [{clusterId: my-cluster, zone: us-central1-b, nodes: 3, storageType: SSD}]");
        }

        // Validate cluster configurations
        for (ClusterConfig cluster : clusters) {
            if (cluster.getClusterId() == null || cluster.getClusterId().isEmpty()) {
                throw new IllegalArgumentException("clusterId is required for all clusters");
            }
            if (cluster.getZone() == null || cluster.getZone().isEmpty()) {
                throw new IllegalArgumentException(
                        "zone is required for cluster: " + cluster.getClusterId());
            }

            // Either nodes or autoscaling must be specified
            if (cluster.getAutoscaling() == null) {
                if (cluster.getNodes() == null || cluster.getNodes() <= 0) {
                    throw new IllegalArgumentException(
                            "nodes or autoscaling configuration is required for cluster: "
                                    + cluster.getClusterId());
                }
            } else {
                // Validate autoscaling config
                AutoscalingConfig autoscaling = cluster.getAutoscaling();
                if (autoscaling.getMinNodes() == null || autoscaling.getMinNodes() <= 0) {
                    throw new IllegalArgumentException(
                            "autoscaling.minNodes must be > 0 for cluster: "
                                    + cluster.getClusterId());
                }
                if (autoscaling.getMaxNodes() == null
                        || autoscaling.getMaxNodes() < autoscaling.getMinNodes()) {
                    throw new IllegalArgumentException(
                            "autoscaling.maxNodes must be >= minNodes for cluster: "
                                    + cluster.getClusterId());
                }
                if (autoscaling.getCpuTargetPercent() == null
                        || autoscaling.getCpuTargetPercent() <= 0
                        || autoscaling.getCpuTargetPercent() > 100) {
                    throw new IllegalArgumentException(
                            "autoscaling.cpuTargetPercent must be between 1 and 100 for cluster: "
                                    + cluster.getClusterId());
                }
            }
        }

        // Dry-run mode: validate but don't create
        if (Boolean.TRUE.equals(dryRun)) {
            return;
        }

        // Build instance creation request
        Instance.Type type =
                "DEVELOPMENT".equalsIgnoreCase(instanceType)
                        ? Instance.Type.DEVELOPMENT
                        : Instance.Type.PRODUCTION;

        CreateInstanceRequest request =
                CreateInstanceRequest.of(instanceId)
                        .setDisplayName(displayName != null ? displayName : instanceId)
                        .setType(type);

        // Add labels if provided
        if (labels != null && !labels.isEmpty()) {
            for (Map.Entry<String, String> entry : labels.entrySet()) {
                request.addLabel(entry.getKey(), entry.getValue());
            }
        }

        // Add clusters
        for (ClusterConfig clusterConfig : clusters) {
            StorageType storageType =
                    "HDD".equalsIgnoreCase(clusterConfig.getStorageType())
                            ? StorageType.HDD
                            : StorageType.SSD;

            if (clusterConfig.getAutoscaling() != null) {
                // Cluster with autoscaling
                AutoscalingConfig autoscaling = clusterConfig.getAutoscaling();
                // Note: Autoscaling API may vary - using manual cluster creation for now
                // TODO: Check if autoscaling is available in current API version
                request.addCluster(
                        clusterConfig.getClusterId(),
                        clusterConfig.getZone(),
                        autoscaling.getMinNodes(),
                        storageType);
            } else {
                // Cluster with fixed node count
                request.addCluster(
                        clusterConfig.getClusterId(),
                        clusterConfig.getZone(),
                        clusterConfig.getNodes(),
                        storageType);
            }
        }

        // Create instance (this is asynchronous - may take several minutes)
        // Note: The API returns immediately, but the instance creation happens asynchronously.
        // The instance will be in CREATING state initially. For synchronous behavior,
        // users can poll adminClient.getInstance(instanceId) until state becomes READY.
        adminClient.createInstance(request);
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public List<ClusterConfig> getClusters() {
        return clusters;
    }

    public void setClusters(List<ClusterConfig> clusters) {
        this.clusters = clusters;
    }

    public String getInstanceType() {
        return instanceType;
    }

    public void setInstanceType(String instanceType) {
        this.instanceType = instanceType;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    public void setLabels(Map<String, String> labels) {
        this.labels = labels;
    }

    public Boolean getDryRun() {
        return dryRun;
    }

    public void setDryRun(Boolean dryRun) {
        this.dryRun = dryRun;
    }

    @Override
    public String toString() {
        return "CreateInstance{"
                + "instanceId='"
                + instanceId
                + '\''
                + ", displayName='"
                + displayName
                + '\''
                + ", clusters="
                + clusters
                + ", instanceType='"
                + instanceType
                + '\''
                + ", labels="
                + labels
                + ", dryRun="
                + dryRun
                + '}';
    }
}
