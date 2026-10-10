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

/** Tests for {@link SetTableIamPolicy} and {@link SetTableIamPolicyActionFactory}. */
public class SetTableIamPolicyTest {

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
    public void testSetTableIamPolicyFromMap() {
        // Given: a YAML-like configuration map
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> actionConfig = new HashMap<>();
        actionConfig.put("tableId", "users");

        List<Map<String, Object>> bindingsConfig = new ArrayList<>();
        Map<String, Object> binding1 = new HashMap<>();
        binding1.put("role", "roles/bigtable.reader");
        binding1.put(
                "members", Arrays.asList("serviceAccount:test@project.iam.gserviceaccount.com"));
        bindingsConfig.add(binding1);

        actionConfig.put("bindings", bindingsConfig);
        config.put("SetTableIamPolicy", actionConfig);

        // When: creating an action from the config
        Action<?> action = ActionFactory.getAction(config, mockContext);

        // Then: it should be a SetTableIamPolicy instance with correct values
        assertNotNull(action);
        assertTrue(action instanceof SetTableIamPolicy);

        SetTableIamPolicy setPolicy = (SetTableIamPolicy) action;
        assertEquals(setPolicy.getTableId(), "users");
        assertNotNull(setPolicy.getBindings());
        assertEquals(setPolicy.getBindings().size(), 1);
        assertEquals(setPolicy.getBindings().get(0).getRole(), "roles/bigtable.reader");
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*tableId is required.*")
    public void testSetTableIamPolicyRequiresTableId() {
        // Given: an action without tableId
        SetTableIamPolicy action = new SetTableIamPolicy();
        action.setTableId(null);
        action.setBindings(createValidBindings());

        // When/Then: execute should throw exception
        action.execute(null);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*bindings is required.*")
    public void testSetTableIamPolicyRequiresBindings() {
        // Given: an action without bindings
        SetTableIamPolicy action = new SetTableIamPolicy();
        action.setTableId("users");
        action.setBindings(null);

        // When/Then: execute should throw exception
        action.execute(null);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*bindings is required.*")
    public void testSetTableIamPolicyRequiresNonEmptyBindings() {
        // Given: an action with empty bindings
        SetTableIamPolicy action = new SetTableIamPolicy();
        action.setTableId("users");
        action.setBindings(Collections.emptyList());

        // When/Then: execute should throw exception
        action.execute(null);
    }

    @Test
    public void testSetTableIamPolicyExecute() {
        // Given: a valid action
        SetTableIamPolicy action = new SetTableIamPolicy();
        action.setTableId("users");
        action.setBindings(createValidBindings());

        BigtableTableAdminClient mockClient = mock(BigtableTableAdminClient.class);

        // Mock the current policy with at least one existing binding
        Policy.Builder policyBuilder = Policy.newBuilder();
        Role existingRole = Role.of("roles/bigtable.user");
        policyBuilder.addIdentity(existingRole, Identity.valueOf("user:existing@company.com"));
        Policy currentPolicy = policyBuilder.build();
        when(mockClient.getIamPolicy("users")).thenReturn(currentPolicy);

        // Expect setIamPolicy to be called
        when(mockClient.setIamPolicy(eq("users"), any(Policy.class))).thenReturn(null);

        // When: executing the action
        action.execute(mockClient);

        // Then: verify the client was called
        verify(mockClient).getIamPolicy("users");
        verify(mockClient).setIamPolicy(eq("users"), any(Policy.class));
    }

    @Test
    public void testSetTableIamPolicyMultipleBindings() {
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
                        "serviceAccount:analytics@project.iam.gserviceaccount.com",
                        "user:analyst@company.com"));
        bindingsConfig.add(binding1);

        Map<String, Object> binding2 = new HashMap<>();
        binding2.put("role", "roles/bigtable.user");
        binding2.put(
                "members", Arrays.asList("serviceAccount:app@project.iam.gserviceaccount.com"));
        bindingsConfig.add(binding2);

        actionConfig.put("bindings", bindingsConfig);
        config.put("SetTableIamPolicy", actionConfig);

        // When: creating an action
        Action<?> action = ActionFactory.getAction(config, mockContext);

        // Then: should have multiple bindings
        assertNotNull(action);
        assertTrue(action instanceof SetTableIamPolicy);
        SetTableIamPolicy setPolicy = (SetTableIamPolicy) action;
        assertEquals(setPolicy.getBindings().size(), 2);
    }

    @Test
    public void testSetTableIamPolicyToString() {
        // Given: an action
        SetTableIamPolicy action = new SetTableIamPolicy();
        action.setTableId("test-table");
        action.setBindings(createValidBindings());

        // When: calling toString
        String result = action.toString();

        // Then: it should contain relevant information
        assertTrue(result.contains("test-table"));
        assertTrue(result.contains("SetTableIamPolicy"));
    }

    @Test
    public void testSetTableIamPolicyGettersSetters() {
        // Given: an action
        SetTableIamPolicy action = new SetTableIamPolicy();

        // When: setting values
        action.setTableId("my-table");
        List<IamBinding> bindings = createValidBindings();
        action.setBindings(bindings);

        // Then: getters should return the values
        assertEquals(action.getTableId(), "my-table");
        assertEquals(action.getBindings(), bindings);
    }

    @Test
    public void testIamBindingValidation() {
        // Given: a binding
        IamBinding binding = new IamBinding();
        binding.setRole("roles/bigtable.reader");
        binding.setMembers(Arrays.asList("serviceAccount:test@project.iam.gserviceaccount.com"));

        // When: validating
        binding.validate();

        // Then: should not throw exception
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*role is required.*")
    public void testIamBindingRequiresRole() {
        // Given: a binding without role
        IamBinding binding = new IamBinding();
        binding.setMembers(Arrays.asList("serviceAccount:test@project.iam.gserviceaccount.com"));

        // When/Then: validation should throw exception
        binding.validate();
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*members is required.*")
    public void testIamBindingRequiresMembers() {
        // Given: a binding without members
        IamBinding binding = new IamBinding();
        binding.setRole("roles/bigtable.reader");
        binding.setMembers(null);

        // When/Then: validation should throw exception
        binding.validate();
    }

    @Test
    public void testIamBindingValidMemberFormats() {
        // Given: bindings with various valid member formats
        List<String> validMembers =
                Arrays.asList(
                        "user:test@example.com",
                        "serviceAccount:test@project.iam.gserviceaccount.com",
                        "group:team@example.com",
                        "domain:example.com",
                        "allUsers",
                        "allAuthenticatedUsers");

        for (String member : validMembers) {
            IamBinding binding = new IamBinding();
            binding.setRole("roles/bigtable.reader");
            binding.setMembers(Arrays.asList(member));

            // When: validating
            binding.validate();

            // Then: should not throw exception
        }
    }

    @Test
    public void testSetTableIamPolicyPreservesExistingBindings() {
        // Given: an action
        SetTableIamPolicy action = new SetTableIamPolicy();
        action.setTableId("users");
        action.setBindings(createValidBindings());

        BigtableTableAdminClient mockClient = mock(BigtableTableAdminClient.class);

        // Mock a current policy with existing bindings
        Policy.Builder existingPolicyBuilder = Policy.newBuilder();
        Role existingRole = Role.of("roles/bigtable.admin");
        existingPolicyBuilder.addIdentity(existingRole, Identity.valueOf("user:admin@company.com"));
        Policy currentPolicy = existingPolicyBuilder.build();

        when(mockClient.getIamPolicy("users")).thenReturn(currentPolicy);
        when(mockClient.setIamPolicy(eq("users"), any(Policy.class))).thenReturn(null);

        // When: executing the action
        action.execute(mockClient);

        // Then: verify the client was called
        verify(mockClient).getIamPolicy("users");
        verify(mockClient).setIamPolicy(eq("users"), any(Policy.class));
    }

    /** Helper method to create valid bindings for testing. */
    private List<IamBinding> createValidBindings() {
        IamBinding binding = new IamBinding();
        binding.setRole("roles/bigtable.reader");
        binding.setMembers(Arrays.asList("serviceAccount:test@project.iam.gserviceaccount.com"));
        return Arrays.asList(binding);
    }
}
