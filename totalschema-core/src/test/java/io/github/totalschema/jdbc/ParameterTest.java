/*
 * totalschema: tool for managing database versioning and schema changes with ease.
 * Copyright (C) 2026 totalschema development team
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

import static org.easymock.EasyMock.*;
import static org.testng.Assert.*;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ParameterTest {

    private PreparedStatement preparedStatement;

    @BeforeMethod
    public void setUp() {
        preparedStatement = createMock(PreparedStatement.class);
    }

    @Test
    public void testStringParameterCreation() {
        Parameter.StringParameter param = Parameter.string("test-value");

        assertNotNull(param);
        assertEquals(param.getValue(), "test-value");
    }

    @Test
    public void testStringParameterSetValue() throws SQLException {
        Parameter.StringParameter param = Parameter.string("test-value");

        preparedStatement.setString(1, "test-value");
        expectLastCall();
        replay(preparedStatement);

        param.setValue(preparedStatement, 1);

        verify(preparedStatement);
    }

    @Test
    public void testStringParameterToString() {
        Parameter.StringParameter param = Parameter.string("test-value");

        assertEquals(param.toString(), "String(test-value)");
    }

    @Test
    public void testStringParameterWithNullValue() throws SQLException {
        Parameter.StringParameter param = Parameter.string(null);

        assertNull(param.getValue());

        preparedStatement.setString(1, null);
        expectLastCall();
        replay(preparedStatement);

        param.setValue(preparedStatement, 1);

        verify(preparedStatement);
    }

    @Test
    public void testTimestampParameterCreation() {
        ZonedDateTime zonedDateTime =
                ZonedDateTime.of(2026, 1, 26, 12, 30, 45, 0, ZoneId.of("UTC"));
        Parameter.TimestampParameter param = Parameter.timestamp(zonedDateTime);

        assertNotNull(param);
        assertNotNull(param.getValue());
        assertTrue(param.getValue() instanceof Timestamp);
    }

    @Test
    public void testTimestampParameterSetValue() throws SQLException {
        ZonedDateTime zonedDateTime =
                ZonedDateTime.of(2026, 1, 26, 12, 30, 45, 0, ZoneId.of("UTC"));
        Parameter.TimestampParameter param = Parameter.timestamp(zonedDateTime);

        preparedStatement.setTimestamp(
                eq(2), anyObject(Timestamp.class), anyObject(Calendar.class));
        expectLastCall();
        replay(preparedStatement);

        param.setValue(preparedStatement, 2);

        verify(preparedStatement);
    }

    @Test
    public void testTimestampParameterToString() {
        ZonedDateTime zonedDateTime =
                ZonedDateTime.of(2026, 1, 26, 12, 30, 45, 0, ZoneId.of("UTC"));
        Parameter.TimestampParameter param = Parameter.timestamp(zonedDateTime);

        String result = param.toString();
        assertTrue(result.startsWith("Timestamp("));
        assertTrue(result.endsWith(")"));
    }
}
