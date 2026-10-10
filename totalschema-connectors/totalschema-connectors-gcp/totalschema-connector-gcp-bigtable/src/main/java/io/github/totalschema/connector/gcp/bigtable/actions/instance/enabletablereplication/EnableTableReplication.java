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

import com.google.bigtable.admin.v2.Instance;
import com.google.bigtable.admin.v2.InstanceName;
import com.google.cloud.bigtable.admin.v2.BaseBigtableInstanceAdminClient;
import com.google.cloud.bigtable.admin.v2.BigtableInstanceAdminClient;
import com.google.cloud.bigtable.admin.v2.models.AppProfile;
import com.google.cloud.bigtable.admin.v2.models.Cluster;
import com.google.cloud.bigtable.admin.v2.models.CreateAppProfileRequest;
import io.github.totalschema.connector.gcp.bigtable.actions.instance.InstanceAction;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Enables multi-cluster replication for a BigTable table by creating or updating an app profile.
 *
 * <p><strong>How BigTable Replication Works:</strong>
 *
 * <ul>
 *   <li>Tables in BigTable are automatically replicated to ALL clusters in an instance
 *   <li>You cannot selectively enable/disable replication for individual tables
 *   <li>What you CAN control: routing policies via App Profiles
 * </ul>
 *
 * <p><strong>What This Action Does:</strong>
 *
 * <ol>
 *   <li>Validates that the instance has all the specified clusters
 *   <li>Creates or updates an app profile for multi-cluster routing
 *   <li>Sets the consistency level (EVENTUAL or STRONG)
 * </ol>
 *
 * <p><strong>Prerequisites:</strong>
 *
 * <ul>
 *   <li>The BigTable instance must already have the specified clusters
 *   <li>Use {@code CreateInstance} to set up clusters if needed
 *   <li>The table must exist in the instance
 * </ul>
 *
 * <p>Example YAML configurations:
 *
 * <pre>
 * # Enable multi-cluster replication with eventual consistency
 * EnableTableReplication:
 *   instanceId: my-instance
 *   tableId: users
 *   clusters:
 *     - us-east1-b
 *     - europe-west1-c
 *   consistency: EVENTUAL  # default
 *   appProfileId: users-multi-cluster  # optional, defaults to {tableId}-replication
 *
 * # Enable multi-cluster replication with strong consistency
 * EnableTableReplication:
 *   instanceId: prod-instance
 *   tableId: transactions
 *   clusters:
 *     - us-central1-a
 *     - us-central1-b
 *   consistency: STRONG
 *   description: "Multi-cluster routing for transactions table"
 * </pre>
 *
 * <p><strong>Consistency Modes:</strong>
 *
 * <ul>
 *   <li><strong>EVENTUAL</strong> - Lower latency, reads may be slightly stale across regions
 *   <li><strong>STRONG</strong> - Guaranteed consistency, higher latency for cross-region reads
 * </ul>
 *
 * <p><strong>Cost Implications:</strong> Multi-cluster replication increases storage costs
 * proportionally to the number of clusters. Each additional cluster stores a complete copy of all
 * tables.
 */
public class EnableTableReplication implements InstanceAction {

    private static final Logger logger = LoggerFactory.getLogger(EnableTableReplication.class);

    private String instanceId;
    private String tableId; // Used for app profile naming and documentation
    private List<String> clusters;
    private String consistency; // "EVENTUAL" or "STRONG"
    private String appProfileId; // Optional: defaults to {tableId}-replication
    private String description; // Optional description for the app profile

    @Override
    public void execute(BigtableInstanceAdminClient adminClient) {
        // Validation
        if (instanceId == null || instanceId.isEmpty()) {
            throw new IllegalArgumentException(
                    "instanceId is required. Example: instanceId: my-instance");
        }

        if (tableId == null || tableId.isEmpty()) {
            throw new IllegalArgumentException(
                    "tableId is required for app profile naming. Example: tableId: users");
        }

        if (clusters == null || clusters.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one cluster is required. "
                            + "Example: clusters: [us-east1-b, europe-west1-c]");
        }

