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

package io.github.totalschema.cli.secret;

import io.github.totalschema.cli.SecretManagerServiceCliCommand;
import io.github.totalschema.spi.secrets.SecretsManager;
import picocli.CommandLine;

@CommandLine.Command(
        name = "encrypt-string",
        mixinStandardHelpOptions = true,
        description = "encrypts a value and displays it")
public class SecretsEncryptStringCliCommand extends SecretManagerServiceCliCommand {

    @CommandLine.Option(
            names = {"--clearTextValue"},
            description = "The clear text value for secret manager to encrypt")
    protected String clearTextValue;

    @CommandLine.Option(
            names = {"--multiline"},
            description =
                    "Adds line breaks after a certain number of characters, suitable for storing in individual files")
    protected boolean multiline;

    protected void run(SecretsManager secretsManager) {

        String cipherText = secretsManager.encode(clearTextValue, multiline);

        System.out.println(cipherText);
    }
}
