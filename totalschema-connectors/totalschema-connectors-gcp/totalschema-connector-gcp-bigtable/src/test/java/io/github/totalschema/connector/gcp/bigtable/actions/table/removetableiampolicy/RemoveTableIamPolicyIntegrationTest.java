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

import static org.testng.Assert.*;

import com.google.cloud.Identity;
import com.google.cloud.Policy;
import com.google.cloud.Role;
import io.github.totalschema.connector.gcp.bigtable.AbstractBigtableAdminIntegrationTest;
import io.github.totalschema.connector.gcp.bigtable.actions.table.createtable.ColumnFamily;
import io.github.totalschema.connector.gcp.bigtable.actions.table.createtable.CreateTable;
import io.github.totalschema.connector.gcp.bigtable.actions.table.settableiampolicy.SetTableIamPolicy;
import io.github.totalschema.connector.gcp.core.model.iam.IamBinding;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;

/**
 * Integration tests for {@link RemoveTableIamPolicy} using BigTable emulator in a TestContainer.
 *
 * <p>These tests verify that RemoveTableIamPolicy works correctly against the BigTable emulator.
 * The tests are skipped if Docker/Podman is not available.
 */
@Ignore(
        "Feature is not implemented in the BigTable emulator. Tests are skipped until emulator support is available.")
public class RemoveTableIamPolicyIntegrationTest extends AbstractBigtableAdminIntegrationTest {

    @Test(description = "Remove a single member from a single role")
    public void testRemoveSingleMemberFromSingleRole() {
        // Given: a table with IAM policy bindings
        String tableId = "test-remove-single-member";
        createTableWithIamPolicy(
                tableId,
                "roles/bigtable.reader",
                Arrays.asList(
                        "user:alice@company.com",
                        "user:bob@company.com",
                        "user:charlie@company.com"));

        // And: a RemoveTableIamPolicy action to remove one member
        RemoveTableIamPolicy action = new RemoveTableIamPolicy();
        action.setTableId(tableId);

        IamBinding binding = new IamBinding();
        binding.setRole("roles/bigtable.reader");
        binding.setMembers(Collections.singletonList("user:bob@company.com"));
        action.setBindings(Collections.singletonList(binding));

        // When: executing the action
        action.execute(adminClient);

        // Then: the specified member should be removed but others remain
        Policy policy = adminClient.getIamPolicy(tableId);
        Role role = Role.of("roles/bigtable.reader");
        Set<Identity> members = policy.getBindings().get(role);

        assertNotNull(members, "Role should still exist");
        assertEquals(members.size(), 2, "Should have 2 members remaining");
        assertTrue(
                members.contains(Identity.valueOf("user:alice@company.com")),
                "Alice should remain");
        assertFalse(
                members.contains(Identity.valueOf("user:bob@company.com")),
                "Bob should be removed");
        assertTrue(
                members.contains(Identity.valueOf("user:charlie@company.com")),
                "Charlie should remain");
    }

    @Test(description = "Remove multiple members from a single role")
    public void testRemoveMultipleMembersFromSingleRole() {
        // Given: a table with IAM policy bindings
        String tableId = "test-remove-multiple-members";
        createTableWithIamPolicy(
                tableId,
                "roles/bigtable.reader",
                Arrays.asList(
                        "user:alice@company.com",
                        "user:bob@company.com",
                        "user:charlie@company.com",
                        "user:dave@company.com"));

        // And: a RemoveTableIamPolicy action to remove multiple members
        RemoveTableIamPolicy action = new RemoveTableIamPolicy();
        action.setTableId(tableId);

        IamBinding binding = new IamBinding();
        binding.setRole("roles/bigtable.reader");
        binding.setMembers(Arrays.asList("user:bob@company.com", "user:charlie@company.com"));
        action.setBindings(Collections.singletonList(binding));

        // When: executing the action
        action.execute(adminClient);

        // Then: the specified members should be removed but others remain
        Policy policy = adminClient.getIamPolicy(tableId);
        Role role = Role.of("roles/bigtable.reader");
        Set<Identity> members = policy.getBindings().get(role);

        assertNotNull(members, "Role should still exist");
        assertEquals(members.size(), 2, "Should have 2 members remaining");
        assertTrue(
                members.contains(Identity.valueOf("user:alice@company.com")),
                "Alice should remain");
        assertTrue(
                members.contains(Identity.valueOf("user:dave@company.com")), "Dave should remain");
    }

