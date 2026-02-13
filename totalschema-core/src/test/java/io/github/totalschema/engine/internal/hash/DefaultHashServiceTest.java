/*
 * totalschema: tool for managing database versioning and schema changes with ease.
 * Copyright (C) 2026 totalschema development team
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

import static org.testng.Assert.*;

import io.github.totalschema.config.Configuration;
import io.github.totalschema.config.MapConfiguration;
import java.util.Map;
import org.testng.annotations.Test;

public class DefaultHashServiceTest {

    @Test
    public void testHashToHexStringWithDefaultAlgorithm() {
        Configuration config = new MapConfiguration(Map.of());

        DefaultHashService hashService = new DefaultHashService(config);
        String hash = hashService.hashToHexString("test");

        assertNotNull(hash);
        // SHA-256 produces 64 hex characters (32 bytes * 2)
        assertEquals(hash.length(), 64);
    }

    @Test
    public void testHashToHexStringConsistency() {
        Configuration config = new MapConfiguration(Map.of());

        DefaultHashService hashService = new DefaultHashService(config);
        String hash1 = hashService.hashToHexString("test");
        String hash2 = hashService.hashToHexString("test");

        assertEquals(hash1, hash2);
    }

    @Test
    public void testHashToHexStringDifferentInputs() {
        Configuration config = new MapConfiguration(Map.of());

        DefaultHashService hashService = new DefaultHashService(config);
        String hash1 = hashService.hashToHexString("test1");
        String hash2 = hashService.hashToHexString("test2");

        assertNotEquals(hash1, hash2);
    }

    @Test
    public void testHashToHexStringWithCustomAlgorithm() {
        Configuration config = new MapConfiguration(Map.of("hash.algorithm", "MD5"));

        DefaultHashService hashService = new DefaultHashService(config);
        String hash = hashService.hashToHexString("test");

        assertNotNull(hash);
        // MD5 produces 32 hex characters (16 bytes * 2)
        assertEquals(hash.length(), 32);
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testHashToHexStringWithInvalidAlgorithm() {
        Configuration config = new MapConfiguration(Map.of("hash.algorithm", "INVALID-ALGORITHM"));

        new DefaultHashService(config);
    }

    @Test
    public void testHashToHexStringWithEmptyString() {
        Configuration config = new MapConfiguration(Map.of());

        DefaultHashService hashService = new DefaultHashService(config);
        String hash = hashService.hashToHexString("");

        assertNotNull(hash);
        assertEquals(hash.length(), 64);
    }

    @Test
    public void testHashToHexStringKnownValue() {
        Configuration config = new MapConfiguration(Map.of());

        DefaultHashService hashService = new DefaultHashService(config);
        String hash = hashService.hashToHexString("test");

        // Known SHA-256 hash of "test" (uppercase)
        assertEquals(
                hash.toUpperCase(),
                "9F86D081884C7D659A2FEAA0C55AD015A3BF4F1B2B0B822CD15D6C15B0F00A08");
    }
}
