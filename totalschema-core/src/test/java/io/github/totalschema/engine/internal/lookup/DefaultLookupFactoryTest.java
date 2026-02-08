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

package io.github.totalschema.engine.internal.lookup;

import static org.easymock.EasyMock.*;
import static org.testng.Assert.*;

import io.github.totalschema.spi.lookup.ExpressionLookup;
import io.github.totalschema.spi.secrets.SecretsManager;
import java.util.List;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class DefaultLookupFactoryTest {

    private DefaultLookupFactory lookupFactory;

    private SecretsManager secretsManager;

    @BeforeMethod
    public void setUp() {
        secretsManager = createMock(SecretsManager.class);
        lookupFactory = new DefaultLookupFactory();
    }

    @Test
    public void testGetLookupsReturnsBuiltInLookups() {
        List<ExpressionLookup> lookups = lookupFactory.getLookups(secretsManager);

        assertNotNull(lookups);
        assertTrue(lookups.size() >= 3, "Should have at least 3 built-in lookups");

        // Check for the built-in lookups
        boolean hasSecret = false;
        boolean hasSecretFileContent = false;
        boolean hasDecodedFilePath = false;

        for (ExpressionLookup lookup : lookups) {
            if ("secret".equals(lookup.getKey())) {
                hasSecret = true;
            } else if ("secretFileContent".equals(lookup.getKey())) {
                hasSecretFileContent = true;
            } else if ("decodedFilePath".equals(lookup.getKey())) {
                hasDecodedFilePath = true;
            }
        }

        assertTrue(hasSecret, "Should have 'secret' lookup");
        assertTrue(hasSecretFileContent, "Should have 'secretFileContent' lookup");
        assertTrue(hasDecodedFilePath, "Should have 'decodedFilePath' lookup");
    }

    @Test
    public void testSecretLookupDelegatesToSecretsManager() {
        expect(secretsManager.decode("encrypted-value")).andReturn("decrypted-value");
        replay(secretsManager);

        List<ExpressionLookup> lookups = lookupFactory.getLookups(secretsManager);
        ExpressionLookup secretLookup =
                lookups.stream()
                        .filter(lookup -> "secret".equals(lookup.getKey()))
                        .findFirst()
                        .orElseThrow();

        String result = secretLookup.apply("encrypted-value");

        assertEquals(result, "decrypted-value");
        verify(secretsManager);
    }

    @Test
    public void testSecretFileContentLookupDelegatesToSecretsManager() {
        expect(secretsManager.decodedFileContent("file-path")).andReturn("file-content");
        replay(secretsManager);

        List<ExpressionLookup> lookups = lookupFactory.getLookups(secretsManager);
        ExpressionLookup secretFileContentLookup =
                lookups.stream()
                        .filter(lookup -> "secretFileContent".equals(lookup.getKey()))
                        .findFirst()
                        .orElseThrow();

        String result = secretFileContentLookup.apply("file-path");

        assertEquals(result, "file-content");
        verify(secretsManager);
    }

    @Test
    public void testDecodedFilePathLookupDelegatesToSecretsManager() {
        expect(secretsManager.decodedFilePath("encrypted-path")).andReturn("/decoded/path");
        replay(secretsManager);

        List<ExpressionLookup> lookups = lookupFactory.getLookups(secretsManager);
        ExpressionLookup decodedFilePathLookup =
                lookups.stream()
                        .filter(lookup -> "decodedFilePath".equals(lookup.getKey()))
                        .findFirst()
                        .orElseThrow();

        String result = decodedFilePathLookup.apply("encrypted-path");

        assertEquals(result, "/decoded/path");
        verify(secretsManager);
    }

    @Test
    public void testGetLookupsReturnsImmutableList() {
        List<ExpressionLookup> lookups = lookupFactory.getLookups(secretsManager);

        assertNotNull(lookups);
        // Should not throw when we try to access it multiple times
        assertEquals(lookups.size(), lookups.size());
    }
}
