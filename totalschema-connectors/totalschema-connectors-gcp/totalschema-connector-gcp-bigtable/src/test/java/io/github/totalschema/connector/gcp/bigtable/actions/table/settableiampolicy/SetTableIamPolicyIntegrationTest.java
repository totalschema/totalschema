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

import static org.testng.Assert.*;

import com.google.cloud.Identity;
import com.google.cloud.Policy;
import com.google.cloud.Role;
import io.github.totalschema.connector.gcp.bigtable.AbstractBigtableAdminIntegrationTest;
import io.github.totalschema.connector.gcp.bigtable.actions.table.createtable.CreateTable;
import io.github.totalschema.connector.gcp.core.model.iam.IamBinding;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;

/**
 * Integration tests for {@link SetTableIamPolicy} using BigTable emulator in a TestContainer.
 *
 * <p>These tests verify that SetTableIamPolicy works correctly against the BigTable emulator. The
 * tests are skipped if Docker/Podman is not available.
 *
 * <p><strong>Note:</strong> The BigTable emulator has limited IAM policy support. Some tests may
 * verify basic functionality but might not fully replicate production IAM behavior.
 */
@Ignore(
        "Feature is not implemented in the BigTable emulator. Tests are skipped until emulator support is available.")
public class SetTableIamPolicyIntegrationTest extends AbstractBigtableAdminIntegrationTest {

    @Test(description = "Set IAM policy with a single service account binding")
    public void testSetIamPolicyWithSingleServiceAccount() {
        // Given: an existing table
        String tableId = "test-iam-single-sa";
        createEmptyTable(tableId);

        // And: a SetTableIamPolicy action with single service account binding
        SetTableIamPolicy action = new SetTableIamPolicy();
        action.setTableId(tableId);

        IamBinding binding = new IamBinding();
        binding.setRole("roles/bigtable.reader");
        binding.setMembers(
                Arrays.asList("serviceAccount:analytics@project.iam.gserviceaccount.com"));
        action.setBindings(Arrays.asList(binding));

        // When: executing the action
        action.execute(adminClient);

        // Then: the policy should be set
        Policy policy = adminClient.getIamPolicy(tableId);
        assertNotNull(policy, "Policy should be retrievable");

        // Verify the binding exists
        Role role = Role.of("roles/bigtable.reader");
        Set<Identity> identities = policy.getBindings().get(role);
        assertNotNull(identities, "Role binding should exist");
        assertTrue(
                identities.contains(
                        Identity.valueOf(
                                "serviceAccount:analytics@project.iam.gserviceaccount.com")),
                "Service account should be in the binding");
    }

    @Test(description = "Set IAM policy with multiple members for a single role")
    public void testSetIamPolicyWithMultipleMembers() {
        // Given: an existing table
        String tableId = "test-iam-multiple-members";
        createEmptyTable(tableId);

        // And: a SetTableIamPolicy action with multiple members
        SetTableIamPolicy action = new SetTableIamPolicy();
        action.setTableId(tableId);

        IamBinding binding = new IamBinding();
        binding.setRole("roles/bigtable.reader");
        binding.setMembers(
                Arrays.asList(
                        "serviceAccount:analytics@project.iam.gserviceaccount.com",
                        "serviceAccount:reporting@project.iam.gserviceaccount.com",
                        "user:analyst@company.com"));
        action.setBindings(Arrays.asList(binding));

        // When: executing the action
        action.execute(adminClient);

        // Then: all members should be in the policy
        Policy policy = adminClient.getIamPolicy(tableId);
        Role role = Role.of("roles/bigtable.reader");
        Set<Identity> identities = policy.getBindings().get(role);
        assertNotNull(identities, "Role binding should exist");
        assertTrue(identities.size() >= 3, "Should have at least 3 members");
    }

