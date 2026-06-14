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

package io.github.totalschema.spi.script;

import io.github.totalschema.config.Configuration;
import io.github.totalschema.config.environment.Environment;
import io.github.totalschema.engine.api.Context;
import io.github.totalschema.jdbc.JdbcDatabase;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for JDBC-based script executors.
 *
 * <p>This class provides the common infrastructure for executing scripts against a JDBC database:
 *
 * <ul>
 *   <li>Retrieves {@link JdbcDatabase} from the {@link Context}
 *   <li>Manages connection lifecycle via {@link JdbcDatabase#withConnection}
 *   <li>Provides consistent error handling for SQL, runtime, and classpath errors
 *   <li>Creates common script bindings ({@code configuration}, {@code environment})
 * </ul>
 *
 * <p>Subclasses provide the script engine name via the constructor and implement engine-specific
 * execution logic by overriding {@link #executeScriptWithConnection(String, Connection, Context)}.
 *
 * <p>The {@link JdbcDatabase} is placed in the context by {@link
 * io.github.totalschema.connector.jdbc.JdbcConnector} before this executor is invoked.
 */
public abstract class AbstractJdbcScriptExecutor implements ScriptExecutor {

    private static final Logger log = LoggerFactory.getLogger(AbstractJdbcScriptExecutor.class);

    private final String scriptEngineName;
    private final Configuration configuration;

    /**
     * Creates a new JDBC script executor.
     *
     * @param scriptEngineName the name of the script engine for logging and error messages (e.g.,
     *     "Groovy", "Kotlin", "JavaScript")
     * @param configuration configuration for the script executor (typically injected into script
     *     bindings)
     */
    protected AbstractJdbcScriptExecutor(String scriptEngineName, Configuration configuration) {
        this.scriptEngineName = scriptEngineName;
        this.configuration = configuration;
    }

    /**
     * Returns the configuration for this script executor.
     *
     * @return the configuration
     */
    protected final Configuration getConfiguration() {
        return configuration;
    }

    /**
     * Returns the name of the script engine for logging and error messages.
     *
     * @return the script engine name
     */
    protected final String getScriptEngineName() {
        return scriptEngineName;
    }

    /**
     * Executes the script with an active database connection.
     *
     * <p>Subclasses implement engine-specific logic here (e.g., creating a Groovy shell or JSR-223
     * script engine, setting up bindings, and evaluating the script).
     *
     * @param script the script content to execute
     * @param connection the active JDBC connection
     * @param context the command context
     */
    protected abstract void executeScriptWithConnection(
            String script, Connection connection, Context context);

    @Override
    public final void execute(String script, Context context) throws InterruptedException {

        log.info("Initializing {} Script interpreter", getScriptEngineName());

        JdbcDatabase jdbcDatabase = context.get(JdbcDatabase.class);

        try {
            jdbcDatabase.withConnection(
                    connection -> {
                        executeScriptWithConnection(script, connection, context);
                        return null;
                    });

        } catch (SQLException | RuntimeException ex) {
            throw new RuntimeException(
                    "Failure executing " + getScriptEngineName() + " script", ex);

        } catch (NoClassDefFoundError error) {
            throw new RuntimeException(
                    "Failure initializing "
                            + getScriptEngineName()
                            + " Script interpreter: "
                            + "is the "
                            + getScriptEngineName()
                            + " dependency / JAR missing or incorrect?",
                    error);
        }
    }

    /**
     * Creates a map with common script bindings.
     *
     * <p>The map includes:
     *
     * <ul>
     *   <li>{@code configuration} — the connector configuration
     *   <li>{@code environment} — the current environment (if present in the context)
     * </ul>
     *
     * <p>Subclasses can add engine-specific bindings (such as {@code sql} or {@code connection}) to
     * this map before passing it to the script engine.
     *
     * @param context the command context
     * @return a mutable map containing common bindings
     */
    protected Map<String, Object> createBaseBindings(Context context) {
        Map<String, Object> bindings = new HashMap<>();
        bindings.put("configuration", configuration);

        if (context.has(Environment.class)) {
            bindings.put("environment", context.get(Environment.class));
        }

        return bindings;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{configuration=" + configuration + '}';
    }
}
