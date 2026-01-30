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

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.TimeZone;

public abstract class Parameter<T> {

    private final T value;

    protected Parameter(T value) {
        this.value = value;
    }

    public T getValue() {
        return value;
    }

    @Override
    public abstract String toString(); // forces custom implementation for each subclass

    public static StringParameter string(String value) {
        return new StringParameter(value);
    }

    public static TimestampParameter timestamp(ZonedDateTime value) {
        return new TimestampParameter(TypeConversions.getInstance().toUtcTimestamp(value));
    }

    public abstract void setValue(PreparedStatement ps, int parameterIndex)
            throws java.sql.SQLException;

    public static final class StringParameter extends Parameter<String> {

        public StringParameter(String value) {
            super(value);
        }

        @Override
        public void setValue(PreparedStatement ps, int parameterIndex) throws SQLException {
            ps.setString(parameterIndex, getValue());
        }

        @Override
        public String toString() {
            return String.format("String(%s)", getValue());
        }
    }

    public static final class TimestampParameter extends Parameter<Timestamp> {

        private static final Calendar UTC_CALENDAR =
                Calendar.getInstance(TimeZone.getTimeZone("UTC"));

        public TimestampParameter(Timestamp value) {
            super(value);
        }

        @Override
        public void setValue(PreparedStatement ps, int parameterIndex) throws SQLException {
            ps.setTimestamp(parameterIndex, getValue(), UTC_CALENDAR);
        }

        @Override
        public String toString() {
            return String.format("Timestamp(%s)", getValue());
        }
    }
}
