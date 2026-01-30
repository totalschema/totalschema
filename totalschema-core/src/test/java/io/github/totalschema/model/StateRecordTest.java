/*
 * totalschema: tool for managing database versioning and schema changes with ease.
 * Copyright (C) 2025 totalschema development team
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

package io.github.totalschema.model;

import static org.testng.Assert.*;

import java.time.ZonedDateTime;
import org.testng.annotations.Test;

public class StateRecordTest {

    @Test
    public void testStateRecordConstruction() {
        StateRecord record = new StateRecord();

        assertNotNull(record);
        assertNull(record.getChangeFileId());
        assertNull(record.getFileHash());
        assertNull(record.getApplyTimeStamp());
        assertNull(record.getAppliedBy());
    }

    @Test
    public void testSetAndGetChangeFileId() {
        StateRecord record = new StateRecord();
        ChangeFile.Id id =
                new ChangeFile.Id("1.X", "001", "test", "DEV", ChangeType.APPLY, "jdbc", "sql");

        record.setChangeFileId(id);
        assertEquals(record.getChangeFileId(), id);
    }

    @Test
    public void testSetAndGetFileHash() {
        StateRecord record = new StateRecord();
        String hash = "abc123def456";

        record.setFileHash(hash);
        assertEquals(record.getFileHash(), hash);
    }

    @Test
    public void testSetAndGetApplyTimeStamp() {
        StateRecord record = new StateRecord();
        ZonedDateTime now = ZonedDateTime.now();

        record.setApplyTimeStamp(now);
        assertEquals(record.getApplyTimeStamp(), now);
    }

    @Test
    public void testSetAndGetAppliedBy() {
        StateRecord record = new StateRecord();
        String user = "testuser";

        record.setAppliedBy(user);
        assertEquals(record.getAppliedBy(), user);
    }

    @Test
    public void testEqualsWithSameValues() {
        ChangeFile.Id id =
                new ChangeFile.Id("1.X", "001", "test", "DEV", ChangeType.APPLY, "jdbc", "sql");
        ZonedDateTime timestamp = ZonedDateTime.now();

        StateRecord record1 = new StateRecord();
        record1.setChangeFileId(id);
        record1.setFileHash("hash123");
        record1.setApplyTimeStamp(timestamp);
        record1.setAppliedBy("user1");

        StateRecord record2 = new StateRecord();
        record2.setChangeFileId(id);
        record2.setFileHash("hash123");
        record2.setApplyTimeStamp(timestamp);
        record2.setAppliedBy("user1");

        assertEquals(record1, record2);
    }

    @Test
    public void testEqualsWithDifferentValues() {
        StateRecord record1 = new StateRecord();
        record1.setFileHash("hash1");

        StateRecord record2 = new StateRecord();
        record2.setFileHash("hash2");

        assertNotEquals(record1, record2);
    }

    @Test
    public void testEqualsWithSameInstance() {
        StateRecord record = new StateRecord();
        assertEquals(record, record);
    }

    @Test
    public void testEqualsWithNull() {
        StateRecord record = new StateRecord();
        assertNotEquals(record, null);
    }

    @Test
    public void testHashCodeConsistency() {
        ChangeFile.Id id =
                new ChangeFile.Id("1.X", "001", "test", "DEV", ChangeType.APPLY, "jdbc", "sql");

        StateRecord record1 = new StateRecord();
        record1.setChangeFileId(id);
        record1.setFileHash("hash123");

        StateRecord record2 = new StateRecord();
        record2.setChangeFileId(id);
        record2.setFileHash("hash123");

        assertEquals(record1.hashCode(), record2.hashCode());
    }

    @Test
    public void testToString() {
        StateRecord record = new StateRecord();
        record.setFileHash("hash123");
        record.setAppliedBy("testuser");

        String result = record.toString();

        assertNotNull(result);
        assertTrue(result.contains("StateRecord"));
        assertTrue(result.contains("hash123"));
        assertTrue(result.contains("testuser"));
    }
}
