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

import static org.easymock.EasyMock.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.*;

import com.google.bigtable.admin.v2.Instance;
import com.google.bigtable.admin.v2.InstanceName;
import com.google.cloud.bigtable.admin.v2.BaseBigtableInstanceAdminClient;
import com.google.cloud.bigtable.admin.v2.BigtableInstanceAdminClient;
import com.google.cloud.bigtable.admin.v2.models.AppProfile;
import com.google.cloud.bigtable.admin.v2.models.Cluster;
import com.google.cloud.bigtable.admin.v2.models.CreateAppProfileRequest;
import io.github.totalschema.connector.gcp.bigtable.actions.Action;
import io.github.totalschema.connector.gcp.bigtable.actions.ActionFactory;
import io.github.totalschema.engine.api.Context;
import io.github.totalschema.spi.expression.evaluator.ExpressionEvaluator;
import io.github.totalschema.spi.variables.VariableService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Tests for {@link EnableTableReplication} and {@link EnableTableReplicationActionFactory}. */
public class EnableTableReplicationTest {

    private Context mockContext;
    private ExpressionEvaluator mockEvaluator;
    private VariableService mockVariableService;

    @BeforeMethod
    public void setUp() {
        mockContext = createMock(Context.class);
        mockEvaluator = createMock(ExpressionEvaluator.class);
        mockVariableService = createMock(VariableService.class);

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

        replay(mockContext, mockEvaluator, mockVariableService);
    }

    /**
     * Helper method to mock the low-level protobuf API for instance validation.
     *
     * @param mockClient the mock BigtableInstanceAdminClient
     * @param instanceId the instance ID to mock
     */
    private void mockInstanceValidation(BigtableInstanceAdminClient mockClient, String instanceId) {
        when(mockClient.getProjectId()).thenReturn("test-project");
        BaseBigtableInstanceAdminClient mockBaseClient =
                mock(BaseBigtableInstanceAdminClient.class);
        when(mockClient.getBaseClient()).thenReturn(mockBaseClient);

        Instance mockInstance = mock(Instance.class);
        when(mockBaseClient.getInstance(ArgumentMatchers.any(InstanceName.class)))
                .thenReturn(mockInstance);
    }

    @Test
    public void testEnableTableReplicationActionFromMap() {
        // Given: a YAML-like configuration map
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> actionConfig = new HashMap<>();
        actionConfig.put("instanceId", "my-instance");
        actionConfig.put("tableId", "users");
        actionConfig.put("clusters", List.of("us-east1-b", "europe-west1-c"));
        config.put("EnableTableReplication", actionConfig);

        // When: creating an action from the config
        Action<?> action = ActionFactory.getAction(config, mockContext);

        // Then: it should be an EnableTableReplication instance with correct values
        assertNotNull(action);
        assertTrue(action instanceof EnableTableReplication);

        EnableTableReplication enableReplication = (EnableTableReplication) action;
        assertEquals(enableReplication.getInstanceId(), "my-instance");
        assertEquals(enableReplication.getTableId(), "users");
        assertEquals(enableReplication.getClusters().size(), 2);
        assertTrue(enableReplication.getClusters().contains("us-east1-b"));
        assertTrue(enableReplication.getClusters().contains("europe-west1-c"));
    }

