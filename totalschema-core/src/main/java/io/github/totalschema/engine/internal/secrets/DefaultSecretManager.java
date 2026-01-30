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

import io.github.totalschema.spi.secrets.SecretCipher;
import io.github.totalschema.spi.secrets.SecretsManager;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

final class DefaultSecretManager implements SecretsManager {

    // NOTE: reverse order: newer cipher versions must appear earlier
    private static final List<SecretCipher> SECRET_CIPHERS = List.of(new DefaultSecretCipherV1());

    private static final SecretCipher MOST_RECENT_SECRET_CIPHER = SECRET_CIPHERS.get(0);

    private final String password;

    public DefaultSecretManager(String password) {
        this.password = password;
    }

    @Override
    public String encode(String plainText) {
        return encode(plainText, false);
    }

    @Override
    public String encode(String plainText, boolean insertNewLines) {

        if (password == null) {
            throw new IllegalStateException(
                    "For secret encoding to work a password must be specified");
        }

        return MOST_RECENT_SECRET_CIPHER.encrypt(plainText, password, insertNewLines);
    }

    @Override
    public String decodedFileContent(String expression) {

        Path filePath = Path.of(expression);

        return decodedFileContent(filePath);
    }

    @Override
    public String decodedFilePath(String expression) {

        try {
            Path filePath = Path.of(expression);

            String decodedFileContent = decodedFileContent(filePath);

            Path fileName = filePath.getFileName();
            Objects.requireNonNull(fileName, "Filename is null in: " + filePath);

            String desiredExtension = getDesiredExtension(fileName);

            Path tempFile = Files.createTempFile(fileName.toString(), desiredExtension);

            Files.writeString(tempFile, decodedFileContent);

            tempFile.toFile().deleteOnExit();

            return tempFile.toAbsolutePath().toString();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String getDesiredExtension(Path fileName) {

        String desiredExtension;

        String fileNameString = fileName.toString();

        fileNameString = trimExtension(fileNameString, ".secret");
        fileNameString = trimExtension(fileNameString, ".secret.txt");

        int realExtensionIndex = fileNameString.lastIndexOf('.');
        if (realExtensionIndex < 0) {
            desiredExtension = null;
        } else {
            desiredExtension = fileNameString.substring(realExtensionIndex);
        }

        return desiredExtension;
    }

    private static String trimExtension(String fileNameString, String extension) {

        Objects.requireNonNull(fileNameString, "fileNameString cannot be null");
        Objects.requireNonNull(extension, "extension cannot be null");

        if (fileNameString.endsWith(extension)) {
            fileNameString =
                    fileNameString.substring(0, fileNameString.length() - extension.length());
        }

        return fileNameString;
    }

    private String decodedFileContent(Path filePath) {
        if (!Files.exists(filePath)) {
            throw new RuntimeException("File not found: " + filePath);
        }

        if (!Files.isRegularFile(filePath)) {
            throw new RuntimeException("File is not a regular file: " + filePath);
        }

        if (!Files.isReadable(filePath)) {
            throw new RuntimeException("File is not readable: " + filePath);
        }

        try {
            String fileContent = Files.readString(filePath).trim();

            return decode(fileContent);

        } catch (IOException e) {
            throw new RuntimeException("I/O error reading: " + filePath, e);
        }
    }

    @Override
    public String decode(String expression) {

        if (password == null) {
            throw new IllegalStateException(
                    "If secrets are used in configuration, a password must be specified");
        }

        for (SecretCipher secretCipher : SECRET_CIPHERS) {
            if (secretCipher.canDecrypt(expression)) {
                return secretCipher.decrypt(expression, password);
            }
        }

        throw new IllegalStateException(
                "No SecretCipher could decrypt the expression: " + expression);
    }
}
