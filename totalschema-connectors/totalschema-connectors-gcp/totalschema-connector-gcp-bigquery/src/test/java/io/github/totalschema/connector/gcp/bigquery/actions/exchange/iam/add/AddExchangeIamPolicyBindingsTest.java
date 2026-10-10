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

package io.github.totalschema.connector.gcp.bigquery.actions.exchange.iam.add;

import static org.easymock.EasyMock.*;
import static org.testng.Assert.*;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutures;
import com.google.api.gax.rpc.ApiCallContext;
import com.google.api.gax.rpc.UnaryCallable;
import com.google.cloud.bigquery.dataexchange.v1beta1.AnalyticsHubServiceClient;
import com.google.cloud.bigquery.dataexchange.v1beta1.stub.AnalyticsHubServiceStub;
import com.google.iam.v1.Binding;
import com.google.iam.v1.Policy;
import com.google.iam.v1.SetIamPolicyRequest;
import io.github.totalschema.config.Configuration;
import io.github.totalschema.connector.gcp.bigquery.BigQueryConnectorException;
import io.github.totalschema.connector.gcp.bigquery.actions.Action;
import io.github.totalschema.connector.gcp.bigquery.actions.ActionFactory;
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
import java.util.Optional;
import org.easymock.Capture;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Tests for {@link AddExchangeIamPolicyBindings} and {@link
 * AddExchangeIamPolicyBindingsActionFactory}.
 */
public class AddExchangeIamPolicyBindingsTest {

    private Context mockContext;
    private ExpressionEvaluator mockEvaluator;
    private VariableService mockVariableService;
    private Configuration mockConfiguration;

    @BeforeMethod
    public void setUp() {
        mockContext = createMock(Context.class);
        mockEvaluator = createMock(ExpressionEvaluator.class);
        mockVariableService = createMock(VariableService.class);
        mockConfiguration = createMock(Configuration.class);

        // Setup mock configuration to return empty Optional for any getString() call
        expect(mockConfiguration.getString(anyString())).andReturn(Optional.empty()).anyTimes();

        // Setup mock to return the evaluator
        expect(mockContext.get(ExpressionEvaluator.class)).andReturn(mockEvaluator).anyTimes();

        // Setup mock to return the variable service
        expect(mockContext.get(VariableService.class)).andReturn(mockVariableService).anyTimes();

        // Mock variable service to return empty variables
        expect(mockVariableService.getEffectiveVariables(mockContext))
                .andReturn(Collections.emptyMap())
                .anyTimes();

        // Mock evaluator to perform identity substitution (no variable substitution)
        expect(mockEvaluator.evaluate(anyObject(String.class), anyObject(Map.class)))
                .andAnswer(() -> (String) getCurrentArguments()[0])
                .anyTimes();

        replay(mockContext, mockEvaluator, mockVariableService, mockConfiguration);
    }

    private static Map<String, Object> binding(String role, String... members) {
        Map<String, Object> binding = new HashMap<>();
        binding.put("role", role);
        binding.put("members", new ArrayList<>(Arrays.asList(members)));
        return binding;
    }

    // ---- Configuration parsing / getters-setters / factory tests ----

    @Test
    public void testAddExchangeIamPolicyBindingActionFromMap() {
        // Given: a YAML-like configuration map with all fields
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> actionConfig = new HashMap<>();
        actionConfig.put("project", "my-project");
        actionConfig.put("location", "US");
        actionConfig.put("dataExchangeId", "my_exchange");
        actionConfig.put(
                "bindings",
                Collections.singletonList(
                        binding("roles/analyticshub.subscriber", "user:analyst@company.com")));
        config.put("AddExchangeIamPolicyBindings", actionConfig);

        // When: creating an action from the config
        Action<?> action = ActionFactory.getAction(config, mockContext);

        // Then: it should be an AddExchangeIamPolicyBindings instance with correct values
        assertNotNull(action);
        assertTrue(action instanceof AddExchangeIamPolicyBindings);

        AddExchangeIamPolicyBindings addExchangeIamPolicyBindings =
                (AddExchangeIamPolicyBindings) action;
        assertEquals(addExchangeIamPolicyBindings.getProject(), "my-project");
        assertEquals(addExchangeIamPolicyBindings.getLocation(), "US");
        assertEquals(addExchangeIamPolicyBindings.getDataExchangeId(), "my_exchange");
        assertEquals(addExchangeIamPolicyBindings.getBindings().size(), 1);
        assertEquals(
                addExchangeIamPolicyBindings.getBindings().get(0).getRole(),
                "roles/analyticshub.subscriber");
        assertEquals(
                addExchangeIamPolicyBindings.getBindings().get(0).getMembers(),
                Collections.singletonList("user:analyst@company.com"));
    }

