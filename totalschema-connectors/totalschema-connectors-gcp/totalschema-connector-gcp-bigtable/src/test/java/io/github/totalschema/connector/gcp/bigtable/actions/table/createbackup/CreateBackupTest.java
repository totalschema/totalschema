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

package io.github.totalschema.connector.gcp.bigtable.actions.table.createbackup;

import static org.easymock.EasyMock.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.*;

import com.google.cloud.bigtable.admin.v2.BigtableTableAdminClient;
import com.google.cloud.bigtable.admin.v2.models.CreateBackupRequest;
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

/** Tests for {@link CreateBackup} and {@link CreateBackupActionFactory}. */
public class CreateBackupTest {

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
    public void testCreateBackupActionFromMap() {
        // Given: a YAML-like configuration map
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> createBackupConfig = new HashMap<>();
        createBackupConfig.put("sourceTableId", "users");
        createBackupConfig.put("backupId", "users_backup_20260731");
        createBackupConfig.put("expireTime", "30d");
        createBackupConfig.put("clusterId", "my-cluster");
        config.put("CreateBackup", createBackupConfig);

        // When: creating an action from the config
        Action<?> action = ActionFactory.getAction(config, mockContext);

        // Then: it should be a CreateBackup instance with correct values
        assertNotNull(action);
        assertTrue(action instanceof CreateBackup);

        CreateBackup createBackup = (CreateBackup) action;
        assertEquals(createBackup.getSourceTableId(), "users");
        assertEquals(createBackup.getBackupId(), "users_backup_20260731");
        assertEquals(createBackup.getExpireTime(), "30d");
        assertEquals(createBackup.getClusterId(), "my-cluster");
    }

    @Test
    public void testCreateBackupWithClusterId() {
        // Given: configuration with clusterId
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> createBackupConfig = new HashMap<>();
        createBackupConfig.put("sourceTableId", "users");
        createBackupConfig.put("backupId", "users_backup");
        createBackupConfig.put("expireTime", "30d");
        createBackupConfig.put("clusterId", "my-cluster");
        config.put("CreateBackup", createBackupConfig);

        // When: creating an action
        Action<?> action = ActionFactory.getAction(config, mockContext);

        // Then: clusterId should be set
        assertNotNull(action);
        assertTrue(action instanceof CreateBackup);

        CreateBackup createBackup = (CreateBackup) action;
        assertEquals(createBackup.getClusterId(), "my-cluster");
    }

    @Test
    public void testCreateBackupWithTimestampExpiration() {
        // Given: configuration with ISO-8601 timestamp
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> createBackupConfig = new HashMap<>();
        createBackupConfig.put("sourceTableId", "users");
        createBackupConfig.put("backupId", "users_backup");
        createBackupConfig.put("expireTime", "2026-08-31T00:00:00Z");
        config.put("CreateBackup", createBackupConfig);

        // When: creating an action
        Action<?> action = ActionFactory.getAction(config, mockContext);

        // Then: expireTime should be set
        assertNotNull(action);
        assertTrue(action instanceof CreateBackup);

        CreateBackup createBackup = (CreateBackup) action;
        assertEquals(createBackup.getExpireTime(), "2026-08-31T00:00:00Z");
    }

    @Test
    public void testCreateBackupWithBackupType() {
        // Given: configuration with backupType
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> createBackupConfig = new HashMap<>();
        createBackupConfig.put("sourceTableId", "users");
        createBackupConfig.put("backupId", "users_backup_hot");
        createBackupConfig.put("expireTime", "7d");
        createBackupConfig.put("clusterId", "my-cluster");
        createBackupConfig.put("backupType", "HOT");
        config.put("CreateBackup", createBackupConfig);

        // When: creating an action
        Action<?> action = ActionFactory.getAction(config, mockContext);

        // Then: backupType should be set
        assertNotNull(action);
        assertTrue(action instanceof CreateBackup);

        CreateBackup createBackup = (CreateBackup) action;
        assertEquals(createBackup.getBackupType(), "HOT");
    }

