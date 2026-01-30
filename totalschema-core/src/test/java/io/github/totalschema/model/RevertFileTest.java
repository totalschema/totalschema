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

package io.github.totalschema.model;

import static org.testng.Assert.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.testng.annotations.Test;

public class RevertFileTest {

    @Test
    public void testRevertFileCreation() {
        Path changesDir = Paths.get("/changes");
        Path file = Paths.get("/changes/1.X/001.test.DEV.revert.jdbc.sql");
        ChangeFile.Id id =
                new ChangeFile.Id("1.X", "001", "test", "DEV", ChangeType.REVERT, "jdbc", "sql");

        RevertFile revertFile = new RevertFile(changesDir, file, id);

        assertNotNull(revertFile);
        assertEquals(revertFile.getFile(), file);
        assertEquals(revertFile.getId(), id);
    }

    @Test
    public void testRevertFileWithNoEnvironment() {
        Path changesDir = Paths.get("/changes");
        Path file = Paths.get("/changes/1.X/001.test.revert.jdbc.sql");
        ChangeFile.Id id =
                new ChangeFile.Id("1.X", "001", "test", null, ChangeType.REVERT, "jdbc", "sql");

        RevertFile revertFile = new RevertFile(changesDir, file, id);

        assertNotNull(revertFile);
        assertFalse(revertFile.getEnvironment().isPresent());
    }

    @Test
    public void testRevertFileInheritsFromChangeFile() {
        Path changesDir = Paths.get("/changes");
        Path file = Paths.get("/changes/2.X/042.rollback.PROD.revert.shell.sh");
        ChangeFile.Id id =
                new ChangeFile.Id(
                        "2.X", "042", "rollback", "PROD", ChangeType.REVERT, "shell", "sh");

        RevertFile revertFile = new RevertFile(changesDir, file, id);

        assertEquals(revertFile.getConnector(), "shell");
        assertEquals(revertFile.getEnvironment().orElse(null), "PROD");
    }
}
