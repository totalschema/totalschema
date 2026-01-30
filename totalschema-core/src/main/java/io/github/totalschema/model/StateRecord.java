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

public class StateRecord {

    private ChangeFile.Id id;

    private String fileHash;

    private ZonedDateTime applyTimeStamp;

    private String appliedBy;

    public ChangeFile.Id getChangeFileId() {
        return id;
    }

    public void setChangeFileId(ChangeFile.Id id) {
        this.id = id;
    }

    public String getFileHash() {
        return fileHash;
    }

    public void setFileHash(String fileHash) {
        this.fileHash = fileHash;
    }

    public ZonedDateTime getApplyTimeStamp() {
        return applyTimeStamp;
    }

    public void setApplyTimeStamp(ZonedDateTime applyTimeStamp) {
        this.applyTimeStamp = applyTimeStamp;
    }

    public String getAppliedBy() {
        return appliedBy;
    }

    public void setAppliedBy(String appliedBy) {
        this.appliedBy = appliedBy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StateRecord that = (StateRecord) o;
        return Objects.equals(id, that.id)
                && Objects.equals(fileHash, that.fileHash)
                && Objects.equals(applyTimeStamp, that.applyTimeStamp)
                && Objects.equals(appliedBy, that.appliedBy);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, fileHash, applyTimeStamp, appliedBy);
    }

    @Override
    public String toString() {
        return "StateRecord{"
                + "id="
                + id
                + ", fileHash='"
                + fileHash
                + '\''
                + ", applyTimeStamp="
                + applyTimeStamp
                + ", appliedByUserId='"
                + appliedBy
                + '\''
                + '}';
    }
}
