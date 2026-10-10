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

/**
 * Column name constants for the state table, shared between SQL statement construction ({@link
 * SqlDatabaseStateRecordRepository}) and result row mapping ({@link StateRecordMapper}).
 */
final class StateTableColumns {

    private StateTableColumns() {
        throw new AssertionError("static constant class, no instances allowed");
    }

    static final String CHANGE_FILE_ID = "change_file_id";
    static final String FILE_HASH = "file_hash";
    static final String APPLY_TIMESTAMP = "apply_timestamp";
    static final String APPLIED_BY = "applied_by";
}
