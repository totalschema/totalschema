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

package io.github.totalschema.spi.state;

import io.github.totalschema.model.ChangeFile;
import io.github.totalschema.model.StateRecord;
import java.io.Closeable;
import java.util.List;
import java.util.Set;

/**
 * Service Provider Interface for persisting change execution state.
 *
 * <p>Implementations store and retrieve records of executed changes.
 */
public interface StateRepository extends Closeable {
    /**
     * Saves a state record for an executed change.
     *
     * @param stateRecord the state record to save
     */
    void saveStateRecord(StateRecord stateRecord);

    /**
     * Retrieves all state records.
     *
     * @return a list of all state records
     */
    List<StateRecord> getAllStateRecords();

    /**
     * Deletes state records by their change file IDs.
     *
     * @param changeFileMetadata the set of change file IDs to delete
     * @return the number of records deleted
     */
    int deleteStateRecordByIds(Set<ChangeFile.Id> changeFileMetadata);
}