    @Test(description = "Set IAM policy with multiple role bindings")
    public void testSetIamPolicyWithMultipleRoles() {
        // Given: an existing table
        String tableId = "test-iam-multiple-roles";
        createEmptyTable(tableId);

        // And: a SetTableIamPolicy action with multiple roles
        SetTableIamPolicy action = new SetTableIamPolicy();
        action.setTableId(tableId);

        IamBinding readerBinding = new IamBinding();
        readerBinding.setRole("roles/bigtable.reader");
        readerBinding.setMembers(
                Arrays.asList("serviceAccount:analytics@project.iam.gserviceaccount.com"));

        IamBinding userBinding = new IamBinding();
        userBinding.setRole("roles/bigtable.user");
        userBinding.setMembers(
                Arrays.asList("serviceAccount:app-backend@project.iam.gserviceaccount.com"));

        action.setBindings(Arrays.asList(readerBinding, userBinding));

        // When: executing the action
        action.execute(adminClient);

        // Then: both roles should exist in the policy
        Policy policy = adminClient.getIamPolicy(tableId);

        Set<Identity> readerIdentities = policy.getBindings().get(Role.of("roles/bigtable.reader"));
        assertNotNull(readerIdentities, "Reader role binding should exist");

        Set<Identity> userIdentities = policy.getBindings().get(Role.of("roles/bigtable.user"));
        assertNotNull(userIdentities, "User role binding should exist");
    }

    @Test(description = "Set IAM policy preserves existing bindings")
    public void testSetIamPolicyPreservesExistingBindings() {
        // Given: an existing table with an initial IAM policy
        String tableId = "test-iam-preserve-existing";
        createEmptyTable(tableId);

        // Set initial policy
        SetTableIamPolicy initialAction = new SetTableIamPolicy();
        initialAction.setTableId(tableId);

        IamBinding adminBinding = new IamBinding();
        adminBinding.setRole("roles/bigtable.admin");
        adminBinding.setMembers(Arrays.asList("user:admin@company.com"));
        initialAction.setBindings(Arrays.asList(adminBinding));
        initialAction.execute(adminClient);

        // And: a second action to add a new binding
        SetTableIamPolicy action = new SetTableIamPolicy();
        action.setTableId(tableId);

        IamBinding readerBinding = new IamBinding();
        readerBinding.setRole("roles/bigtable.reader");
        readerBinding.setMembers(
                Arrays.asList("serviceAccount:analytics@project.iam.gserviceaccount.com"));
        action.setBindings(Arrays.asList(readerBinding));

        // When: executing the second action
        action.execute(adminClient);

        // Then: both bindings should exist
        Policy policy = adminClient.getIamPolicy(tableId);

        Set<Identity> adminIdentities = policy.getBindings().get(Role.of("roles/bigtable.admin"));
        assertNotNull(adminIdentities, "Admin role binding should still exist");

        Set<Identity> readerIdentities = policy.getBindings().get(Role.of("roles/bigtable.reader"));
        assertNotNull(readerIdentities, "Reader role binding should be added");
    }

    @Test(description = "Set IAM policy adds to existing members for same role")
    public void testSetIamPolicyAddsToExistingMembers() {
        // Given: an existing table with an IAM policy
        String tableId = "test-iam-add-members";
        createEmptyTable(tableId);

        // Set initial policy with one member
        SetTableIamPolicy initialAction = new SetTableIamPolicy();
        initialAction.setTableId(tableId);

        IamBinding initialBinding = new IamBinding();
        initialBinding.setRole("roles/bigtable.reader");
        initialBinding.setMembers(Arrays.asList("user:user1@company.com"));
        initialAction.setBindings(Arrays.asList(initialBinding));
        initialAction.execute(adminClient);

        // And: a second action to add another member to the same role
        SetTableIamPolicy action = new SetTableIamPolicy();
        action.setTableId(tableId);

        IamBinding additionalBinding = new IamBinding();
        additionalBinding.setRole("roles/bigtable.reader");
        additionalBinding.setMembers(Arrays.asList("user:user2@company.com"));
        action.setBindings(Arrays.asList(additionalBinding));

        // When: executing the second action
        action.execute(adminClient);

        // Then: both members should exist for the role
        Policy policy = adminClient.getIamPolicy(tableId);
        Set<Identity> identities = policy.getBindings().get(Role.of("roles/bigtable.reader"));
        assertNotNull(identities, "Reader role binding should exist");
        assertTrue(identities.size() >= 2, "Should have at least 2 members");
        assertTrue(
                identities.contains(Identity.valueOf("user:user1@company.com")),
                "First member should still exist");
        assertTrue(
                identities.contains(Identity.valueOf("user:user2@company.com")),
                "Second member should be added");
    }

