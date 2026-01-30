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

package io.github.totalschema.engine.internal.shell;

import io.github.totalschema.engine.internal.shell.direct.TerminalSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

/**
 * Abstract base class for terminal session implementations.
 *
 * @param <C> the type of connection this session manages
 */
public abstract class AbstractTerminalSession<C> implements TerminalSession<C> {

    /** Shared executor service for reading terminal output streams. */
    protected static final ExecutorService executorService = Executors.newCachedThreadPool();

    /**
     * Submits a task to read from an input stream and consume the output.
     *
     * @param inputStream the input stream to read from
     * @param consumer the consumer for each line of output
     * @return a Future representing the reader task
     */
    protected Future<?> submitReaderTask(InputStream inputStream, Consumer<String> consumer) {
        return executorService.submit(new StreamReaderTask(inputStream, consumer));
    }

    private static final class StreamReaderTask implements Runnable {

        private final InputStream is;
        private final Consumer<String> consumer;

        private StreamReaderTask(InputStream is, Consumer<String> consumer) {
            this.is = is;
            this.consumer = consumer;
        }

        @Override
        public void run() {
            try (BufferedReader bufferedReader =
                    new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    consumer.accept(line);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
