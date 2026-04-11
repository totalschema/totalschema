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

package io.github.totalschema.extensions.groovy;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.sql.Sql;
import io.github.totalschema.config.Configuration;
import io.github.totalschema.config.environment.Environment;
import io.github.totalschema.engine.api.Context;
import io.github.totalschema.jdbc.JdbcDatabase;
import io.github.totalschema.spi.script.ScriptExecutor;
import java.sql.Connection;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Executes Groovy scripts against a JDBC database.
 *
 * <p>The following bindings are injected into every script:
 *
 * <ul>
 *   <li>{@code sql} ({@link groovy.sql.Sql}) — open database connection
 *   <li>{@code configuration} ({@link io.github.totalschema.config.Configuration}) — connector
 *       configuration
 *   <li>{@code environment} ({@link io.github.totalschema.config.environment.Environment}) —
 *       current environment, if present in the context
 * </ul>
 *
 * <p>No variable substitution is applied — Groovy's own {@code ${...}} GString syntax handles
 * dynamic values at the language level.
 *
 * <p>The {@link io.github.totalschema.jdbc.JdbcDatabase} is retrieved from the {@link
 * io.github.totalschema.engine.api.Context} at execution time; it is placed there by {@link
 * io.github.totalschema.connector.jdbc.JdbcConnector} before this executor is invoked.
 */
final class GroovyScriptExecutor implements ScriptExecutor {

    private final Logger log = LoggerFactory.getLogger(GroovyScriptExecutor.class);

    private final Configuration configuration;

    /**
     * @param configuration Configuration for the script executor (injected into Groovy binding)
     */
    GroovyScriptExecutor(Configuration configuration) {

        this.configuration = configuration;
    }

    @Override
    public void execute(String groovyScript, Context context) throws InterruptedException {

        log.info("Initializing Groovy Script interpreter");

        JdbcDatabase jdbcDatabase = context.get(JdbcDatabase.class);

        try {
            jdbcDatabase.withConnection(
                    connection -> {
                        executeGroovyScript(groovyScript, connection, context);
                        return null;
                    });

        } catch (SQLException | RuntimeException ex) {
            throw new RuntimeException("Failure executing Groovy script", ex);

        } catch (NoClassDefFoundError groovyUseError) {

            throw new RuntimeException(
                    "Failure initializing Groovy Script interpreter: "
                            + "is the Groovy dependency / JAR missing or incorrect?",
                    groovyUseError);
        }
    }

    private void executeGroovyScript(String groovyScript, Connection connection, Context context) {

        Binding binding = new Binding();
        binding.setProperty("configuration", configuration);

        if (context.has(Environment.class)) {
            binding.setProperty("environment", context.get(Environment.class));
        }

        try (Sql sql = new Sql(connection)) {
            binding.setProperty("sql", sql);
            new GroovyShell(null, binding).evaluate(groovyScript);
        }
    }

    @Override
    public String toString() {
        return "GroovyScriptExecutor{configuration=" + configuration + '}';
    }
}
