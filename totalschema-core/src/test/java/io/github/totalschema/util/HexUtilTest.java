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

import static org.testng.Assert.*;

import org.testng.annotations.Test;

public class HexUtilTest {

    @Test
    public void testEncodeToStringSimpleBytes() {
        byte[] bytes = {0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xAB, (byte) 0xCD, (byte) 0xEF};
        String encoded = HexUtil.encodeToString(bytes);

        assertEquals(encoded, "0123456789ABCDEF");
    }

    @Test
    public void testEncodeToStringEmptyArray() {
        byte[] bytes = {};
        String encoded = HexUtil.encodeToString(bytes);

        assertEquals(encoded, "");
    }

    @Test
    public void testEncodeToStringSingleByte() {
        byte[] bytes = {0x0F};
        String encoded = HexUtil.encodeToString(bytes);

        assertEquals(encoded, "0F");
    }

    @Test
    public void testEncodeToStringWithNewLines() {
        // Create 50 bytes (100 hex chars, should have 1 newline at position 80)
        byte[] bytes = new byte[50];
        for (int i = 0; i < 50; i++) {
            bytes[i] = (byte) i;
        }

        String encoded = HexUtil.encodeToString(bytes, true);

        assertTrue(encoded.contains("\n"));
        // At position 80, there should be a newline
        String[] lines = encoded.split("\n");
        assertTrue(lines.length >= 1);
    }

    @Test
    public void testEncodeToStringWithoutNewLines() {
        byte[] bytes = new byte[50];
        for (int i = 0; i < 50; i++) {
            bytes[i] = (byte) i;
        }

        String encoded = HexUtil.encodeToString(bytes, false);

        assertFalse(encoded.contains("\n"));
    }

    @Test
    public void testDecodeFromStringSimple() {
        String hexString = "0123456789ABCDEF";
        byte[] decoded = HexUtil.decodeFromString(hexString);

        assertEquals(decoded.length, 8);
        assertEquals(decoded[0], (byte) 0x01);
        assertEquals(decoded[1], (byte) 0x23);
        assertEquals(decoded[2], (byte) 0x45);
        assertEquals(decoded[3], (byte) 0x67);
        assertEquals(decoded[4], (byte) 0x89);
        assertEquals(decoded[5], (byte) 0xAB);
        assertEquals(decoded[6], (byte) 0xCD);
        assertEquals(decoded[7], (byte) 0xEF);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = "Invalid hex string character.*")
    public void testDecodeFromStringLowerCaseThrowsException() {
        // HexUtil only supports uppercase hex characters
        String hexString = "0123456789abcdef";
        HexUtil.decodeFromString(hexString);
    }

    @Test
    public void testDecodeFromStringEmptyString() {
        String hexString = "";
        byte[] decoded = HexUtil.decodeFromString(hexString);

        assertEquals(decoded.length, 0);
    }

    @Test
    public void testDecodeFromStringWithWhitespace() {
        String hexString = "01 23 45\n67 89";
        byte[] decoded = HexUtil.decodeFromString(hexString);

        assertEquals(decoded.length, 5);
        assertEquals(decoded[0], (byte) 0x01);
        assertEquals(decoded[1], (byte) 0x23);
        assertEquals(decoded[2], (byte) 0x45);
        assertEquals(decoded[3], (byte) 0x67);
        assertEquals(decoded[4], (byte) 0x89);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = "Invalid hex string length.*")
    public void testDecodeFromStringOddLength() {
        String hexString = "012";
        HexUtil.decodeFromString(hexString);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = "Invalid hex string character.*")
    public void testDecodeFromStringInvalidCharacter() {
        String hexString = "01GZ";
        HexUtil.decodeFromString(hexString);
    }

    @Test
    public void testEncodeDecodeRoundTrip() {
        byte[] original = {
            0x00,
            0x11,
            0x22,
            0x33,
            0x44,
            0x55,
            0x66,
            0x77,
            (byte) 0x88,
            (byte) 0x99,
            (byte) 0xAA,
            (byte) 0xBB,
            (byte) 0xCC,
            (byte) 0xDD,
            (byte) 0xEE,
            (byte) 0xFF
        };

        String encoded = HexUtil.encodeToString(original);
        byte[] decoded = HexUtil.decodeFromString(encoded);

        assertEquals(decoded, original);
    }

    @Test
    public void testEncodeDecodeRoundTripWithNewLines() {
        byte[] original = new byte[100];
        for (int i = 0; i < 100; i++) {
            original[i] = (byte) (i % 256);
        }

        String encoded = HexUtil.encodeToString(original, true);
        byte[] decoded = HexUtil.decodeFromString(encoded);

        assertEquals(decoded, original);
    }

    @Test
    public void testDecodeAllHexDigits() {
        String hexString = "0123456789ABCDEF";
        byte[] decoded = HexUtil.decodeFromString(hexString);

        assertNotNull(decoded);
        assertEquals(decoded.length, 8);
    }

    @Test
    public void testEncodeNegativeBytes() {
        byte[] bytes = {-1, -128, 127};
        String encoded = HexUtil.encodeToString(bytes);

        assertEquals(encoded, "FF807F");
    }
}
