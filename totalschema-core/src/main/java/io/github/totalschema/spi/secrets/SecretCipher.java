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

package io.github.totalschema.spi.secrets;

/**
 * Service Provider Interface for encrypting and decrypting sensitive data.
 *
 * <p>Implementations provide cryptographic operations for protecting sensitive information such as
 * passwords and connection strings in configuration files.
 */
public interface SecretCipher {

    /**
     * Checks if this cipher can decrypt the given encrypted expression.
     *
     * @param expression the encrypted expression to check
     * @return true if this cipher can decrypt the expression, false otherwise
     */
    boolean canDecrypt(String expression);

    /**
     * Encrypts plain text using a string password.
     *
     * @param plainText the plain text to encrypt
     * @param password the password to use for encryption
     * @param insertNewLines whether to insert new lines in the output
     * @return the encrypted text as a string
     */
    String encrypt(String plainText, String password, boolean insertNewLines);

    /**
     * Encrypts binary data using a character array password.
     *
     * @param plainText the binary data to encrypt
     * @param password the password to use for encryption
     * @param insertNewLines whether to insert new lines in the output
     * @return the encrypted data as a string
     */
    String encrypt(byte[] plainText, char[] password, boolean insertNewLines);

    /**
     * Encrypts binary data using specific salt and initialization vector.
     *
     * @param plainText the binary data to encrypt
     * @param password the password to use for encryption
     * @param salt the salt for key derivation
     * @param iv the initialization vector
     * @return the encrypted binary data
     */
    byte[] encrypt(byte[] plainText, char[] password, byte[] salt, byte[] iv);

    /**
     * Decrypts an encrypted expression using a string password.
     *
     * @param expression the encrypted expression to decrypt
     * @param password the password to use for decryption
     * @return the decrypted plain text
     */
    String decrypt(String expression, String password);

    /**
     * Decrypts an encrypted expression using a character array password.
     *
     * @param expression the encrypted expression to decrypt
     * @param password the password to use for decryption
     * @return the decrypted plain text
     */
    String decrypt(String expression, char[] password);
}
