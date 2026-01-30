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

import static java.util.Objects.requireNonNull;

import io.github.totalschema.util.HexUtil;
import java.io.*;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;

public final class DefaultSecretSerializer implements SecretSerializer {

    private static final byte[] MAGIC_BYTES = new byte[] {0xF, 0xE};

    static final int INT_BYTES = 4;

    @Override
    public String serializeToString(SecretPayload secretPayload) {
        return serializeToString(secretPayload, false);
    }

    @Override
    public String serializeToString(SecretPayload secretPayload, boolean insertNewLines) {

        Objects.requireNonNull(secretPayload);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        bos.writeBytes(MAGIC_BYTES);

        writeField(requireNonNull(secretPayload.getSalt(), "salt cannot be null"), bos);
        writeField(requireNonNull(secretPayload.getIv(), "iv cannot be null"), bos);
        writeField(requireNonNull(secretPayload.getCipherText(), "cipherText cannot be null"), bos);

        return HexUtil.encodeToString(bos.toByteArray(), insertNewLines);
    }

    private static void writeField(byte[] bytes, ByteArrayOutputStream byteArrayOutputStream) {

        ByteBuffer sizeBuffer = ByteBuffer.allocate(INT_BYTES);
        sizeBuffer.putInt(bytes.length);
        byteArrayOutputStream.writeBytes(sizeBuffer.array());

        byteArrayOutputStream.writeBytes(bytes);
    }

    @Override
    public SecretPayload deserializeFromString(String string) {

        try {
            byte[] bytes = HexUtil.decodeFromString(string);

            ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);

            byte[] readMagicBytesArray = new byte[MAGIC_BYTES.length];
            byteBuffer.get(readMagicBytesArray);

            if (!Arrays.equals(readMagicBytesArray, MAGIC_BYTES)) {
                throw new IllegalArgumentException(
                        String.format(
                                "Magic bytes mismatch. Expected: %s, got: %s",
                                Arrays.toString(MAGIC_BYTES),
                                Arrays.toString(readMagicBytesArray)));
            }

            byte[] salt = readFieldValue(byteBuffer);
            byte[] iv = readFieldValue(byteBuffer);
            byte[] cipherText = readFieldValue(byteBuffer);

            return new SecretPayload(salt, iv, cipherText);

        } catch (BufferUnderflowException ex) {
            throw new RuntimeException(
                    "Failure deserializing secret value. The content is corrupted", ex);
        }
    }

    private static byte[] readFieldValue(ByteBuffer byteBuffer) {
        int fieldLength = byteBuffer.getInt();
        byte[] fieldValue = new byte[fieldLength];
        byteBuffer.get(fieldValue);
        return fieldValue;
    }
}
