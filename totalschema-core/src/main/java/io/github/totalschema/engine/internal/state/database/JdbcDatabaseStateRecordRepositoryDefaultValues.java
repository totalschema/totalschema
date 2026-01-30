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

package io.github.totalschema.engine.internal.state.database;

import static io.github.totalschema.ProjectConventions.PROJECT_SYSTEM_NAME;

final class JdbcDatabaseStateRecordRepositoryDefaultValues {

    public static final String STATE_TABLE_NAME = String.format("%s_state_v1", PROJECT_SYSTEM_NAME);

    public static final String QUERY_RECORDS_SQL =
            "SELECT "
                    + "change_file_id, "
                    + "file_hash, "
                    + "apply_timestamp, "
                    + "applied_by "
                    + "FROM %s";

    public static final String INSERT_RECORDS_SQL =
            "INSERT INTO %s ("
                    + "change_file_id, "
                    + "file_hash, "
                    + "apply_timestamp, "
                    + "applied_by "
                    + ") VALUES (?, ?, ?, ?)";

    public static final int HASH_COLUMN_LENGTH = 255;
    public static final int APPLIED_BY_COLUMN_LENGTH = 255;
}