    @Test(description = "Set IAM policy with group member")
    public void testSetIamPolicyWithGroup() {
        // Given: an existing table
        String tableId = "test-iam-group";
        createEmptyTable(tableId);

        // And: a SetTableIamPolicy action with group member
        SetTableIamPolicy action = new SetTableIamPolicy();
        action.setTableId(tableId);

        IamBinding binding = new IamBinding();
        binding.setRole("roles/bigtable.reader");
        binding.setMembers(Arrays.asList("group:data-team@company.com"));
        action.setBindings(Arrays.asList(binding));

        // When: executing the action
        action.execute(adminClient);

        // Then: the group should be in the policy
        Policy policy = adminClient.getIamPolicy(tableId);
        Role role = Role.of("roles/bigtable.reader");
        Set<Identity> identities = policy.getBindings().get(role);
        assertNotNull(identities, "Role binding should exist");
        assertTrue(
                identities.contains(Identity.valueOf("group:data-team@company.com")),
                "Group should be in the binding");
    }

    @Test(description = "Set IAM policy with domain member")
    public void testSetIamPolicyWithDomain() {
        // Given: an existing table
        String tableId = "test-iam-domain";
        createEmptyTable(tableId);

        // And: a SetTableIamPolicy action with domain member
        SetTableIamPolicy action = new SetTableIamPolicy();
        action.setTableId(tableId);

        IamBinding binding = new IamBinding();
        binding.setRole("roles/bigtable.reader");
        binding.setMembers(Arrays.asList("domain:example.com"));
        action.setBindings(Arrays.asList(binding));

        // When: executing the action
        action.execute(adminClient);

        // Then: the domain should be in the policy
        Policy policy = adminClient.getIamPolicy(tableId);
        Role role = Role.of("roles/bigtable.reader");
        Set<Identity> identities = policy.getBindings().get(role);
        assertNotNull(identities, "Role binding should exist");
        assertTrue(
                identities.contains(Identity.valueOf("domain:example.com")),
                "Domain should be in the binding");
    }

    @Test(description = "Set IAM policy with user member")
    public void testSetIamPolicyWithUser() {
        // Given: an existing table
        String tableId = "test-iam-user";
        createEmptyTable(tableId);

        // And: a SetTableIamPolicy action with user member
        SetTableIamPolicy action = new SetTableIamPolicy();
        action.setTableId(tableId);

        IamBinding binding = new IamBinding();
        binding.setRole("roles/bigtable.reader");
        binding.setMembers(Arrays.asList("user:analyst@company.com"));
        action.setBindings(Arrays.asList(binding));

        // When: executing the action
        action.execute(adminClient);

        // Then: the user should be in the policy
        Policy policy = adminClient.getIamPolicy(tableId);
        Role role = Role.of("roles/bigtable.reader");
        Set<Identity> identities = policy.getBindings().get(role);
        assertNotNull(identities, "Role binding should exist");
        assertTrue(
                identities.contains(Identity.valueOf("user:analyst@company.com")),
                "User should be in the binding");
    }

    @Test(description = "Set IAM policy multiple times is idempotent")
    public void testSetIamPolicyIdempotent() {
        // Given: an existing table
        String tableId = "test-iam-idempotent";
        createEmptyTable(tableId);

        // And: a SetTableIamPolicy action
        SetTableIamPolicy action = new SetTableIamPolicy();
        action.setTableId(tableId);

        IamBinding binding = new IamBinding();
        binding.setRole("roles/bigtable.reader");
        binding.setMembers(Arrays.asList("user:test@company.com"));
        action.setBindings(Arrays.asList(binding));

        // When: executing the action multiple times
        action.execute(adminClient);
        action.execute(adminClient);
        action.execute(adminClient);

        // Then: the policy should still be valid
        Policy policy = adminClient.getIamPolicy(tableId);
        Set<Identity> identities = policy.getBindings().get(Role.of("roles/bigtable.reader"));
        assertNotNull(identities, "Role binding should exist");
        assertTrue(
                identities.contains(Identity.valueOf("user:test@company.com")),
                "User should be in the binding");
    }

