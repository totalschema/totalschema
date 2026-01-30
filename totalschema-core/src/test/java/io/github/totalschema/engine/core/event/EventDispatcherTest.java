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

package io.github.totalschema.engine.core.event;

import static org.testng.Assert.*;

import java.util.concurrent.atomic.AtomicInteger;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class EventDispatcherTest {

    private EventDispatcher eventDispatcher;

    @BeforeMethod
    public void setUp() {
        eventDispatcher = new EventDispatcher();
    }

    @Test
    public void testSubscribeListener() {
        AtomicInteger callCount = new AtomicInteger(0);

        Subscription subscription =
                eventDispatcher.subscribe(
                        ChangeEngineCloseEvent.class, event -> callCount.incrementAndGet());

        assertNotNull(subscription);
        assertTrue(subscription.isActive());
    }

    @Test
    public void testSubscribeDuplicateListener() {
        AtomicInteger callCount = new AtomicInteger(0);

        // With the new API, subscribing the same lambda twice is allowed
        // Each subscription is independent
        eventDispatcher.subscribe(
                ChangeEngineCloseEvent.class, event -> callCount.incrementAndGet());
        eventDispatcher.subscribe(
                ChangeEngineCloseEvent.class, event -> callCount.incrementAndGet());

        eventDispatcher.dispatch(new ChangeEngineCloseEvent());

        assertEquals(callCount.get(), 2); // Called twice (once per subscription)
    }

    @Test
    public void testUnsubscribeListener() {
        AtomicInteger callCount = new AtomicInteger(0);

        Subscription subscription =
                eventDispatcher.subscribe(
                        ChangeEngineCloseEvent.class, event -> callCount.incrementAndGet());

        boolean result = subscription.unsubscribe();
        assertTrue(result);
        assertFalse(subscription.isActive());

        eventDispatcher.dispatch(new ChangeEngineCloseEvent());
        assertEquals(callCount.get(), 0); // Listener was unsubscribed, so not called
    }

    @Test
    public void testUnsubscribeNonExistentListener() {
        boolean result = eventDispatcher.unsubscribe(ChangeEngineCloseEvent.class, event -> {});

        assertFalse(result); // Returns false for non-existent listener
    }

    @Test
    public void testDispatchEventNoListeners() {
        // Should not throw exception with no listeners
        eventDispatcher.dispatch(new ChangeEngineCloseEvent());
    }

    @Test
    public void testDispatchEventWithListeners() {
        AtomicInteger listener1Calls = new AtomicInteger(0);
        AtomicInteger listener2Calls = new AtomicInteger(0);

        eventDispatcher.subscribe(
                ChangeEngineCloseEvent.class, event -> listener1Calls.incrementAndGet());
        eventDispatcher.subscribe(
                ChangeEngineCloseEvent.class, event -> listener2Calls.incrementAndGet());

        eventDispatcher.dispatch(new ChangeEngineCloseEvent());

        assertEquals(listener1Calls.get(), 1);
        assertEquals(listener2Calls.get(), 1);
    }

    @Test
    public void testDispatchEventListenerException() {
        AtomicInteger listener2Calls = new AtomicInteger(0);

        eventDispatcher.subscribe(
                ChangeEngineCloseEvent.class,
                event -> {
                    throw new RuntimeException("Listener 1 failed");
                });
        eventDispatcher.subscribe(
                ChangeEngineCloseEvent.class, event -> listener2Calls.incrementAndGet());

        try {
            eventDispatcher.dispatch(new ChangeEngineCloseEvent());
            fail("Expected RuntimeException");
        } catch (RuntimeException e) {
            // Expected - should still call all listeners
            assertTrue(e.getMessage().contains("Listener notification failed"));
        }

        assertEquals(listener2Calls.get(), 1); // Second listener was still called
    }

    @Test
    public void testSubscribeAndDispatchMultipleTimes() {
        AtomicInteger callCount = new AtomicInteger(0);

        eventDispatcher.subscribe(
                ChangeEngineCloseEvent.class, event -> callCount.incrementAndGet());

        eventDispatcher.dispatch(new ChangeEngineCloseEvent()); // First fire
        assertEquals(callCount.get(), 1);

        eventDispatcher.subscribe(
                ChangeEngineCloseEvent.class, event -> callCount.incrementAndGet());

        eventDispatcher.dispatch(new ChangeEngineCloseEvent()); // Second fire, 2 subscriptions
        assertEquals(callCount.get(), 3); // 1 + 2 = 3
    }

    @Test
    public void testCloseableChangeEngineCloseListener() {
        AtomicInteger closeCount = new AtomicInteger(0);

        CloseResourceChangeEngineCloseListener closeableListener =
                new CloseResourceChangeEngineCloseListener(() -> closeCount.incrementAndGet());

        eventDispatcher.subscribe(ChangeEngineCloseEvent.class, closeableListener);

        eventDispatcher.dispatch(new ChangeEngineCloseEvent());

        assertEquals(closeCount.get(), 1);
    }

    @Test
    public void testGetListenerCount() {
        assertEquals(eventDispatcher.getListenerCount(ChangeEngineCloseEvent.class), 0);

        eventDispatcher.subscribe(ChangeEngineCloseEvent.class, event -> {});
        assertEquals(eventDispatcher.getListenerCount(ChangeEngineCloseEvent.class), 1);

        eventDispatcher.subscribe(ChangeEngineCloseEvent.class, event -> {});
        assertEquals(eventDispatcher.getListenerCount(ChangeEngineCloseEvent.class), 2);
    }

    @Test
    public void testClearAllListeners() {
        eventDispatcher.subscribe(ChangeEngineCloseEvent.class, event -> {});
        eventDispatcher.subscribe(ChangeEngineCloseEvent.class, event -> {});

        assertEquals(eventDispatcher.getListenerCount(ChangeEngineCloseEvent.class), 2);

        eventDispatcher.clear();

        assertEquals(eventDispatcher.getListenerCount(ChangeEngineCloseEvent.class), 0);
    }

    @Test
    public void testSubscriptionHandle() {
        AtomicInteger callCount = new AtomicInteger(0);

        Subscription sub =
                eventDispatcher.subscribe(
                        ChangeEngineCloseEvent.class, event -> callCount.incrementAndGet());

        assertTrue(sub.isActive());

        eventDispatcher.dispatch(new ChangeEngineCloseEvent());
        assertEquals(callCount.get(), 1);

        // Unsubscribe using the handle
        sub.unsubscribe();
        assertFalse(sub.isActive());

        // Fire again - should not be called
        eventDispatcher.dispatch(new ChangeEngineCloseEvent());
        assertEquals(callCount.get(), 1); // Still 1, not incremented
    }

    @Test
    public void testMultipleExceptionsAggregated() {
        eventDispatcher.subscribe(
                ChangeEngineCloseEvent.class,
                event -> {
                    throw new RuntimeException("Exception 1");
                });
        eventDispatcher.subscribe(
                ChangeEngineCloseEvent.class,
                event -> {
                    throw new RuntimeException("Exception 2");
                });

        try {
            eventDispatcher.dispatch(new ChangeEngineCloseEvent());
            fail("Expected RuntimeException");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("2 listener notifications failed"));
            assertEquals(e.getSuppressed().length, 2);
        }
    }

    @Test
    public void testEventNotNull() {
        AtomicInteger callCount = new AtomicInteger(0);

        eventDispatcher.subscribe(
                ChangeEngineCloseEvent.class,
                event -> {
                    assertNotNull(event);
                    callCount.incrementAndGet();
                });

        eventDispatcher.dispatch(new ChangeEngineCloseEvent());
        assertEquals(callCount.get(), 1);
    }
}
