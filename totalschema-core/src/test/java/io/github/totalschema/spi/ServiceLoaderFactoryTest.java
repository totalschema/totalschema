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

package io.github.totalschema.spi;

import static org.testng.Assert.*;

import io.github.totalschema.connector.ConnectorFactory;
import java.util.List;
import org.testng.annotations.Test;

public class ServiceLoaderFactoryTest {

    @Test
    public void testGetSingleServiceWithMultipleImplementations() {
        // ConnectorFactory has multiple implementations, should throw exception
        try {
            ServiceLoaderFactory.getSingleService(ConnectorFactory.class);
            fail("Expected IllegalStateException for multiple services");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("Multiple ServiceLoader services found"));
        }
    }

    @Test
    public void testGetAllServicesWithMultipleImplementations() {
        List<ConnectorFactory> services =
                ServiceLoaderFactory.getAllServices(ConnectorFactory.class);

        assertNotNull(services);
        assertFalse(services.isEmpty());
        assertTrue(services.size() >= 4, "Expected at least 4 connector factories");
    }

    @Test
    public void testGetAllServicesReturnsUnmodifiableList() {
        List<ConnectorFactory> services =
                ServiceLoaderFactory.getAllServices(ConnectorFactory.class);

        try {
            services.add(null);
            fail("Expected UnsupportedOperationException for unmodifiable list");
        } catch (UnsupportedOperationException e) {
            // Expected
        }
    }

    @Test
    public void testGetAllServicesCaching() {
        // Call twice to test caching
        List<ConnectorFactory> services1 =
                ServiceLoaderFactory.getAllServices(ConnectorFactory.class);
        List<ConnectorFactory> services2 =
                ServiceLoaderFactory.getAllServices(ConnectorFactory.class);

        // Should return same list instance due to caching
        assertSame(services1, services2);
    }
}
