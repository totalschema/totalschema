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

package io.github.totalschema.cli;

import io.github.totalschema.spi.secrets.SecretManagerFactory;
import io.github.totalschema.spi.secrets.SecretsManager;
import picocli.CommandLine;

/**
 * Abstract base class for all Command Line Interface Commands where {@link SecretsManager} is used.
 * This is used in almost all CLI command classes.
 */
public abstract class SecretManagerServiceCliCommand implements Runnable {

    @CommandLine.Option(
            names = {"--password"},
            description = "The secret value for secret manager")
    private String password;

    @CommandLine.Option(
            names = {"--passwordFile"},
            description = "The file containing the secret value for secret manager")
    private String passwordFile;

    @Override
    public final void run() {

        SecretManagerFactory secretManagerFactory = SecretManagerFactory.getInstance();

        SecretsManager secretsManager =
                secretManagerFactory.getSecretsManager(password, passwordFile);

        run(secretsManager);
    }

    /**
     * Template method for commands that need access to {@link SecretsManager}. Invoked when the CLI
     * command is executed.
     *
     * @param secretsManager the {@code SecretsManagerService} instance (neven {@code null})
     */
    protected abstract void run(SecretsManager secretsManager);
}
