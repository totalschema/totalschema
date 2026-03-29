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

import io.github.totalschema.spi.factory.ComponentFactory;
import java.util.List;
import org.testng.annotations.Test;

public class ServiceLoaderFactoryTest {

    @Test
    public void testGetSingleServiceWithMultipleImplementations() {
        // ComponentFactory has multiple implementations, should throw exception
        try {
            ServiceLoaderFactory.getSingleService(ComponentFactory.class);
            fail("Expected IllegalStateException for multiple services");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("Multiple ServiceLoader services found"));
        }
    }

    @Test
    public void testGetAllServicesWithMultipleImplementations() {
        List<ComponentFactory> services =
                ServiceLoaderFactory.getAllServices(ComponentFactory.class);

        assertNotNull(services);
        assertFalse(services.isEmpty());
        assertTrue(services.size() >= 10, "Expected at least 10 component factories");
    }

    @Test
    public void testGetAllServicesReturnsUnmodifiableList() {
        List<ComponentFactory> services =
                ServiceLoaderFactory.getAllServices(ComponentFactory.class);

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
        List<ComponentFactory> services1 =
                ServiceLoaderFactory.getAllServices(ComponentFactory.class);
        List<ComponentFactory> services2 =
                ServiceLoaderFactory.getAllServices(ComponentFactory.class);

        // Should return same list instance due to caching
        assertSame(services1, services2);
    }
}
