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

import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Secret cipher implementation using AES-256-CBC with PBKDF2-HMAC-SHA256 key derivation.
 *
 * <p>Cryptographic Configuration: - Algorithm: AES-256-CBC - Key Derivation: PBKDF2-HMAC-SHA256 -
 * Salt Length: 16 bytes (128 bits - meets NIST SP 800-132 recommendation) - Iteration Count:
 * 120,000 (meets OWASP 2021 minimum standard) - Key Length: 256 bits - IV Length: 16 bytes (128
 * bits)
 *
 * <p>This configuration provides: - Strong security meeting current NIST and OWASP standards -
 * Balanced performance for CI/CD automation scenarios (~50-75ms per secret) - Protection against
 * rainbow table and brute-force attacks
 *
 * <p>Security Standards Met: - NIST SP 800-132 (Salt length requirement) - OWASP 2021 (Minimum
 * iteration count) - AES-256 (Industry standard encryption)
 */
final class DefaultSecretCipherV1 extends AbstractSecretCipher {

    private static final String SECRET_PREFIX = "!SECRET;1.0;";

    /**
     * Salt length in bytes. Meets NIST SP 800-132 recommendation of at least 128 bits. Provides
     * strong protection against rainbow table attacks.
     */
    private static final int SALT_LENGTH = 16;

    /** Initialization Vector length in bytes for AES-CBC mode. */
    private static final int IV_LENGTH = 16;

    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";

    private static final String SECRET_KEY_FACTORY_ALGORITHM = "PBKDF2WithHmacSHA256";

    private static final String AES_ALGO = "AES";

    /**
     * PBKDF2 iteration count. Set to 120,000 to meet OWASP 2021 minimum standards while maintaining
     * reasonable performance for automation scenarios.
     *
     * <p>This provides a good balance between security and performance: - Meets current security
     * standards (OWASP 2021 minimum) - Provides strong protection against brute-force attacks -
     * Allows efficient processing of multiple secrets in CI/CD pipelines - ~50-75ms decryption time
     * per secret on modern hardware
     */
    private static final int ITERATION_COUNT = 120_000;

    private static final int KEY_LENGTH = 256;

    DefaultSecretCipherV1() {
        super(SECRET_PREFIX, new DefaultSecretSerializer(), SALT_LENGTH, IV_LENGTH);
    }

    @Override
    public byte[] encrypt(byte[] plainText, char[] password, byte[] salt, byte[] iv) {

        try {
            SecretKey secretKey = getSecretKey(salt, password);

            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec);

            return cipher.doFinal(plainText);

        } catch (GeneralSecurityException gse) {
            throw new RuntimeException("Encryption failed", gse);
        }
    }

    @Override
    protected byte[] decrypt(char[] password, byte[] salt, byte[] iv, byte[] cipherText)
            throws GeneralSecurityException {

        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

        SecretKey secretKey = getSecretKey(salt, password);

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec);

        return cipher.doFinal(cipherText);
    }

    private SecretKey getSecretKey(byte[] salt, char[] passwordCharArray)
            throws NoSuchAlgorithmException, InvalidKeySpecException {

        SecretKeyFactory secretKeyFactory =
                SecretKeyFactory.getInstance(SECRET_KEY_FACTORY_ALGORITHM);

        KeySpec keySpec = new PBEKeySpec(passwordCharArray, salt, ITERATION_COUNT, KEY_LENGTH);
        SecretKey secretKey = secretKeyFactory.generateSecret(keySpec);
        byte[] encodedSecret = secretKey.getEncoded();

        return new SecretKeySpec(encodedSecret, AES_ALGO);
    }
}
