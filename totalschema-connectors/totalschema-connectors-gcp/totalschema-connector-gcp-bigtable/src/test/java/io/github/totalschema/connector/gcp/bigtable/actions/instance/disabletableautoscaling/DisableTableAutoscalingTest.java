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

package io.github.totalschema.connector.gcp.bigtable.actions.instance.disabletableautoscaling;

import static org.easymock.EasyMock.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.*;

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
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Tests for {@link DisableTableAutoscaling} and {@link DisableTableAutoscalingActionFactory}. */
public class DisableTableAutoscalingTest {

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
    public void testDisableTableAutoscalingActionFromMap() {
        // Given: a YAML-like configuration map
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> actionConfig = new HashMap<>();
        actionConfig.put("instanceId", "my-instance");
        actionConfig.put("clusterId", "my-cluster");
        actionConfig.put("fixedNodes", 5);
        config.put("DisableTableAutoscaling", actionConfig);

        // When: creating an action from the config
        Action<?> action = ActionFactory.getAction(config, mockContext);

        // Then: it should be a DisableTableAutoscaling instance with correct values
        assertNotNull(action);
        assertTrue(action instanceof DisableTableAutoscaling);

        DisableTableAutoscaling disableAutoscaling = (DisableTableAutoscaling) action;
        assertEquals(disableAutoscaling.getInstanceId(), "my-instance");
        assertEquals(disableAutoscaling.getClusterId(), "my-cluster");
        assertEquals(disableAutoscaling.getFixedNodes(), Integer.valueOf(5));
    }

