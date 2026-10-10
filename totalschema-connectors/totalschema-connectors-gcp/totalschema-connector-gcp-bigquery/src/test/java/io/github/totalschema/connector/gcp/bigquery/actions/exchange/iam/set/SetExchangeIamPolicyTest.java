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

package io.github.totalschema.connector.gcp.bigquery.actions.exchange.iam.set;

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
import java.util.Map;
import java.util.Optional;
import org.easymock.Capture;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Tests for {@link SetExchangeIamPolicy} and {@link SetExchangeIamPolicyActionFactory}. */
public class SetExchangeIamPolicyTest {

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
    public void testSetExchangeIamPolicyActionFromMap() {
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
        config.put("SetExchangeIamPolicy", actionConfig);

        // When: creating an action from the config
        Action<?> action = ActionFactory.getAction(config, mockContext);

        // Then: it should be a SetExchangeIamPolicy instance with correct values
        assertNotNull(action);
        assertTrue(action instanceof SetExchangeIamPolicy);

        SetExchangeIamPolicy setExchangeIamPolicy = (SetExchangeIamPolicy) action;
        assertEquals(setExchangeIamPolicy.getProject(), "my-project");
        assertEquals(setExchangeIamPolicy.getLocation(), "US");
        assertEquals(setExchangeIamPolicy.getDataExchangeId(), "my_exchange");
        assertEquals(setExchangeIamPolicy.getBindings().size(), 1);
        assertEquals(
                setExchangeIamPolicy.getBindings().get(0).getRole(),
                "roles/analyticshub.subscriber");
        assertEquals(
                setExchangeIamPolicy.getBindings().get(0).getMembers(),
                Collections.singletonList("user:analyst@company.com"));
    }

    @Test
    public void testSetExchangeIamPolicyMultipleBindingsFromMap() {
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
        config.put("SetExchangeIamPolicy", actionConfig);

        // When: creating an action
        Action<?> action = ActionFactory.getAction(config, mockContext);

        // Then: both bindings should be present with their members
        SetExchangeIamPolicy setExchangeIamPolicy = (SetExchangeIamPolicy) action;
        assertEquals(setExchangeIamPolicy.getBindings().size(), 2);
        assertEquals(setExchangeIamPolicy.getBindings().get(0).getMembers().size(), 2);
        assertEquals(
                setExchangeIamPolicy.getBindings().get(1).getRole(), "roles/analyticshub.viewer");
    }

    @Test
    public void testSetExchangeIamPolicyGettersSetters() {
        // Given: a SetExchangeIamPolicy action
        SetExchangeIamPolicy setExchangeIamPolicy = new SetExchangeIamPolicy();

        IamBinding entry = new IamBinding();
        entry.setRole("roles/analyticshub.subscriber");
        entry.setMembers(Collections.singletonList("user:analyst@company.com"));

        // When: setting values
        setExchangeIamPolicy.setProject("test-project");
        setExchangeIamPolicy.setLocation("EU");
        setExchangeIamPolicy.setDataExchangeId("exchange_id");
        setExchangeIamPolicy.setBindings(Collections.singletonList(entry));

        // Then: getters should return the values
        assertEquals(setExchangeIamPolicy.getProject(), "test-project");
        assertEquals(setExchangeIamPolicy.getLocation(), "EU");
        assertEquals(setExchangeIamPolicy.getDataExchangeId(), "exchange_id");
        assertEquals(setExchangeIamPolicy.getBindings(), Collections.singletonList(entry));
    }