    @Test(description = "Remove members from multiple roles")
    public void testRemoveMembersFromMultipleRoles() {
        // Given: a table with IAM policy bindings in multiple roles
        String tableId = "test-remove-multiple-roles";
        createTableWithMultipleRoles(tableId);

        // And: a RemoveTableIamPolicy action to remove members from multiple roles
        RemoveTableIamPolicy action = new RemoveTableIamPolicy();
        action.setTableId(tableId);

        IamBinding binding1 = new IamBinding();
        binding1.setRole("roles/bigtable.reader");
        binding1.setMembers(Collections.singletonList("user:reader1@company.com"));

        IamBinding binding2 = new IamBinding();
        binding2.setRole("roles/bigtable.user");
        binding2.setMembers(Collections.singletonList("user:user1@company.com"));

        action.setBindings(Arrays.asList(binding1, binding2));

        // When: executing the action
        action.execute(adminClient);

        // Then: specified members should be removed from their respective roles
        Policy policy = adminClient.getIamPolicy(tableId);

        Role readerRole = Role.of("roles/bigtable.reader");
        Set<Identity> readerMembers = policy.getBindings().get(readerRole);
        assertNotNull(readerMembers, "Reader role should still exist");
        assertEquals(readerMembers.size(), 1, "Should have 1 reader remaining");
        assertTrue(
                readerMembers.contains(Identity.valueOf("user:reader2@company.com")),
                "reader2 should remain");

        Role userRole = Role.of("roles/bigtable.user");
        Set<Identity> userMembers = policy.getBindings().get(userRole);
        assertNotNull(userMembers, "User role should still exist");
        assertEquals(userMembers.size(), 1, "Should have 1 user remaining");
        assertTrue(
                userMembers.contains(Identity.valueOf("user:user2@company.com")),
                "user2 should remain");
    }

    @Test(description = "Remove all members from a role - role should be removed")
    public void testRemoveAllMembersFromRole() {
        // Given: a table with IAM policy bindings
        String tableId = "test-remove-all-members";
        createTableWithIamPolicy(
                tableId,
                "roles/bigtable.reader",
                Arrays.asList("user:alice@company.com", "user:bob@company.com"));

        // And: a RemoveTableIamPolicy action to remove all members
        RemoveTableIamPolicy action = new RemoveTableIamPolicy();
        action.setTableId(tableId);

        IamBinding binding = new IamBinding();
        binding.setRole("roles/bigtable.reader");
        binding.setMembers(Arrays.asList("user:alice@company.com", "user:bob@company.com"));
        action.setBindings(Collections.singletonList(binding));

        // When: executing the action
        action.execute(adminClient);

        // Then: the role should no longer exist in the policy
        Policy policy = adminClient.getIamPolicy(tableId);
        Role role = Role.of("roles/bigtable.reader");
        Set<Identity> members = policy.getBindings().get(role);

        // The role may be null or empty depending on implementation
        assertTrue(
                members == null || members.isEmpty(),
                "Role should be removed or empty when all members are removed");
    }

    @Test(description = "Remove a member that doesn't exist - should succeed silently")
    public void testRemoveNonExistentMember() {
        // Given: a table with IAM policy bindings
        String tableId = "test-remove-nonexistent-member";
        createTableWithIamPolicy(
                tableId,
                "roles/bigtable.reader",
                Collections.singletonList("user:alice@company.com"));

        // And: a RemoveTableIamPolicy action to remove a non-existent member
        RemoveTableIamPolicy action = new RemoveTableIamPolicy();
        action.setTableId(tableId);

        IamBinding binding = new IamBinding();
        binding.setRole("roles/bigtable.reader");
        binding.setMembers(Collections.singletonList("user:nonexistent@company.com"));
        action.setBindings(Collections.singletonList(binding));

        // When: executing the action (should succeed silently)
        action.execute(adminClient);

        // Then: the existing member should remain unchanged
        Policy policy = adminClient.getIamPolicy(tableId);
        Role role = Role.of("roles/bigtable.reader");
        Set<Identity> members = policy.getBindings().get(role);

        assertNotNull(members, "Role should still exist");
        assertEquals(members.size(), 1, "Should still have 1 member");
        assertTrue(
                members.contains(Identity.valueOf("user:alice@company.com")),
                "Alice should remain");
    }

