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

package io.github.totalschema.connector.gcp.bigtable.actions.table.restoretable;

import static org.easymock.EasyMock.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.*;

import com.google.cloud.bigtable.admin.v2.BigtableTableAdminClient;
import com.google.cloud.bigtable.admin.v2.models.RestoreTableRequest;
import io.github.totalschema.connector.gcp.bigtable.actions.Action;
import io.github.totalschema.connector.gcp.bigtable.actions.ActionFactory;
import io.github.totalschema.engine.api.Context;
import io.github.totalschema.spi.expression.evaluator.ExpressionEvaluator;
import io.github.totalschema.spi.variables.VariableService;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Tests for {@link RestoreTable} and {@link RestoreTableActionFactory}. */
public class RestoreTableTest {

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
    public void testRestoreTableActionFromMap() {
        // Given: a YAML-like configuration map
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> restoreTableConfig = new HashMap<>();
        restoreTableConfig.put("backupId", "users_backup_20260720");
        restoreTableConfig.put("tableId", "users_restored");
        restoreTableConfig.put("clusterId", "my-cluster");
        config.put("RestoreTable", restoreTableConfig);

        // When: creating an action from the config
        Action<?> action = ActionFactory.getAction(config, mockContext);

        // Then: it should be a RestoreTable instance with correct values
        assertNotNull(action);
        assertTrue(action instanceof RestoreTable);

        RestoreTable restoreTable = (RestoreTable) action;
        assertEquals(restoreTable.getBackupId(), "users_backup_20260720");
        assertEquals(restoreTable.getTableId(), "users_restored");
        assertEquals(restoreTable.getClusterId(), "my-cluster");
        assertNull(restoreTable.getOverwrite());
    }

