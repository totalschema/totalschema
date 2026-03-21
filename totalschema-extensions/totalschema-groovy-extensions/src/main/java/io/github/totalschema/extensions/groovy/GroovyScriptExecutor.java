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
import io.github.totalschema.engine.core.command.api.CommandContext;
import io.github.totalschema.jdbc.ConnectionAction;
import io.github.totalschema.jdbc.JdbcDatabase;
import io.github.totalschema.spi.script.ScriptExecutor;
import java.sql.Connection;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class GroovyScriptExecutor implements ScriptExecutor {

    private final Logger log = LoggerFactory.getLogger(GroovyScriptExecutor.class);

    private final String name;

    private final JdbcDatabase jdbcDatabase;
    private final Configuration configuration;

    /**
     * Constructs a GroovyScriptExecutor with the provided dependencies.
     *
     * @param name The name of the connector
     * @param configuration Configuration for the script executor
     * @param jdbcDatabase The JDBC database connection (injected via IoC container)
     */
    GroovyScriptExecutor(String name, Configuration configuration, JdbcDatabase jdbcDatabase) {

        this.name = name;
        this.jdbcDatabase = jdbcDatabase;
        this.configuration = configuration;
    }

    @Override
    public void execute(String groovyScript, CommandContext context) throws InterruptedException {

        log.info("[{}] database: Initializing Groovy Script interpreter", name);

        try {
            jdbcDatabase.withConnection(
                    new ConnectionAction<Void>() {
                        @Override
                        public Void execute(Connection connection) {

                            executeGroovyScript(groovyScript, connection, context);

                            return null;
                        }
                    });

        } catch (SQLException | RuntimeException ex) {
            throw new RuntimeException(
                    "Failure executing Groovy script against the database: " + name, ex);

        } catch (NoClassDefFoundError groovyUseError) {

            throw new RuntimeException(
                    "Failure initializing Groovy Script interpreter: "
                            + "is the Groovy dependency / JAR missing or incorrect?",
                    groovyUseError);
        }
    }

    private void executeGroovyScript(
            String groovyScript, Connection connection, CommandContext context) {

        Binding binding = new Binding();

        try (Sql sql = new Sql(connection)) {
            binding.setProperty("sql", sql);
            binding.setProperty("configuration", configuration);

            if (context.has(Environment.class)) {
                Environment environment = context.get(Environment.class);

                binding.setProperty("environment", environment);
            }

            GroovyShell shell = new GroovyShell(null, binding);
            shell.evaluate(groovyScript);
        }
    }

    @Override
    public String toString() {
        return "GroovyScriptExecutor{"
                + "name='"
                + name
                + '\''
                + ", jdbcDatabase="
                + jdbcDatabase
                + ", configuration="
                + configuration
                + '}';
    }
}
