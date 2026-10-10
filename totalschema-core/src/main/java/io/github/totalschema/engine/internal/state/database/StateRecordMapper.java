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

package io.github.totalschema.engine.internal.state.database;

import static io.github.totalschema.engine.internal.state.database.StateTableColumns.*;
import static java.util.Objects.requireNonNull;

import io.github.totalschema.engine.internal.changefile.ChangeFileFactory;
import io.github.totalschema.jdbc.TypeConversions;
import io.github.totalschema.model.StateRecord;
import io.github.totalschema.spi.database.sql.ResultRow;
import io.github.totalschema.spi.database.sql.RowMapper;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZonedDateTime;

/**
 * Maps a single {@link ResultRow} from the state table into a {@link StateRecord} instance.
 *
 * <p>Encapsulates the column-by-column mapping logic (including {@link
 * io.github.totalschema.model.ChangeFile.Id} parsing and timestamp conversion) so that it can be
 * unit-tested independently of the repository and the underlying {@link
 * io.github.totalschema.spi.database.sql.SQLDatabase}.
 *
 * @see SqlDatabaseStateRecordRepository
 */
final class StateRecordMapper implements RowMapper<StateRecord> {

    private final ChangeFileFactory changeFileFactory;

    StateRecordMapper(ChangeFileFactory changeFileFactory) {
        this.changeFileFactory = requireNonNull(changeFileFactory, "changeFileFactory is null");
    }

    @Override
    public StateRecord mapResultRow(ResultRow resultRow) throws SQLException {
        StateRecord stateRecord = new StateRecord();

        stateRecord.setChangeFileId(
                changeFileFactory.getIdFromString(resultRow.getString(CHANGE_FILE_ID)));

        stateRecord.setFileHash(resultRow.getString(FILE_HASH));

        Timestamp applyTimestamp = resultRow.getTimestamp(APPLY_TIMESTAMP);

        ZonedDateTime applyZonedDateTime =
                TypeConversions.getInstance().toZonedDateTime(applyTimestamp);

        stateRecord.setApplyTimeStamp(applyZonedDateTime);

        stateRecord.setAppliedBy(resultRow.getString(APPLIED_BY));

        return stateRecord;
    }
}
