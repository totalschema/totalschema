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
import io.github.totalschema.spi.change.executor.ObjectBindingChangeExecutor;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class GroovyChangeExecutor extends ObjectBindingChangeExecutor {

    private static final Logger logger = LoggerFactory.getLogger(GroovyChangeExecutor.class);

    private final boolean injectSql;

    GroovyChangeExecutor(Configuration configuration, Context context) {
        super(context);

        injectSql = configuration.getBoolean("injectSql").orElse(true);
    }

    @Override
    protected void execute(InputStream inputStream, Context context, Map<String, Object> bindings)
            throws InterruptedException {

        String script = readAsString(inputStream);

        // Create mutable copy to add Groovy-specific bindings
        Map<String, Object> groovyBindings = new HashMap<>(bindings);

        DataSource dataSource = context.getOptional(DataSource.class).orElse(null);

        if (injectSql && dataSource != null) {
            try (Sql sql = new Sql(dataSource)) {
                groovyBindings.put("sql", sql);
                executeGroovyScript(groovyBindings, script);
            }
        } else {
            // No database connection - execute script without sql binding
            executeGroovyScript(groovyBindings, script);
        }
    }

    private void executeGroovyScript(Map<String, Object> bindings, String groovyScript) {

        logger.info("Injecting bindings to Groovy script: {}", bindings.keySet());

        GroovyShell groovyShell = new GroovyShell(null, new Binding(bindings));
        groovyShell.evaluate(groovyScript);
    }

    @Override
    public String toString() {
        return "GroovyChangeExecutor{" + "injectSql=" + injectSql + '}';
    }
}
