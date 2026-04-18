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

package io.github.totalschema.connector.python;

/**
 * Utility class for runtime operating-system detection.
 *
 * <p>All detection is based on the {@code os.name} system property, evaluated once at class-load
 * time to avoid repeated look-ups.
 */
final class OperatingSystemInfo {

    private static final String OS_NAME =
            System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT);

    /** {@code true} when the JVM is running on a Microsoft Windows host. */
    static final boolean IS_WINDOWS = OS_NAME.contains("win");

    private OperatingSystemInfo() {
        // utility class — not instantiable
    }
}
