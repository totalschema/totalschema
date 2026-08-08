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

package io.github.totalschema.extensions.kotlin;

import io.github.totalschema.config.Configuration;
import io.github.totalschema.engine.api.Context;
import io.github.totalschema.spi.change.executor.ObjectBindingChangeExecutor;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Kotlin script executor using the stable JSR-223 API.
 *
 * <p>This implementation uses the standard JSR-223 (javax.script) API which is stable and
 * compatible with Kotlin 1.x. It requires {@code kotlin-scripting-jsr223} in the classpath.
 *
 * <p>The following bindings are injected into every script:
 *
 * <ul>
 *   <li>{@code sql} ({@link KotlinSql}) — database connection wrapper with query methods
 *   <li>{@code connection} ({@link Connection}) — raw JDBC connection
 *   <li>{@code configuration} ({@link Configuration}) — connector configuration
 *   <li>{@code environment} ({@link io.github.totalschema.config.environment.Environment}) —
 *       current environment, if present in the context
 * </ul>
 *
 * <p>This is the preferred implementation for production use with Kotlin 1.x.
 */
final class Jsr223ChangeExecutor extends ObjectBindingChangeExecutor {

    private static final Logger logger = LoggerFactory.getLogger(Jsr223ChangeExecutor.class);

    private final boolean injectSql;

    /**
     * @param configuration Configuration for the script executor
     */
    Jsr223ChangeExecutor(Configuration configuration, Context context) {
        super(context);

        injectSql = configuration.getBoolean("injectSql").orElse(true);
    }

    @Override
    protected void execute(InputStream inputStream, Context context, Map<String, Object> bindings)
            throws InterruptedException {
        logger.info("Using JSR-223 (stable) Kotlin 1.x script engine");

        String kotlinScript = readAsString(inputStream);

        try {
            ScriptEngineManager manager = new ScriptEngineManager();
            ScriptEngine engine = manager.getEngineByExtension("kts");

            if (engine == null) {
                throw new RuntimeException(
                        "Kotlin JSR-223 script engine not found. "
                                + "Ensure kotlin-scripting-jsr223 is in user_libs/");
            }

            // Create mutable copy to add Kotlin-specific bindings
            Map<String, Object> kotlinBindings = new java.util.HashMap<>(bindings);

            DataSource dataSource = context.getOptional(DataSource.class).orElse(null);

            if (injectSql && dataSource != null) {
                try (Connection connection = dataSource.getConnection()) {

                    KotlinSql sql = new KotlinSql(connection);
                    kotlinBindings.put("sql", sql);
                    kotlinBindings.put("connection", connection);

                    executeKotlinScript(engine, kotlinBindings, kotlinScript);
                }
            } else {
                // No database connection - execute script without sql binding
                executeKotlinScript(engine, kotlinBindings, kotlinScript);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failure acquiring connection", e);
        }
    }

    private void executeKotlinScript(
            ScriptEngine engine, Map<String, Object> bindings, String script) {

        try {
            logger.info("Injecting bindings to Kotlin script: {}", bindings.keySet());

            Bindings engineBindings = engine.createBindings();
            engineBindings.putAll(bindings);

            engine.eval(script, engineBindings);

        } catch (ScriptException ex) {
            throw new RuntimeException("Error evaluating Kotlin script (JSR-223)", ex);
        }
    }
}
