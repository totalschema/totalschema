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

public class LockRecordTest {

    @Test
    public void testLockRecordConstruction() {
        LockRecord record = new LockRecord();

        assertNotNull(record);
        assertNull(record.getLockId());
        assertNull(record.getLockExpiration());
        assertNull(record.getLockedByUserId());
    }

    @Test
    public void testSetAndGetLockId() {
        LockRecord record = new LockRecord();
        String lockId = "lock-123";

        record.setLockId(lockId);
        assertEquals(record.getLockId(), lockId);
    }

    @Test
    public void testSetAndGetLockExpiration() {
        LockRecord record = new LockRecord();
        ZonedDateTime expiration = ZonedDateTime.now().plusHours(1);

        record.setLockExpiration(expiration);
        assertEquals(record.getLockExpiration(), expiration);
    }

    @Test
    public void testSetAndGetLockedByUserId() {
        LockRecord record = new LockRecord();
        String userId = "user123";

        record.setLockedByUserId(userId);
        assertEquals(record.getLockedByUserId(), userId);
    }

    @Test
    public void testEqualsWithSameValues() {
        ZonedDateTime expiration = ZonedDateTime.now();

        LockRecord record1 = new LockRecord();
        record1.setLockId("lock-1");
        record1.setLockExpiration(expiration);
        record1.setLockedByUserId("user1");

        LockRecord record2 = new LockRecord();
        record2.setLockId("lock-1");
        record2.setLockExpiration(expiration);
        record2.setLockedByUserId("user1");

        assertEquals(record1, record2);
    }

    @Test
    public void testEqualsWithDifferentValues() {
        LockRecord record1 = new LockRecord();
        record1.setLockId("lock-1");

        LockRecord record2 = new LockRecord();
        record2.setLockId("lock-2");

        assertNotEquals(record1, record2);
    }

    @Test
    public void testEqualsWithSameInstance() {
        LockRecord record = new LockRecord();
        assertEquals(record, record);
    }

    @Test
    public void testEqualsWithNull() {
        LockRecord record = new LockRecord();
        assertNotEquals(record, null);
    }

    @Test
    public void testHashCodeConsistency() {
        ZonedDateTime expiration = ZonedDateTime.now();

        LockRecord record1 = new LockRecord();
        record1.setLockId("lock-1");
        record1.setLockExpiration(expiration);

        LockRecord record2 = new LockRecord();
        record2.setLockId("lock-1");
        record2.setLockExpiration(expiration);

        assertEquals(record1.hashCode(), record2.hashCode());
    }

    @Test
    public void testToString() {
        LockRecord record = new LockRecord();
        record.setLockId("lock-123");
        record.setLockedByUserId("testuser");

        String result = record.toString();

        assertNotNull(result);
        assertTrue(result.contains("LockRecord"));
        assertTrue(result.contains("lock-123"));
        assertTrue(result.contains("testuser"));
    }
}