    @Test(description = "Set IAM policy with mixed member types")
    public void testSetIamPolicyWithMixedMemberTypes() {
        // Given: an existing table
        String tableId = "test-iam-mixed-types";
        createEmptyTable(tableId);

        // And: a SetTableIamPolicy action with various member types
        SetTableIamPolicy action = new SetTableIamPolicy();
        action.setTableId(tableId);

        IamBinding binding = new IamBinding();
        binding.setRole("roles/bigtable.reader");
        binding.setMembers(
                Arrays.asList(
                        "user:user@company.com",
                        "serviceAccount:sa@project.iam.gserviceaccount.com",
                        "group:team@company.com",
                        "domain:example.com"));
        action.setBindings(Arrays.asList(binding));

        // When: executing the action
        action.execute(adminClient);

        // Then: all members should be in the policy
        Policy policy = adminClient.getIamPolicy(tableId);
        Set<Identity> identities = policy.getBindings().get(Role.of("roles/bigtable.reader"));
        assertNotNull(identities, "Role binding should exist");
        assertTrue(identities.size() >= 4, "Should have at least 4 members");
    }

    @Test(
            description = "Verify setting IAM policy on non-existent table fails",
            expectedExceptions = Exception.class)
    public void testSetIamPolicyOnNonExistentTableFails() {
        // Given: a SetTableIamPolicy action for non-existent table
        SetTableIamPolicy action = new SetTableIamPolicy();
        action.setTableId("non-existent-table");

        IamBinding binding = new IamBinding();
        binding.setRole("roles/bigtable.reader");
        binding.setMembers(Arrays.asList("user:test@company.com"));
        action.setBindings(Arrays.asList(binding));

        // When/Then: should throw exception
        action.execute(adminClient);
    }

    @Test(
            description = "Verify missing tableId throws IllegalArgumentException",
            expectedExceptions = IllegalArgumentException.class)
    public void testMissingTableIdFails() {
        // Given: a SetTableIamPolicy action without tableId
        SetTableIamPolicy action = new SetTableIamPolicy();
        action.setTableId(null);

        IamBinding binding = new IamBinding();
        binding.setRole("roles/bigtable.reader");
        binding.setMembers(Arrays.asList("user:test@company.com"));
        action.setBindings(Arrays.asList(binding));

        // When/Then: should throw IllegalArgumentException
        action.execute(adminClient);
    }

    @Test(
            description = "Verify empty tableId throws IllegalArgumentException",
            expectedExceptions = IllegalArgumentException.class)
    public void testEmptyTableIdFails() {
        // Given: a SetTableIamPolicy action with empty tableId
        SetTableIamPolicy action = new SetTableIamPolicy();
        action.setTableId("");

        IamBinding binding = new IamBinding();
        binding.setRole("roles/bigtable.reader");
        binding.setMembers(Arrays.asList("user:test@company.com"));
        action.setBindings(Arrays.asList(binding));

        // When/Then: should throw IllegalArgumentException
        action.execute(adminClient);
    }

    @Test(
            description = "Verify missing bindings throws IllegalArgumentException",
            expectedExceptions = IllegalArgumentException.class)
    public void testMissingBindingsFails() {
        // Given: a SetTableIamPolicy action without bindings
        SetTableIamPolicy action = new SetTableIamPolicy();
        action.setTableId("test-table");
        action.setBindings(null);

        // When/Then: should throw IllegalArgumentException
        action.execute(adminClient);
    }

    @Test(
            description = "Verify empty bindings throws IllegalArgumentException",
            expectedExceptions = IllegalArgumentException.class)
    public void testEmptyBindingsFails() {
        // Given: a SetTableIamPolicy action with empty bindings
        SetTableIamPolicy action = new SetTableIamPolicy();
        action.setTableId("test-table");
        action.setBindings(Arrays.asList());

        // When/Then: should throw IllegalArgumentException
        action.execute(adminClient);
    }

    @Test(
            description = "Verify binding without role throws IllegalArgumentException",
            expectedExceptions = IllegalArgumentException.class)
    public void testBindingWithoutRoleFails() {
        // Given: an existing table
        String tableId = "test-iam-no-role";
        createEmptyTable(tableId);

        // And: a SetTableIamPolicy action with binding without role
        SetTableIamPolicy action = new SetTableIamPolicy();
        action.setTableId(tableId);

        IamBinding binding = new IamBinding();
        binding.setRole(null);
        binding.setMembers(Arrays.asList("user:test@company.com"));
        action.setBindings(Arrays.asList(binding));

        // When/Then: should throw IllegalArgumentException during validation
        action.execute(adminClient);
    }