    @Test(description = "Remove members from a role that doesn't exist - should succeed silently")
    public void testRemoveFromNonExistentRole() {
        // Given: a table with IAM policy bindings in one role
        String tableId = "test-remove-nonexistent-role";
        createTableWithIamPolicy(
                tableId,
                "roles/bigtable.reader",
                Collections.singletonList("user:alice@company.com"));

        // And: a RemoveTableIamPolicy action for a different role
        RemoveTableIamPolicy action = new RemoveTableIamPolicy();
        action.setTableId(tableId);

        IamBinding binding = new IamBinding();
        binding.setRole("roles/bigtable.admin");
        binding.setMembers(Collections.singletonList("user:bob@company.com"));
        action.setBindings(Collections.singletonList(binding));

        // When: executing the action (should succeed silently)
        action.execute(adminClient);

        // Then: the existing role should remain unchanged
        Policy policy = adminClient.getIamPolicy(tableId);
        Role readerRole = Role.of("roles/bigtable.reader");
        Set<Identity> members = policy.getBindings().get(readerRole);

        assertNotNull(members, "Reader role should still exist");
        assertEquals(members.size(), 1, "Should still have 1 reader");
        assertTrue(
                members.contains(Identity.valueOf("user:alice@company.com")),
                "Alice should remain");

        Role adminRole = Role.of("roles/bigtable.admin");
        Set<Identity> adminMembers = policy.getBindings().get(adminRole);
        assertTrue(
                adminMembers == null || adminMembers.isEmpty(),
                "Admin role should not exist or be empty");
    }

    @Test(description = "Remove using service account identity")
    public void testRemoveServiceAccount() {
        // Given: a table with service account bindings
        String tableId = "test-remove-service-account";
        createTableWithIamPolicy(
                tableId,
                "roles/bigtable.user",
                Arrays.asList(
                        "serviceAccount:app1@project.iam.gserviceaccount.com",
                        "serviceAccount:app2@project.iam.gserviceaccount.com"));

        // And: a RemoveTableIamPolicy action to remove one service account
        RemoveTableIamPolicy action = new RemoveTableIamPolicy();
        action.setTableId(tableId);

        IamBinding binding = new IamBinding();
        binding.setRole("roles/bigtable.user");
        binding.setMembers(
                Collections.singletonList("serviceAccount:app1@project.iam.gserviceaccount.com"));
        action.setBindings(Collections.singletonList(binding));

        // When: executing the action
        action.execute(adminClient);

        // Then: the service account should be removed
        Policy policy = adminClient.getIamPolicy(tableId);
        Role role = Role.of("roles/bigtable.user");
        Set<Identity> members = policy.getBindings().get(role);

        assertNotNull(members, "Role should still exist");
        assertEquals(members.size(), 1, "Should have 1 member remaining");
        assertTrue(
                members.contains(
                        Identity.valueOf("serviceAccount:app2@project.iam.gserviceaccount.com")),
                "app2 service account should remain");
    }

    @Test(description = "Remove and verify idempotency - removing same member twice")
    public void testIdempotency() {
        // Given: a table with IAM policy bindings
        String tableId = "test-idempotency";
        createTableWithIamPolicy(
                tableId,
                "roles/bigtable.reader",
                Arrays.asList("user:alice@company.com", "user:bob@company.com"));

        // And: a RemoveTableIamPolicy action
        RemoveTableIamPolicy action = new RemoveTableIamPolicy();
        action.setTableId(tableId);

        IamBinding binding = new IamBinding();
        binding.setRole("roles/bigtable.reader");
        binding.setMembers(Collections.singletonList("user:bob@company.com"));
        action.setBindings(Collections.singletonList(binding));

        // When: executing the action twice
        action.execute(adminClient);
        action.execute(adminClient);

        // Then: the result should be the same
        Policy policy = adminClient.getIamPolicy(tableId);
        Role role = Role.of("roles/bigtable.reader");
        Set<Identity> members = policy.getBindings().get(role);

        assertNotNull(members, "Role should still exist");
        assertEquals(members.size(), 1, "Should have 1 member remaining");
        assertTrue(
                members.contains(Identity.valueOf("user:alice@company.com")),
                "Alice should remain");
    }

