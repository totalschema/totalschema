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

package io.github.totalschema.config;

import static org.testng.Assert.*;

import org.testng.annotations.Test;

public class MisconfigurationExceptionTest {

    @Test
    public void testConstructorWithMessage() {
        String message = "Test configuration error";
        MisconfigurationException exception = new MisconfigurationException(message);

        assertEquals(exception.getMessage(), message);
    }

    @Test
    public void testForMessageWithNoArgs() {
        String message = "Simple error message";
        MisconfigurationException exception = MisconfigurationException.forMessage(message);

        assertEquals(exception.getMessage(), message);
    }

    @Test
    public void testForMessageWithArgs() {
        MisconfigurationException exception =
                MisconfigurationException.forMessage(
                        "Configuration '%s' has invalid value: %d", "timeout", 42);

        assertEquals(exception.getMessage(), "Configuration 'timeout' has invalid value: 42");
    }

    @Test
    public void testExceptionIsRuntimeException() {
        MisconfigurationException exception = new MisconfigurationException("test");
        assertTrue(exception instanceof RuntimeException);
    }
}