    @Test(
            description = "Verify binding without members throws IllegalArgumentException",
            expectedExceptions = IllegalArgumentException.class)
    public void testBindingWithoutMembersFails() {
        // Given: an existing table
        String tableId = "test-iam-no-members";
        createEmptyTable(tableId);

        // And: a SetTableIamPolicy action with binding without members
        SetTableIamPolicy action = new SetTableIamPolicy();
        action.setTableId(tableId);

        IamBinding binding = new IamBinding();
        binding.setRole("roles/bigtable.reader");
        binding.setMembers(null);
        action.setBindings(Arrays.asList(binding));

        // When/Then: should throw IllegalArgumentException during validation
        action.execute(adminClient);
    }

    @Test(
            description = "Verify binding with empty members list throws IllegalArgumentException",
            expectedExceptions = IllegalArgumentException.class)
    public void testBindingWithEmptyMembersListFails() {
        // Given: an existing table
        String tableId = "test-iam-empty-members";
        createEmptyTable(tableId);

        // And: a SetTableIamPolicy action with binding with empty members
        SetTableIamPolicy action = new SetTableIamPolicy();
        action.setTableId(tableId);

        IamBinding binding = new IamBinding();
        binding.setRole("roles/bigtable.reader");
        binding.setMembers(Arrays.asList());
        action.setBindings(Arrays.asList(binding));

        // When/Then: should throw IllegalArgumentException during validation
        action.execute(adminClient);
    }

    @Test(description = "Verify SetTableIamPolicy toString contains all fields")
    public void testSetTableIamPolicyToString() {
        // Given: a SetTableIamPolicy action with bindings
        SetTableIamPolicy action = new SetTableIamPolicy();
        action.setTableId("test-table");

        IamBinding binding = new IamBinding();
        binding.setRole("roles/bigtable.reader");
        binding.setMembers(Arrays.asList("user:test@company.com"));
        action.setBindings(Arrays.asList(binding));

        // When: calling toString
        String result = action.toString();

        // Then: it should contain all fields
        assertTrue(result.contains("test-table"), "toString should contain tableId");
        assertTrue(result.contains("SetTableIamPolicy"), "toString should contain class name");
        assertTrue(result.contains("bindings"), "toString should mention bindings");
    }

    @Test(description = "Verify SetTableIamPolicy getters and setters")
    public void testSetTableIamPolicyGettersSetters() {
        // Given: a SetTableIamPolicy action
        SetTableIamPolicy action = new SetTableIamPolicy();

        // When: setting values
        action.setTableId("my-table");

        IamBinding binding = new IamBinding();
        binding.setRole("roles/bigtable.reader");
        binding.setMembers(Arrays.asList("user:test@company.com"));
        List<IamBinding> bindings = Arrays.asList(binding);
        action.setBindings(bindings);

        // Then: getters should return the values
        assertEquals(action.getTableId(), "my-table", "tableId should match");
        assertEquals(action.getBindings(), bindings, "bindings should match");
        assertEquals(action.getBindings().size(), 1, "should have 1 binding");
        assertEquals(
                action.getBindings().get(0).getRole(),
                "roles/bigtable.reader",
                "role should match");
    }

    @Test(description = "Verify IamBinding getters and setters")
    public void testIamBindingGettersSetters() {
        // Given: an IamBinding
        IamBinding binding = new IamBinding();

        // When: setting values
        binding.setRole("roles/bigtable.user");
        List<String> members = Arrays.asList("serviceAccount:app@project.iam.gserviceaccount.com");
        binding.setMembers(members);

        // Then: getters should return the values
        assertEquals(binding.getRole(), "roles/bigtable.user", "role should match");
        assertEquals(binding.getMembers(), members, "members should match");
    }

    @Test(description = "Verify IamBinding toString contains all fields")
    public void testIamBindingToString() {
        // Given: an IamBinding
        IamBinding binding = new IamBinding();
        binding.setRole("roles/bigtable.reader");
        binding.setMembers(Arrays.asList("user:test@company.com"));

        // When: calling toString
        String result = binding.toString();

        // Then: it should contain all fields
        assertTrue(result.contains("roles/bigtable.reader"), "toString should contain role");
        assertTrue(result.contains("user:test@company.com"), "toString should contain member");
        assertTrue(result.contains("IamBinding"), "toString should contain class name");
    }

    /**
     * Helper method to create an empty table for testing.
     *
     * @param tableId the ID of the table to create
     */
    private void createEmptyTable(String tableId) {
        CreateTable createTable = new CreateTable();
        createTable.setTableId(tableId);
        createTable.execute(adminClient);
    }
}
