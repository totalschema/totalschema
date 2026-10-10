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

import static org.easymock.EasyMock.*;
import static org.testng.Assert.*;

import io.github.totalschema.engine.internal.changefile.ChangeFileFactory;
import io.github.totalschema.model.ChangeFile;
import io.github.totalschema.model.ChangeType;
import io.github.totalschema.model.StateRecord;
import io.github.totalschema.spi.database.sql.ResultRow;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import org.testng.annotations.Test;

public class StateRecordMapperTest {

    @Test
    public void testMapResultRowMapsAllColumns() throws SQLException {
        // Given
        ChangeFileFactory changeFileFactory = createMock(ChangeFileFactory.class);
        ChangeFile.Id changeFileId =
                new ChangeFile.Id(
                        "1.X/1.0.0", "01", "create_table", null, ChangeType.APPLY, "h2", "sql");

        expect(changeFileFactory.getIdFromString("1.X/1.0.0/01.create_table.apply.h2.sql"))
                .andReturn(changeFileId);

        ResultRow resultRow = createMock(ResultRow.class);
        expect(resultRow.getString("change_file_id"))
                .andReturn("1.X/1.0.0/01.create_table.apply.h2.sql");
        expect(resultRow.getString("file_hash")).andReturn("abc123");

        ZonedDateTime applyTimestamp = ZonedDateTime.now(ZoneOffset.UTC);
        expect(resultRow.getTimestamp("apply_timestamp"))
                .andReturn(Timestamp.from(applyTimestamp.toInstant()));
        expect(resultRow.getString("applied_by")).andReturn("test-user");

        replay(changeFileFactory, resultRow);

        StateRecordMapper mapper = new StateRecordMapper(changeFileFactory);

        // When
        StateRecord stateRecord = mapper.mapResultRow(resultRow);

        // Then
        assertEquals(stateRecord.getChangeFileId(), changeFileId);
        assertEquals(stateRecord.getFileHash(), "abc123");
        assertEquals(stateRecord.getApplyTimeStamp().toInstant(), applyTimestamp.toInstant());
        assertEquals(stateRecord.getAppliedBy(), "test-user");

        verify(changeFileFactory, resultRow);
    }

    @Test
    public void testMapResultRowWithNullTimestampAndNullValues() throws SQLException {
        // Given
        ChangeFileFactory changeFileFactory = createMock(ChangeFileFactory.class);
        expect(changeFileFactory.getIdFromString(null)).andReturn(null);

        ResultRow resultRow = createMock(ResultRow.class);
        expect(resultRow.getString("change_file_id")).andReturn(null);
        expect(resultRow.getString("file_hash")).andReturn(null);
        expect(resultRow.getTimestamp("apply_timestamp")).andReturn(null);
        expect(resultRow.getString("applied_by")).andReturn(null);

        replay(changeFileFactory, resultRow);

        StateRecordMapper mapper = new StateRecordMapper(changeFileFactory);

        // When
        StateRecord stateRecord = mapper.mapResultRow(resultRow);

        // Then
        assertNull(stateRecord.getChangeFileId());
        assertNull(stateRecord.getFileHash());
        assertNull(stateRecord.getApplyTimeStamp());
        assertNull(stateRecord.getAppliedBy());

        verify(changeFileFactory, resultRow);
    }
}
