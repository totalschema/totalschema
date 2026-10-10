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

/**
 * Cluster configuration for a BigTable instance.
 *
 * <p>Example YAML:
 *
 * <pre>
 * clusters:
 *   - clusterId: dev-cluster-us-central1
 *     zone: us-central1-b
 *     nodes: 3
 *     storageType: SSD
 *   - clusterId: dev-cluster-europe-west1
 *     zone: europe-west1-c
 *     nodes: 3
 *     storageType: SSD
 *     autoscaling:
 *       minNodes: 3
 *       maxNodes: 10
 *       cpuTargetPercent: 70
 * </pre>
 */
public class ClusterConfig {

    private String clusterId;
    private String zone;
    private Integer nodes;
    private String storageType; // "SSD" or "HDD"
    private AutoscalingConfig autoscaling;

    public String getClusterId() {
        return clusterId;
    }

    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    public Integer getNodes() {
        return nodes;
    }

    public void setNodes(Integer nodes) {
        this.nodes = nodes;
    }

    public String getStorageType() {
        return storageType;
    }

    public void setStorageType(String storageType) {
        this.storageType = storageType;
    }

    public AutoscalingConfig getAutoscaling() {
        return autoscaling;
    }

    public void setAutoscaling(AutoscalingConfig autoscaling) {
        this.autoscaling = autoscaling;
    }

    @Override
    public String toString() {
        return "ClusterConfig{"
                + "clusterId='"
                + clusterId
                + '\''
                + ", zone='"
                + zone
                + '\''
                + ", nodes="
                + nodes
                + ", storageType='"
                + storageType
                + '\''
                + ", autoscaling="
                + autoscaling
                + '}';
    }
}
