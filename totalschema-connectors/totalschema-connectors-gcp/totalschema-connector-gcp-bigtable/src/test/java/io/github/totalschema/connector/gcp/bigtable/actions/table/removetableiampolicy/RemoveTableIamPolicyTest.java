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

import static org.easymock.EasyMock.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.*;

import com.google.cloud.Identity;
import com.google.cloud.Policy;
import com.google.cloud.Role;
import com.google.cloud.bigtable.admin.v2.BigtableTableAdminClient;
import io.github.totalschema.connector.gcp.bigtable.actions.Action;
import io.github.totalschema.connector.gcp.bigtable.actions.ActionFactory;
import io.github.totalschema.connector.gcp.core.model.iam.IamBinding;
import io.github.totalschema.engine.api.Context;
import io.github.totalschema.spi.expression.evaluator.ExpressionEvaluator;
import io.github.totalschema.spi.variables.VariableService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Tests for {@link RemoveTableIamPolicy} and {@link RemoveTableIamPolicyActionFactory}. */
public class RemoveTableIamPolicyTest {

    private Context mockContext;
    private ExpressionEvaluator mockEvaluator;
    private VariableService mockVariableService;

    @BeforeMethod
    public void setUp() {
        mockContext = createMock(Context.class);
        mockEvaluator = createMock(ExpressionEvaluator.class);
        mockVariableService = createMock(VariableService.class);

        expect(mockContext.get(ExpressionEvaluator.class)).andReturn(mockEvaluator).anyTimes();
        expect(mockContext.get(VariableService.class)).andReturn(mockVariableService).anyTimes();
        expect(mockVariableService.getEffectiveVariables(mockContext))
                .andReturn(Collections.emptyMap())
                .anyTimes();
        expect(mockEvaluator.evaluate(anyObject(String.class), anyObject(Map.class)))
                .andAnswer(() -> (String) getCurrentArguments()[0])
                .anyTimes();

        replay(mockContext, mockEvaluator, mockVariableService);
    }

