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

package io.github.totalschema.extensions.groovy;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.sql.Sql;
import io.github.totalschema.config.Configuration;
import io.github.totalschema.engine.api.Context;
import io.github.totalschema.spi.script.AbstractJdbcScriptExecutor;
import java.sql.Connection;
import java.util.Map;

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
 */
final class GroovyScriptExecutor extends AbstractJdbcScriptExecutor {

    /**
     * @param configuration Configuration for the script executor (injected into Groovy binding)
     */
    GroovyScriptExecutor(Configuration configuration) {
        super("Groovy", configuration);
    }

    @Override
    protected void executeScriptWithConnection(
            String groovyScript, Connection connection, Context context) {

        // Create base bindings (configuration, environment)
        Map<String, Object> baseBindings = createBaseBindings(context);

        // Create Groovy binding and populate with base bindings
        Binding binding = new Binding();
        for (Map.Entry<String, Object> entry : baseBindings.entrySet()) {
            binding.setProperty(entry.getKey(), entry.getValue());
        }

        // Add Groovy-specific bindings
        try (Sql sql = new Sql(connection)) {
            binding.setProperty("sql", sql);
            new GroovyShell(null, binding).evaluate(groovyScript);
        }
    }
}