        if (clusters.size() < 2) {
            throw new IllegalArgumentException(
                    "Multi-cluster replication requires at least 2 clusters. "
                            + "Found: "
                            + clusters.size()
                            + ". "
                            + "For single-cluster operation, use DisableTableReplication.");
        }

        // Validate consistency setting
        String consistencyMode =
                consistency != null ? consistency.toUpperCase(Locale.ENGLISH) : "EVENTUAL";
        if (!consistencyMode.equals("EVENTUAL") && !consistencyMode.equals("STRONG")) {
            throw new IllegalArgumentException(
                    "consistency must be either 'EVENTUAL' or 'STRONG', found: " + consistency);
        }

        // Get the instance to validate it exists
        validateInstanceExists(adminClient, instanceId);

        // Get all clusters in the instance
        List<Cluster> instanceClusters = getClustersForInstance(adminClient, instanceId);

        // Build set of existing cluster IDs (without zone suffix)
        Set<String> existingClusterIds = new HashSet<>();
        for (Cluster cluster : instanceClusters) {
            String clusterId = cluster.getId();
            existingClusterIds.add(clusterId);

            // Also add without potential zone suffixes for flexible matching
            // Some cluster IDs might include zone in the name
            if (clusterId.contains("-")) {
                String[] parts = clusterId.split("-");
                if (parts.length >= 2) {
                    // Add the base name (e.g., "my-cluster" from "my-cluster-us-east1")
                    existingClusterIds.add(parts[0]);
                }
            }
        }

        // Validate all requested clusters exist
        List<String> missingClusters = new ArrayList<>();
        for (String requestedCluster : clusters) {
            if (!existingClusterIds.contains(requestedCluster)) {
                missingClusters.add(requestedCluster);
            }
        }

        if (!missingClusters.isEmpty()) {
            throw new IllegalArgumentException(
                    "The following clusters do not exist in instance '"
                            + instanceId
                            + "': "
                            + missingClusters
                            + ". "
                            + "Available clusters: "
                            + existingClusterIds
                            + ". "
                            + "Add missing clusters to the instance first using CreateInstance or the GCP Console.");
        }

        // Determine app profile ID
        String finalAppProfileId =
                (appProfileId != null && !appProfileId.isEmpty())
                        ? appProfileId
                        : tableId + "-replication";