    @Test
    public void testAddExchangeIamPolicyBindingMultipleBindingsFromMap() {
        // Given: configuration with multiple bindings, each with multiple members
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> actionConfig = new HashMap<>();
        actionConfig.put("dataExchangeId", "my_exchange");
        actionConfig.put(
                "bindings",
                Arrays.asList(
                        binding(
                                "roles/analyticshub.subscriber",
                                "serviceAccount:analytics@project.iam.gserviceaccount.com",
                                "user:analyst@company.com"),
                        binding("roles/analyticshub.viewer", "group:viewers@company.com")));
        config.put("AddExchangeIamPolicyBindings", actionConfig);

        // When: creating an action
        Action<?> action = ActionFactory.getAction(config, mockContext);

        // Then: both bindings should be present with their members
        AddExchangeIamPolicyBindings addExchangeIamPolicyBindings =
                (AddExchangeIamPolicyBindings) action;
        assertEquals(addExchangeIamPolicyBindings.getBindings().size(), 2);
        assertEquals(addExchangeIamPolicyBindings.getBindings().get(0).getMembers().size(), 2);
        assertEquals(
                addExchangeIamPolicyBindings.getBindings().get(1).getRole(),
                "roles/analyticshub.viewer");
    }

    @Test
    public void testAddExchangeIamPolicyBindingGettersSetters() {
        // Given: an AddExchangeIamPolicyBindings action
        AddExchangeIamPolicyBindings addExchangeIamPolicyBindings =
                new AddExchangeIamPolicyBindings();

        IamBinding entry = new IamBinding();
        entry.setRole("roles/analyticshub.subscriber");
        entry.setMembers(Collections.singletonList("user:analyst@company.com"));

        // When: setting values
        addExchangeIamPolicyBindings.setProject("test-project");
        addExchangeIamPolicyBindings.setLocation("EU");
        addExchangeIamPolicyBindings.setDataExchangeId("exchange_id");
        addExchangeIamPolicyBindings.setBindings(Collections.singletonList(entry));

        // Then: getters should return the values
        assertEquals(addExchangeIamPolicyBindings.getProject(), "test-project");
        assertEquals(addExchangeIamPolicyBindings.getLocation(), "EU");
        assertEquals(addExchangeIamPolicyBindings.getDataExchangeId(), "exchange_id");
        assertEquals(addExchangeIamPolicyBindings.getBindings(), Collections.singletonList(entry));
    }

    @Test
    public void testAddExchangeIamPolicyBindingToString() {
        // Given: an AddExchangeIamPolicyBindings action
        AddExchangeIamPolicyBindings addExchangeIamPolicyBindings =
                new AddExchangeIamPolicyBindings();
        addExchangeIamPolicyBindings.setProject("test-project");
        addExchangeIamPolicyBindings.setLocation("US");
        addExchangeIamPolicyBindings.setDataExchangeId("exchange_id");

        IamBinding entry = new IamBinding();
        entry.setRole("roles/analyticshub.subscriber");
        entry.setMembers(Collections.singletonList("user:analyst@company.com"));
        addExchangeIamPolicyBindings.setBindings(Collections.singletonList(entry));

        // When: calling toString
        String result = addExchangeIamPolicyBindings.toString();

        // Then: it should contain relevant information
        assertTrue(result.contains("AddExchangeIamPolicyBindings"));
        assertTrue(result.contains("test-project"));
        assertTrue(result.contains("US"));
        assertTrue(result.contains("exchange_id"));
        assertTrue(result.contains("roles/analyticshub.subscriber"));
    }

