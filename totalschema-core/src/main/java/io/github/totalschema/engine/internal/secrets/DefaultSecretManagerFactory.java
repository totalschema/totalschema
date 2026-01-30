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

package io.github.totalschema.engine.internal.secrets;

import io.github.totalschema.spi.secrets.SecretManagerFactory;
import io.github.totalschema.spi.secrets.SecretsManager;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class DefaultSecretManagerFactory implements SecretManagerFactory {

    @Override
    public SecretsManager getSecretsManager(String secretValue, String secretValueFile) {

        try {
            String password;

            if (secretValue != null) {
                password = secretValue;

            } else if (secretValueFile != null) {
                Path path = Paths.get(secretValueFile);
                password = Files.readString(path);

            } else {
                password = null;
            }

            return new DefaultSecretManager(password);

        } catch (IOException e) {
            throw new RuntimeException("Failure reading: " + secretValueFile, e);
        }
    }
}