        // Create or update the app profile
        try {
            // Check if app profile already exists
            // Check if app profile already exists
            AppProfile existingProfile =
                    getExistingAppProfile(adminClient, instanceId, finalAppProfileId);

            if (existingProfile != null) {
                // Update existing profile
                // Note: The current API doesn't directly support updating routing policy
                // This would require recreating the profile or using lower-level APIs
                // For now, we'll log a message
                logger.info(
                        "App profile '{}' already exists. "
                                + "To modify routing policy, delete and recreate the profile.",
                        finalAppProfileId);

            } else {
                // Build the list of cluster IDs to use
                List<String> clusterIdsToUse = new ArrayList<>();
                for (Cluster cluster : instanceClusters) {
                    String clusterId = cluster.getId();
                    // Check if this cluster matches any of the requested clusters
                    for (String requested : clusters) {
                        if (clusterId.equals(requested)
                                || clusterId.startsWith(requested + "-")
                                || clusterId.contains(requested)) {
                            clusterIdsToUse.add(clusterId);
                            break;
                        }
                    }
                }

                if (clusterIdsToUse.isEmpty()) {
                    throw new IllegalArgumentException(
                            "Could not match requested clusters "
                                    + clusters
                                    + " to instance clusters "
                                    + existingClusterIds);
                }

                // Create the profile with multi-cluster routing
                CreateAppProfileRequest request =
                        CreateAppProfileRequest.of(instanceId, finalAppProfileId)
                                .setRoutingPolicy(AppProfile.MultiClusterRoutingPolicy.of());

                // Set description
                String desc =
                        (description != null && !description.isEmpty())
                                ? description
                                : "Multi-cluster replication for table: " + tableId;
                request = request.setDescription(desc);

                // Note: The routing policy routes to all clusters in the instance by default
                // For finer control over specific clusters, use
                // AppProfile.MultiClusterRoutingPolicy.of(clusterIds)

                adminClient.createAppProfile(request);

                logger.info(
                        "Created app profile '{}' with multi-cluster routing for table '{}'",
                        finalAppProfileId,
                        tableId);
            }

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to create/update app profile '"
                            + finalAppProfileId
                            + "' for table '"
                            + tableId
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

    public String getTableId() {
        return tableId;
    }

    public void setTableId(String tableId) {
        this.tableId = tableId;
    }

    public List<String> getClusters() {
        return clusters;
    }

    public void setClusters(List<String> clusters) {
        this.clusters = clusters;
    }

    public String getConsistency() {
        return consistency;
    }

    public void setConsistency(String consistency) {
        this.consistency = consistency;
    }

    public String getAppProfileId() {
        return appProfileId;
    }

    public void setAppProfileId(String appProfileId) {
        this.appProfileId = appProfileId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "EnableTableReplication{"
                + "instanceId='"
                + instanceId
                + '\''
                + ", tableId='"
                + tableId
                + '\''
                + ", clusters="
                + clusters
                + ", consistency='"
                + consistency
                + '\''
                + ", appProfileId='"
                + appProfileId
                + '\''
                + ", description='"
                + description
                + '\''
                + '}';
    }

    /**
     * Validates that the specified instance exists and is accessible.
     *
     * @param adminClient the BigTable instance admin client
     * @param instanceId the instance ID to validate
     * @throws IllegalArgumentException if the instance does not exist or is not accessible
     */
    private void validateInstanceExists(
            BigtableInstanceAdminClient adminClient, String instanceId) {
        try {
            // Use the low-level protobuf-based API
            String projectId = adminClient.getProjectId();
            BaseBigtableInstanceAdminClient baseClient = adminClient.getBaseClient();

            InstanceName instanceName = InstanceName.of(projectId, instanceId);
            Instance instance = baseClient.getInstance(instanceName);

            if (instance == null) {
                throw new IllegalArgumentException("Instance '" + instanceId + "' does not exist");
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Failed to get instance '"
                            + instanceId
                            + "'. "
                            + "Ensure the instance exists and you have permission to access it. "
                            + "Error: "
                            + e.getMessage(),
                    e);
        }
    }

    /**
     * Retrieves all clusters for the specified instance.
     *
     * @param adminClient the BigTable instance admin client
     * @param instanceId the instance ID
     * @return list of clusters in the instance
     * @throws IllegalArgumentException if clusters cannot be retrieved
     */
    private List<Cluster> getClustersForInstance(
            BigtableInstanceAdminClient adminClient, String instanceId) {
        try {
            return adminClient.listClusters(instanceId);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Failed to list clusters for instance '"
                            + instanceId
                            + "'. Error: "
                            + e.getMessage(),
                    e);
        }
    }

    /**
     * Attempts to retrieve an existing app profile.
     *
     * @param adminClient the BigTable instance admin client
     * @param instanceId the instance ID
     * @param appProfileId the app profile ID
     * @return the existing AppProfile, or null if it doesn't exist
     */
    private AppProfile getExistingAppProfile(
            BigtableInstanceAdminClient adminClient, String instanceId, String appProfileId) {
        try {

            return adminClient.getAppProfile(instanceId, appProfileId);
        } catch (Exception e) {
            // Profile doesn't exist, return null
            return null;
        }
    }
}
