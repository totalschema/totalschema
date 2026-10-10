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

package io.github.totalschema.connector.gcp.bigtable.actions.instance.settableautoscaling;

import static org.easymock.EasyMock.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.*;

import com.google.api.gax.longrunning.OperationFuture;
import com.google.bigtable.admin.v2.PartialUpdateClusterMetadata;
import com.google.bigtable.admin.v2.PartialUpdateClusterRequest;
import com.google.cloud.bigtable.admin.v2.BaseBigtableInstanceAdminClient;
import com.google.cloud.bigtable.admin.v2.BigtableInstanceAdminClient;
import com.google.cloud.bigtable.admin.v2.models.Cluster;
import io.github.totalschema.connector.gcp.bigtable.actions.Action;
import io.github.totalschema.connector.gcp.bigtable.actions.ActionFactory;
import io.github.totalschema.engine.api.Context;
import io.github.totalschema.spi.expression.evaluator.ExpressionEvaluator;
import io.github.totalschema.spi.variables.VariableService;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Tests for {@link SetTableAutoscaling} and {@link SetTableAutoscalingActionFactory}. */
public class SetTableAutoscalingTest {

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

    @Test
    public void testSetTableAutoscalingActionFromMap() {
        // Given: a YAML-like configuration map
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> actionConfig = new HashMap<>();
        actionConfig.put("instanceId", "my-instance");
        actionConfig.put("clusterId", "my-cluster");
        actionConfig.put("minNodes", 3);
        actionConfig.put("maxNodes", 10);
        actionConfig.put("cpuTargetPercent", 70);
        config.put("SetTableAutoscaling", actionConfig);

        // When: creating an action from the config
        Action<?> action = ActionFactory.getAction(config, mockContext);

        // Then: it should be a SetTableAutoscaling instance with correct values
        assertNotNull(action);
        assertTrue(action instanceof SetTableAutoscaling);

        SetTableAutoscaling setAutoscaling = (SetTableAutoscaling) action;
        assertEquals(setAutoscaling.getInstanceId(), "my-instance");
        assertEquals(setAutoscaling.getClusterId(), "my-cluster");
        assertEquals(setAutoscaling.getMinNodes(), Integer.valueOf(3));
        assertEquals(setAutoscaling.getMaxNodes(), Integer.valueOf(10));
        assertEquals(setAutoscaling.getCpuTargetPercent(), Integer.valueOf(70));
    }