    @Test(description = "Complex scenario - remove some members, keep others across multiple roles")
    public void testComplexRemovalScenario() {
        // Given: a table with complex IAM policy
        String tableId = "test-complex-removal";
        createTableWithComplexIamPolicy(tableId);

        // And: a RemoveTableIamPolicy action to selectively remove members
        RemoveTableIamPolicy action = new RemoveTableIamPolicy();
        action.setTableId(tableId);

        IamBinding binding1 = new IamBinding();
        binding1.setRole("roles/bigtable.reader");
        binding1.setMembers(Arrays.asList("user:contractor@company.com", "user:temp@company.com"));

        IamBinding binding2 = new IamBinding();
        binding2.setRole("roles/bigtable.user");
        binding2.setMembers(
                Collections.singletonList(
                        "serviceAccount:old-app@project.iam.gserviceaccount.com"));

        action.setBindings(Arrays.asList(binding1, binding2));

        // When: executing the action
        action.execute(adminClient);

        // Then: only specified members should be removed
        Policy policy = adminClient.getIamPolicy(tableId);

        // Check reader role
        Role readerRole = Role.of("roles/bigtable.reader");
        Set<Identity> readerMembers = policy.getBindings().get(readerRole);
        assertNotNull(readerMembers, "Reader role should still exist");
        assertEquals(readerMembers.size(), 2, "Should have 2 readers remaining");
        assertTrue(
                readerMembers.contains(Identity.valueOf("user:alice@company.com")),
                "alice should remain");
        assertTrue(
                readerMembers.contains(Identity.valueOf("user:bob@company.com")),
                "bob should remain");

        // Check user role
        Role userRole = Role.of("roles/bigtable.user");
        Set<Identity> userMembers = policy.getBindings().get(userRole);
        assertNotNull(userMembers, "User role should still exist");
        assertEquals(userMembers.size(), 1, "Should have 1 user remaining");
        assertTrue(
                userMembers.contains(
                        Identity.valueOf("serviceAccount:app@project.iam.gserviceaccount.com")),
                "app service account should remain");

        // Check admin role (should remain unchanged)
        Role adminRole = Role.of("roles/bigtable.admin");
        Set<Identity> adminMembers = policy.getBindings().get(adminRole);
        assertNotNull(adminMembers, "Admin role should still exist");
        assertEquals(adminMembers.size(), 1, "Should have 1 admin");
        assertTrue(
                adminMembers.contains(Identity.valueOf("user:admin@company.com")),
                "admin should remain");
    }

    @Test(
            description = "Verify removing from non-existent table fails",
            expectedExceptions = Exception.class)
    public void testRemoveFromNonExistentTableFails() {
        // Given: a RemoveTableIamPolicy action for non-existent table
        RemoveTableIamPolicy action = new RemoveTableIamPolicy();
        action.setTableId("non-existent-table");

        IamBinding binding = new IamBinding();
        binding.setRole("roles/bigtable.reader");
        binding.setMembers(Collections.singletonList("user:alice@company.com"));
        action.setBindings(Collections.singletonList(binding));

        // When/Then: should throw exception
        action.execute(adminClient);
    }

    @Test(
            description = "Verify missing tableId throws IllegalArgumentException",
            expectedExceptions = IllegalArgumentException.class)
    public void testMissingTableIdFails() {
        // Given: a RemoveTableIamPolicy action without tableId
        RemoveTableIamPolicy action = new RemoveTableIamPolicy();
        action.setBindings(createValidBindings());

        // When/Then: should throw IllegalArgumentException
        action.execute(adminClient);
    }

    @Test(
            description = "Verify missing bindings throws IllegalArgumentException",
            expectedExceptions = IllegalArgumentException.class)
    public void testMissingBindingsFails() {
        // Given: a RemoveTableIamPolicy action without bindings
        RemoveTableIamPolicy action = new RemoveTableIamPolicy();
        action.setTableId("test-table");

        // When/Then: should throw IllegalArgumentException
        action.execute(adminClient);
    }

