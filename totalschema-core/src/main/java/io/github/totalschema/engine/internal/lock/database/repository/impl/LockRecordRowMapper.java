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

package io.github.totalschema.engine.internal.lock.database.repository.impl;

import io.github.totalschema.jdbc.TypeConversions;
import io.github.totalschema.model.LockRecord;
import io.github.totalschema.spi.database.sql.ResultRow;
import io.github.totalschema.spi.database.sql.RowMapper;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZonedDateTime;

/**
 * Maps a single result row from the lock table into a {@link LockRecord}.
 *
 * <p>Extracted from {@link DefaultLockStateRepository} to isolate the row-to-object mapping logic
 * from the query execution and error handling concerns.
 */
final class LockRecordRowMapper implements RowMapper<LockRecord> {

    static final LockRecordRowMapper INSTANCE = new LockRecordRowMapper();

    private LockRecordRowMapper() {}

    @Override
    public LockRecord mapResultRow(ResultRow resultRow) throws SQLException {

        LockRecord lockRecord = new LockRecord();

        lockRecord.setLockId(resultRow.getString(LockTableColumns.LOCK_ID));

        Timestamp expirationTimestamp = resultRow.getTimestamp(LockTableColumns.LOCK_EXPIRATION);

        ZonedDateTime expirationZonedDateTime =
                TypeConversions.getInstance().toZonedDateTime(expirationTimestamp);

        lockRecord.setLockExpiration(expirationZonedDateTime);

        lockRecord.setLockedByUserId(resultRow.getString(LockTableColumns.LOCKED_BY));

        return lockRecord;
    }
}
