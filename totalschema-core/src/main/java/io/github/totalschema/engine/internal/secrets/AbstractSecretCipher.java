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
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Objects;

/**
 * Abstract base implementation of SecretCipher providing common encryption/decryption logic.
 *
 * <p>Subclasses must implement the specific encryption and decryption algorithms.
 */
public abstract class AbstractSecretCipher implements SecretCipher {

    // From SecureRandom Javadoc: "SecureRandom objects are safe for use by multiple concurrent
    // threads."
    private final SecureRandom secureRandom = new SecureRandom();

    private final String secretStringPrefix;
    private final SecretSerializer secretSerializer;

    private final int saltLength;
    private final int ivLength;

    /**
     * Constructs an AbstractSecretCipher with the specified parameters.
     *
     * @param secretStringPrefix the prefix for encrypted strings
     * @param secretSerializer the serializer for secret payloads
     * @param saltLength the length of the salt in bytes
     * @param ivLength the length of the initialization vector in bytes
     */
    protected AbstractSecretCipher(
            String secretStringPrefix,
            SecretSerializer secretSerializer,
            int saltLength,
            int ivLength) {

        this.secretStringPrefix = secretStringPrefix;
        this.secretSerializer = secretSerializer;
        this.saltLength = saltLength;
        this.ivLength = ivLength;
    }

    @Override
    public boolean canDecrypt(String value) {
        return value != null && value.startsWith(secretStringPrefix);
    }

    @Override
    public String encrypt(String plainText, String password, boolean insertNewLines) {

        Objects.requireNonNull(plainText, "Argument plainText can not be null");
        Objects.requireNonNull(password, "Argument password can not be null");

        return encrypt(
                plainText.getBytes(StandardCharsets.UTF_8), password.toCharArray(), insertNewLines);
    }

    @Override
    public String decrypt(String expression, String password) {

        Objects.requireNonNull(expression, "Argument expression can not be null");
        Objects.requireNonNull(password, "Argument password can not be null");

        return decrypt(expression, password.toCharArray());
    }

    @Override
    public String encrypt(byte[] plainText, char[] password, boolean insertNewLines) {

        Objects.requireNonNull(plainText, "Argument plainText can not be null");
        Objects.requireNonNull(password, "Argument password can not be null");

        byte[] salt = new byte[saltLength];
        secureRandom.nextBytes(salt);

        byte[] iv = new byte[ivLength];
        secureRandom.nextBytes(iv);

        byte[] cipherText = encrypt(plainText, password, salt, iv);

        SecretPayload secretPayload = new SecretPayload(salt, iv, cipherText);
        String serializedPayload =
                secretSerializer.serializeToString(secretPayload, insertNewLines);

        StringBuilder sb = new StringBuilder(secretStringPrefix);
        if (insertNewLines) {
            sb.append("\n");
        }
        sb.append(serializedPayload);

        return sb.toString();
    }

    @Override
    public String decrypt(String value, char[] password) {

        Objects.requireNonNull(value, "Argument value can not be null");
        Objects.requireNonNull(password, "Argument password can not be null");

        if (!canDecrypt(value)) {
            throw new IllegalStateException(
                    String.format(
                            "Secret value [%s] does not begin with expected header [%s]",
                            value, secretStringPrefix));
        }

        try {
            String payloadString = value.substring(secretStringPrefix.length());

            SecretPayload secretPayload = secretSerializer.deserializeFromString(payloadString);

            byte[] salt = secretPayload.getSalt();
            byte[] iv = secretPayload.getIv();
            byte[] cipherText = secretPayload.getCipherText();

            byte[] clearText = decrypt(password, salt, iv, cipherText);

            return new String(clearText, StandardCharsets.UTF_8);

        } catch (GeneralSecurityException gse) {
            throw new RuntimeException("Decryption failed", gse);
        }
    }

    /**
     * Decrypts cipher text using the provided password, salt, and initialization vector.
     *
     * @param password the password for decryption
     * @param salt the salt used in key derivation
     * @param iv the initialization vector
     * @param cipherText the encrypted data
     * @return the decrypted plain text bytes
     * @throws GeneralSecurityException if decryption fails
     */
    protected abstract byte[] decrypt(char[] password, byte[] salt, byte[] iv, byte[] cipherText)
            throws GeneralSecurityException;
}
