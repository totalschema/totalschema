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

import io.github.totalschema.engine.core.command.api.Command;
import io.github.totalschema.engine.core.command.api.CommandContext;
import io.github.totalschema.engine.core.command.api.CommandExecutor;
import io.github.totalschema.engine.core.command.api.CommandInterceptor;
import io.github.totalschema.spi.secrets.SecretManagerFactory;
import io.github.totalschema.spi.secrets.SecretsManager;

public final class SecretsManagerInitializerInterceptor extends CommandInterceptor {

    private final SecretsManager secretsManager;

    public SecretsManagerInitializerInterceptor(
            CommandExecutor next, SecretsManager secretsManager) {
        super(next);
        if (secretsManager != null) {
            this.secretsManager = secretsManager;

        } else {
            SecretManagerFactory secretManagerFactory = SecretManagerFactory.getInstance();
            this.secretsManager = secretManagerFactory.getSecretsManager(null, null);
        }
    }

    @Override
    public <R> R intercept(CommandContext context, Command<R> command, CommandExecutor next)
            throws InterruptedException {

        context.setValue(SecretsManager.class, secretsManager);

        return next.execute(context, command);
    }
}
