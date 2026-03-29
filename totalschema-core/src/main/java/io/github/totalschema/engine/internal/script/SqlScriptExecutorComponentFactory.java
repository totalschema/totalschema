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

package io.github.totalschema.engine.internal.script;

import static io.github.totalschema.spi.ArgumentSpecification.*;

import io.github.totalschema.config.Configuration;
import io.github.totalschema.engine.api.Context;
import io.github.totalschema.jdbc.JdbcDatabase;
import io.github.totalschema.spi.ArgumentSpecification;
import io.github.totalschema.spi.ComponentFactory;
import io.github.totalschema.spi.script.ScriptExecutor;
import java.util.List;

/**
 * ComponentFactory for creating SQL script executors.
 *
 * <p>This factory creates {@link ScriptExecutor} instances with qualifier "sql" that can execute
 * SQL scripts against JDBC databases.
 *
 * <p>Usage: {@code context.get(ScriptExecutor.class, "sql", connectorName, configuration)}
 */
public final class SqlScriptExecutorComponentFactory extends ComponentFactory<ScriptExecutor> {

    private static final ArgumentSpecification<String> NAME_ARGUMENT = string("name");
    private static final ArgumentSpecification<Configuration> CONFIGURATION_ARGUMENT =
            configuration("configuration");

    @Override
    public boolean isLazy() {
        return true; // Created on-demand with arguments
    }

    @Override
    public Class<ScriptExecutor> getComponentType() {
        return ScriptExecutor.class;
    }

    @Override
    public String getQualifier() {
        return "sql";
    }

    @Override
    public List<Class<?>> getRequiredContextTypes() {
        return List.of(JdbcDatabase.class);
    }

    @Override
    public List<ArgumentSpecification<?>> getArgumentSpecifications() {
        return List.of(NAME_ARGUMENT, CONFIGURATION_ARGUMENT);
    }

    @Override
    public ScriptExecutor newComponent(Context context, Object... arguments) {
        String name = getArgument(NAME_ARGUMENT, arguments, 0);
        Configuration configuration = getArgument(CONFIGURATION_ARGUMENT, arguments, 1);

        // Get JdbcDatabase from IoC container - container manages lifecycle
        JdbcDatabase jdbcDatabase = context.get(JdbcDatabase.class, null, name, configuration);

        return new SqlScriptExecutor(configuration, jdbcDatabase);
    }
}
