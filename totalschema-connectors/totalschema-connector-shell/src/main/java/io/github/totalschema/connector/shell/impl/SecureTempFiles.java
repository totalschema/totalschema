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

package io.github.totalschema.connector.shell.impl;

import static java.nio.file.attribute.PosixFilePermission.*;

import io.github.totalschema.os.OperatingSystemInfo;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

/**
 * Utility class for creating temporary files with restrictive, owner-only permissions.
 *
 * <p>On POSIX systems (Linux, macOS) permissions are applied atomically at creation time via {@link
 * java.nio.file.attribute.PosixFilePermissions}. On Windows, the equivalent owner-only flags are
 * set immediately after creation using {@link File} permission setters.
 */
final class SecureTempFiles {

    private static final Set<PosixFilePermission> OWNER_ONLY_POSIX_PERMISSIONS =
            Set.of(OWNER_READ, OWNER_WRITE, OWNER_EXECUTE);

    private SecureTempFiles() {
        throw new AssertionError("utility class: not instantiable");
    }

    /**
     * Creates a temporary file whose permissions are restricted to the owning user only (read,
     * write, and execute; no group or others access).
     *
     * <p>The file is created in the default temporary-file directory. Callers are responsible for
     * deleting the file when it is no longer needed.
     *
     * @param prefix the prefix string used in generating the file's name; may be empty
     * @param suffix the suffix string used in generating the file's name (e.g. {@code ".sh"}); may
     *     be empty
     * @return the path to the newly created temporary file
     * @throws IOException if the file could not be created
     * @throws IllegalStateException if owner-only permissions could not be applied (Windows only)
     */
    static Path createSecureTempFile(String prefix, String suffix) throws IOException {

        Path tempFilePath;

        if (OperatingSystemInfo.isWindows()) {

            tempFilePath = Files.createTempFile(prefix, suffix);
            File file = tempFilePath.toFile();

            if (!file.setReadable(true, true)) {
                throw new IllegalStateException(
                        "Failed to set owner-only read permission on temporary file: "
                                + tempFilePath);
            }

            if (!file.setWritable(true, true)) {
                throw new IllegalStateException(
                        "Failed to set owner-only write permission on temporary file: "
                                + tempFilePath);
            }

            if (!file.setExecutable(true, true)) {
                throw new IllegalStateException(
                        "Failed to set owner-only execute permission on temporary file: "
                                + tempFilePath);
            }
        } else {

            FileAttribute<Set<PosixFilePermission>> fileAttribute =
                    PosixFilePermissions.asFileAttribute(OWNER_ONLY_POSIX_PERMISSIONS);

            tempFilePath = Files.createTempFile(prefix, suffix, fileAttribute);
        }

        return tempFilePath;
    }
}
