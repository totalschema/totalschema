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

package io.github.totalschema.connector.gcp.bigtable.actions.table.resetcolumnfamily;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.*;

import com.google.cloud.bigtable.admin.v2.BigtableTableAdminClient;
import com.google.cloud.bigtable.admin.v2.models.ModifyColumnFamiliesRequest;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.Test;

/** Tests for {@link ResetColumnFamily} action. */
public class ResetColumnFamilyTest {

    @Test
    public void testResetColumnFamilyGettersSetters() {
        // Given: a ResetColumnFamily action
        ResetColumnFamily action = new ResetColumnFamily();

        // When: setting properties
        action.setTableId("users");
        action.setFamilyName("profile");

        // Then: getters should return the values
        assertEquals(action.getTableId(), "users");
        assertEquals(action.getFamilyName(), "profile");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testResetColumnFamilyMissingTableId() {
        // Given: a ResetColumnFamily action without tableId
        ResetColumnFamily action = new ResetColumnFamily();
        action.setFamilyName("profile");

        // When/Then: should throw IllegalArgumentException
        action.execute(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testResetColumnFamilyMissingFamilyName() {
        // Given: a ResetColumnFamily action without familyName
        ResetColumnFamily action = new ResetColumnFamily();
        action.setTableId("users");

        // When/Then: should throw IllegalArgumentException
        action.execute(null);
    }

    @Test
    public void testResetColumnFamilyToString() {
        // Given: a ResetColumnFamily action
        ResetColumnFamily action = new ResetColumnFamily();
        action.setTableId("users");
        action.setFamilyName("profile");

        // When/Then: toString should work
        String str = action.toString();
        assertTrue(str.contains("ResetColumnFamily"));
        assertTrue(str.contains("users"));
        assertTrue(str.contains("profile"));
    }

    @Test
    public void testResetColumnFamilyEmptyTableId() {
        // Given: a ResetColumnFamily action with empty tableId
        ResetColumnFamily action = new ResetColumnFamily();
        action.setTableId("");
        action.setFamilyName("profile");

        // When/Then: should throw IllegalArgumentException
        try {
            action.execute(null);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("tableId"));
        }
    }

    @Test
    public void testResetColumnFamilyEmptyFamilyName() {
        // Given: a ResetColumnFamily action with empty familyName
        ResetColumnFamily action = new ResetColumnFamily();
        action.setTableId("users");
        action.setFamilyName("");

        // When/Then: should throw IllegalArgumentException
        try {
            action.execute(null);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("familyName"));
        }
    }

    @Test
    public void testResetColumnFamilyExecute() {
        // Given: a valid ResetColumnFamily action
        ResetColumnFamily action = new ResetColumnFamily();
        action.setTableId("users");
        action.setFamilyName("profile");

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
                                        mod.hasUpdate()
                                                && mod.getId().equals("profile")
                                                && mod.getUpdate().hasGcRule()),
                "Expected family 'profile' to be reset with max versions rule");
    }
}