    @Test(
            description = "Verify empty bindings throws IllegalArgumentException",
            expectedExceptions = IllegalArgumentException.class)
    public void testEmptyBindingsFails() {
        // Given: a RemoveTableIamPolicy action with empty bindings
        RemoveTableIamPolicy action = new RemoveTableIamPolicy();
        action.setTableId("test-table");
        action.setBindings(Collections.emptyList());

        // When/Then: should throw IllegalArgumentException
        action.execute(adminClient);
    }

    // ==================== Helper Methods ====================

    /**
     * Creates a table with a single column family and sets up IAM policy with specified role and
     * members.
     *
     * @param tableId the ID of the table to create
     * @param role the IAM role to set
     * @param members the members to add to the role
     */
    private void createTableWithIamPolicy(String tableId, String role, List<String> members) {
        // Create table
        CreateTable createTable = new CreateTable();
        createTable.setTableId(tableId);

        ColumnFamily family = new ColumnFamily();
        family.setName("cf");
        createTable.setFamilies(Collections.singletonList(family));
        createTable.execute(adminClient);

        // Set IAM policy
        SetTableIamPolicy setPolicy = new SetTableIamPolicy();
        setPolicy.setTableId(tableId);

        IamBinding binding = new IamBinding();
        binding.setRole(role);
        binding.setMembers(members);
        setPolicy.setBindings(Collections.singletonList(binding));
        setPolicy.execute(adminClient);
    }

    /**
     * Creates a table with multiple IAM roles for testing multi-role scenarios.
     *
     * @param tableId the ID of the table to create
     */
    private void createTableWithMultipleRoles(String tableId) {
        // Create table
        CreateTable createTable = new CreateTable();
        createTable.setTableId(tableId);

        ColumnFamily family = new ColumnFamily();
        family.setName("cf");
        createTable.setFamilies(Collections.singletonList(family));
        createTable.execute(adminClient);

        // Set IAM policy with multiple roles
        SetTableIamPolicy setPolicy = new SetTableIamPolicy();
        setPolicy.setTableId(tableId);

        IamBinding readerBinding = new IamBinding();
        readerBinding.setRole("roles/bigtable.reader");
        readerBinding.setMembers(
                Arrays.asList("user:reader1@company.com", "user:reader2@company.com"));

        IamBinding userBinding = new IamBinding();
        userBinding.setRole("roles/bigtable.user");
        userBinding.setMembers(Arrays.asList("user:user1@company.com", "user:user2@company.com"));

        setPolicy.setBindings(Arrays.asList(readerBinding, userBinding));
        setPolicy.execute(adminClient);
    }

    /**
     * Creates a table with complex IAM policy for comprehensive testing.
     *
     * @param tableId the ID of the table to create
     */
    private void createTableWithComplexIamPolicy(String tableId) {
        // Create table
        CreateTable createTable = new CreateTable();
        createTable.setTableId(tableId);

        ColumnFamily family = new ColumnFamily();
        family.setName("cf");
        createTable.setFamilies(Collections.singletonList(family));
        createTable.execute(adminClient);

        // Set complex IAM policy
        SetTableIamPolicy setPolicy = new SetTableIamPolicy();
        setPolicy.setTableId(tableId);

        IamBinding readerBinding = new IamBinding();
        readerBinding.setRole("roles/bigtable.reader");
        readerBinding.setMembers(
                Arrays.asList(
                        "user:alice@company.com",
                        "user:bob@company.com",
                        "user:contractor@company.com",
                        "user:temp@company.com"));

        IamBinding userBinding = new IamBinding();
        userBinding.setRole("roles/bigtable.user");
        userBinding.setMembers(
                Arrays.asList(
                        "serviceAccount:app@project.iam.gserviceaccount.com",
                        "serviceAccount:old-app@project.iam.gserviceaccount.com"));

        IamBinding adminBinding = new IamBinding();
        adminBinding.setRole("roles/bigtable.admin");
        adminBinding.setMembers(Collections.singletonList("user:admin@company.com"));

        setPolicy.setBindings(Arrays.asList(readerBinding, userBinding, adminBinding));
        setPolicy.execute(adminClient);
    }

    /**
     * Helper method to create valid bindings for testing.
     *
     * @return a list of valid IAM bindings
     */
    private List<IamBinding> createValidBindings() {
        IamBinding binding = new IamBinding();
        binding.setRole("roles/bigtable.reader");
        binding.setMembers(Collections.singletonList("user:test@company.com"));
        return Collections.singletonList(binding);
    }
}