    @Test
    public void testRemoveTableIamPolicyFromMap() {
        // Given: a YAML-like configuration map
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> actionConfig = new HashMap<>();
        actionConfig.put("tableId", "users");

        List<Map<String, Object>> bindingsConfig = new ArrayList<>();
        Map<String, Object> binding1 = new HashMap<>();
        binding1.put("role", "roles/bigtable.reader");
        binding1.put("members", Collections.singletonList("user:ex-employee@company.com"));
        bindingsConfig.add(binding1);

        actionConfig.put("bindings", bindingsConfig);
        config.put("RemoveTableIamPolicy", actionConfig);

        // When: creating an action from the config
        Action<?> action = ActionFactory.getAction(config, mockContext);

        // Then: it should be a RemoveTableIamPolicy instance with correct values
        assertNotNull(action);
        assertTrue(action instanceof RemoveTableIamPolicy);

        RemoveTableIamPolicy removePolicy = (RemoveTableIamPolicy) action;
        assertEquals(removePolicy.getTableId(), "users");
        assertNotNull(removePolicy.getBindings());
        assertEquals(removePolicy.getBindings().size(), 1);
        assertEquals(removePolicy.getBindings().get(0).getRole(), "roles/bigtable.reader");
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*tableId is required.*")
    public void testRemoveTableIamPolicyRequiresTableId() {
        // Given: an action without tableId
        RemoveTableIamPolicy action = new RemoveTableIamPolicy();
        action.setTableId(null);
        action.setBindings(createValidBindings());

        // When/Then: execute should throw exception
        action.execute(null);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*bindings is required.*")
    public void testRemoveTableIamPolicyRequiresBindings() {
        // Given: an action without bindings
        RemoveTableIamPolicy action = new RemoveTableIamPolicy();
        action.setTableId("users");
        action.setBindings(null);

        // When/Then: execute should throw exception
        action.execute(null);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*bindings is required.*")
    public void testRemoveTableIamPolicyRequiresNonEmptyBindings() {
        // Given: an action with empty bindings
        RemoveTableIamPolicy action = new RemoveTableIamPolicy();
        action.setTableId("users");
        action.setBindings(Collections.emptyList());

        // When/Then: execute should throw exception
        action.execute(null);
    }

    @Test
    public void testRemoveTableIamPolicyExecute() {
        // Given: a valid action that removes one member from a role
        RemoveTableIamPolicy action = new RemoveTableIamPolicy();
        action.setTableId("users");

        IamBinding binding = new IamBinding();
        binding.setRole("roles/bigtable.reader");
        binding.setMembers(
                Collections.singletonList("serviceAccount:test@project.iam.gserviceaccount.com"));
        action.setBindings(Collections.singletonList(binding));

        BigtableTableAdminClient mockClient = mock(BigtableTableAdminClient.class);

        // Mock a current policy with multiple members, one of which should be removed
        Policy.Builder policyBuilder = Policy.newBuilder();
        Role role = Role.of("roles/bigtable.reader");
        policyBuilder.addIdentity(
                role,
                Identity.valueOf("serviceAccount:test@project.iam.gserviceaccount.com"),
                Identity.valueOf("user:keep@company.com"));
        Policy currentPolicy = policyBuilder.build();

        when(mockClient.getIamPolicy("users")).thenReturn(currentPolicy);
        when(mockClient.setIamPolicy(eq("users"), any(Policy.class))).thenReturn(null);

        // When: executing the action
        action.execute(mockClient);

        // Then: verify the client was called
        verify(mockClient).getIamPolicy("users");

        // Capture and verify the modified policy
        org.mockito.ArgumentCaptor<Policy> policyCaptor =
                org.mockito.ArgumentCaptor.forClass(Policy.class);
        verify(mockClient).setIamPolicy(eq("users"), policyCaptor.capture());

        Policy modifiedPolicy = policyCaptor.getValue();
        // Verify the serviceAccount was removed but user remains
        assertFalse(
                modifiedPolicy
                        .getBindings()
                        .get(role)
                        .contains(
                                Identity.valueOf(
                                        "serviceAccount:test@project.iam.gserviceaccount.com")),
                "Expected serviceAccount to be removed from policy");
        assertTrue(
                modifiedPolicy
                        .getBindings()
                        .get(role)
                        .contains(Identity.valueOf("user:keep@company.com")),
                "Expected user to remain in policy");
    }

    @Test
    public void testRemoveTableIamPolicyExecuteRemovesAllSpecifiedMembers() {
        // Given: action that removes multiple members from a role
        RemoveTableIamPolicy action = new RemoveTableIamPolicy();
        action.setTableId("users");

        IamBinding binding = new IamBinding();
        binding.setRole("roles/bigtable.reader");
        binding.setMembers(
                Arrays.asList(
                        "serviceAccount:old1@project.iam.gserviceaccount.com",
                        "serviceAccount:old2@project.iam.gserviceaccount.com"));
        action.setBindings(Collections.singletonList(binding));

        BigtableTableAdminClient mockClient = mock(BigtableTableAdminClient.class);

        // Mock current policy with multiple members
        Policy.Builder policyBuilder = Policy.newBuilder();
        Role role = Role.of("roles/bigtable.reader");
        policyBuilder.addIdentity(
                role,
                Identity.valueOf("serviceAccount:old1@project.iam.gserviceaccount.com"),
                Identity.valueOf("serviceAccount:old2@project.iam.gserviceaccount.com"),
                Identity.valueOf("user:keeper@company.com"));
        Policy currentPolicy = policyBuilder.build();

        when(mockClient.getIamPolicy("users")).thenReturn(currentPolicy);
        when(mockClient.setIamPolicy(eq("users"), any(Policy.class))).thenReturn(null);

        // When: executing the action
        action.execute(mockClient);

        // Then: verify both serviceAccounts were removed
        org.mockito.ArgumentCaptor<Policy> policyCaptor =
                org.mockito.ArgumentCaptor.forClass(Policy.class);
        verify(mockClient).setIamPolicy(eq("users"), policyCaptor.capture());

        Policy modifiedPolicy = policyCaptor.getValue();
        assertFalse(
                modifiedPolicy
                        .getBindings()
                        .get(role)
                        .contains(
                                Identity.valueOf(
                                        "serviceAccount:old1@project.iam.gserviceaccount.com")),
                "Expected old1 serviceAccount to be removed");
        assertFalse(
                modifiedPolicy
                        .getBindings()
                        .get(role)
                        .contains(
                                Identity.valueOf(
                                        "serviceAccount:old2@project.iam.gserviceaccount.com")),
                "Expected old2 serviceAccount to be removed");
        assertTrue(
                modifiedPolicy
                        .getBindings()
                        .get(role)
                        .contains(Identity.valueOf("user:keeper@company.com")),
                "Expected keeper user to remain");
    }

    @Test
    public void testRemoveTableIamPolicyExecuteRemovesFromMultipleRoles() {
        // Given: action that removes members from multiple different roles
        RemoveTableIamPolicy action = new RemoveTableIamPolicy();
        action.setTableId("users");

        IamBinding binding1 = new IamBinding();
        binding1.setRole("roles/bigtable.reader");
        binding1.setMembers(Collections.singletonList("user:reader@company.com"));

        IamBinding binding2 = new IamBinding();
        binding2.setRole("roles/bigtable.user");
        binding2.setMembers(Collections.singletonList("user:writer@company.com"));

        action.setBindings(Arrays.asList(binding1, binding2));

        BigtableTableAdminClient mockClient = mock(BigtableTableAdminClient.class);

        // Mock current policy with members in both roles
        Policy.Builder policyBuilder = Policy.newBuilder();
        Role readerRole = Role.of("roles/bigtable.reader");
        Role userRole = Role.of("roles/bigtable.user");
        policyBuilder.addIdentity(
                readerRole,
                Identity.valueOf("user:reader@company.com"),
                Identity.valueOf("user:admin@company.com"));
        policyBuilder.addIdentity(
                userRole,
                Identity.valueOf("user:writer@company.com"),
                Identity.valueOf("serviceAccount:app@project.iam.gserviceaccount.com"));
        Policy currentPolicy = policyBuilder.build();

        when(mockClient.getIamPolicy("users")).thenReturn(currentPolicy);
        when(mockClient.setIamPolicy(eq("users"), any(Policy.class))).thenReturn(null);

        // When: executing the action
        action.execute(mockClient);

        // Then: verify members were removed from both roles
        org.mockito.ArgumentCaptor<Policy> policyCaptor =
                org.mockito.ArgumentCaptor.forClass(Policy.class);
        verify(mockClient).setIamPolicy(eq("users"), policyCaptor.capture());

        Policy modifiedPolicy = policyCaptor.getValue();

        // Verify reader@company.com removed from reader role, but admin remains
        assertFalse(
                modifiedPolicy
                        .getBindings()
                        .get(readerRole)
                        .contains(Identity.valueOf("user:reader@company.com")),
                "Expected reader to be removed from reader role");
        assertTrue(
                modifiedPolicy
                        .getBindings()
                        .get(readerRole)
                        .contains(Identity.valueOf("user:admin@company.com")),
                "Expected admin to remain in reader role");

        // Verify writer@company.com removed from user role, but serviceAccount remains
        assertFalse(
                modifiedPolicy
                        .getBindings()
                        .get(userRole)
                        .contains(Identity.valueOf("user:writer@company.com")),
                "Expected writer to be removed from user role");
        assertTrue(
                modifiedPolicy
                        .getBindings()
                        .get(userRole)
                        .contains(
                                Identity.valueOf(
                                        "serviceAccount:app@project.iam.gserviceaccount.com")),
                "Expected serviceAccount to remain in user role");
    }

    @Test
    public void testRemoveTableIamPolicyExecuteWhenMemberNotInPolicy() {
        // Given: action that tries to remove a member that doesn't exist in the policy
        RemoveTableIamPolicy action = new RemoveTableIamPolicy();
        action.setTableId("users");

        IamBinding binding = new IamBinding();
        binding.setRole("roles/bigtable.reader");
        binding.setMembers(Collections.singletonList("user:nonexistent@company.com"));
        action.setBindings(Collections.singletonList(binding));

        BigtableTableAdminClient mockClient = mock(BigtableTableAdminClient.class);

        // Mock current policy without the member to be removed
        Policy.Builder policyBuilder = Policy.newBuilder();
        Role role = Role.of("roles/bigtable.reader");
        policyBuilder.addIdentity(role, Identity.valueOf("user:existing@company.com"));
        Policy currentPolicy = policyBuilder.build();

        when(mockClient.getIamPolicy("users")).thenReturn(currentPolicy);
        when(mockClient.setIamPolicy(eq("users"), any(Policy.class))).thenReturn(null);

        // When: executing the action
        action.execute(mockClient);

        // Then: verify policy is still updated (idempotent operation)
        verify(mockClient).getIamPolicy("users");
        verify(mockClient).setIamPolicy(eq("users"), any(Policy.class));

        // Capture and verify the existing member wasn't affected
        org.mockito.ArgumentCaptor<Policy> policyCaptor =
                org.mockito.ArgumentCaptor.forClass(Policy.class);
        verify(mockClient).setIamPolicy(eq("users"), policyCaptor.capture());

        Policy modifiedPolicy = policyCaptor.getValue();
        assertTrue(
                modifiedPolicy
                        .getBindings()
                        .get(role)
                        .contains(Identity.valueOf("user:existing@company.com")),
                "Expected existing member to remain in policy");
    }

    @Test
    public void testRemoveTableIamPolicyExecuteWhenRoleNotInPolicy() {
        // Given: action that tries to remove members from a role that doesn't exist in the policy
        RemoveTableIamPolicy action = new RemoveTableIamPolicy();
        action.setTableId("users");

        IamBinding binding = new IamBinding();
        binding.setRole("roles/bigtable.admin");
        binding.setMembers(Collections.singletonList("user:admin@company.com"));
        action.setBindings(Collections.singletonList(binding));

        BigtableTableAdminClient mockClient = mock(BigtableTableAdminClient.class);

        // Mock current policy with only reader role, not admin
        Policy.Builder policyBuilder = Policy.newBuilder();
        Role readerRole = Role.of("roles/bigtable.reader");
        policyBuilder.addIdentity(readerRole, Identity.valueOf("user:reader@company.com"));
        Policy currentPolicy = policyBuilder.build();

        when(mockClient.getIamPolicy("users")).thenReturn(currentPolicy);
        when(mockClient.setIamPolicy(eq("users"), any(Policy.class))).thenReturn(null);

        // When: executing the action (should handle gracefully)
        action.execute(mockClient);

        // Then: verify the policy was still fetched and set (idempotent)
        verify(mockClient).getIamPolicy("users");
        verify(mockClient).setIamPolicy(eq("users"), any(Policy.class));
    }

    @Test
    public void testRemoveTableIamPolicyExecuteRemovesAllMembersFromRole() {
        // Given: action that removes all members from a role
        RemoveTableIamPolicy action = new RemoveTableIamPolicy();
        action.setTableId("users");

        IamBinding binding = new IamBinding();
        binding.setRole("roles/bigtable.reader");
        binding.setMembers(Arrays.asList("user:user1@company.com", "user:user2@company.com"));
        action.setBindings(Collections.singletonList(binding));

        BigtableTableAdminClient mockClient = mock(BigtableTableAdminClient.class);

        // Mock current policy with only the members to be removed
        Policy.Builder policyBuilder = Policy.newBuilder();
        Role role = Role.of("roles/bigtable.reader");
        policyBuilder.addIdentity(
                role,
                Identity.valueOf("user:user1@company.com"),
                Identity.valueOf("user:user2@company.com"));
        Policy currentPolicy = policyBuilder.build();

        when(mockClient.getIamPolicy("users")).thenReturn(currentPolicy);
        when(mockClient.setIamPolicy(eq("users"), any(Policy.class))).thenReturn(null);

        // When: executing the action
        action.execute(mockClient);

        // Then: verify all members were removed and role has no bindings
        org.mockito.ArgumentCaptor<Policy> policyCaptor =
                org.mockito.ArgumentCaptor.forClass(Policy.class);
        verify(mockClient).setIamPolicy(eq("users"), policyCaptor.capture());

        Policy modifiedPolicy = policyCaptor.getValue();
        // The role should either not exist or have no members
        if (modifiedPolicy.getBindings().containsKey(role)) {
            assertTrue(
                    modifiedPolicy.getBindings().get(role).isEmpty(),
                    "Expected role to have no members after removing all");
        }
    }

    @Test
    public void testRemoveTableIamPolicyMultipleBindings() {
        // Given: configuration with multiple bindings
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> actionConfig = new HashMap<>();
        actionConfig.put("tableId", "users");

        List<Map<String, Object>> bindingsConfig = new ArrayList<>();

        Map<String, Object> binding1 = new HashMap<>();
        binding1.put("role", "roles/bigtable.reader");
        binding1.put(
                "members",
                Arrays.asList(
                        "serviceAccount:old@project.iam.gserviceaccount.com",
                        "user:contractor@company.com"));
        bindingsConfig.add(binding1);

        Map<String, Object> binding2 = new HashMap<>();
        binding2.put("role", "roles/bigtable.user");
        binding2.put(
                "members",
                Collections.singletonList(
                        "serviceAccount:decommissioned@project.iam.gserviceaccount.com"));
        bindingsConfig.add(binding2);

        actionConfig.put("bindings", bindingsConfig);
        config.put("RemoveTableIamPolicy", actionConfig);

        // When: creating an action
        Action<?> action = ActionFactory.getAction(config, mockContext);

        // Then: should have multiple bindings
        assertNotNull(action);
        assertTrue(action instanceof RemoveTableIamPolicy);
        RemoveTableIamPolicy removePolicy = (RemoveTableIamPolicy) action;
        assertEquals(removePolicy.getBindings().size(), 2);
    }

    @Test
    public void testRemoveTableIamPolicyToString() {
        // Given: an action
        RemoveTableIamPolicy action = new RemoveTableIamPolicy();
        action.setTableId("test-table");
        action.setBindings(createValidBindings());

        // When: calling toString
        String result = action.toString();

        // Then: it should contain relevant information
        assertTrue(result.contains("test-table"));
        assertTrue(result.contains("RemoveTableIamPolicy"));
    }

    @Test
    public void testRemoveTableIamPolicyGettersSetters() {
        // Given: an action
        RemoveTableIamPolicy action = new RemoveTableIamPolicy();

        // When: setting values
        action.setTableId("my-table");
        List<IamBinding> bindings = createValidBindings();
        action.setBindings(bindings);

        // Then: getters should return the values
        assertEquals(action.getTableId(), "my-table");
        assertEquals(action.getBindings(), bindings);
    }

    /** Helper method to create valid bindings for testing. */
    private List<IamBinding> createValidBindings() {
        IamBinding binding = new IamBinding();
        binding.setRole("roles/bigtable.reader");
        binding.setMembers(
                Collections.singletonList("serviceAccount:test@project.iam.gserviceaccount.com"));
        return Collections.singletonList(binding);
    }
}
