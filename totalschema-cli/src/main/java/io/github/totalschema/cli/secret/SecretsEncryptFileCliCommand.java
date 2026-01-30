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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import picocli.CommandLine;

@CommandLine.Command(
        name = "encrypt-file",
        mixinStandardHelpOptions = true,
        description = "creates a new encrypted file from the contents of a file")
public class SecretsEncryptFileCliCommand extends SecretManagerServiceCliCommand {

    @CommandLine.Option(
            names = {"--clearTextFile"},
            required = true,
            description = "The clear text value for secret manager to encrypt")
    private Path clearTextFile;

    @CommandLine.Option(
            names = {"--encryptedFile"},
            required = true,
            description = "The file to create with the encrypted content")
    private Path encryptedFile;

    protected void run(SecretsManager secretsManager) {

        if (!Files.exists(clearTextFile)) {
            throw new IllegalArgumentException("clearTextFile does not exist");
        }

        if (Files.exists(encryptedFile)) {
            throw new IllegalArgumentException("encryptedFile exist");
        }

        try {
            String clearText = Files.readString(clearTextFile);

            String cipherText = secretsManager.encode(clearText, true);

            Files.writeString(encryptedFile, cipherText, StandardOpenOption.CREATE_NEW);

            System.out.format(
                    "Encrypted file content from %s to %s %n", clearTextFile, encryptedFile);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
