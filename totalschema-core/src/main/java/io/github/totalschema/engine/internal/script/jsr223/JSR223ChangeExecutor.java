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

package io.github.totalschema.engine.internal.script.jsr223;

import io.github.totalschema.config.Configuration;
import io.github.totalschema.config.environment.Environment;
import io.github.totalschema.engine.api.Context;
import io.github.totalschema.spi.change.executor.ObjectBindingChangeExecutor;
import io.github.totalschema.spi.script.ScriptExecutor;
import java.io.InputStream;
import java.util.Map;
import javax.script.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link ScriptExecutor} that delegates script evaluation to a JSR-223 compatible {@link
 * ScriptEngine}.
 *
 * <p>One instance is created per supported file extension (e.g. {@code js}, {@code py}). The
 * underlying engine is resolved at construction time from the classpath via {@link
 * ScriptEngineManager}. At execution time the following objects are bound into the script's {@link
 * Bindings} when present in the supplied {@link Context}:
 *
 * <ul>
 *   <li>{@code configuration} — the active {@link Configuration}
 *   <li>{@code connection} — the active JDBC {@link java.sql.Connection}
 *   <li>{@code environment} — the active {@link Environment}
 * </ul>
 *
 * <p>Instances should be obtained via {@link JSR223ChangeExecutorFactory#createFactories()} rather
 * than constructed directly.
 */
public class JSR223ChangeExecutor extends ObjectBindingChangeExecutor {

    private static final Logger log = LoggerFactory.getLogger(JSR223ChangeExecutor.class);

    /** The file extension (without leading dot) this executor handles, e.g. {@code "js"}. */
    private final String extension;

    /** The JSR-223 engine used to evaluate scripts. */
    private final ScriptEngine scriptEngine;

    /**
     * Creates a new {@link JSR223ChangeExecutor} for the given file extension.
     *
     * <p>The corresponding {@link ScriptEngine} is resolved from the current thread's context class
     * loader via {@link ScriptEngineManager#getEngineByExtension(String)}.
     *
     * @param extension the file extension to associate with this executor (e.g. {@code "js"}); must
     *     not be {@code null}
     * @return a new {@link JSR223ChangeExecutor} bound to the resolved engine
     * @throws IllegalStateException if the engine cannot be loaded for the given extension
     */
    static JSR223ChangeExecutor newInstance(String extension, Context context) {

        try {
            ScriptEngineManager manager =
                    new ScriptEngineManager(Thread.currentThread().getContextClassLoader());

            ScriptEngine scriptEngine = manager.getEngineByExtension(extension);

            log.debug("Created ScriptEngine for extension: {}", extension);

            return new JSR223ChangeExecutor(extension, scriptEngine, context);

        } catch (RuntimeException ex) {
            throw new IllegalStateException("Failed to load JSR223 engine: " + extension, ex);
        }
    }

    private JSR223ChangeExecutor(String extension, ScriptEngine scriptEngine, Context context) {
        super(context);
        this.extension = extension;
        this.scriptEngine = scriptEngine;
    }

    /**
     * Returns the file extension handled by this executor.
     *
     * @return the file extension (without a leading dot), e.g. {@code "js"}; never {@code null}
     */
    public String getExtension() {
        return extension;
    }

    /**
     * Evaluates the supplied script using the underlying JSR-223 engine.
     *
     * <p>The provided bindings are passed directly to the JSR-223 engine for script execution.
     *
     * @param inputStream stream containing the script source code; must not be {@code null}
     * @param context the dependency injection context for accessing engine services; must not be
     *     {@code null}
     * @param bindings named objects to expose as variables in the script execution environment;
     *     must not be {@code null}
     * @throws InterruptedException if the current thread is interrupted during execution
     * @throws RuntimeException if the script raises a {@link ScriptException}
     */
    @Override
    protected void execute(InputStream inputStream, Context context, Map<String, Object> bindings)
            throws InterruptedException {

        String script = readAsString(inputStream);

        try {
            Bindings engineBindings = scriptEngine.createBindings();
            engineBindings.putAll(bindings);

            scriptEngine.eval(script, engineBindings);

        } catch (ScriptException ex) {
            throw new RuntimeException("Failure evaluating script", ex);
        }
    }

    @Override
    public String toString() {
        return "JSR223ChangeExecutor{" + "extension='" + extension + '\'' + '}';
    }
}