    @Test
    public void testEnableTableReplicationWithAllOptions() {
        // Given: configuration with all optional fields
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> actionConfig = new HashMap<>();
        actionConfig.put("instanceId", "my-instance");
        actionConfig.put("tableId", "users");
        actionConfig.put("clusters", List.of("us-east1-b", "europe-west1-c"));
        actionConfig.put("consistency", "STRONG");
        actionConfig.put("appProfileId", "custom-profile");
        actionConfig.put("description", "Custom multi-cluster profile");
        config.put("EnableTableReplication", actionConfig);

        // When: creating an action
        Action<?> action = ActionFactory.getAction(config, mockContext);

        // Then: should have all options set
        assertNotNull(action);
        assertTrue(action instanceof EnableTableReplication);

        EnableTableReplication enableReplication = (EnableTableReplication) action;
        assertEquals(enableReplication.getInstanceId(), "my-instance");
        assertEquals(enableReplication.getTableId(), "users");
        assertEquals(enableReplication.getClusters().size(), 2);
        assertEquals(enableReplication.getConsistency(), "STRONG");
        assertEquals(enableReplication.getAppProfileId(), "custom-profile");
        assertEquals(enableReplication.getDescription(), "Custom multi-cluster profile");
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*instanceId is required.*")
    public void testRequiresInstanceId() {
        // Given: an action without instanceId
        EnableTableReplication action = new EnableTableReplication();
        action.setInstanceId(null);
        action.setTableId("users");
        action.setClusters(List.of("cluster1", "cluster2"));

        // When/Then: execute should throw exception
        action.execute(null);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*instanceId is required.*")
    public void testRequiresNonEmptyInstanceId() {
        // Given: an action with empty instanceId
        EnableTableReplication action = new EnableTableReplication();
        action.setInstanceId("");
        action.setTableId("users");
        action.setClusters(List.of("cluster1", "cluster2"));

        // When/Then: execute should throw exception
        action.execute(null);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*tableId is required.*")
    public void testRequiresTableId() {
        // Given: an action without tableId
        EnableTableReplication action = new EnableTableReplication();
        action.setInstanceId("my-instance");
        action.setTableId(null);
        action.setClusters(List.of("cluster1", "cluster2"));

        // When/Then: execute should throw exception
        action.execute(null);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*tableId is required.*")
    public void testRequiresNonEmptyTableId() {
        // Given: an action with empty tableId
        EnableTableReplication action = new EnableTableReplication();
        action.setInstanceId("my-instance");
        action.setTableId("");
        action.setClusters(List.of("cluster1", "cluster2"));

        // When/Then: execute should throw exception
        action.execute(null);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*At least one cluster is required.*")
    public void testRequiresClusters() {
        // Given: an action without clusters
        EnableTableReplication action = new EnableTableReplication();
        action.setInstanceId("my-instance");
        action.setTableId("users");
        action.setClusters(null);

        // When/Then: execute should throw exception
        action.execute(null);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*At least one cluster is required.*")
    public void testRequiresNonEmptyClusters() {
        // Given: an action with empty clusters list
        EnableTableReplication action = new EnableTableReplication();
        action.setInstanceId("my-instance");
        action.setTableId("users");
        action.setClusters(Collections.emptyList());

        // When/Then: execute should throw exception
        action.execute(null);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp =
                    ".*Multi-cluster replication requires at least 2 clusters.*")
    public void testRequiresAtLeastTwoClusters() {
        // Given: an action with only one cluster
        EnableTableReplication action = new EnableTableReplication();
        action.setInstanceId("my-instance");
        action.setTableId("users");
        action.setClusters(List.of("cluster1"));

        // When/Then: execute should throw exception
        action.execute(null);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp =
                    ".*consistency must be either 'EVENTUAL' or 'STRONG'.*")
    public void testValidatesConsistencyMode() {
        // Given: an action with invalid consistency
        EnableTableReplication action = new EnableTableReplication();
        action.setInstanceId("my-instance");
        action.setTableId("users");
        action.setClusters(List.of("cluster1", "cluster2"));
        action.setConsistency("INVALID");

        BigtableInstanceAdminClient mockClient = mock(BigtableInstanceAdminClient.class);

        // Mock the low-level API calls
        mockInstanceValidation(mockClient, "my-instance");

        // When/Then: execute should throw exception for invalid consistency
        action.execute(mockClient);
    }

    @Test
    public void testDefaultConsistencyIsEventual() {
        // Given: an action without explicit consistency
        EnableTableReplication action = new EnableTableReplication();
        action.setInstanceId("my-instance");
        action.setTableId("users");
        action.setClusters(List.of("cluster1", "cluster2"));
        // consistency not set, should default to EVENTUAL

        BigtableInstanceAdminClient mockClient = mock(BigtableInstanceAdminClient.class);

        // Mock the low-level API calls
        mockInstanceValidation(mockClient, "my-instance");

        // Mock listClusters to return matching clusters
        List<Cluster> clusters = new ArrayList<>();
        Cluster cluster1 = mock(Cluster.class);
        when(cluster1.getId()).thenReturn("cluster1");
        Cluster cluster2 = mock(Cluster.class);
        when(cluster2.getId()).thenReturn("cluster2");
        clusters.add(cluster1);
        clusters.add(cluster2);
        when(mockClient.listClusters("my-instance")).thenReturn(clusters);

        // Mock getAppProfile to return null (doesn't exist)
        when(mockClient.getAppProfile("my-instance", "users-replication")).thenReturn(null);

        // When: executing the action
        action.execute(mockClient);

        // Then: should succeed with default EVENTUAL consistency
        ArgumentCaptor<CreateAppProfileRequest> requestCaptor =
                ArgumentCaptor.forClass(CreateAppProfileRequest.class);
        verify(mockClient).createAppProfile(requestCaptor.capture());

        CreateAppProfileRequest capturedRequest = requestCaptor.getValue();
        assertNotNull(capturedRequest);
    }

    @Test
    public void testDefaultAppProfileIdIsTableIdPlusReplication() {
        // Given: an action without explicit appProfileId
        EnableTableReplication action = new EnableTableReplication();
        action.setInstanceId("my-instance");
        action.setTableId("users");
        action.setClusters(List.of("cluster1", "cluster2"));

        BigtableInstanceAdminClient mockClient = mock(BigtableInstanceAdminClient.class);

        // Mock the low-level API calls
        mockInstanceValidation(mockClient, "my-instance");

        // Mock listClusters
        List<Cluster> clusters = new ArrayList<>();
        Cluster cluster1 = mock(Cluster.class);
        when(cluster1.getId()).thenReturn("cluster1");
        Cluster cluster2 = mock(Cluster.class);
        when(cluster2.getId()).thenReturn("cluster2");
        clusters.add(cluster1);
        clusters.add(cluster2);
        when(mockClient.listClusters("my-instance")).thenReturn(clusters);

        // Mock getAppProfile to return null (doesn't exist)
        when(mockClient.getAppProfile("my-instance", "users-replication")).thenReturn(null);

        // When: executing the action
        action.execute(mockClient);

        // Then: should use default app profile ID "users-replication"
        verify(mockClient).getAppProfile("my-instance", "users-replication");
    }

    @Test
    public void testExecuteWithValidConfiguration() {
        // Given: a valid action
        EnableTableReplication action = new EnableTableReplication();
        action.setInstanceId("my-instance");
        action.setTableId("users");
        action.setClusters(List.of("us-east1-b", "europe-west1-c"));
        action.setConsistency("STRONG");
        action.setAppProfileId("custom-profile");
        action.setDescription("Test profile");

        BigtableInstanceAdminClient mockClient = mock(BigtableInstanceAdminClient.class);

        // Mock the low-level API calls
        mockInstanceValidation(mockClient, "my-instance");

        // Mock listClusters
        List<Cluster> clusters = new ArrayList<>();
        Cluster cluster1 = mock(Cluster.class);
        when(cluster1.getId()).thenReturn("us-east1-b");
        Cluster cluster2 = mock(Cluster.class);
        when(cluster2.getId()).thenReturn("europe-west1-c");
        clusters.add(cluster1);
        clusters.add(cluster2);
        when(mockClient.listClusters("my-instance")).thenReturn(clusters);

        // Mock getAppProfile to return null (doesn't exist)
        when(mockClient.getAppProfile("my-instance", "custom-profile")).thenReturn(null);

        // When: executing the action
        action.execute(mockClient);

        // Then: should create app profile
        ArgumentCaptor<CreateAppProfileRequest> requestCaptor =
                ArgumentCaptor.forClass(CreateAppProfileRequest.class);
        verify(mockClient).createAppProfile(requestCaptor.capture());

        CreateAppProfileRequest capturedRequest = requestCaptor.getValue();
        assertNotNull(capturedRequest);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*The following clusters do not exist.*")
    public void testValidatesClusterExistence() {
        // Given: an action with non-existent clusters
        EnableTableReplication action = new EnableTableReplication();
        action.setInstanceId("my-instance");
        action.setTableId("users");
        action.setClusters(List.of("non-existent-cluster", "another-missing-cluster"));

        BigtableInstanceAdminClient mockClient = mock(BigtableInstanceAdminClient.class);

        // Mock the low-level API calls
        mockInstanceValidation(mockClient, "my-instance");

        // Mock listClusters to return different clusters
        List<Cluster> clusters = new ArrayList<>();
        Cluster cluster1 = mock(Cluster.class);
        when(cluster1.getId()).thenReturn("existing-cluster-1");
        clusters.add(cluster1);
        when(mockClient.listClusters("my-instance")).thenReturn(clusters);

        // When/Then: execute should throw exception
        action.execute(mockClient);
    }

    @Test
    public void testSkipsCreationIfAppProfileExists() {
        // Given: an action when app profile already exists
        EnableTableReplication action = new EnableTableReplication();
        action.setInstanceId("my-instance");
        action.setTableId("users");
        action.setClusters(List.of("cluster1", "cluster2"));

        BigtableInstanceAdminClient mockClient = mock(BigtableInstanceAdminClient.class);

        // Mock the low-level API calls
        mockInstanceValidation(mockClient, "my-instance");

        // Mock listClusters
        List<Cluster> clusters = new ArrayList<>();
        Cluster cluster1 = mock(Cluster.class);
        when(cluster1.getId()).thenReturn("cluster1");
        Cluster cluster2 = mock(Cluster.class);
        when(cluster2.getId()).thenReturn("cluster2");
        clusters.add(cluster1);
        clusters.add(cluster2);
        when(mockClient.listClusters("my-instance")).thenReturn(clusters);

        // Mock getAppProfile to return existing profile
        AppProfile existingProfile = mock(AppProfile.class);
        when(mockClient.getAppProfile("my-instance", "users-replication"))
                .thenReturn(existingProfile);

        // When: executing the action
        action.execute(mockClient);

        // Then: should NOT call createAppProfile
        verify(mockClient, org.mockito.Mockito.never()).createAppProfile(org.mockito.Mockito.any());
    }

    @Test
    public void testGettersSetters() {
        // Given: an action
        EnableTableReplication action = new EnableTableReplication();

        // When: setting values
        action.setInstanceId("my-instance");
        action.setTableId("users");
        action.setClusters(List.of("cluster1", "cluster2"));
        action.setConsistency("STRONG");
        action.setAppProfileId("custom-profile");
        action.setDescription("Test description");

        // Then: getters should return the values
        assertEquals(action.getInstanceId(), "my-instance");
        assertEquals(action.getTableId(), "users");
        assertEquals(action.getClusters().size(), 2);
        assertEquals(action.getConsistency(), "STRONG");
        assertEquals(action.getAppProfileId(), "custom-profile");
        assertEquals(action.getDescription(), "Test description");
    }

    @Test
    public void testToString() {
        // Given: an action
        EnableTableReplication action = new EnableTableReplication();
        action.setInstanceId("my-instance");
        action.setTableId("users");
        action.setClusters(List.of("cluster1", "cluster2"));
        action.setConsistency("EVENTUAL");

        // When: calling toString
        String result = action.toString();

        // Then: it should contain relevant information
        assertTrue(result.contains("my-instance"));
        assertTrue(result.contains("users"));
        assertTrue(result.contains("EnableTableReplication"));
    }

    @Test
    public void testActionFactoryRegistration() {
        // Given: a YAML-like configuration
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> actionConfig = new HashMap<>();
        actionConfig.put("instanceId", "test-instance");
        actionConfig.put("tableId", "test-table");
        actionConfig.put("clusters", List.of("c1", "c2"));
        config.put("EnableTableReplication", actionConfig);

        // When: getting the action via ActionFactory
        Action<?> action = ActionFactory.getAction(config, mockContext);

        // Then: should successfully create EnableTableReplication instance
        assertNotNull(action);
        assertTrue(action instanceof EnableTableReplication);
    }

    @Test
    public void testMinimalConfiguration() {
        // Given: minimal valid configuration (2 clusters)
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> actionConfig = new HashMap<>();
        actionConfig.put("instanceId", "my-instance");
        actionConfig.put("tableId", "users");
        actionConfig.put("clusters", List.of("cluster1", "cluster2"));
        config.put("EnableTableReplication", actionConfig);

        // When: creating an action
        Action<?> action = ActionFactory.getAction(config, mockContext);

        // Then: should work with minimal config
        assertNotNull(action);
        assertTrue(action instanceof EnableTableReplication);

        EnableTableReplication enableReplication = (EnableTableReplication) action;
        assertEquals(enableReplication.getInstanceId(), "my-instance");
        assertEquals(enableReplication.getTableId(), "users");
        assertEquals(enableReplication.getClusters().size(), 2);
        assertNull(enableReplication.getConsistency());
        assertNull(enableReplication.getAppProfileId());
        assertNull(enableReplication.getDescription());
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*Failed to get instance.*")
    public void testInstanceValidationFailure() {
        // Given: an action when getInstance fails
        EnableTableReplication action = new EnableTableReplication();
        action.setInstanceId("non-existent-instance");
        action.setTableId("users");
        action.setClusters(List.of("cluster1", "cluster2"));

        BigtableInstanceAdminClient mockClient = mock(BigtableInstanceAdminClient.class);

        // Mock getInstance to throw exception
        when(mockClient.getProjectId()).thenReturn("test-project");
        BaseBigtableInstanceAdminClient mockBaseClient =
                mock(BaseBigtableInstanceAdminClient.class);
        when(mockClient.getBaseClient()).thenReturn(mockBaseClient);
        when(mockBaseClient.getInstance(ArgumentMatchers.any(InstanceName.class)))
                .thenThrow(new RuntimeException("Instance not found"));

        // When/Then: execute should throw exception
        action.execute(mockClient);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*Failed to list clusters.*")
    public void testClusterListingFailure() {
        // Given: an action when listClusters fails
        EnableTableReplication action = new EnableTableReplication();
        action.setInstanceId("my-instance");
        action.setTableId("users");
        action.setClusters(List.of("cluster1", "cluster2"));

        BigtableInstanceAdminClient mockClient = mock(BigtableInstanceAdminClient.class);

        // Mock the low-level API calls
        mockInstanceValidation(mockClient, "my-instance");

        // Mock listClusters to throw exception
        when(mockClient.listClusters("my-instance"))
                .thenThrow(new RuntimeException("Failed to list clusters"));

        // When/Then: execute should throw exception
        action.execute(mockClient);
    }
}
