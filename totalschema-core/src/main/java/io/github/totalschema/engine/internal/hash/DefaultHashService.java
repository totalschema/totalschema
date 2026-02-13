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

package io.github.totalschema.engine.internal.hash;

import io.github.totalschema.config.Configuration;
import io.github.totalschema.spi.hash.HashService;
import io.github.totalschema.util.HexUtil;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

final class DefaultHashService implements HashService {

    private static final String DEFAULT_HASH_ALGORITHM = "SHA-256";

    private final String algorithm;

    DefaultHashService(Configuration configuration) {

        this.algorithm =
                configuration.getString("hash", "algorithm").orElse(DEFAULT_HASH_ALGORITHM);

        try {
            MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Hashing algorithm is not available: " + algorithm, e);
        }
    }

    @Override
    public String hashToHexString(String value) {

        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);

            byte[] valueBytes = value.getBytes(StandardCharsets.UTF_8);

            byte[] encodedHash = digest.digest(valueBytes);

            return HexUtil.encodeToString(encodedHash);

        } catch (NoSuchAlgorithmException e) {
            // should not happen, as we check the availability
            // of the algorithm in the constructor
            throw new RuntimeException(e);
        }
    }
}