    @Test
    public void testAddExchangeIamPolicyBindingActionFactoryRegistration() {
        // Given: a YAML-like configuration with AddExchangeIamPolicyBindings
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> actionConfig = new HashMap<>();
        actionConfig.put("dataExchangeId", "exchange");
        actionConfig.put(
                "bindings",
                Collections.singletonList(binding("roles/analyticshub.viewer", "user:a@b.com")));
        config.put("AddExchangeIamPolicyBindings", actionConfig);

        // When: getting the action via ActionFactory
        Action<?> action = ActionFactory.getAction(config, mockContext);

        // Then: should successfully create AddExchangeIamPolicyBindings instance
        assertNotNull(action);
        assertTrue(action instanceof AddExchangeIamPolicyBindings);
    }

    // ---- execute() tests ----
    //
    // AnalyticsHubServiceClient's getIamPolicy/setIamPolicy methods are `final`, so the client
    // itself cannot be mocked directly. Instead, a mock AnalyticsHubServiceStub is used to back
    // a real client instance created via AnalyticsHubServiceClient.create(stub).

    private static AnalyticsHubServiceClient clientBackedBy(AnalyticsHubServiceStub stub) {
        return AnalyticsHubServiceClient.create(stub);
    }

    private static <RequestT, ResponseT> UnaryCallable<RequestT, ResponseT> callableReturning(
            ResponseT response) {
        return new UnaryCallable<RequestT, ResponseT>() {
            @Override
            public ApiFuture<ResponseT> futureCall(RequestT request, ApiCallContext context) {
                return ApiFutures.immediateFuture(response);
            }
        };
    }

    private static <RequestT, ResponseT> UnaryCallable<RequestT, ResponseT> callableThrowing(
            RuntimeException exception) {
        return new UnaryCallable<RequestT, ResponseT>() {
            @Override
            public ApiFuture<ResponseT> futureCall(RequestT request, ApiCallContext context) {
                throw exception;
            }
        };
    }

    @Test
    public void testExecuteAddsNewBindingWhenRoleNotPresent() {
        // Given: an action with a single binding for a role not yet present on the policy
        AddExchangeIamPolicyBindings action = new AddExchangeIamPolicyBindings();
        action.setProject("test-project");
        action.setDataExchangeId("my_exchange");

        IamBinding entry = new IamBinding();
        entry.setRole("roles/analyticshub.subscriber");
        entry.setMembers(Collections.singletonList("user:analyst@company.com"));
        action.setBindings(Collections.singletonList(entry));

        Policy existingPolicy = Policy.newBuilder().build();

        AnalyticsHubServiceStub mockStub = createMock(AnalyticsHubServiceStub.class);
        Capture<SetIamPolicyRequest> setRequestCapture = Capture.newInstance();
        expect(mockStub.getIamPolicyCallable()).andReturn(callableReturning(existingPolicy)).once();
        expect(mockStub.setIamPolicyCallable())
                .andAnswer(() -> capturingSetPolicyCallable(setRequestCapture, existingPolicy))
                .once();

        replay(mockStub);

        AnalyticsHubServiceClient client = clientBackedBy(mockStub);

        // When: executing the action
        action.execute(client, mockConfiguration);

        // Then: verify the resulting policy has the new binding
        verify(mockStub);
        Policy submittedPolicy = setRequestCapture.getValue().getPolicy();
        assertEquals(submittedPolicy.getBindingsCount(), 1);
        Binding newBinding = submittedPolicy.getBindings(0);
        assertEquals(newBinding.getRole(), "roles/analyticshub.subscriber");
        assertEquals(
                newBinding.getMembersList(), Collections.singletonList("user:analyst@company.com"));
    }

