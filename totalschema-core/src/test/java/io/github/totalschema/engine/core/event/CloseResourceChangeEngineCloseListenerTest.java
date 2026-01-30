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

package io.github.totalschema.engine.core.event;

import static org.easymock.EasyMock.*;
import static org.testng.Assert.*;

import java.io.Closeable;
import java.io.IOException;
import org.testng.annotations.Test;

public class CloseResourceChangeEngineCloseListenerTest {

    @Test
    public void testOnEventClosesResource() throws IOException {
        Closeable mockResource = createMock(Closeable.class);
        mockResource.close();
        expectLastCall().once();
        replay(mockResource);

        CloseResourceChangeEngineCloseListener listener =
                new CloseResourceChangeEngineCloseListener(mockResource);
        listener.onEvent(new ChangeEngineCloseEvent());

        verify(mockResource);
    }

    @Test
    public void testOnEventWrapsIOException() throws IOException {
        Closeable mockResource = createMock(Closeable.class);
        IOException testException = new IOException("Test exception");
        mockResource.close();
        expectLastCall().andThrow(testException);
        replay(mockResource);

        CloseResourceChangeEngineCloseListener listener =
                new CloseResourceChangeEngineCloseListener(mockResource);

        RuntimeException thrown =
                expectThrows(
                        RuntimeException.class,
                        () -> listener.onEvent(new ChangeEngineCloseEvent()));

        assertTrue(thrown.getMessage().contains("Failure closing"));
        assertEquals(thrown.getCause(), testException);
        verify(mockResource);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testConstructorRejectsNull() {
        new CloseResourceChangeEngineCloseListener(null);
    }

    @Test
    public void testFactoryMethod() throws IOException {
        Closeable mockResource = createMock(Closeable.class);
        mockResource.close();
        expectLastCall().once();
        replay(mockResource);

        CloseResourceChangeEngineCloseListener listener =
                CloseResourceChangeEngineCloseListener.create(mockResource);
        assertNotNull(listener);
        listener.onEvent(new ChangeEngineCloseEvent());

        verify(mockResource);
    }

    @Test
    public void testEquals() {
        Closeable resource1 = createMock(Closeable.class);
        Closeable resource2 = createMock(Closeable.class);

        CloseResourceChangeEngineCloseListener listener1 =
                new CloseResourceChangeEngineCloseListener(resource1);
        CloseResourceChangeEngineCloseListener listener2 =
                new CloseResourceChangeEngineCloseListener(resource1);
        CloseResourceChangeEngineCloseListener listener3 =
                new CloseResourceChangeEngineCloseListener(resource2);

        assertEquals(listener1, listener2);
        assertNotEquals(listener1, listener3);
        assertNotEquals(listener1, null);
        assertNotEquals(listener1, "not a listener");
    }

    @Test
    public void testHashCode() {
        Closeable resource = createMock(Closeable.class);
        CloseResourceChangeEngineCloseListener listener1 =
                new CloseResourceChangeEngineCloseListener(resource);
        CloseResourceChangeEngineCloseListener listener2 =
                new CloseResourceChangeEngineCloseListener(resource);

        assertEquals(listener1.hashCode(), listener2.hashCode());
    }

    @Test
    public void testToString() {
        Closeable resource = createMock(Closeable.class);
        CloseResourceChangeEngineCloseListener listener =
                new CloseResourceChangeEngineCloseListener(resource);

        String toString = listener.toString();
        assertTrue(toString.contains("CloseResourceChangeEngineCloseListener"));
        assertTrue(toString.contains("closeableResource"));
    }
}
