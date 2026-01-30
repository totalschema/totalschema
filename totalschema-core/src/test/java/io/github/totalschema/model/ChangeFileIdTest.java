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

package io.github.totalschema.model;

import static org.testng.Assert.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import org.testng.annotations.Test;

public class ChangeFileIdTest {

    @Test
    public void testIdConstruction() {
        ChangeFile.Id id =
                new ChangeFile.Id(
                        "1.X", "001", "create_table", "DEV", ChangeType.APPLY, "jdbc", "sql");

        assertNotNull(id);
        assertEquals(id.getParentDirectory(), "1.X");
        assertEquals(id.getOrder(), 1);
        assertEquals(id.getDescription(), "create_table");
        assertEquals(id.getEnvironment(), Optional.of("DEV"));
        assertEquals(id.getChangeType(), ChangeType.APPLY);
        assertEquals(id.getConnector(), "jdbc");
        assertEquals(id.getExtension(), "sql");
    }

    @Test
    public void testIdWithoutEnvironment() {
        ChangeFile.Id id =
                new ChangeFile.Id(
                        "2.X", "005", "add_column", null, ChangeType.APPLY_ALWAYS, "jdbc", "sql");

        assertEquals(id.getEnvironment(), Optional.empty());
    }

    @Test
    public void testGetOrderParsingFromString() {
        ChangeFile.Id id =
                new ChangeFile.Id(
                        "1.X", "042", "description", null, ChangeType.APPLY, "jdbc", "sql");

        assertEquals(id.getOrder(), 42);
    }

    @Test
    public void testGetOrderCalledMultipleTimes() {
        ChangeFile.Id id =
                new ChangeFile.Id(
                        "1.X", "123", "description", null, ChangeType.APPLY, "jdbc", "sql");

        // First call should parse
        assertEquals(id.getOrder(), 123);
        // Subsequent calls should use cached value
        assertEquals(id.getOrder(), 123);
        assertEquals(id.getOrder(), 123);
    }

    @Test
    public void testToStringRepresentationWithEnvironment() {
        ChangeFile.Id id =
                new ChangeFile.Id(
                        "1.X", "001", "create_table", "DEV", ChangeType.APPLY, "jdbc", "sql");

        String expected = "1.X/001.create_table.DEV.apply.jdbc.sql";
        assertEquals(id.toStringRepresentation(), expected);
    }

    @Test
    public void testToStringRepresentationWithoutEnvironment() {
        ChangeFile.Id id =
                new ChangeFile.Id(
                        "2.X", "005", "add_column", null, ChangeType.APPLY_ALWAYS, "jdbc", "sql");

        String expected = "2.X/005.add_column.apply_always.jdbc.sql";
        assertEquals(id.toStringRepresentation(), expected);
    }

    @Test
    public void testEqualsWithSameValues() {
        ChangeFile.Id id1 =
                new ChangeFile.Id("1.X", "001", "desc", "DEV", ChangeType.APPLY, "jdbc", "sql");
        ChangeFile.Id id2 =
                new ChangeFile.Id("1.X", "001", "desc", "DEV", ChangeType.APPLY, "jdbc", "sql");

        assertEquals(id1, id2);
    }

    @Test
    public void testEqualsWithDifferentValues() {
        ChangeFile.Id id1 =
                new ChangeFile.Id("1.X", "001", "desc", "DEV", ChangeType.APPLY, "jdbc", "sql");
        ChangeFile.Id id2 =
                new ChangeFile.Id("1.X", "002", "desc", "DEV", ChangeType.APPLY, "jdbc", "sql");

        assertNotEquals(id1, id2);
    }

    @Test
    public void testEqualsWithSameInstance() {
        ChangeFile.Id id =
                new ChangeFile.Id("1.X", "001", "desc", "DEV", ChangeType.APPLY, "jdbc", "sql");

        assertEquals(id, id);
    }

    @Test
    public void testEqualsWithNull() {
        ChangeFile.Id id =
                new ChangeFile.Id("1.X", "001", "desc", "DEV", ChangeType.APPLY, "jdbc", "sql");

        assertNotEquals(id, null);
    }

    @Test
    public void testHashCodeConsistency() {
        ChangeFile.Id id1 =
                new ChangeFile.Id("1.X", "001", "desc", "DEV", ChangeType.APPLY, "jdbc", "sql");
        ChangeFile.Id id2 =
                new ChangeFile.Id("1.X", "001", "desc", "DEV", ChangeType.APPLY, "jdbc", "sql");

        assertEquals(id1.hashCode(), id2.hashCode());
    }

    @Test
    public void testApplyFileCreation() {
        Path changesDir = Paths.get("/path/to/changes");
        Path file = Paths.get("/path/to/changes/1.X/001.test.apply.jdbc.sql");
        ChangeFile.Id id =
                new ChangeFile.Id("1.X", "001", "test", null, ChangeType.APPLY, "jdbc", "sql");

        ApplyFile applyFile = new ApplyFile(changesDir, file, id);

        assertNotNull(applyFile);
        assertEquals(applyFile.getId(), id);
    }

    @Test
    public void testRevertFileCreation() {
        Path changesDir = Paths.get("/path/to/changes");
        Path file = Paths.get("/path/to/changes/1.X/001.test.revert.jdbc.sql");
        ChangeFile.Id id =
                new ChangeFile.Id("1.X", "001", "test", null, ChangeType.REVERT, "jdbc", "sql");

        RevertFile revertFile = new RevertFile(changesDir, file, id);

        assertNotNull(revertFile);
        assertEquals(revertFile.getId(), id);
    }
}