    @Test
    public void testExecuteMergesMembersIntoExistingBindingForSameRole() {
        // Given: a policy that already has a binding for the target role
        AddExchangeIamPolicyBindings action = new AddExchangeIamPolicyBindings();
        action.setProject("test-project");
        action.setDataExchangeId("my_exchange");

        IamBinding entry = new IamBinding();
        entry.setRole("roles/analyticshub.subscriber");
        entry.setMembers(Collections.singletonList("user:new@company.com"));
        action.setBindings(Collections.singletonList(entry));

        Policy existingPolicy =
                Policy.newBuilder()
                        .addBindings(
                                Binding.newBuilder()
                                        .setRole("roles/analyticshub.subscriber")
                                        .addMembers("user:existing@company.com")
                                        .build())
                        .build();

        AnalyticsHubServiceStub mockStub = createMock(AnalyticsHubServiceStub.class);
        Capture<SetIamPolicyRequest> setRequestCapture = Capture.newInstance();
        expect(mockStub.getIamPolicyCallable()).andReturn(callableReturning(existingPolicy)).once();
        expect(mockStub.setIamPolicyCallable())
                .andAnswer(() -> capturingSetPolicyCallable(setRequestCapture, existingPolicy))
                .once();

        replay(mockStub);

        AnalyticsHubServiceClient client = clientBackedBy(mockStub);

        // When: executing the action
        action.execute(client, mockConfiguration);

        // Then: only one binding should exist for the role (merged, not duplicated)
        verify(mockStub);
        Policy submittedPolicy = setRequestCapture.getValue().getPolicy();
        assertEquals(submittedPolicy.getBindingsCount(), 1);
        Binding mergedBinding = submittedPolicy.getBindings(0);
        assertEquals(mergedBinding.getRole(), "roles/analyticshub.subscriber");
        assertEquals(
                mergedBinding.getMembersList(),
                Arrays.asList("user:existing@company.com", "user:new@company.com"));
    }

    @Test
    public void testExecuteDoesNotDuplicateExistingMember() {
        // Given: a policy that already has a binding containing the exact same member
        AddExchangeIamPolicyBindings action = new AddExchangeIamPolicyBindings();
        action.setProject("test-project");
        action.setDataExchangeId("my_exchange");

        IamBinding entry = new IamBinding();
        entry.setRole("roles/analyticshub.subscriber");
        entry.setMembers(Collections.singletonList("user:existing@company.com"));
        action.setBindings(Collections.singletonList(entry));

        Policy existingPolicy =
                Policy.newBuilder()
                        .addBindings(
                                Binding.newBuilder()
                                        .setRole("roles/analyticshub.subscriber")
                                        .addMembers("user:existing@company.com")
                                        .build())
                        .build();

        AnalyticsHubServiceStub mockStub = createMock(AnalyticsHubServiceStub.class);
        Capture<SetIamPolicyRequest> setRequestCapture = Capture.newInstance();
        expect(mockStub.getIamPolicyCallable()).andReturn(callableReturning(existingPolicy)).once();
        expect(mockStub.setIamPolicyCallable())
                .andAnswer(() -> capturingSetPolicyCallable(setRequestCapture, existingPolicy))
                .once();

        replay(mockStub);

        AnalyticsHubServiceClient client = clientBackedBy(mockStub);

        // When: executing the action
        action.execute(client, mockConfiguration);

        // Then: the member should not be duplicated
        verify(mockStub);
        Policy submittedPolicy = setRequestCapture.getValue().getPolicy();
        assertEquals(submittedPolicy.getBindingsCount(), 1);
        assertEquals(submittedPolicy.getBindings(0).getMembersCount(), 1);
    }

    @Test
    public void testExecutePreservesUnrelatedExistingBindings() {
        // Given: a policy with a binding for a different role that should be left untouched
        AddExchangeIamPolicyBindings action = new AddExchangeIamPolicyBindings();
        action.setProject("test-project");
        action.setDataExchangeId("my_exchange");

        IamBinding entry = new IamBinding();
        entry.setRole("roles/analyticshub.subscriber");
        entry.setMembers(Collections.singletonList("user:analyst@company.com"));
        action.setBindings(Collections.singletonList(entry));

        Policy existingPolicy =
                Policy.newBuilder()
                        .addBindings(
                                Binding.newBuilder()
                                        .setRole("roles/analyticshub.admin")
                                        .addMembers("user:admin@company.com")
                                        .build())
                        .build();

        AnalyticsHubServiceStub mockStub = createMock(AnalyticsHubServiceStub.class);
        Capture<SetIamPolicyRequest> setRequestCapture = Capture.newInstance();
        expect(mockStub.getIamPolicyCallable()).andReturn(callableReturning(existingPolicy)).once();
        expect(mockStub.setIamPolicyCallable())
                .andAnswer(() -> capturingSetPolicyCallable(setRequestCapture, existingPolicy))
                .once();

        replay(mockStub);

        AnalyticsHubServiceClient client = clientBackedBy(mockStub);

        // When: executing the action
        action.execute(client, mockConfiguration);

        // Then: the pre-existing admin binding plus the newly added subscriber binding
        verify(mockStub);
        Policy submittedPolicy = setRequestCapture.getValue().getPolicy();
        assertEquals(submittedPolicy.getBindingsCount(), 2);
        List<String> roles = new ArrayList<>();
        for (Binding b : submittedPolicy.getBindingsList()) {
            roles.add(b.getRole());
        }
        assertTrue(roles.contains("roles/analyticshub.admin"));
        assertTrue(roles.contains("roles/analyticshub.subscriber"));
    }