    @Test
    public void testCreateBackupWithStandardBackupType() {
        // Given: configuration with STANDARD backupType
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> createBackupConfig = new HashMap<>();
        createBackupConfig.put("sourceTableId", "users");
        createBackupConfig.put("backupId", "users_backup_standard");
        createBackupConfig.put("expireTime", "30d");
        createBackupConfig.put("clusterId", "my-cluster");
        createBackupConfig.put("backupType", "STANDARD");
        config.put("CreateBackup", createBackupConfig);

        // When: creating an action
        Action<?> action = ActionFactory.getAction(config, mockContext);

        // Then: backupType should be set to STANDARD
        assertNotNull(action);
        assertTrue(action instanceof CreateBackup);

        CreateBackup createBackup = (CreateBackup) action;
        assertEquals(createBackup.getBackupType(), "STANDARD");
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*sourceTableId is required.*")
    public void testCreateBackupRequiresSourceTableId() {
        // Given: a CreateBackup action without sourceTableId
        CreateBackup createBackup = new CreateBackup();
        createBackup.setSourceTableId(null);
        createBackup.setBackupId("backup");
        createBackup.setExpireTime("30d");
        createBackup.setClusterId("cluster");

        // When/Then: execute should throw exception
        createBackup.execute(null);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*backupId is required.*")
    public void testCreateBackupRequiresBackupId() {
        // Given: a CreateBackup action without backupId
        CreateBackup createBackup = new CreateBackup();
        createBackup.setSourceTableId("users");
        createBackup.setBackupId(null);
        createBackup.setExpireTime("30d");
        createBackup.setClusterId("cluster");

        // When/Then: execute should throw exception
        createBackup.execute(null);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*expireTime is required.*")
    public void testCreateBackupRequiresExpireTime() {
        // Given: a CreateBackup action without expireTime
        CreateBackup createBackup = new CreateBackup();
        createBackup.setSourceTableId("users");
        createBackup.setBackupId("backup");
        createBackup.setExpireTime(null);
        createBackup.setClusterId("cluster");

        // When/Then: execute should throw exception
        createBackup.execute(null);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*clusterId is required.*")
    public void testCreateBackupRequiresClusterId() {
        // Given: a CreateBackup action without clusterId
        CreateBackup createBackup = new CreateBackup();
        createBackup.setSourceTableId("users");
        createBackup.setBackupId("backup");
        createBackup.setExpireTime("30d");
        createBackup.setClusterId(null);

        // When/Then: execute should throw exception
        createBackup.execute(null);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*clusterId is required.*")
    public void testCreateBackupRequiresNonEmptyClusterId() {
        // Given: a CreateBackup action with empty clusterId
        CreateBackup createBackup = new CreateBackup();
        createBackup.setSourceTableId("users");
        createBackup.setBackupId("backup");
        createBackup.setExpireTime("30d");
        createBackup.setClusterId("");

        // When/Then: execute should throw exception
        createBackup.execute(null);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*sourceTableId is required.*")
    public void testCreateBackupRequiresNonEmptySourceTableId() {
        // Given: a CreateBackup action with empty sourceTableId
        CreateBackup createBackup = new CreateBackup();
        createBackup.setSourceTableId("");
        createBackup.setBackupId("backup");
        createBackup.setExpireTime("30d");
        createBackup.setClusterId("cluster");

        // When/Then: execute should throw exception
        createBackup.execute(null);
    }

    @Test
    public void testCreateBackupToString() {
        // Given: a CreateBackup action
        CreateBackup createBackup = new CreateBackup();
        createBackup.setSourceTableId("users");
        createBackup.setBackupId("users_backup");
        createBackup.setExpireTime("30d");
        createBackup.setClusterId("my-cluster");
        createBackup.setBackupType("HOT");

        // When: calling toString
        String result = createBackup.toString();

        // Then: it should contain all fields
        assertTrue(result.contains("users"));
        assertTrue(result.contains("users_backup"));
        assertTrue(result.contains("30d"));
        assertTrue(result.contains("my-cluster"));
        assertTrue(result.contains("HOT"));
        assertTrue(result.contains("CreateBackup"));
    }

    @Test
    public void testCreateBackupGettersSetters() {
        // Given: a CreateBackup action
        CreateBackup createBackup = new CreateBackup();

        // When: setting values
        createBackup.setSourceTableId("users");
        createBackup.setBackupId("backup");
        createBackup.setExpireTime("30d");
        createBackup.setClusterId("cluster");
        createBackup.setBackupType("HOT");

        // Then: getters should return the values
        assertEquals(createBackup.getSourceTableId(), "users");
        assertEquals(createBackup.getBackupId(), "backup");
        assertEquals(createBackup.getExpireTime(), "30d");
        assertEquals(createBackup.getClusterId(), "cluster");
        assertEquals(createBackup.getBackupType(), "HOT");
    }

    @Test
    public void testCreateBackupActionFactoryRegistration() {
        // Given: a YAML-like configuration with CreateBackup
        Map<String, Object> config = new HashMap<>();
        config.put(
                "CreateBackup",
                new HashMap<String, Object>() {
                    {
                        put("sourceTableId", "users");
                        put("backupId", "backup");
                        put("expireTime", "30d");
                        put("clusterId", "cluster");
                    }
                });

        // When: getting the action via ActionFactory
        Action<?> action = ActionFactory.getAction(config, mockContext);

        // Then: should successfully create CreateBackup instance
        assertNotNull(action);
        assertTrue(action instanceof CreateBackup);
    }

    @Test
    public void testCreateBackupMinimalConfig() {
        // Given: minimal valid configuration
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> createBackupConfig = new HashMap<>();
        createBackupConfig.put("sourceTableId", "users");
        createBackupConfig.put("backupId", "backup");
        createBackupConfig.put("expireTime", "7d");
        createBackupConfig.put("clusterId", "cluster");
        config.put("CreateBackup", createBackupConfig);

        // When: creating an action
        Action<?> action = ActionFactory.getAction(config, mockContext);

        // Then: should work with required fields only
        assertNotNull(action);
        assertTrue(action instanceof CreateBackup);

        CreateBackup createBackup = (CreateBackup) action;
        assertEquals(createBackup.getSourceTableId(), "users");
        assertEquals(createBackup.getBackupId(), "backup");
        assertEquals(createBackup.getExpireTime(), "7d");
        assertEquals(createBackup.getClusterId(), "cluster");
    }

    @Test
    public void testCreateBackupExecute() {
        // Given: a valid CreateBackup action
        CreateBackup createBackup = new CreateBackup();
        createBackup.setSourceTableId("users");
        createBackup.setBackupId("users_backup");
        createBackup.setExpireTime("30d");
        createBackup.setClusterId("my-cluster");

        BigtableTableAdminClient mockClient = mock(BigtableTableAdminClient.class);

        // When: executing the action
        createBackup.execute(mockClient);

        // Then: verify createBackup was called with correct content
        ArgumentCaptor<CreateBackupRequest> requestCaptor =
                ArgumentCaptor.forClass(CreateBackupRequest.class);
        verify(mockClient).createBackup(requestCaptor.capture());

        CreateBackupRequest capturedRequest = requestCaptor.getValue();
        // Verify the request contains correct cluster and backup IDs
        assertTrue(
                capturedRequest.toProto("foo", "bar").getParent().contains("my-cluster"),
                "Expected clusterId 'my-cluster' in request");
        assertEquals(
                capturedRequest.toProto("foo", "bar").getBackupId(),
                "users_backup",
                "Expected backupId 'users_backup'");
        assertTrue(
                capturedRequest
                        .toProto("foo", "bar")
                        .getBackup()
                        .getSourceTable()
                        .contains("users"),
                "Expected sourceTableId 'users' in request");
        assertTrue(
                capturedRequest.toProto("foo", "bar").getBackup().hasExpireTime(),
                "Expected expireTime to be set");
    }
}