    @Test
    public void testRestoreTableWithOverwrite() {
        // Given: configuration with overwrite flag
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> restoreTableConfig = new HashMap<>();
        restoreTableConfig.put("backupId", "users_backup_20260720");
        restoreTableConfig.put("tableId", "users");
        restoreTableConfig.put("clusterId", "my-cluster");
        restoreTableConfig.put("overwrite", true);
        config.put("RestoreTable", restoreTableConfig);

        // When: creating an action
        Action<?> action = ActionFactory.getAction(config, mockContext);

        // Then: overwrite should be set to true
        assertNotNull(action);
        assertTrue(action instanceof RestoreTable);

        RestoreTable restoreTable = (RestoreTable) action;
        assertEquals(restoreTable.getOverwrite(), Boolean.TRUE);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*backupId is required.*")
    public void testRestoreTableRequiresBackupId() {
        // Given: a RestoreTable action without backupId
        RestoreTable restoreTable = new RestoreTable();
        restoreTable.setBackupId(null);
        restoreTable.setTableId("users");
        restoreTable.setClusterId("cluster");

        // When/Then: execute should throw exception
        restoreTable.execute(null);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*backupId is required.*")
    public void testRestoreTableRequiresNonEmptyBackupId() {
        // Given: a RestoreTable action with empty backupId
        RestoreTable restoreTable = new RestoreTable();
        restoreTable.setBackupId("");
        restoreTable.setTableId("users");
        restoreTable.setClusterId("cluster");

        // When/Then: execute should throw exception
        restoreTable.execute(null);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*tableId is required.*")
    public void testRestoreTableRequiresTableId() {
        // Given: a RestoreTable action without tableId
        RestoreTable restoreTable = new RestoreTable();
        restoreTable.setBackupId("backup");
        restoreTable.setTableId(null);
        restoreTable.setClusterId("cluster");

        // When/Then: execute should throw exception
        restoreTable.execute(null);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*tableId is required.*")
    public void testRestoreTableRequiresNonEmptyTableId() {
        // Given: a RestoreTable action with empty tableId
        RestoreTable restoreTable = new RestoreTable();
        restoreTable.setBackupId("backup");
        restoreTable.setTableId("");
        restoreTable.setClusterId("cluster");

        // When/Then: execute should throw exception
        restoreTable.execute(null);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*clusterId is required.*")
    public void testRestoreTableRequiresClusterId() {
        // Given: a RestoreTable action without clusterId
        RestoreTable restoreTable = new RestoreTable();
        restoreTable.setBackupId("backup");
        restoreTable.setTableId("users");
        restoreTable.setClusterId(null);

        // When/Then: execute should throw exception
        restoreTable.execute(null);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*clusterId is required.*")
    public void testRestoreTableRequiresNonEmptyClusterId() {
        // Given: a RestoreTable action with empty clusterId
        RestoreTable restoreTable = new RestoreTable();
        restoreTable.setBackupId("backup");
        restoreTable.setTableId("users");
        restoreTable.setClusterId("");

        // When/Then: execute should throw exception
        restoreTable.execute(null);
    }

    @Test
    public void testRestoreTableToString() {
        // Given: a RestoreTable action
        RestoreTable restoreTable = new RestoreTable();
        restoreTable.setBackupId("users_backup_20260720");
        restoreTable.setTableId("users_restored");
        restoreTable.setClusterId("my-cluster");
        restoreTable.setOverwrite(true);

        // When: calling toString
        String result = restoreTable.toString();

        // Then: it should contain all fields
        assertTrue(result.contains("users_backup_20260720"));
        assertTrue(result.contains("users_restored"));
        assertTrue(result.contains("my-cluster"));
        assertTrue(result.contains("true"));
        assertTrue(result.contains("RestoreTable"));
    }

    @Test
    public void testRestoreTableGettersSetters() {
        // Given: a RestoreTable action
        RestoreTable restoreTable = new RestoreTable();

        // When: setting values
        restoreTable.setBackupId("backup");
        restoreTable.setTableId("table");
        restoreTable.setClusterId("cluster");
        restoreTable.setOverwrite(true);

        // Then: getters should return the values
        assertEquals(restoreTable.getBackupId(), "backup");
        assertEquals(restoreTable.getTableId(), "table");
        assertEquals(restoreTable.getClusterId(), "cluster");
        assertEquals(restoreTable.getOverwrite(), Boolean.TRUE);
    }

    @Test
    public void testRestoreTableActionFactoryRegistration() {
        // Given: a YAML-like configuration with RestoreTable
        Map<String, Object> config = new HashMap<>();
        config.put(
                "RestoreTable",
                new HashMap<String, Object>() {
                    {
                        put("backupId", "backup");
                        put("tableId", "table");
                        put("clusterId", "cluster");
                    }
                });

        // When: getting the action via ActionFactory
        Action<?> action = ActionFactory.getAction(config, mockContext);

        // Then: should successfully create RestoreTable instance
        assertNotNull(action);
        assertTrue(action instanceof RestoreTable);
    }

    @Test
    public void testRestoreTableMinimalConfig() {
        // Given: minimal valid configuration
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> restoreTableConfig = new HashMap<>();
        restoreTableConfig.put("backupId", "users_backup");
        restoreTableConfig.put("tableId", "users_restored");
        restoreTableConfig.put("clusterId", "prod-cluster");
        config.put("RestoreTable", restoreTableConfig);

        // When: creating an action
        Action<?> action = ActionFactory.getAction(config, mockContext);

        // Then: should work with required fields only
        assertNotNull(action);
        assertTrue(action instanceof RestoreTable);

        RestoreTable restoreTable = (RestoreTable) action;
        assertEquals(restoreTable.getBackupId(), "users_backup");
        assertEquals(restoreTable.getTableId(), "users_restored");
        assertEquals(restoreTable.getClusterId(), "prod-cluster");
        assertNull(restoreTable.getOverwrite());
    }

    @Test
    public void testRestoreTableExecuteWithoutOverwrite()
            throws ExecutionException, InterruptedException {
        // Given: a valid RestoreTable action without overwrite
        RestoreTable restoreTable = new RestoreTable();
        restoreTable.setBackupId("users_backup");
        restoreTable.setTableId("users_restored");
        restoreTable.setClusterId("my-cluster");

        BigtableTableAdminClient mockClient = mock(BigtableTableAdminClient.class);

        // When: executing the action
        restoreTable.execute(mockClient);

        // Then: verify restoreTable was called with correct content
        ArgumentCaptor<RestoreTableRequest> requestCaptor =
                ArgumentCaptor.forClass(RestoreTableRequest.class);
        verify(mockClient).restoreTable(requestCaptor.capture());

        RestoreTableRequest capturedRequest = requestCaptor.getValue();
        // Verify the request contains correct cluster, backup and table IDs
        // Note: parent is the instance path, cluster ID is in the backup path
        assertTrue(
                capturedRequest
                        .toProto("test-project", "test-instance")
                        .getBackup()
                        .contains("my-cluster"),
                "Expected clusterId 'my-cluster' in backup path");
        assertTrue(
                capturedRequest
                        .toProto("test-project", "test-instance")
                        .getBackup()
                        .contains("users_backup"),
                "Expected backupId 'users_backup' in request");
        assertEquals(
                capturedRequest.toProto("test-project", "test-instance").getTableId(),
                "users_restored",
                "Expected tableId 'users_restored'");

        // Verify exists and deleteTable were not called
        verify(mockClient, never()).exists(any(String.class));
        verify(mockClient, never()).deleteTable(any(String.class));
    }

    @Test
    public void testRestoreTableExecuteWithOverwriteAndTableExists()
            throws ExecutionException, InterruptedException {
        // Given: a valid RestoreTable action with overwrite and existing table
        RestoreTable restoreTable = new RestoreTable();
        restoreTable.setBackupId("users_backup");
        restoreTable.setTableId("users");
        restoreTable.setClusterId("my-cluster");
        restoreTable.setOverwrite(true);

        BigtableTableAdminClient mockClient = mock(BigtableTableAdminClient.class);
        when(mockClient.exists("users")).thenReturn(true);

        // When: executing the action
        restoreTable.execute(mockClient);

        // Then: verify exists, deleteTable, and restoreTable were all called with correct arguments
        verify(mockClient).exists(eq("users"));
        verify(mockClient).deleteTable(eq("users"));

        ArgumentCaptor<RestoreTableRequest> requestCaptor =
                ArgumentCaptor.forClass(RestoreTableRequest.class);
        verify(mockClient).restoreTable(requestCaptor.capture());

        RestoreTableRequest capturedRequest = requestCaptor.getValue();
        // Note: parent is the instance path, cluster ID is in the backup path
        assertTrue(
                capturedRequest
                        .toProto("test-project", "test-instance")
                        .getBackup()
                        .contains("my-cluster"),
                "Expected clusterId 'my-cluster' in backup path");
        assertTrue(
                capturedRequest
                        .toProto("test-project", "test-instance")
                        .getBackup()
                        .contains("users_backup"),
                "Expected backupId 'users_backup' in request");
        assertEquals(
                capturedRequest.toProto("test-project", "test-instance").getTableId(),
                "users",
                "Expected tableId 'users'");
    }

    @Test
    public void testRestoreTableExecuteWithOverwriteButTableDoesNotExist()
            throws ExecutionException, InterruptedException {
        // Given: a valid RestoreTable action with overwrite but table doesn't exist
        RestoreTable restoreTable = new RestoreTable();
        restoreTable.setBackupId("users_backup");
        restoreTable.setTableId("users");
        restoreTable.setClusterId("my-cluster");
        restoreTable.setOverwrite(true);

        BigtableTableAdminClient mockClient = mock(BigtableTableAdminClient.class);
        when(mockClient.exists("users")).thenReturn(false);

        // When: executing the action
        restoreTable.execute(mockClient);

        // Then: verify exists and restoreTable were called, but not deleteTable
        verify(mockClient).exists(eq("users"));
        verify(mockClient, never()).deleteTable(any(String.class));

        ArgumentCaptor<RestoreTableRequest> requestCaptor =
                ArgumentCaptor.forClass(RestoreTableRequest.class);
        verify(mockClient).restoreTable(requestCaptor.capture());

        RestoreTableRequest capturedRequest = requestCaptor.getValue();
        // Note: parent is the instance path, cluster ID is in the backup path
        assertTrue(
                capturedRequest
                        .toProto("test-project", "test-instance")
                        .getBackup()
                        .contains("my-cluster"),
                "Expected clusterId 'my-cluster' in backup path");
        assertTrue(
                capturedRequest
                        .toProto("test-project", "test-instance")
                        .getBackup()
                        .contains("users_backup"),
                "Expected backupId 'users_backup' in request");
        assertEquals(
                capturedRequest.toProto("test-project", "test-instance").getTableId(),
                "users",
                "Expected tableId 'users'");
    }
}
