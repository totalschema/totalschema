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

package io.github.totalschema.engine.internal.storage;

import io.github.totalschema.model.ChangeFile;
import io.github.totalschema.spi.storage.StorageService;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * A mock StorageService that allows specifying a String content for ChangeFiles. Instead of writing
 * temporary files, this test utility allows easy mocking of file contents.
 */
public final class MockStorageService implements StorageService {

    private final Map<ChangeFile, String> storageObjects;

    public static MockStorageService of(ChangeFile cf1, String content1) {
        return new MockStorageService(Map.of(cf1, content1));
    }

    public static MockStorageService of(
            ChangeFile cf1, String content1, ChangeFile cf2, String content2) {
        return new MockStorageService(Map.of(cf1, content1, cf2, content2));
    }

    public static MockStorageService of(
            ChangeFile cf1,
            String content1,
            ChangeFile cf2,
            String content2,
            ChangeFile cf3,
            String content3) {
        return new MockStorageService(Map.of(cf1, content1, cf2, content2, cf3, content3));
    }

    public MockStorageService(Map<ChangeFile, String> storageObjects) {
        this.storageObjects = storageObjects;
    }

    @Override
    public InputStream openInputStream(ChangeFile changeFile) throws IOException {
        String content = storageObjects.get(changeFile);
        if (content == null) {
            throw new IOException("ChangeFile not found: " + changeFile);
        }
        return new java.io.ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }
}
