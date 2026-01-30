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

import org.testng.annotations.Test;

public class SecretPayloadTest {

    @Test
    public void testConstructorWithAllFields() {
        byte[] salt = new byte[] {1, 2, 3};
        byte[] iv = new byte[] {4, 5, 6};
        byte[] cipherText = new byte[] {7, 8, 9};

        SecretPayload payload = new SecretPayload(salt, iv, cipherText);

        assertNotNull(payload.getSalt());
        assertNotNull(payload.getIv());
        assertNotNull(payload.getCipherText());

        assertNotSame(payload.getSalt(), salt); // Defensive copy
        assertNotSame(payload.getIv(), iv); // Defensive copy
        assertNotSame(payload.getCipherText(), cipherText); // Defensive copy

        assertEquals(payload.getSalt(), salt);
        assertEquals(payload.getIv(), iv);
        assertEquals(payload.getCipherText(), cipherText);
    }

    @Test
    public void testConstructorWithNullValues() {
        SecretPayload payload = new SecretPayload(null, null, null);

        assertNull(payload.getSalt());
        assertNull(payload.getIv());
        assertNull(payload.getCipherText());
    }

    @Test
    public void testDefensiveCopyOnGet() {
        byte[] original = new byte[] {1, 2, 3};
        SecretPayload payload = new SecretPayload(original, original, original);

        byte[] retrieved = payload.getSalt();
        retrieved[0] = 99; // Modify retrieved array

        // Original should not be affected due to defensive copy
        assertEquals(payload.getSalt()[0], 1);
    }

    @Test
    public void testDefensiveCopyOnSet() {
        SecretPayload payload = new SecretPayload(null, null, null);

        byte[] salt = new byte[] {1, 2, 3};
        payload.setSalt(salt);

        salt[0] = 99; // Modify original array

        // Payload should not be affected due to defensive copy
        assertEquals(payload.getSalt()[0], 1);
    }

    @Test
    public void testSetAndGetSalt() {
        SecretPayload payload = new SecretPayload(null, null, null);
        byte[] salt = new byte[] {10, 20, 30};

        payload.setSalt(salt);

        assertNotNull(payload.getSalt());
        assertEquals(payload.getSalt(), salt);
    }

    @Test
    public void testSetAndGetIv() {
        SecretPayload payload = new SecretPayload(null, null, null);
        byte[] iv = new byte[] {40, 50, 60};

        payload.setIv(iv);

        assertNotNull(payload.getIv());
        assertEquals(payload.getIv(), iv);
    }

    @Test
    public void testSetAndGetCipherText() {
        SecretPayload payload = new SecretPayload(null, null, null);
        byte[] cipherText = new byte[] {70, 80, 90};

        payload.setCipherText(cipherText);

        assertNotNull(payload.getCipherText());
        assertEquals(payload.getCipherText(), cipherText);
    }

    @Test
    public void testMultipleGetCalls() {
        byte[] salt = new byte[] {1, 2, 3};
        SecretPayload payload = new SecretPayload(salt, null, null);

        byte[] first = payload.getSalt();
        byte[] second = payload.getSalt();

        // Each get should return a new copy
        assertNotSame(first, second);
        assertEquals(first, second);
    }

    @Test
    public void testEmptyArrays() {
        byte[] empty = new byte[0];
        SecretPayload payload = new SecretPayload(empty, empty, empty);

        assertEquals(payload.getSalt().length, 0);
        assertEquals(payload.getIv().length, 0);
        assertEquals(payload.getCipherText().length, 0);
    }
}
