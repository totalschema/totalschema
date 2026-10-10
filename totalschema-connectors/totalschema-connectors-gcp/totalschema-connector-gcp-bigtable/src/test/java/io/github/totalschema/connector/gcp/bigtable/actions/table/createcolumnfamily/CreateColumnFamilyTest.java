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

package io.github.totalschema.connector.gcp.bigtable.actions.table.createcolumnfamily;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.*;

import com.google.cloud.bigtable.admin.v2.BigtableTableAdminClient;
import com.google.cloud.bigtable.admin.v2.models.ModifyColumnFamiliesRequest;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.Test;

/** Tests for {@link CreateColumnFamily} action. */
public class CreateColumnFamilyTest {

    @Test
    public void testCreateColumnFamilyGettersSetters() {
        // Given: a CreateColumnFamily action
        CreateColumnFamily action = new CreateColumnFamily();

        // When: setting properties
        action.setTableId("users");
        action.setFamilyName("analytics");
        action.setMaxVersions(5);
        action.setMaxAge("24h");

        // Then: getters should return the values
        assertEquals(action.getTableId(), "users");
        assertEquals(action.getFamilyName(), "analytics");
        assertEquals(action.getMaxVersions(), Integer.valueOf(5));
        assertEquals(action.getMaxAge(), "24h");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testCreateColumnFamilyMissingTableId() {
        // Given: a CreateColumnFamily action without tableId
        CreateColumnFamily action = new CreateColumnFamily();
        action.setFamilyName("analytics");

        // When/Then: should throw IllegalArgumentException
        action.execute(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testCreateColumnFamilyMissingFamilyName() {
        // Given: a CreateColumnFamily action without familyName
        CreateColumnFamily action = new CreateColumnFamily();
        action.setTableId("users");

        // When/Then: should throw IllegalArgumentException
        action.execute(null);
    }

    @Test
    public void testCreateColumnFamilyToString() {
        // Given: a CreateColumnFamily action
        CreateColumnFamily action = new CreateColumnFamily();
        action.setTableId("users");
        action.setFamilyName("analytics");
        action.setMaxVersions(5);

        // When/Then: toString should work
        String str = action.toString();
        assertTrue(str.contains("CreateColumnFamily"));
        assertTrue(str.contains("users"));
        assertTrue(str.contains("analytics"));
    }

    @Test
    public void testCreateColumnFamilyExecuteWithoutGcRules() {
        // Given: a CreateColumnFamily action without GC rules
        CreateColumnFamily action = new CreateColumnFamily();
        action.setTableId("users");
        action.setFamilyName("profile");

        BigtableTableAdminClient mockClient = mock(BigtableTableAdminClient.class);

        // When: executing the action
        action.execute(mockClient);

        // Then: verify modifyFamilies was called with correct tableId
        ArgumentCaptor<ModifyColumnFamiliesRequest> requestCaptor =
                ArgumentCaptor.forClass(ModifyColumnFamiliesRequest.class);
        verify(mockClient).modifyFamilies(requestCaptor.capture());

        ModifyColumnFamiliesRequest capturedRequest = requestCaptor.getValue();
        // Verify the request contains the correct table ID
        assertTrue(
                capturedRequest.toProto("foo", "bar").getName().contains("users"),
                "Expected tableId 'users' in request");
        // Verify the family is being added
        assertTrue(
                capturedRequest.toProto("foo", "bar").getModificationsList().stream()
                        .anyMatch(mod -> mod.hasCreate() && mod.getId().equals("profile")),
                "Expected family 'profile' to be created");
    }

    @Test
    public void testCreateColumnFamilyExecuteWithMaxVersions() {
        // Given: a CreateColumnFamily action with maxVersions
        CreateColumnFamily action = new CreateColumnFamily();
        action.setTableId("users");
        action.setFamilyName("profile");
        action.setMaxVersions(3);

        BigtableTableAdminClient mockClient = mock(BigtableTableAdminClient.class);

        // When: executing the action
        action.execute(mockClient);

        // Then: verify modifyFamilies was called with correct content
        ArgumentCaptor<ModifyColumnFamiliesRequest> requestCaptor =
                ArgumentCaptor.forClass(ModifyColumnFamiliesRequest.class);
        verify(mockClient).modifyFamilies(requestCaptor.capture());

        ModifyColumnFamiliesRequest capturedRequest = requestCaptor.getValue();
        assertTrue(
                capturedRequest.toProto("foo", "bar").getName().contains("users"),
                "Expected tableId 'users' in request");
        assertTrue(
                capturedRequest.toProto("foo", "bar").getModificationsList().stream()
                        .anyMatch(
                                mod ->
                                        mod.hasCreate()
                                                && mod.getId().equals("profile")
                                                && mod.getCreate().hasGcRule()),
                "Expected family 'profile' with GC rule to be created");
    }

    @Test
    public void testCreateColumnFamilyExecuteWithMaxAge() {
        // Given: a CreateColumnFamily action with maxAge
        CreateColumnFamily action = new CreateColumnFamily();
        action.setTableId("users");
        action.setFamilyName("cache");
        action.setMaxAge("24h");

        BigtableTableAdminClient mockClient = mock(BigtableTableAdminClient.class);

        // When: executing the action
        action.execute(mockClient);

        // Then: verify modifyFamilies was called with correct content
        ArgumentCaptor<ModifyColumnFamiliesRequest> requestCaptor =
                ArgumentCaptor.forClass(ModifyColumnFamiliesRequest.class);
        verify(mockClient).modifyFamilies(requestCaptor.capture());

        ModifyColumnFamiliesRequest capturedRequest = requestCaptor.getValue();
        assertTrue(
                capturedRequest.toProto("foo", "bar").getName().contains("users"),
                "Expected tableId 'users' in request");
        assertTrue(
                capturedRequest.toProto("foo", "bar").getModificationsList().stream()
                        .anyMatch(
                                mod ->
                                        mod.hasCreate()
                                                && mod.getId().equals("cache")
                                                && mod.getCreate().hasGcRule()),
                "Expected family 'cache' with GC rule to be created");
    }

    @Test
    public void testCreateColumnFamilyExecuteWithBothRules() {
        // Given: a CreateColumnFamily action with both maxVersions and maxAge
        CreateColumnFamily action = new CreateColumnFamily();
        action.setTableId("users");
        action.setFamilyName("audit");
        action.setMaxVersions(100);
        action.setMaxAge("365d");

        BigtableTableAdminClient mockClient = mock(BigtableTableAdminClient.class);

        // When: executing the action
        action.execute(mockClient);

        // Then: verify modifyFamilies was called with correct content
        ArgumentCaptor<ModifyColumnFamiliesRequest> requestCaptor =
                ArgumentCaptor.forClass(ModifyColumnFamiliesRequest.class);
        verify(mockClient).modifyFamilies(requestCaptor.capture());

        ModifyColumnFamiliesRequest capturedRequest = requestCaptor.getValue();
        assertTrue(
                capturedRequest.toProto("foo", "bar").getName().contains("users"),
                "Expected tableId 'users' in request");
        assertTrue(
                capturedRequest.toProto("foo", "bar").getModificationsList().stream()
                        .anyMatch(
                                mod ->
                                        mod.hasCreate()
                                                && mod.getId().equals("audit")
                                                && mod.getCreate().hasGcRule()),
                "Expected family 'audit' with GC rule to be created");
    }
}