    @Test
    public void testExecuteThrowsWhenBindingsMissing() {
        // Given: an action without bindings configured
        AddExchangeIamPolicyBindings action = new AddExchangeIamPolicyBindings();
        action.setProject("test-project");
        action.setDataExchangeId("my_exchange");

        AnalyticsHubServiceStub mockStub = createMock(AnalyticsHubServiceStub.class);
        replay(mockStub);
        AnalyticsHubServiceClient client = clientBackedBy(mockStub);

        // When/Then: executing should throw IllegalArgumentException without contacting the stub
        assertThrows(
                IllegalArgumentException.class, () -> action.execute(client, mockConfiguration));

        verify(mockStub);
    }

    @Test
    public void testExecuteThrowsWhenBindingHasInvalidMember() {
        // Given: an action with a binding containing an invalid member (empty string)
        AddExchangeIamPolicyBindings action = new AddExchangeIamPolicyBindings();
        action.setProject("test-project");
        action.setDataExchangeId("my_exchange");

        IamBinding entry = new IamBinding();
        entry.setRole("roles/analyticshub.subscriber");
        entry.setMembers(Collections.singletonList(""));
        action.setBindings(Collections.singletonList(entry));

        AnalyticsHubServiceStub mockStub = createMock(AnalyticsHubServiceStub.class);
        replay(mockStub);
        AnalyticsHubServiceClient client = clientBackedBy(mockStub);

        // When/Then: executing should throw IllegalArgumentException without contacting the stub
        assertThrows(
                IllegalArgumentException.class, () -> action.execute(client, mockConfiguration));

        verify(mockStub);
    }

    @Test
    public void testExecuteWrapsClientFailureInBigQueryConnectorException() {
        // Given: a stub whose getIamPolicy call fails
        AddExchangeIamPolicyBindings action = new AddExchangeIamPolicyBindings();
        action.setProject("test-project");
        action.setDataExchangeId("my_exchange");

        IamBinding entry = new IamBinding();
        entry.setRole("roles/analyticshub.subscriber");
        entry.setMembers(Collections.singletonList("user:analyst@company.com"));
        action.setBindings(Collections.singletonList(entry));

        AnalyticsHubServiceStub mockStub = createMock(AnalyticsHubServiceStub.class);
        expect(mockStub.getIamPolicyCallable())
                .andReturn(callableThrowing(new RuntimeException("boom")))
                .once();

        replay(mockStub);
        AnalyticsHubServiceClient client = clientBackedBy(mockStub);

        // When/Then: the failure should be wrapped in a BigQueryConnectorException
        BigQueryConnectorException exception =
                expectThrows(
                        BigQueryConnectorException.class,
                        () -> action.execute(client, mockConfiguration));

        assertTrue(exception.getMessage().contains("my_exchange"));

        verify(mockStub);
    }

    private static UnaryCallable<SetIamPolicyRequest, Policy> capturingSetPolicyCallable(
            Capture<SetIamPolicyRequest> capture, Policy response) {
        return new UnaryCallable<SetIamPolicyRequest, Policy>() {
            @Override
            public ApiFuture<Policy> futureCall(
                    SetIamPolicyRequest request, ApiCallContext context) {
                capture.setValue(request);
                return ApiFutures.immediateFuture(response);
            }
        };
    }
}
