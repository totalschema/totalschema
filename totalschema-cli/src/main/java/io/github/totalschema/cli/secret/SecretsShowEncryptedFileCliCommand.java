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
import java.nio.file.Path;
import picocli.CommandLine;

@CommandLine.Command(
        name = "show-encrypted-file",
        mixinStandardHelpOptions = true,
        description = "displays the content of an encrypted file")
public class SecretsShowEncryptedFileCliCommand extends SecretManagerServiceCliCommand {

    @CommandLine.Option(
            names = {"--encryptedFile"},
            required = true,
            description = "The file decrypt and show the contents of")
    private Path encryptedFile;

    protected void run(SecretsManager secretsManager) {

        String decodedFileContent =
                secretsManager.decodedFileContent(encryptedFile.toAbsolutePath().toString());
        System.out.println(decodedFileContent);
    }
}
