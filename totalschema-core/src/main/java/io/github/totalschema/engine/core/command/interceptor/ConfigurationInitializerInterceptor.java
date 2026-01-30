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

package io.github.totalschema.engine.core.command.interceptor;

import io.github.totalschema.config.Configuration;
import io.github.totalschema.config.ConfigurationFactory;
import io.github.totalschema.config.environment.Environment;
import io.github.totalschema.engine.core.command.api.Command;
import io.github.totalschema.engine.core.command.api.CommandContext;
import io.github.totalschema.engine.core.command.api.CommandExecutor;
import io.github.totalschema.engine.core.command.api.CommandInterceptor;
import io.github.totalschema.spi.config.ConfigurationSupplier;
import io.github.totalschema.spi.expression.evaluator.ExpressionEvaluator;

public final class ConfigurationInitializerInterceptor extends CommandInterceptor {

    private final ConfigurationSupplier configurationSupplier;

    public ConfigurationInitializerInterceptor(
            CommandExecutor next, ConfigurationSupplier configurationSupplier) {
        super(next);
        this.configurationSupplier = configurationSupplier;
    }

    @Override
    public <R> R intercept(CommandContext context, Command<R> command, CommandExecutor next)
            throws InterruptedException {

        ExpressionEvaluator expressionEvaluator = context.get(ExpressionEvaluator.class);

        ConfigurationFactory configurationFactory = ConfigurationFactory.getInstance();

        Environment environment;
        if (context.has(Environment.class)) {
            environment = context.get(Environment.class);
        } else {
            environment = null;
        }

        Configuration configuration =
                configurationFactory.getEvaluatedConfiguration(
                        configurationSupplier, expressionEvaluator, environment);

        context.setValue(Configuration.class, configuration);

        return next.execute(context, command);
    }
}
