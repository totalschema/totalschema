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
import java.io.IOException;
import java.io.InputStream;

/**
 * Abstract executor that does not require runtime object bindings.
 *
 * <p>Used for SQL executors (with text-based variable substitution), YML executors (declarative),
 * and other non-scripting executors.
 *
 * <p>Implements the Template Method pattern: the public execute method handles file I/O, while
 * subclasses implement the abstract execute method that receives an InputStream.
 *
 * <p><strong>When to use:</strong>
 *
 * <ul>
 *   <li>Executors that only use text-based variable substitution (e.g., {@code ${varName}})
 *   <li>Declarative executors (YAML, JSON, etc.)
 *   <li>Any executor that doesn't inject runtime objects into user-facing scripts
 * </ul>
 *
 * <p><strong>When NOT to use:</strong> If your executor needs to inject runtime objects (database
 * connections, API clients) into the script's variable namespace, extend {@link
 * ObjectBindingChangeExecutor} instead.
 */
public abstract class SimpleChangeExecutor extends ChangeExecutor {

    public SimpleChangeExecutor(Context context) {
        super(context);
    }

    /**
     * Executes a change file without runtime object bindings.
     *
     * <p>This method is final to enforce consistent file handling. Subclasses must implement {@link
     * #execute(InputStream, Context)} instead.
     *
     * @param changeFile the change file to execute
     * @param context dependency injection context for accessing engine services
     * @throws InterruptedException if the thread is interrupted during execution
     */
    public final void execute(ChangeFile changeFile, Context context) throws InterruptedException {
        try (InputStream inputStream = openChangeFileStream(changeFile)) {
            execute(inputStream, context);
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failure reading Change content: " + changeFile.getFile(), e);
        }
    }

    /**
     * Executes the change using content from the provided InputStream.
     *
     * <p>Concrete executors implement this method to read from the stream and execute the change.
     *
     * <p><strong>Reading strategies:</strong>
     *
     * <ul>
     *   <li>Full read: {@code new String(inputStream.readAllBytes(), StandardCharsets.UTF_8)}
     *   <li>Line-by-line: {@code new BufferedReader(new InputStreamReader(inputStream))}
     *   <li>Binary: read bytes directly from the stream
     * </ul>
     *
     * @param inputStream stream containing the change file content
     * @param context dependency injection context for accessing engine services
     * @throws InterruptedException if execution is interrupted
     */
    public abstract void execute(InputStream inputStream, Context context)
            throws InterruptedException;
}
