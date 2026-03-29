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

import static io.github.totalschema.spi.factory.ArgumentSpecification.*;

import io.github.totalschema.config.Configuration;
import io.github.totalschema.engine.api.Context;
import io.github.totalschema.jdbc.JdbcDatabase;
import io.github.totalschema.spi.factory.ArgumentHandler;
import io.github.totalschema.spi.factory.ArgumentSpecification;
import io.github.totalschema.spi.factory.ComponentFactory;
import io.github.totalschema.spi.script.ScriptExecutor;
import java.util.List;
import java.util.Optional;

/**
 * ComponentFactory for creating Groovy script executors.
 *
 * <p>This factory creates {@link ScriptExecutor} instances with qualifier "groovy" that can execute
 * Groovy scripts with database connectivity.
 *
 * <p>Usage: {@code context.get(ScriptExecutor.class, "groovy", connectorName, configuration)}
 */
public final class GroovyScriptExecutorComponentFactory extends ComponentFactory<ScriptExecutor> {

    private static final ArgumentSpecification<String> NAME =
            string("name").withConstraint(notBlank());
    private static final ArgumentSpecification<Configuration> CONFIGURATION =
            configuration("configuration");

    private static final ArgumentHandler ARGUMENTS =
            ArgumentHandler.getInstance(
                    GroovyScriptExecutorComponentFactory.class, NAME, CONFIGURATION);

    @Override
    public boolean isLazy() {
        return true; // Created on-demand with arguments
    }

    @Override
    public Class<ScriptExecutor> getComponentType() {
        return ScriptExecutor.class;
    }

    @Override
    public Optional<String> getQualifier() {
        return Optional.of("groovy");
    }

    @Override
    public List<Class<?>> getRequiredContextTypes() {
        return List.of(JdbcDatabase.class);
    }

    @Override
    public List<ArgumentSpecification<?>> getArgumentSpecifications() {
        return ARGUMENTS.getSpecifications();
    }

    @Override
    public ScriptExecutor createComponent(Context context, List<Object> arguments) {
        ARGUMENTS.validateStructure(arguments);

        String name = ARGUMENTS.getArgument(NAME, arguments);
        Configuration configuration = ARGUMENTS.getArgument(CONFIGURATION, arguments);

        // Get JdbcDatabase from IoC container - container manages lifecycle
        JdbcDatabase jdbcDatabase = context.get(JdbcDatabase.class, null, name, configuration);

        return new GroovyScriptExecutor(name, configuration, jdbcDatabase);
    }
}
