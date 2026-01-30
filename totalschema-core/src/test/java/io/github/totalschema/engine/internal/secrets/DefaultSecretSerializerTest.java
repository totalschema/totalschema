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

import static org.testng.Assert.*;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class DefaultSecretSerializerTest {

    private final byte[] salt = new byte[] {0x0, 0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x7};
    private final byte[] iv =
            new byte[] {
                0x0, 0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x7, 0x8, 0x9, 0xA, 0xB, 0xC, 0xD, 0xE, 0xF
            };

    private final byte[] cipherText = new byte[] {0xc, 0xa, 0xf, 0xf, 0xe};

    private final String maxIntSizeFieldEncoded =
            "0F0E7FFFFFFF000102030405060700000010000102030405060708090A0B0C0D0E0F000000050C0A0F0F0E";

    private DefaultSecretSerializer serializer;

    @BeforeMethod
    public void beforeEach() {
        serializer = new DefaultSecretSerializer();
    }

    @Test
    public void testErrorHandling() {

        expectThrows(
                NullPointerException.class,
                () -> {
                    serializer.serializeToString(null);
                });

        expectThrows(
                NullPointerException.class,
                () -> serializer.serializeToString(new SecretPayload(null, iv, cipherText)));

        expectThrows(
                NullPointerException.class,
                () -> serializer.serializeToString(new SecretPayload(salt, null, cipherText)));

        expectThrows(
                NullPointerException.class,
                () -> serializer.serializeToString(new SecretPayload(salt, iv, null)));
    }

    @Test
    public void testSerializing() {

        String serialized = serializer.serializeToString(new SecretPayload(salt, iv, cipherText));
        String encoded =
                "0F0E00000008000102030405060700000010000102030405060708090A0B0C0D0E0F000000050C0A0F0F0E";
        assertEquals(serialized, encoded);
    }

    @Test
    public void testSerializingAndDeserializing() {

        SecretPayload input = new SecretPayload(salt, iv, cipherText);

        String serialized = serializer.serializeToString(input);

        SecretPayload deserialized = serializer.deserializeFromString(serialized);

        assertEquals(deserialized, input);
    }

    @Test
    public void testSerializingAndDeserializingWithNonHexValue() {

        final int corruptedIndex = 2;

        SecretPayload input = new SecretPayload(salt, iv, cipherText);

        String serialized = serializer.serializeToString(input);
        char[] charArray = serialized.toCharArray();
        charArray[corruptedIndex] = 'X';

        String corruptedString = new String(charArray);

        IllegalArgumentException caughtException =
                expectThrows(
                        IllegalArgumentException.class,
                        () -> serializer.deserializeFromString(corruptedString));

        String message = caughtException.getMessage();
        assertEquals(message, String.format("Invalid hex string character at: %s", corruptedIndex));
    }

    @Test
    public void testSerializingAndDeserializingWithNonInvalidHeader() {

        final int corruptedIndex = 1;

        SecretPayload input = new SecretPayload(salt, iv, cipherText);

        String serialized = serializer.serializeToString(input);
        char[] charArray = serialized.toCharArray();
        charArray[corruptedIndex] = 'A';

        String corruptedString = new String(charArray);

        IllegalArgumentException caughtException =
                expectThrows(
                        IllegalArgumentException.class,
                        () -> serializer.deserializeFromString(corruptedString));

        String message = caughtException.getMessage();
        assertEquals(message, "Magic bytes mismatch. Expected: [15, 14], got: [10, 14]");
    }

    @Test
    public void testDeserializingWithMaxIntSizeFieldEncodedFailsButSerializerStillWorks() {

        System.out.println(Integer.MAX_VALUE);

        // an input where the field length is incorrect fails with OutOfMemoryError
        expectThrows(
                OutOfMemoryError.class,
                () -> serializer.deserializeFromString(maxIntSizeFieldEncoded));

        // but the next calls with correct data succeed
        SecretPayload input = new SecretPayload(salt, iv, cipherText);

        String serialized = serializer.serializeToString(input);

        SecretPayload deserialized = serializer.deserializeFromString(serialized);

        assertEquals(deserialized, input);
    }
}
