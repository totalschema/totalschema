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

import java.nio.file.Path;

/**
 * Represents an apply change file that contains SQL or script statements to be executed.
 *
 * <p>Apply files are executed in order based on their identifier to apply schema changes.
 */
public final class ApplyFile extends ChangeFile {

    /**
     * Constructs an ApplyFile instance.
     *
     * @param changesDirectory the base changes directory
     * @param file the path to the change file
     * @param id the unique identifier for this change file
     */
    public ApplyFile(Path changesDirectory, Path file, Id id) {
        super(changesDirectory, file, id);
    }
}
