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

package io.github.totalschema.extensions.kotlin;

import io.github.totalschema.config.Configuration;
import io.github.totalschema.engine.api.Context;
import io.github.totalschema.spi.script.AbstractJdbcScriptExecutor;
import java.sql.Connection;
import java.util.Map;
import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
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
final class Jsr223ScriptExecutor extends AbstractJdbcScriptExecutor {

    private static final Logger log = LoggerFactory.getLogger(Jsr223ScriptExecutor.class);

    /**
     * @param configuration Configuration for the script executor
     */
    Jsr223ScriptExecutor(Configuration configuration) {
        super("Kotlin", configuration);
    }

    @Override
    protected void executeScriptWithConnection(
            String kotlinScript, Connection connection, Context context) {

        log.info("Using JSR-223 (stable) Kotlin 1.x script engine");

        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByExtension("kts");

        if (engine == null) {
            throw new RuntimeException(
                    "Kotlin JSR-223 script engine not found. "
                            + "Ensure kotlin-scripting-jsr223 is in user_libs/");
        }

        // Create base bindings (configuration, environment)
        Map<String, Object> bindingsMap = createBaseBindings(context);

        // Add Kotlin-specific bindings
        KotlinSql sql = new KotlinSql(connection);
        bindingsMap.put("sql", sql);
        bindingsMap.put("connection", connection);

        // Create JSR-223 bindings and populate
        Bindings bindings = engine.createBindings();
        bindings.putAll(bindingsMap);

        try {
            engine.eval(kotlinScript, bindings);
        } catch (ScriptException ex) {
            throw new RuntimeException("Error evaluating Kotlin script (JSR-223)", ex);
        }
    }
}