    @Test
    public void testSetExchangeIamPolicyToString() {
        // Given: a SetExchangeIamPolicy action
        SetExchangeIamPolicy setExchangeIamPolicy = new SetExchangeIamPolicy();
        setExchangeIamPolicy.setProject("test-project");
        setExchangeIamPolicy.setLocation("US");
        setExchangeIamPolicy.setDataExchangeId("exchange_id");

        IamBinding entry = new IamBinding();
        entry.setRole("roles/analyticshub.subscriber");
        entry.setMembers(Collections.singletonList("user:analyst@company.com"));
        setExchangeIamPolicy.setBindings(Collections.singletonList(entry));

        // When: calling toString
        String result = setExchangeIamPolicy.toString();

        // Then: it should contain relevant information
        assertTrue(result.contains("SetExchangeIamPolicy"));
        assertTrue(result.contains("test-project"));
        assertTrue(result.contains("US"));
        assertTrue(result.contains("exchange_id"));
        assertTrue(result.contains("roles/analyticshub.subscriber"));
    }

    @Test
    public void testSetExchangeIamPolicyActionFactoryRegistration() {
        // Given: a YAML-like configuration with SetExchangeIamPolicy
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> actionConfig = new HashMap<>();
        actionConfig.put("dataExchangeId", "exchange");
        actionConfig.put(
                "bindings",
                Collections.singletonList(binding("roles/analyticshub.viewer", "user:a@b.com")));
        config.put("SetExchangeIamPolicy", actionConfig);

        // When: getting the action via ActionFactory
        Action<?> action = ActionFactory.getAction(config, mockContext);

        // Then: should successfully create SetExchangeIamPolicy instance
        assertNotNull(action);
        assertTrue(action instanceof SetExchangeIamPolicy);
    }

    // ---- execute() tests ----
    //
    // AnalyticsHubServiceClient's getIamPolicy/setIamPolicy methods are `final`, so the client
    // itself cannot be mocked directly. Instead, a mock AnalyticsHubServiceStub is used to back
    // a real client instance created via AnalyticsHubServiceClient.create(stub).
    //
    // Note: unlike Add/RemoveExchangeIamPolicyBindings, SetExchangeIamPolicy never calls
    // getIamPolicy - the mock stub is never given a getIamPolicyCallable() expectation, so any
    // accidental call to it will fail these tests with an "unexpected method call" error.