    @Test
    public void testDisableTableAutoscalingGettersSetters() {
        // Given: a DisableTableAutoscaling action
        DisableTableAutoscaling action = new DisableTableAutoscaling();

        // When: setting properties
        action.setInstanceId("test-instance");
        action.setClusterId("test-cluster");
        action.setFixedNodes(10);

        // Then: getters should return the values
        assertEquals(action.getInstanceId(), "test-instance");
        assertEquals(action.getClusterId(), "test-cluster");
        assertEquals(action.getFixedNodes(), Integer.valueOf(10));
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*instanceId is required.*")
    public void testDisableTableAutoscalingMissingInstanceId() {
        // Given: a DisableTableAutoscaling action without instanceId
        DisableTableAutoscaling action = new DisableTableAutoscaling();
        action.setClusterId("my-cluster");
        action.setFixedNodes(5);

        // When/Then: should throw IllegalArgumentException
        action.execute(null);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*instanceId is required.*")
    public void testDisableTableAutoscalingEmptyInstanceId() {
        // Given: a DisableTableAutoscaling action with empty instanceId
        DisableTableAutoscaling action = new DisableTableAutoscaling();
        action.setInstanceId("");
        action.setClusterId("my-cluster");
        action.setFixedNodes(5);

        // When/Then: should throw IllegalArgumentException
        action.execute(null);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*clusterId is required.*")
    public void testDisableTableAutoscalingMissingClusterId() {
        // Given: a DisableTableAutoscaling action without clusterId
        DisableTableAutoscaling action = new DisableTableAutoscaling();
        action.setInstanceId("my-instance");
        action.setFixedNodes(5);

        // When/Then: should throw IllegalArgumentException
        action.execute(null);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*fixedNodes must be > 0.*")
    public void testDisableTableAutoscalingMissingFixedNodes() {
        // Given: a DisableTableAutoscaling action without fixedNodes
        DisableTableAutoscaling action = new DisableTableAutoscaling();
        action.setInstanceId("my-instance");
        action.setClusterId("my-cluster");

        // When/Then: should throw IllegalArgumentException
        action.execute(null);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*fixedNodes must be > 0.*")
    public void testDisableTableAutoscalingZeroFixedNodes() {
        // Given: a DisableTableAutoscaling action with zero fixedNodes
        DisableTableAutoscaling action = new DisableTableAutoscaling();
        action.setInstanceId("my-instance");
        action.setClusterId("my-cluster");
        action.setFixedNodes(0);

        // When/Then: should throw IllegalArgumentException
        action.execute(null);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*fixedNodes must be > 0.*")
    public void testDisableTableAutoscalingNegativeFixedNodes() {
        // Given: a DisableTableAutoscaling action with negative fixedNodes
        DisableTableAutoscaling action = new DisableTableAutoscaling();
        action.setInstanceId("my-instance");
        action.setClusterId("my-cluster");
        action.setFixedNodes(-5);

        // When/Then: should throw IllegalArgumentException
        action.execute(null);
    }

    @Test
    public void testDisableTableAutoscalingExecuteSuccess() {
        // Given: a valid DisableTableAutoscaling action
        DisableTableAutoscaling action = new DisableTableAutoscaling();
        action.setInstanceId("test-instance");
        action.setClusterId("test-cluster");
        action.setFixedNodes(5);

        BigtableInstanceAdminClient mockClient = mock(BigtableInstanceAdminClient.class);
        Cluster mockCluster = mock(Cluster.class);

        // Mock cluster existence validation
        when(mockClient.getCluster("test-instance", "test-cluster")).thenReturn(mockCluster);

        // When: executing the action
        action.execute(mockClient);

        // Then: verify resizeCluster was called with correct parameters
        verify(mockClient).resizeCluster("test-instance", "test-cluster", 5);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*Failed to get cluster.*")
    public void testDisableTableAutoscalingNonExistentCluster() {
        // Given: a DisableTableAutoscaling action with non-existent cluster
        DisableTableAutoscaling action = new DisableTableAutoscaling();
        action.setInstanceId("test-instance");
        action.setClusterId("non-existent-cluster");
        action.setFixedNodes(5);

        BigtableInstanceAdminClient mockClient = mock(BigtableInstanceAdminClient.class);

        // Mock cluster not found
        when(mockClient.getCluster("test-instance", "non-existent-cluster"))
                .thenThrow(new RuntimeException("Cluster not found"));

        // When/Then: should throw IllegalArgumentException
        action.execute(mockClient);
    }

    @Test
    public void testDisableTableAutoscalingToString() {
        // Given: a DisableTableAutoscaling action
        DisableTableAutoscaling action = new DisableTableAutoscaling();
        action.setInstanceId("test-instance");
        action.setClusterId("test-cluster");
        action.setFixedNodes(5);

        // When/Then: toString should work and contain relevant info
        String str = action.toString();
        assertTrue(str.contains("DisableTableAutoscaling"));
        assertTrue(str.contains("test-instance"));
        assertTrue(str.contains("test-cluster"));
    }

    @Test
    public void testDisableTableAutoscalingMinimalNodeCount() {
        // Given: minimal valid configuration (1 node)
        DisableTableAutoscaling action = new DisableTableAutoscaling();
        action.setInstanceId("dev-instance");
        action.setClusterId("dev-cluster");
        action.setFixedNodes(1);

        // When/Then: should have all values set correctly
        assertEquals(action.getFixedNodes(), Integer.valueOf(1));
    }

    @Test
    public void testDisableTableAutoscalingLargeNodeCount() {
        // Given: large-scale production configuration
        DisableTableAutoscaling action = new DisableTableAutoscaling();
        action.setInstanceId("prod-instance");
        action.setClusterId("prod-cluster");
        action.setFixedNodes(100);

        // When/Then: should handle large node counts
        assertEquals(action.getFixedNodes(), Integer.valueOf(100));
    }

    @Test
    public void testDisableTableAutoscalingActionFactoryRegistration() {
        // Given: a YAML-like configuration with DisableTableAutoscaling
        Map<String, Object> config = new HashMap<>();
        config.put(
                "DisableTableAutoscaling",
                new HashMap<String, Object>() {
                    {
                        put("instanceId", "test-instance");
                        put("clusterId", "test-cluster");
                        put("fixedNodes", 5);
                    }
                });

        // When: getting the action via ActionFactory
        Action<?> action = ActionFactory.getAction(config, mockContext);

        // Then: should successfully create DisableTableAutoscaling instance
        assertNotNull(action);
        assertTrue(action instanceof DisableTableAutoscaling);
    }

    @Test
    public void testDisableTableAutoscalingDevelopmentScenario() {
        // Given: typical development scenario (single node)
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> actionConfig = new HashMap<>();
        actionConfig.put("instanceId", "dev-instance");
        actionConfig.put("clusterId", "dev-cluster-us-central1");
        actionConfig.put("fixedNodes", 1);
        config.put("DisableTableAutoscaling", actionConfig);

        // When: creating an action
        Action<?> action = ActionFactory.getAction(config, mockContext);

        // Then: should be configured for single-node dev environment
        assertNotNull(action);
        assertTrue(action instanceof DisableTableAutoscaling);
        DisableTableAutoscaling disableAutoscaling = (DisableTableAutoscaling) action;
        assertEquals(disableAutoscaling.getFixedNodes(), Integer.valueOf(1));
    }

    @Test
    public void testDisableTableAutoscalingProductionScenario() {
        // Given: typical production scenario (multiple nodes)
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> actionConfig = new HashMap<>();
        actionConfig.put("instanceId", "prod-instance");
        actionConfig.put("clusterId", "prod-cluster-us-east1");
        actionConfig.put("fixedNodes", 20);
        config.put("DisableTableAutoscaling", actionConfig);

        // When: creating an action
        Action<?> action = ActionFactory.getAction(config, mockContext);

        // Then: should be configured for multi-node production environment
        assertNotNull(action);
        assertTrue(action instanceof DisableTableAutoscaling);
        DisableTableAutoscaling disableAutoscaling = (DisableTableAutoscaling) action;
        assertEquals(disableAutoscaling.getFixedNodes(), Integer.valueOf(20));
    }
}
