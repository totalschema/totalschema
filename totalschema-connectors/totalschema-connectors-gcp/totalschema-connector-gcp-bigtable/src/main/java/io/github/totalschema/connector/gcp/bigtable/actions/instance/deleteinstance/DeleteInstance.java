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

package io.github.totalschema.connector.gcp.bigtable.actions.instance.deleteinstance;

import com.google.cloud.bigtable.admin.v2.BigtableInstanceAdminClient;
import io.github.totalschema.connector.gcp.bigtable.actions.instance.InstanceAction;

/**
 * Deletes a BigTable instance and all its tables.
 *
 * <p><strong>Warning:</strong> This action permanently deletes the instance, all tables, and all
 * data. This operation cannot be undone.
 *
 * <p><strong>Safety Considerations:</strong>
 *
 * <ul>
 *   <li>Instance deletion is immediate and irreversible
 *   <li>All tables and data within the instance are permanently lost
 *   <li>Billing stops once the instance is deleted
 *   <li>Consider using {@code confirmDeletion} flag for production instances
 * </ul>
 *
 * <p>Example YAML configurations:
 *
 * <pre>
 * # Simple instance deletion (development)
 * DeleteInstance:
 *   instanceId: dev-instance
 *
 * # Safe deletion with confirmation (production)
 * DeleteInstance:
 *   instanceId: prod-instance
 *   confirmDeletion: true
 *   confirmInstanceName: prod-instance
 * </pre>
 */
public class DeleteInstance implements InstanceAction {

    private String instanceId;
    private Boolean confirmDeletion;
    private String confirmInstanceName;

    @Override
    public void execute(BigtableInstanceAdminClient adminClient) {
        // Validation
        if (instanceId == null || instanceId.isEmpty()) {
            throw new IllegalArgumentException("instanceId is required");
        }

        // Safety check: require explicit confirmation if enabled
        if (Boolean.TRUE.equals(confirmDeletion)) {
            if (confirmInstanceName == null || !confirmInstanceName.equals(instanceId)) {
                throw new IllegalArgumentException(
                        "confirmInstanceName must match instanceId when confirmDeletion is true. "
                                + "Expected: '"
                                + instanceId
                                + "', got: '"
                                + confirmInstanceName
                                + "'");
            }
        }

        // Delete the instance
        adminClient.deleteInstance(instanceId);
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public Boolean getConfirmDeletion() {
        return confirmDeletion;
    }

    public void setConfirmDeletion(Boolean confirmDeletion) {
        this.confirmDeletion = confirmDeletion;
    }

    public String getConfirmInstanceName() {
        return confirmInstanceName;
    }

    public void setConfirmInstanceName(String confirmInstanceName) {
        this.confirmInstanceName = confirmInstanceName;
    }

    @Override
    public String toString() {
        return "DeleteInstance{"
                + "instanceId='"
                + instanceId
                + '\''
                + ", confirmDeletion="
                + confirmDeletion
                + ", confirmInstanceName='"
                + confirmInstanceName
                + '\''
                + '}';
    }
}
