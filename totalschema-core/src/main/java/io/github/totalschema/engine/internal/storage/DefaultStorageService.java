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

package io.github.totalschema.engine.internal.storage;

import io.github.totalschema.model.ChangeFile;
import io.github.totalschema.spi.storage.StorageService;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class DefaultStorageService implements StorageService {
    @Override
    public InputStream openInputStream(ChangeFile changeFile) throws IOException {
        Objects.requireNonNull(changeFile, "changeFile cannot be null");

        Path file = changeFile.getFile();

        Objects.requireNonNull(file, "file cannot be null");

        return Files.newInputStream(file);
    }
}
