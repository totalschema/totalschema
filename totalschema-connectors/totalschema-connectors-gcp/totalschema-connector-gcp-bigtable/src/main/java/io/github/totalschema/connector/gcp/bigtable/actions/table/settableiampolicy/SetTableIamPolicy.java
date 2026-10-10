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

package io.github.totalschema.connector.gcp.bigtable.actions.table.settableiampolicy;

import com.google.cloud.Identity;
import com.google.cloud.Policy;
import com.google.cloud.Role;
import com.google.cloud.bigtable.admin.v2.BigtableTableAdminClient;
import io.github.totalschema.connector.gcp.bigtable.actions.table.TableAction;
import io.github.totalschema.connector.gcp.core.model.iam.IamBinding;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Sets IAM policy bindings on a BigTable table.
 *
 * <p>This action uses a read-modify-write pattern to update IAM policies:
 *
 * <ol>
 *   <li>Read the current policy from the table
 *   <li>Add or update the specified role bindings
 *   <li>Write the modified policy back to the table
 * </ol>
 *
 * <p>Example YAML configurations:
 *
 * <pre>
 * # Grant read access to a service account
 * SetTableIamPolicy:
 *   tableId: users
 *   bindings:
 *     - role: roles/bigtable.reader
 *       members:
 *         - serviceAccount:analytics@project.iam.gserviceaccount.com
 *
 * # Grant multiple roles and members
 * SetTableIamPolicy:
 *   tableId: users
 *   bindings:
 *     - role: roles/bigtable.reader
 *       members:
 *         - serviceAccount:analytics@project.iam.gserviceaccount.com
 *         - user:analyst@company.com
 *     - role: roles/bigtable.user
 *       members:
 *         - serviceAccount:app-backend@project.iam.gserviceaccount.com
 *
 * # Multi-tenancy access control
 * SetTableIamPolicy:
 *   tableId: customer_data
 *   bindings:
 *     - role: roles/bigtable.user
 *       members:
 *         - serviceAccount:customer1@project.iam.gserviceaccount.com
 *         - serviceAccount:customer2@project.iam.gserviceaccount.com
 *
 * # Grant access to a group
 * SetTableIamPolicy:
 *   tableId: shared_data
 *   bindings:
 *     - role: roles/bigtable.reader
 *       members:
 *         - group:data-team@company.com
 * </pre>
 *
 * <p><strong>Security Notes:</strong>
 *
 * <ul>
 *   <li>This action adds bindings to the existing policy without removing other bindings
 *   <li>If a role already exists, new members are added to the existing member list
 *   <li>Use {@code RemoveTableIamPolicy} to revoke specific bindings
 *   <li>Member formats: {@code user:email}, {@code serviceAccount:email}, {@code group:email},
 *       {@code domain:domain}
 * </ul>
 */
public class SetTableIamPolicy implements TableAction {

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
        Policy.Builder policyBuilder = currentPolicy.toBuilder();

        // Add or update bindings
        for (IamBinding binding : bindings) {
            Role role = Role.of(binding.getRole());
            List<Identity> identities = new ArrayList<>();

            // Convert string members to Identity objects
            for (String member : binding.getMembers()) {
                identities.add(Identity.valueOf(member));
            }

            // Get existing members for this role (if any) and add them
            Set<Identity> existingIdentities = currentPolicy.getBindings().get(role);
            if (existingIdentities != null) {
                identities.addAll(existingIdentities);
            }

            // Update the role binding - addIdentity requires at least one Identity
            if (!identities.isEmpty()) {
                Identity first = identities.get(0);
                Identity[] rest = identities.subList(1, identities.size()).toArray(new Identity[0]);
                policyBuilder.addIdentity(role, first, rest);
            }
        }

        // Write updated policy back
        adminClient.setIamPolicy(tableId, policyBuilder.build());
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
     * Gets the IAM bindings to set.
     *
     * @return the IAM bindings
     */
    public List<IamBinding> getBindings() {
        return bindings;
    }

    /**
     * Sets the IAM bindings to set.
     *
     * @param bindings the IAM bindings
     */
    public void setBindings(List<IamBinding> bindings) {
        this.bindings = bindings;
    }

    @Override
    public String toString() {
        return "SetTableIamPolicy{" + "tableId='" + tableId + '\'' + ", bindings=" + bindings + '}';
    }
}
