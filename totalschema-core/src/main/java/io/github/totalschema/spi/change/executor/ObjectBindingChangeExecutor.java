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
import java.util.Map;

/**
 * Abstract executor that requires runtime object bindings to be injected into the execution
 * environment.
 *
 * <p>Used for script executors (Groovy, JSR-223, Kotlin) where the connector provides runtime
 * objects (database connections, admin clients) that the user's script can access as variables.
 *
 * <p>Implements the Template Method pattern: the public execute method handles file I/O, while
 * subclasses implement the abstract execute method that receives an InputStream.
 *
 * <p><strong>When to use:</strong>
 *
 * <ul>
 *   <li>Script executors that expose runtime objects as script variables
 *   <li>Executors where user scripts access objects like {@code dataSource}, {@code sql}, {@code
 *       connection}, {@code bigtableClient}, etc.
 *   <li>Any executor that performs runtime object injection (not just text substitution)
 * </ul>
 *
 * <p><strong>When NOT to use:</strong> If your executor only performs text-based variable
 * substitution (e.g., {@code ${varName}}) or doesn't use bindings at all, extend {@link
 * SimpleChangeExecutor} instead.
 *
 * <p><strong>Examples:</strong>
 *
 * <ul>
 *   <li>Groovy scripts with {@code sql} object
 *   <li>JavaScript (JSR-223) with {@code connection} object
 *   <li>Kotlin scripts with {@code dataSource} object
 * </ul>
 */
public abstract class ObjectBindingChangeExecutor extends ChangeExecutor {

    public ObjectBindingChangeExecutor(Context context) {
        super(context);
    }

    /**
     * Executes a change file with runtime object bindings.
     *
     * <p>This method is final to enforce consistent file handling. Subclasses must implement {@link
     * #execute(InputStream, Context, Map)} instead.
     *
     * @param changeFile the change file to execute
     * @param context dependency injection context for accessing engine services
     * @param bindings named objects to expose as variables in script execution (e.g., {@code
     *     "dataSource"}, {@code "bigtableClient"}). The connector is responsible for populating
     *     this map with appropriate objects.
     * @throws InterruptedException if the thread is interrupted during execution
     */
    public final void execute(ChangeFile changeFile, Context context, Map<String, Object> bindings)
            throws InterruptedException {

        try (InputStream inputStream = openChangeFileStream(changeFile)) {

            execute(inputStream, context, bindings);

        } catch (IOException e) {
            throw new RuntimeException(
                    "Failure reading Change content: " + changeFile.getFile(), e);
        }
    }

    /**
     * Executes the change using content from the provided InputStream.
     *
     * <p>Concrete executors implement this method to read from the stream and execute the change
     * with the provided runtime object bindings.
     *
     * <p><strong>Reading strategies:</strong>
     *
     * <ul>
     *   <li>Full read: {@code new String(inputStream.readAllBytes(), StandardCharsets.UTF_8)}
     *   <li>Line-by-line: {@code new BufferedReader(new InputStreamReader(inputStream))}
     *   <li>Binary: read bytes directly from the stream
     * </ul>
     *
     * <p><strong>Using bindings:</strong> Extract objects from the bindings map and inject them
     * into the script environment. For example:
     *
     * <pre>{@code
     * DataSource dataSource = (DataSource) bindings.get("dataSource");
     * scriptBindings.put("dataSource", dataSource);
     * }</pre>
     *
     * @param inputStream stream containing the change file content
     * @param context dependency injection context for accessing engine services
     * @param bindings named objects to inject into the script execution environment
     * @throws InterruptedException if execution is interrupted
     */
    protected abstract void execute(
            InputStream inputStream, Context context, Map<String, Object> bindings)
            throws InterruptedException;
}