    private static AnalyticsHubServiceClient clientBackedBy(AnalyticsHubServiceStub stub) {
        return AnalyticsHubServiceClient.create(stub);
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

    @Test
    public void testExecuteBuildsPolicyFromConfiguredBindingsOnly() {
        // Given: an action with two configured bindings
        SetExchangeIamPolicy action = new SetExchangeIamPolicy();
        action.setProject("test-project");
        action.setDataExchangeId("my_exchange");

        IamBinding subscriberEntry = new IamBinding();
        subscriberEntry.setRole("roles/analyticshub.subscriber");
        subscriberEntry.setMembers(Collections.singletonList("user:analyst@company.com"));

        IamBinding viewerEntry = new IamBinding();
        viewerEntry.setRole("roles/analyticshub.viewer");
        viewerEntry.setMembers(Collections.singletonList("group:viewers@company.com"));

        action.setBindings(Arrays.asList(subscriberEntry, viewerEntry));

        AnalyticsHubServiceStub mockStub = createMock(AnalyticsHubServiceStub.class);
        Capture<SetIamPolicyRequest> setRequestCapture = Capture.newInstance();
        // Deliberately NOT recording getIamPolicyCallable(): it must never be called.
        expect(mockStub.setIamPolicyCallable())
                .andAnswer(
                        () ->
                                capturingSetPolicyCallable(
                                        setRequestCapture, Policy.getDefaultInstance()))
                .once();

        replay(mockStub);

        AnalyticsHubServiceClient client = clientBackedBy(mockStub);

        // When: executing the action
        action.execute(client, mockConfiguration);

        // Then: the submitted policy contains exactly the configured bindings
        verify(mockStub);
        Policy submittedPolicy = setRequestCapture.getValue().getPolicy();
        assertEquals(submittedPolicy.getBindingsCount(), 2);

        Binding subscriberBinding = submittedPolicy.getBindings(0);
        assertEquals(subscriberBinding.getRole(), "roles/analyticshub.subscriber");
        assertEquals(
                subscriberBinding.getMembersList(),
                Collections.singletonList("user:analyst@company.com"));

        Binding viewerBinding = submittedPolicy.getBindings(1);
        assertEquals(viewerBinding.getRole(), "roles/analyticshub.viewer");
        assertEquals(
                viewerBinding.getMembersList(),
                Collections.singletonList("group:viewers@company.com"));
    }

    @Test
    public void testExecuteMergesDuplicateRoleEntriesInConfiguration() {
        // Given: the same role configured twice (e.g. via separate YAML entries)
        SetExchangeIamPolicy action = new SetExchangeIamPolicy();
        action.setProject("test-project");
        action.setDataExchangeId("my_exchange");

        IamBinding firstEntry = new IamBinding();
        firstEntry.setRole("roles/analyticshub.subscriber");
        firstEntry.setMembers(Collections.singletonList("user:first@company.com"));

        IamBinding secondEntry = new IamBinding();
        secondEntry.setRole("roles/analyticshub.subscriber");
        secondEntry.setMembers(Collections.singletonList("user:second@company.com"));

        action.setBindings(Arrays.asList(firstEntry, secondEntry));

        AnalyticsHubServiceStub mockStub = createMock(AnalyticsHubServiceStub.class);
        Capture<SetIamPolicyRequest> setRequestCapture = Capture.newInstance();
        expect(mockStub.setIamPolicyCallable())
                .andAnswer(
                        () ->
                                capturingSetPolicyCallable(
                                        setRequestCapture, Policy.getDefaultInstance()))
                .once();

        replay(mockStub);

        AnalyticsHubServiceClient client = clientBackedBy(mockStub);

        // When: executing the action
        action.execute(client, mockConfiguration);

        // Then: a single binding for the role is produced, with both members merged
        verify(mockStub);
        Policy submittedPolicy = setRequestCapture.getValue().getPolicy();
        assertEquals(submittedPolicy.getBindingsCount(), 1);
        assertEquals(
                submittedPolicy.getBindings(0).getMembersList(),
                Arrays.asList("user:first@company.com", "user:second@company.com"));
    }

    @Test
    public void testExecuteNeverReadsExistingPolicy() {
        // Given: a mock stub with no getIamPolicyCallable() expectation recorded at all
        SetExchangeIamPolicy action = new SetExchangeIamPolicy();
        action.setProject("test-project");
        action.setDataExchangeId("my_exchange");

        IamBinding entry = new IamBinding();
        entry.setRole("roles/analyticshub.subscriber");
        entry.setMembers(Collections.singletonList("user:analyst@company.com"));
        action.setBindings(Collections.singletonList(entry));

        AnalyticsHubServiceStub mockStub = createMock(AnalyticsHubServiceStub.class);
        expect(mockStub.setIamPolicyCallable())
                .andReturn(
                        capturingSetPolicyCallable(
                                Capture.newInstance(), Policy.getDefaultInstance()))
                .once();

        replay(mockStub);

        AnalyticsHubServiceClient client = clientBackedBy(mockStub);

        // When/Then: executing must succeed without ever invoking getIamPolicyCallable(); if it
        // did, EasyMock would throw an "unexpected method call" AssertionError here.
        action.execute(client, mockConfiguration);

        verify(mockStub);
    }

    @Test
    public void testExecuteThrowsWhenBindingsMissing() {
        // Given: an action without bindings configured
        SetExchangeIamPolicy action = new SetExchangeIamPolicy();
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
        SetExchangeIamPolicy action = new SetExchangeIamPolicy();
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
        // Given: a stub whose setIamPolicy call fails
        SetExchangeIamPolicy action = new SetExchangeIamPolicy();
        action.setProject("test-project");
        action.setDataExchangeId("my_exchange");

        IamBinding entry = new IamBinding();
        entry.setRole("roles/analyticshub.subscriber");
        entry.setMembers(Collections.singletonList("user:analyst@company.com"));
        action.setBindings(Collections.singletonList(entry));

        AnalyticsHubServiceStub mockStub = createMock(AnalyticsHubServiceStub.class);
        expect(mockStub.setIamPolicyCallable())
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
}
