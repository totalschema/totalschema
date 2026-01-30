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

package io.github.totalschema;

import static org.testng.Assert.*;

import java.nio.file.FileSystems;
import org.testng.annotations.Test;

public class ProjectConventionsTest {

    @Test
    public void testProjectSystemName() {
        assertEquals(ProjectConventions.PROJECT_SYSTEM_NAME, "totalschema");
    }

    @Test
    public void testYmlConfigFile() {
        assertEquals(ProjectConventions.YML_CONFIG_FILE, "totalschema.yml");
    }

    @Test
    public void testChangeDirectoryPath() {
        String expectedPath = "totalschema" + FileSystems.getDefault().getSeparator() + "changes";
        assertEquals(ProjectConventions.CHANGE_DIRECTORY_PATH, expectedPath);
    }

    @Test
    public void testConfigurationPropertyNames() {
        assertEquals(ProjectConventions.ConfigurationPropertyNames.ENVIRONMENTS, "environments");
        assertEquals(ProjectConventions.ConfigurationPropertyNames.VARIABLES, "variables");
    }
}
