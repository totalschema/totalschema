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

package io.github.totalschema.connector.gcp.bigtable.actions.table.deletebackup;

import static org.easymock.EasyMock.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.*;

import com.google.cloud.bigtable.admin.v2.BigtableTableAdminClient;
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

/** Tests for {@link DeleteBackup} and {@link DeleteBackupActionFactory}. */
public class DeleteBackupTest {

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
    public void testDeleteBackupActionFromMap() {
        // Given: a YAML-like configuration map
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> deleteBackupConfig = new HashMap<>();
        deleteBackupConfig.put("backupId", "users_backup_20260701");
        deleteBackupConfig.put("clusterId", "my-cluster");
        config.put("DeleteBackup", deleteBackupConfig);

        // When: creating an action from the config
        Action<?> action = ActionFactory.getAction(config, mockContext);

        // Then: it should be a DeleteBackup instance with correct values
        assertNotNull(action);
        assertTrue(action instanceof DeleteBackup);

        DeleteBackup deleteBackup = (DeleteBackup) action;
        assertEquals(deleteBackup.getBackupId(), "users_backup_20260701");
        assertEquals(deleteBackup.getClusterId(), "my-cluster");
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*backupId is required.*")
    public void testDeleteBackupRequiresBackupId() {
        // Given: a DeleteBackup action without backupId
        DeleteBackup deleteBackup = new DeleteBackup();
        deleteBackup.setBackupId(null);
        deleteBackup.setClusterId("cluster");

        // When/Then: execute should throw exception
        deleteBackup.execute(null);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*backupId is required.*")
    public void testDeleteBackupRequiresNonEmptyBackupId() {
        // Given: a DeleteBackup action with empty backupId
        DeleteBackup deleteBackup = new DeleteBackup();
        deleteBackup.setBackupId("");
        deleteBackup.setClusterId("cluster");

        // When/Then: execute should throw exception
        deleteBackup.execute(null);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*clusterId is required.*")
    public void testDeleteBackupRequiresClusterId() {
        // Given: a DeleteBackup action without clusterId
        DeleteBackup deleteBackup = new DeleteBackup();
        deleteBackup.setBackupId("backup");
        deleteBackup.setClusterId(null);

        // When/Then: execute should throw exception
        deleteBackup.execute(null);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*clusterId is required.*")
    public void testDeleteBackupRequiresNonEmptyClusterId() {
        // Given: a DeleteBackup action with empty clusterId
        DeleteBackup deleteBackup = new DeleteBackup();
        deleteBackup.setBackupId("backup");
        deleteBackup.setClusterId("");

        // When/Then: execute should throw exception
        deleteBackup.execute(null);
    }

    @Test
    public void testDeleteBackupToString() {
        // Given: a DeleteBackup action
        DeleteBackup deleteBackup = new DeleteBackup();
        deleteBackup.setBackupId("users_backup");
        deleteBackup.setClusterId("my-cluster");

        // When: calling toString
        String result = deleteBackup.toString();

        // Then: it should contain all fields
        assertTrue(result.contains("users_backup"));
        assertTrue(result.contains("my-cluster"));
        assertTrue(result.contains("DeleteBackup"));
    }

    @Test
    public void testDeleteBackupGettersSetters() {
        // Given: a DeleteBackup action
        DeleteBackup deleteBackup = new DeleteBackup();

        // When: setting values
        deleteBackup.setBackupId("backup");
        deleteBackup.setClusterId("cluster");

        // Then: getters should return the values
        assertEquals(deleteBackup.getBackupId(), "backup");
        assertEquals(deleteBackup.getClusterId(), "cluster");
    }

    @Test
    public void testDeleteBackupActionFactoryRegistration() {
        // Given: a YAML-like configuration with DeleteBackup
        Map<String, Object> config = new HashMap<>();
        config.put(
                "DeleteBackup",
                new HashMap<String, Object>() {
                    {
                        put("backupId", "backup");
                        put("clusterId", "cluster");
                    }
                });

        // When: getting the action via ActionFactory
        Action<?> action = ActionFactory.getAction(config, mockContext);

        // Then: should successfully create DeleteBackup instance
        assertNotNull(action);
        assertTrue(action instanceof DeleteBackup);
    }

    @Test
    public void testDeleteBackupMinimalConfig() {
        // Given: minimal valid configuration
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> deleteBackupConfig = new HashMap<>();
        deleteBackupConfig.put("backupId", "old-backup");
        deleteBackupConfig.put("clusterId", "prod-cluster");
        config.put("DeleteBackup", deleteBackupConfig);

        // When: creating an action
        Action<?> action = ActionFactory.getAction(config, mockContext);

        // Then: should work with required fields
        assertNotNull(action);
        assertTrue(action instanceof DeleteBackup);

        DeleteBackup deleteBackup = (DeleteBackup) action;
        assertEquals(deleteBackup.getBackupId(), "old-backup");
        assertEquals(deleteBackup.getClusterId(), "prod-cluster");
    }

    @Test
    public void testDeleteBackupExecute() {
        // Given: a valid DeleteBackup action
        DeleteBackup deleteBackup = new DeleteBackup();
        deleteBackup.setBackupId("users_backup");
        deleteBackup.setClusterId("my-cluster");

        BigtableTableAdminClient mockClient = mock(BigtableTableAdminClient.class);

        // When: executing the action
        deleteBackup.execute(mockClient);

        // Then: verify deleteBackup was called with correct arguments
        verify(mockClient).deleteBackup("my-cluster", "users_backup");
    }
}
