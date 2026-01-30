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

package io.github.totalschema;

import java.nio.file.FileSystems;

public final class ProjectConventions {

    private ProjectConventions() {
        throw new AssertionError("static utility class, no instances allowed");
    }

    public static final String PROJECT_SYSTEM_NAME = "totalschema";

    public static final String YML_CONFIG_FILE = String.format("%s.yml", PROJECT_SYSTEM_NAME);

    public static final String CHANGE_DIRECTORY_PATH =
            String.format(
                    "%s%s%s",
                    PROJECT_SYSTEM_NAME, FileSystems.getDefault().getSeparator(), "changes");

    public static final class ConfigurationPropertyNames {

        private ConfigurationPropertyNames() {
            throw new AssertionError("static utility class, no instances allowed");
        }

        public static final String ENVIRONMENTS = "environments";

        public static final String VARIABLES = "variables";
    }
}
