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

import java.time.ZonedDateTime;
import java.util.Objects;

public class LockRecord {

    private String lockId;
    private ZonedDateTime lockExpiration;

    private String lockedByUserId;

    public String getLockId() {
        return lockId;
    }

    public void setLockId(String lockId) {
        this.lockId = lockId;
    }

    public ZonedDateTime getLockExpiration() {
        return lockExpiration;
    }

    public void setLockExpiration(ZonedDateTime lockExpiration) {
        this.lockExpiration = lockExpiration;
    }

    public String getLockedByUserId() {
        return lockedByUserId;
    }

    public void setLockedByUserId(String lockedByUserId) {
        this.lockedByUserId = lockedByUserId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LockRecord that = (LockRecord) o;
        return Objects.equals(lockId, that.lockId)
                && Objects.equals(lockExpiration, that.lockExpiration)
                && Objects.equals(lockedByUserId, that.lockedByUserId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lockId, lockExpiration, lockedByUserId);
    }

    @Override
    public String toString() {
        return "LockRecord{"
                + "lockId='"
                + lockId
                + '\''
                + ", lockExpiration="
                + lockExpiration
                + ", lockedByUserId='"
                + lockedByUserId
                + '\''
                + '}';
    }
}
