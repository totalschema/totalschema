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

package io.github.totalschema.util;

public final class HexUtil {

    private HexUtil() {
        throw new AssertionError("static utility class, no instances allowed");
    }

    private static final String HEX_CHARS = "0123456789ABCDEF";
    private static final int LINE_LENGTH = 80;

    public static String encodeToString(byte[] bytes) {
        return encodeToString(bytes, false);
    }

    public static String encodeToString(byte[] bytes, boolean insertNewLines) {
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (byte b : bytes) {
            int highNibble = (b >> 4) & 0xF;
            int lowNibble = b & 0xF;
            sb.append(HEX_CHARS.charAt(highNibble)).append(HEX_CHARS.charAt(lowNibble));
            count += 2;
            if (insertNewLines && count % LINE_LENGTH == 0) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    public static byte[] decodeFromString(String hexString) {
        hexString = hexString.replaceAll("\\s", "");
        if (hexString.length() % 2 != 0) {
            throw new IllegalArgumentException("Invalid hex string length.");
        }
        byte[] result = new byte[hexString.length() / 2];
        for (int i = 0; i < hexString.length(); i += 2) {

            int highIndex = i;
            int highNibble = HEX_CHARS.indexOf(hexString.charAt(highIndex));
            if (highNibble == -1) {
                throw new IllegalArgumentException("Invalid hex string character at: " + highIndex);
            }

            int lowIndex = i + 1;
            int lowNibble = HEX_CHARS.indexOf(hexString.charAt(lowIndex));
            if (lowNibble == -1) {
                throw new IllegalArgumentException("Invalid hex string character at: " + lowIndex);
            }
            result[i / 2] = (byte) ((highNibble << 4) | lowNibble);
        }
        return result;
    }
}
