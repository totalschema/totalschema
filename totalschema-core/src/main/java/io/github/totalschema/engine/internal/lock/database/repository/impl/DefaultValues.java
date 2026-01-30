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

package io.github.totalschema.engine.internal.lock.database.repository.impl;

import static io.github.totalschema.ProjectConventions.PROJECT_SYSTEM_NAME;

final class DefaultValues {

    static final String LOCK_TABLE_NAME = String.format("%s_lock_v1", PROJECT_SYSTEM_NAME);

    static final String CREATE_TABLE_SQL =
            "CREATE TABLE %s ("
                    + "lock_id VARCHAR(255), "
                    + "lock_expiration TIMESTAMP, "
                    + "locked_by VARCHAR(255))";

    static final String DROP_TABLE_SQL = "DROP TABLE %s";

    static final String INSERT_RECORD_SQL =
            "INSERT INTO %s ("
                    + "lock_id, "
                    + "lock_expiration, "
                    + "locked_by "
                    + ") VALUES (NULL, NULL, NULL)";

    static final String QUERY_RECORD_SQL =
            "SELECT " + "lock_id, " + "lock_expiration, " + "locked_by " + "FROM %s ";

    static final String ACQUIRE_LOCK_SQL =
            "UPDATE %s SET "
                    + "lock_id = ?, "
                    + "lock_expiration = ?, "
                    + "locked_by = ? "
                    + "WHERE lock_id IS NULL OR lock_expiration < ?";

    static final String RENEW_LOCK_SQL = "UPDATE %s SET lock_expiration = ? WHERE lock_id = ?";

    static final String RELEASE_LOCK_SQL = "UPDATE %s SET lock_id = NULL WHERE lock_id = ?";
}
