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

package io.github.totalschema.connector.gcp.bigtable.actions.table.removetableiampolicy;

import com.google.cloud.Identity;
import com.google.cloud.Policy;
import com.google.cloud.Role;
import com.google.cloud.bigtable.admin.v2.BigtableTableAdminClient;
import io.github.totalschema.connector.gcp.bigtable.actions.table.TableAction;
import io.github.totalschema.connector.gcp.core.model.iam.IamBinding;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Removes IAM policy bindings from a BigTable table.
 *
 * <p>This action uses a read-modify-write pattern to update IAM policies:
 *
 * <ol>
 *   <li>Read the current policy from the table
 *   <li>Remove the specified members from the specified roles
 *   <li>Write the modified policy back to the table
 * </ol>
 *
 * <p>Example YAML configurations:
 *
 * <pre>
 * # Revoke read access from a user
 * RemoveTableIamPolicy:
 *   tableId: users
 *   bindings:
 *     - role: roles/bigtable.reader
 *       members:
 *         - user:ex-employee@company.com
 *
 * # Revoke multiple roles and members
 * RemoveTableIamPolicy:
 *   tableId: users
 *   bindings:
 *     - role: roles/bigtable.reader
 *       members:
 *         - serviceAccount:old-analytics@project.iam.gserviceaccount.com
 *         - user:contractor@company.com
 *     - role: roles/bigtable.user
 *       members:
 *         - serviceAccount:decommissioned-app@project.iam.gserviceaccount.com
 *
 * # Remove service account access
 * RemoveTableIamPolicy:
 *   tableId: sensitive_data
 *   bindings:
 *     - role: roles/bigtable.user
 *       members:
 *         - serviceAccount:test-service@project.iam.gserviceaccount.com
 * </pre>
 *
 * <p><strong>Behavior Notes:</strong>
 *
 * <ul>
 *   <li>Only removes specified members from specified roles
 *   <li>Other members in the same role are preserved
 *   <li>If all members are removed from a role, the role binding is removed
 *   <li>If a member doesn't exist in the specified role, the operation silently succeeds
 *   <li>Use {@code SetTableIamPolicy} to grant new bindings
 * </ul>
 */
public class RemoveTableIamPolicy implements TableAction {

    private String tableId;
    private List<IamBinding> bindings;

    @Override
    public void execute(BigtableTableAdminClient adminClient) {
        if (tableId == null || tableId.isEmpty()) {
            throw new IllegalArgumentException("tableId is required");
        }

        if (bindings == null || bindings.isEmpty()) {
            throw new IllegalArgumentException("bindings is required and must not be empty");
        }

        // Validate all bindings before making any changes
        for (IamBinding binding : bindings) {
            binding.validate();
        }

        // Read current policy
        Policy currentPolicy = adminClient.getIamPolicy(tableId);
        Policy.Builder policyBuilder = Policy.newBuilder();

        // Process each existing binding
        for (Role role : currentPolicy.getBindings().keySet()) {
            Set<Identity> existingIdentities = new HashSet<>(currentPolicy.getBindings().get(role));
            String roleString = role.getValue();

            // Check if this role should have members removed
            IamBinding removalBinding = findBindingForRole(roleString);
            if (removalBinding != null) {
                // Convert removal members to Identity objects and remove them
                for (String member : removalBinding.getMembers()) {
                    existingIdentities.remove(Identity.valueOf(member));
                }
            }

            // Add role binding back if it still has members
            if (!existingIdentities.isEmpty()) {
                List<Identity> identityList = new ArrayList<>(existingIdentities);
                Identity first = identityList.get(0);
                Identity[] rest =
                        identityList.subList(1, identityList.size()).toArray(new Identity[0]);
                policyBuilder.addIdentity(role, first, rest);
            }
        }

        // Write updated policy back
        adminClient.setIamPolicy(tableId, policyBuilder.build());
    }

    /**
     * Finds the binding configuration for a specific role.
     *
     * @param role the role to find
     * @return the binding configuration, or null if not found
     */
    private IamBinding findBindingForRole(String role) {
        if (bindings == null) {
            return null;
        }

        for (IamBinding binding : bindings) {
            if (binding.getRole().equals(role)) {
                return binding;
            }
        }
        return null;
    }

    /**
     * Gets the table ID.
     *
     * @return the table ID
     */
    public String getTableId() {
        return tableId;
    }

    /**
     * Sets the table ID.
     *
     * @param tableId the table ID
     */
    public void setTableId(String tableId) {
        this.tableId = tableId;
    }

    /**
     * Gets the IAM bindings to remove.
     *
     * @return the IAM bindings
     */
    public List<IamBinding> getBindings() {
        return bindings;
    }

    /**
     * Sets the IAM bindings to remove.
     *
     * @param bindings the IAM bindings
     */
    public void setBindings(List<IamBinding> bindings) {
        this.bindings = bindings;
    }

    @Override
    public String toString() {
        return "RemoveTableIamPolicy{"
                + "tableId='"
                + tableId
                + '\''
                + ", bindings="
                + bindings
                + '}';
    }
}
