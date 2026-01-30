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

import java.io.Serializable;
import java.util.Arrays;

/**
 * Container for encrypted secret data with defensive copying to prevent external mutation.
 *
 * <p>This class stores cryptographic material (salt, IV, and cipher text) using defensive copies to
 * ensure that external code cannot modify the internal state after construction or retrieval. All
 * byte arrays are copied using {@link Arrays#copyOf(byte[], int)} instead of {@code clone()} for
 * consistent behavior.
 *
 * @see java.io.Serializable
 */
public class SecretPayload implements Serializable {

    private static final long serialVersionUID = 1L;

    private byte[] salt;
    private byte[] iv;
    private byte[] cipherText;

    /**
     * Constructs a new SecretPayload with defensive copies of the provided arrays.
     *
     * @param salt the salt used in key derivation (will be copied, may be null)
     * @param iv the initialization vector used in encryption (will be copied, may be null)
     * @param cipherText the encrypted data (will be copied, may be null)
     */
    public SecretPayload(byte[] salt, byte[] iv, byte[] cipherText) {
        this.salt = nullSafeCopyOf(salt);
        this.iv = nullSafeCopyOf(iv);
        this.cipherText = nullSafeCopyOf(cipherText);
    }

    /**
     * Returns a defensive copy of the salt array.
     *
     * @return a copy of the salt array, or null if no salt is set
     */
    public byte[] getSalt() {
        return nullSafeCopyOf(salt);
    }

    /**
     * Sets the salt with a defensive copy of the provided array.
     *
     * @param salt the new salt value (will be copied, may be null)
     */
    public void setSalt(byte[] salt) {
        this.salt = nullSafeCopyOf(salt);
    }

    /**
     * Returns a defensive copy of the initialization vector (IV) array.
     *
     * @return a copy of the IV array, or null if no IV is set
     */
    public byte[] getIv() {
        return nullSafeCopyOf(iv);
    }

    /**
     * Sets the initialization vector (IV) with a defensive copy of the provided array.
     *
     * @param iv the new IV value (will be copied, may be null)
     */
    public void setIv(byte[] iv) {
        this.iv = nullSafeCopyOf(iv);
    }

    /**
     * Returns a defensive copy of the cipher text array.
     *
     * @return a copy of the cipher text array, or null if no cipher text is set
     */
    public byte[] getCipherText() {
        return nullSafeCopyOf(cipherText);
    }

    /**
     * Sets the cipher text with a defensive copy of the provided array.
     *
     * @param cipherText the new cipher text value (will be copied, may be null)
     */
    public void setCipherText(byte[] cipherText) {
        this.cipherText = nullSafeCopyOf(cipherText);
    }

    private static byte[] nullSafeCopyOf(byte[] byteArray) {
        return byteArray != null ? Arrays.copyOf(byteArray, byteArray.length) : null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SecretPayload that = (SecretPayload) o;
        return Arrays.equals(salt, that.salt)
                && Arrays.equals(iv, that.iv)
                && Arrays.equals(cipherText, that.cipherText);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(salt);
        result = 31 * result + Arrays.hashCode(iv);
        result = 31 * result + Arrays.hashCode(cipherText);
        return result;
    }
}
