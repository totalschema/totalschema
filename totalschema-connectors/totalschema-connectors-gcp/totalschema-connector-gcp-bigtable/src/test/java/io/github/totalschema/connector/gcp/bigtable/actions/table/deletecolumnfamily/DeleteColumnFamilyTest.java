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

package io.github.totalschema.connector.gcp.bigtable.actions.table.deletecolumnfamily;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.*;

import com.google.cloud.bigtable.admin.v2.BigtableTableAdminClient;
import com.google.cloud.bigtable.admin.v2.models.ModifyColumnFamiliesRequest;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.Test;

/** Tests for {@link DeleteColumnFamily} action. */
public class DeleteColumnFamilyTest {

    @Test
    public void testDeleteColumnFamilyGettersSetters() {
        // Given: a DeleteColumnFamily action
        DeleteColumnFamily action = new DeleteColumnFamily();

        // When: setting properties
        action.setTableId("users");
        action.setFamilyName("old_analytics");

        // Then: getters should return the values
        assertEquals(action.getTableId(), "users");
        assertEquals(action.getFamilyName(), "old_analytics");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testDeleteColumnFamilyMissingTableId() {
        // Given: a DeleteColumnFamily action without tableId
        DeleteColumnFamily action = new DeleteColumnFamily();
        action.setFamilyName("analytics");

        // When/Then: should throw IllegalArgumentException
        action.execute(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testDeleteColumnFamilyMissingFamilyName() {
        // Given: a DeleteColumnFamily action without familyName
        DeleteColumnFamily action = new DeleteColumnFamily();
        action.setTableId("users");

        // When/Then: should throw IllegalArgumentException
        action.execute(null);
    }

    @Test
    public void testDeleteColumnFamilyToString() {
        // Given: a DeleteColumnFamily action
        DeleteColumnFamily action = new DeleteColumnFamily();
        action.setTableId("users");
        action.setFamilyName("old_analytics");

        // When/Then: toString should work
        String str = action.toString();
        assertTrue(str.contains("DeleteColumnFamily"));
        assertTrue(str.contains("users"));
        assertTrue(str.contains("old_analytics"));
    }

    @Test
    public void testDeleteColumnFamilyExecute() {
        // Given: a valid DeleteColumnFamily action
        DeleteColumnFamily action = new DeleteColumnFamily();
        action.setTableId("users");
        action.setFamilyName("old_analytics");

        BigtableTableAdminClient mockClient = mock(BigtableTableAdminClient.class);

        // When: executing the action
        action.execute(mockClient);

        // Then: verify modifyFamilies was called with correct content
        ArgumentCaptor<ModifyColumnFamiliesRequest> requestCaptor =
                ArgumentCaptor.forClass(ModifyColumnFamiliesRequest.class);
        verify(mockClient).modifyFamilies(requestCaptor.capture());

        ModifyColumnFamiliesRequest capturedRequest = requestCaptor.getValue();
        // Verify the request contains the correct table ID
        assertTrue(
                capturedRequest.toProto("foo", "bar").getName().contains("users"),
                "Expected tableId 'users' in request");
        // Verify the family is being dropped
        assertTrue(
                capturedRequest.toProto("foo", "bar").getModificationsList().stream()
                        .anyMatch(mod -> mod.hasDrop() && mod.getId().equals("old_analytics")),
                "Expected family 'old_analytics' to be dropped");
    }
}
