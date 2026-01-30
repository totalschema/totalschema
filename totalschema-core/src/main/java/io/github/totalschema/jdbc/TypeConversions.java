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

package io.github.totalschema.jdbc;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

public final class TypeConversions {

    private static final TypeConversions TYPE_CONVERSIONS =
            new TypeConversions(ZoneId.systemDefault());

    private final ZoneId zoneId;

    public static TypeConversions getInstance() {
        return TYPE_CONVERSIONS;
    }

    TypeConversions(ZoneId zoneId) {
        this.zoneId = zoneId;
    }

    public Timestamp toUtcTimestamp(ZonedDateTime zonedDateTime) {

        Timestamp timestamp;

        if (zonedDateTime != null) {

            Instant instant = zonedDateTime.toInstant();

            /*
            truncated precision to milliseconds: it should be quite enough
            and some database have issues with nanosecond precision provided by ZonedDateTime
             */
            Instant truncatedInstant = instant.truncatedTo(ChronoUnit.MILLIS);

            timestamp = Timestamp.from(truncatedInstant);

        } else {
            timestamp = null;
        }

        return timestamp;
    }

    public ZonedDateTime toZonedDateTime(Timestamp timestamp) {

        ZonedDateTime zonedDateTime;

        if (timestamp != null) {
            zonedDateTime = timestamp.toInstant().atZone(zoneId);

        } else {
            zonedDateTime = null;
        }

        return zonedDateTime;
    }
}