    @Test
    public void testSetTableAutoscalingGettersSetters() {
        // Given: a SetTableAutoscaling action
        SetTableAutoscaling action = new SetTableAutoscaling();

        // When: setting properties
        action.setInstanceId("test-instance");
        action.setClusterId("test-cluster");
        action.setMinNodes(5);
        action.setMaxNodes(20);
        action.setCpuTargetPercent(60);

        // Then: getters should return the values
        assertEquals(action.getInstanceId(), "test-instance");
        assertEquals(action.getClusterId(), "test-cluster");
        assertEquals(action.getMinNodes(), Integer.valueOf(5));
        assertEquals(action.getMaxNodes(), Integer.valueOf(20));
        assertEquals(action.getCpuTargetPercent(), Integer.valueOf(60));
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*instanceId is required.*")
    public void testSetTableAutoscalingMissingInstanceId() {
        // Given: a SetTableAutoscaling action without instanceId
        SetTableAutoscaling action = new SetTableAutoscaling();
        action.setClusterId("my-cluster");
        action.setMinNodes(3);
        action.setMaxNodes(10);
        action.setCpuTargetPercent(70);

        // When/Then: should throw IllegalArgumentException
        action.execute(null);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*instanceId is required.*")
    public void testSetTableAutoscalingEmptyInstanceId() {
        // Given: a SetTableAutoscaling action with empty instanceId
        SetTableAutoscaling action = new SetTableAutoscaling();
        action.setInstanceId("");
        action.setClusterId("my-cluster");
        action.setMinNodes(3);
        action.setMaxNodes(10);
        action.setCpuTargetPercent(70);

        // When/Then: should throw IllegalArgumentException
        action.execute(null);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*clusterId is required.*")
    public void testSetTableAutoscalingMissingClusterId() {
        // Given: a SetTableAutoscaling action without clusterId
        SetTableAutoscaling action = new SetTableAutoscaling();
        action.setInstanceId("my-instance");
        action.setMinNodes(3);
        action.setMaxNodes(10);
        action.setCpuTargetPercent(70);

        // When/Then: should throw IllegalArgumentException
        action.execute(null);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*minNodes must be > 0.*")
    public void testSetTableAutoscalingMissingMinNodes() {
        // Given: a SetTableAutoscaling action without minNodes
        SetTableAutoscaling action = new SetTableAutoscaling();
        action.setInstanceId("my-instance");
        action.setClusterId("my-cluster");
        action.setMaxNodes(10);
        action.setCpuTargetPercent(70);

        // When/Then: should throw IllegalArgumentException
        action.execute(null);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*minNodes must be > 0.*")
    public void testSetTableAutoscalingZeroMinNodes() {
        // Given: a SetTableAutoscaling action with zero minNodes
        SetTableAutoscaling action = new SetTableAutoscaling();
        action.setInstanceId("my-instance");
        action.setClusterId("my-cluster");
        action.setMinNodes(0);
        action.setMaxNodes(10);
        action.setCpuTargetPercent(70);

        // When/Then: should throw IllegalArgumentException
        action.execute(null);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*maxNodes must be >= minNodes.*")
    public void testSetTableAutoscalingMaxNodesLessThanMinNodes() {
        // Given: a SetTableAutoscaling action with maxNodes < minNodes
        SetTableAutoscaling action = new SetTableAutoscaling();
        action.setInstanceId("my-instance");
        action.setClusterId("my-cluster");
        action.setMinNodes(10);
        action.setMaxNodes(5);
        action.setCpuTargetPercent(70);

        // When/Then: should throw IllegalArgumentException
        action.execute(null);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*cpuTargetPercent must be between 1 and 100.*")
    public void testSetTableAutoscalingInvalidCpuTargetZero() {
        // Given: a SetTableAutoscaling action with cpuTargetPercent = 0
        SetTableAutoscaling action = new SetTableAutoscaling();
        action.setInstanceId("my-instance");
        action.setClusterId("my-cluster");
        action.setMinNodes(3);
        action.setMaxNodes(10);
        action.setCpuTargetPercent(0);

        // When/Then: should throw IllegalArgumentException
        action.execute(null);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*cpuTargetPercent must be between 1 and 100.*")
    public void testSetTableAutoscalingInvalidCpuTargetOver100() {
        // Given: a SetTableAutoscaling action with cpuTargetPercent > 100
        SetTableAutoscaling action = new SetTableAutoscaling();
        action.setInstanceId("my-instance");
        action.setClusterId("my-cluster");
        action.setMinNodes(3);
        action.setMaxNodes(10);
        action.setCpuTargetPercent(101);

        // When/Then: should throw IllegalArgumentException
        action.execute(null);
    }

    @Test
    public void testSetTableAutoscalingExecuteSuccess() throws Exception {
        // Given: a valid SetTableAutoscaling action
        SetTableAutoscaling action = new SetTableAutoscaling();
        action.setInstanceId("test-instance");
        action.setClusterId("test-cluster");
        action.setMinNodes(3);
        action.setMaxNodes(10);
        action.setCpuTargetPercent(70);

        BigtableInstanceAdminClient mockClient = mock(BigtableInstanceAdminClient.class);
        BaseBigtableInstanceAdminClient mockBaseClient =
                mock(BaseBigtableInstanceAdminClient.class);
        Cluster mockCluster = mock(Cluster.class);

        // Mock cluster existence validation
        when(mockClient.getCluster("test-instance", "test-cluster")).thenReturn(mockCluster);

        // Mock base client and project ID
        when(mockClient.getProjectId()).thenReturn("test-project");
        when(mockClient.getBaseClient()).thenReturn(mockBaseClient);

        // Mock the async operation - use a mock OperationFuture
        @SuppressWarnings("unchecked")
        OperationFuture<com.google.bigtable.admin.v2.Cluster, PartialUpdateClusterMetadata>
                mockOperation = mock(OperationFuture.class);
        com.google.bigtable.admin.v2.Cluster protoCluster =
                com.google.bigtable.admin.v2.Cluster.getDefaultInstance();
        try {
            when(mockOperation.get()).thenReturn(protoCluster);
        } catch (Exception e) {
            // This is a mock, exception won't occur
        }
        when(mockBaseClient.partialUpdateClusterAsync(any(PartialUpdateClusterRequest.class)))
                .thenReturn(mockOperation);

        // When: executing the action
        action.execute(mockClient);

        // Then: verify partialUpdateClusterAsync was called
        ArgumentCaptor<PartialUpdateClusterRequest> requestCaptor =
                ArgumentCaptor.forClass(PartialUpdateClusterRequest.class);
        verify(mockBaseClient).partialUpdateClusterAsync(requestCaptor.capture());

        PartialUpdateClusterRequest capturedRequest = requestCaptor.getValue();
        assertNotNull(capturedRequest);
        assertTrue(capturedRequest.getCluster().hasClusterConfig());
        assertTrue(capturedRequest.getCluster().getClusterConfig().hasClusterAutoscalingConfig());
        assertEquals(
                capturedRequest
                        .getCluster()
                        .getClusterConfig()
                        .getClusterAutoscalingConfig()
                        .getAutoscalingLimits()
                        .getMinServeNodes(),
                3);
        assertEquals(
                capturedRequest
                        .getCluster()
                        .getClusterConfig()
                        .getClusterAutoscalingConfig()
                        .getAutoscalingLimits()
                        .getMaxServeNodes(),
                10);
        assertEquals(
                capturedRequest
                        .getCluster()
                        .getClusterConfig()
                        .getClusterAutoscalingConfig()
                        .getAutoscalingTargets()
                        .getCpuUtilizationPercent(),
                70);
        assertEquals(
                capturedRequest.getUpdateMask().getPathsList().get(0),
                "cluster_config.cluster_autoscaling_config");
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*Failed to get cluster.*")
    public void testSetTableAutoscalingNonExistentCluster() {
        // Given: a SetTableAutoscaling action with non-existent cluster
        SetTableAutoscaling action = new SetTableAutoscaling();
        action.setInstanceId("test-instance");
        action.setClusterId("non-existent-cluster");
        action.setMinNodes(3);
        action.setMaxNodes(10);
        action.setCpuTargetPercent(70);

        BigtableInstanceAdminClient mockClient = mock(BigtableInstanceAdminClient.class);

        // Mock cluster not found
        when(mockClient.getCluster("test-instance", "non-existent-cluster"))
                .thenThrow(new RuntimeException("Cluster not found"));

        // When/Then: should throw IllegalArgumentException
        action.execute(mockClient);
    }

    @Test
    public void testSetTableAutoscalingToString() {
        // Given: a SetTableAutoscaling action
        SetTableAutoscaling action = new SetTableAutoscaling();
        action.setInstanceId("test-instance");
        action.setClusterId("test-cluster");
        action.setMinNodes(3);
        action.setMaxNodes(10);
        action.setCpuTargetPercent(70);

        // When/Then: toString should work and contain relevant info
        String str = action.toString();
        assertTrue(str.contains("SetTableAutoscaling"));
        assertTrue(str.contains("test-instance"));
        assertTrue(str.contains("test-cluster"));
    }

    @Test
    public void testSetTableAutoscalingMinimalConfig() {
        // Given: minimal valid configuration (1 node min, 1 node max)
        SetTableAutoscaling action = new SetTableAutoscaling();
        action.setInstanceId("dev-instance");
        action.setClusterId("dev-cluster");
        action.setMinNodes(1);
        action.setMaxNodes(1);
        action.setCpuTargetPercent(80);

        // When/Then: should have all values set correctly
        assertEquals(action.getMinNodes(), Integer.valueOf(1));
        assertEquals(action.getMaxNodes(), Integer.valueOf(1));
        assertEquals(action.getCpuTargetPercent(), Integer.valueOf(80));
    }

    @Test
    public void testSetTableAutoscalingMaximalConfig() {
        // Given: large-scale production configuration
        SetTableAutoscaling action = new SetTableAutoscaling();
        action.setInstanceId("prod-instance");
        action.setClusterId("prod-cluster");
        action.setMinNodes(50);
        action.setMaxNodes(100);
        action.setCpuTargetPercent(50);

        // When/Then: should handle large node counts
        assertEquals(action.getMinNodes(), Integer.valueOf(50));
        assertEquals(action.getMaxNodes(), Integer.valueOf(100));
        assertEquals(action.getCpuTargetPercent(), Integer.valueOf(50));
    }

    @Test
    public void testSetTableAutoscalingActionFactoryRegistration() {
        // Given: a YAML-like configuration with SetTableAutoscaling
        Map<String, Object> config = new HashMap<>();
        config.put(
                "SetTableAutoscaling",
                new HashMap<String, Object>() {
                    {
                        put("instanceId", "test-instance");
                        put("clusterId", "test-cluster");
                        put("minNodes", 3);
                        put("maxNodes", 10);
                        put("cpuTargetPercent", 70);
                    }
                });

        // When: getting the action via ActionFactory
        Action<?> action = ActionFactory.getAction(config, mockContext);

        // Then: should successfully create SetTableAutoscaling instance
        assertNotNull(action);
        assertTrue(action instanceof SetTableAutoscaling);
    }
}
