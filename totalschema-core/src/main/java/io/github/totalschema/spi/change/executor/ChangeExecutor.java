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

package io.github.totalschema.spi.change.executor;

import io.github.totalschema.engine.api.Context;
import io.github.totalschema.model.ChangeFile;
import io.github.totalschema.spi.storage.StorageService;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Base class for all change file executors.
 *
 * <p>A ChangeExecutor is a component that encapsulates the logic for executing one particular type
 * of change file. For example, an executor might handle .sql files, .py files, or other change file
 * formats.
 *
 * <p><strong>Direct subclasses:</strong>
 *
 * <ul>
 *   <li>{@link SimpleChangeExecutor} - For simple text substitution-based executors
 *   <li>{@link ObjectBindingChangeExecutor} - For script executors that inject runtime objects
 * </ul>
 *
 * <p>Provides shared file reading functionality through a Template Method pattern: concrete
 * executors receive an {@link InputStream} for flexible content reading (with control over
 * encoding, buffering, etc.), while the base class handles file opening and resource cleanup.
 */
public abstract class ChangeExecutor {

    protected final Context context;

    protected ChangeExecutor(Context context) {
        this.context = Objects.requireNonNull(context, "context must not be null");
    }

    /**
     * Opens the change file as an InputStream.
     *
     * <p>Checks for thread interruption before returning the stream.
     *
     * @param changeFile the change file to read
     * @return an InputStream for reading the file content
     * @throws IOException if file cannot be read
     * @throws InterruptedException if thread is interrupted
     */
    public final InputStream openChangeFileStream(ChangeFile changeFile)
            throws IOException, InterruptedException {

        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException();
        }

        StorageService storageService = context.get(StorageService.class);

        return storageService.openInputStream(changeFile);
    }

    /**
     * Reads the entire content of an InputStream as a UTF-8 encoded string.
     *
     * <p>This utility method is provided for executors that need to read the entire script content
     * into memory as a string before execution. For large files or streaming scenarios, executors
     * should read from the InputStream directly.
     *
     * @param inputStream the input stream to read from
     * @return the content as a UTF-8 string
     * @throws RuntimeException if reading from the stream fails
     */
    protected String readAsString(InputStream inputStream) {
        try {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read script content", e);
        }
    }
}
