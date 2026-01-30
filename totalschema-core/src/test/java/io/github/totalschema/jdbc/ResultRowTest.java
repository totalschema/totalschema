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

import static org.easymock.EasyMock.*;
import static org.testng.Assert.*;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ResultRowTest {

    private ResultSet mockResultSet;
    private ResultSetMetaData mockMetaData;

    @BeforeMethod
    public void setUp() {
        mockResultSet = createMock(ResultSet.class);
        mockMetaData = createMock(ResultSetMetaData.class);
    }

    @Test
    public void testGetStringWithValidColumn() throws SQLException {
        expect(mockResultSet.getMetaData()).andReturn(mockMetaData);
        expect(mockMetaData.getColumnCount()).andReturn(2);
        expect(mockMetaData.getColumnLabel(1)).andReturn("NAME");
        expect(mockMetaData.getColumnLabel(2)).andReturn("VALUE");
        expect(mockResultSet.getString(1)).andReturn("test");

        replay(mockResultSet, mockMetaData);

        ResultRow row = new ResultRow(mockResultSet);
        String result = row.getString("NAME");

        assertEquals(result, "test");
        verify(mockResultSet, mockMetaData);
    }

    @Test
    public void testGetStringWithCaseInsensitiveColumn() throws SQLException {
        expect(mockResultSet.getMetaData()).andReturn(mockMetaData);
        expect(mockMetaData.getColumnCount()).andReturn(1);
        expect(mockMetaData.getColumnLabel(1)).andReturn("MyColumn");
        expect(mockResultSet.getString(1)).andReturn("value");

        replay(mockResultSet, mockMetaData);

        ResultRow row = new ResultRow(mockResultSet);
        // Should work regardless of case
        String result = row.getString("mycolumn");

        assertEquals(result, "value");
        verify(mockResultSet, mockMetaData);
    }

    @Test
    public void testGetTimestampWithValidColumn() throws SQLException {
        Timestamp expectedTimestamp = new Timestamp(System.currentTimeMillis());

        expect(mockResultSet.getMetaData()).andReturn(mockMetaData);
        expect(mockMetaData.getColumnCount()).andReturn(1);
        expect(mockMetaData.getColumnLabel(1)).andReturn("CREATED_AT");
        expect(mockResultSet.getTimestamp(eq(1), anyObject(Calendar.class)))
                .andReturn(expectedTimestamp);

        replay(mockResultSet, mockMetaData);

        ResultRow row = new ResultRow(mockResultSet);
        Timestamp result = row.getTimestamp("CREATED_AT");

        assertEquals(result, expectedTimestamp);
        verify(mockResultSet, mockMetaData);
    }

    @Test(
            expectedExceptions = IllegalStateException.class,
            expectedExceptionsMessageRegExp = ".*unknown in this ResultRow.*")
    public void testGetStringWithInvalidColumn() throws SQLException {
        expect(mockResultSet.getMetaData()).andReturn(mockMetaData);
        expect(mockMetaData.getColumnCount()).andReturn(1);
        expect(mockMetaData.getColumnLabel(1)).andReturn("VALID_COLUMN");

        replay(mockResultSet, mockMetaData);

        ResultRow row = new ResultRow(mockResultSet);
        row.getString("INVALID_COLUMN");

        verify(mockResultSet, mockMetaData);
    }

    @Test(
            expectedExceptions = IllegalStateException.class,
            expectedExceptionsMessageRegExp = ".*unknown in this ResultRow.*")
    public void testGetTimestampWithInvalidColumn() throws SQLException {
        expect(mockResultSet.getMetaData()).andReturn(mockMetaData);
        expect(mockMetaData.getColumnCount()).andReturn(1);
        expect(mockMetaData.getColumnLabel(1)).andReturn("VALID_COLUMN");

        replay(mockResultSet, mockMetaData);

        ResultRow row = new ResultRow(mockResultSet);
        row.getTimestamp("INVALID_COLUMN");

        verify(mockResultSet, mockMetaData);
    }

    @Test
    public void testConstructorWithColumnNameFallback() throws SQLException {
        expect(mockResultSet.getMetaData()).andReturn(mockMetaData);
        expect(mockMetaData.getColumnCount()).andReturn(1);
        expect(mockMetaData.getColumnLabel(1)).andReturn(null);
        expect(mockMetaData.getColumnName(1)).andReturn("FALLBACK_NAME");
        expect(mockResultSet.getString(1)).andReturn("value");

        replay(mockResultSet, mockMetaData);

        ResultRow row = new ResultRow(mockResultSet);
        String result = row.getString("FALLBACK_NAME");

        assertEquals(result, "value");
        verify(mockResultSet, mockMetaData);
    }

    @Test(
            expectedExceptions = IllegalStateException.class,
            expectedExceptionsMessageRegExp = ".*Could not map column index.*")
    public void testConstructorWithNullColumnNameAndLabel() throws SQLException {
        expect(mockResultSet.getMetaData()).andReturn(mockMetaData);
        expect(mockMetaData.getColumnCount()).andReturn(1);
        expect(mockMetaData.getColumnLabel(1)).andReturn(null);
        expect(mockMetaData.getColumnName(1)).andReturn(null);

        replay(mockResultSet, mockMetaData);

        new ResultRow(mockResultSet);

        verify(mockResultSet, mockMetaData);
    }

    @Test
    public void testMultipleColumns() throws SQLException {
        expect(mockResultSet.getMetaData()).andReturn(mockMetaData);
        expect(mockMetaData.getColumnCount()).andReturn(3);
        expect(mockMetaData.getColumnLabel(1)).andReturn("ID");
        expect(mockMetaData.getColumnLabel(2)).andReturn("NAME");
        expect(mockMetaData.getColumnLabel(3)).andReturn("EMAIL");
        expect(mockResultSet.getString(1)).andReturn("123");
        expect(mockResultSet.getString(2)).andReturn("John");
        expect(mockResultSet.getString(3)).andReturn("john@example.com");

        replay(mockResultSet, mockMetaData);

        ResultRow row = new ResultRow(mockResultSet);

        assertEquals(row.getString("ID"), "123");
        assertEquals(row.getString("NAME"), "John");
        assertEquals(row.getString("EMAIL"), "john@example.com");

        verify(mockResultSet, mockMetaData);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testConstructorWithNullResultSet() throws SQLException {
        new ResultRow(null);
    }

    @Test
    public void testCaseInsensitiveColumnAccess() throws SQLException {
        expect(mockResultSet.getMetaData()).andReturn(mockMetaData);
        expect(mockMetaData.getColumnCount()).andReturn(1);
        expect(mockMetaData.getColumnLabel(1)).andReturn("MixedCase");
        expect(mockResultSet.getString(1)).andReturn("value1");
        expect(mockResultSet.getString(1)).andReturn("value2");
        expect(mockResultSet.getString(1)).andReturn("value3");

        replay(mockResultSet, mockMetaData);

        ResultRow row = new ResultRow(mockResultSet);

        // All these should access the same column
        assertEquals(row.getString("MixedCase"), "value1");
        assertEquals(row.getString("mixedcase"), "value2");
        assertEquals(row.getString("MIXEDCASE"), "value3");

        verify(mockResultSet, mockMetaData);
    }
}
