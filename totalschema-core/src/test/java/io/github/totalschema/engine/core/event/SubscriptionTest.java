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

import static org.testng.Assert.*;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class SubscriptionTest {

    private EventDispatcher dispatcher;
    private EventListener<ChangeEngineCloseEvent> listener;
    private Subscription<ChangeEngineCloseEvent> subscription;
    private boolean listenerCalled;

    @BeforeMethod
    public void setUp() {
        dispatcher = new EventDispatcher();
        listenerCalled = false;
        listener = event -> listenerCalled = true;
        subscription = dispatcher.subscribe(ChangeEngineCloseEvent.class, listener);
    }

    @Test
    public void testIsActiveInitially() {
        assertTrue(subscription.isActive());
    }

    @Test
    public void testUnsubscribeDeactivates() {
        boolean result = subscription.unsubscribe();

        assertTrue(result);
        assertFalse(subscription.isActive());
    }

    @Test
    public void testUnsubscribeMultipleTimes() {
        // First unsubscribe should work
        boolean firstResult = subscription.unsubscribe();
        assertTrue(firstResult);
        assertFalse(subscription.isActive());

        // Second unsubscribe should return false without calling dispatcher again
        boolean secondResult = subscription.unsubscribe();
        assertFalse(secondResult);
        assertFalse(subscription.isActive());
    }

    @Test
    public void testUnsubscribePreventsNotification() {
        subscription.unsubscribe();

        // Dispatch event after unsubscribing
        dispatcher.dispatch(new ChangeEngineCloseEvent());

        // Listener should not have been called
        assertFalse(listenerCalled);
    }

    @Test
    public void testActiveSubscriptionReceivesEvents() {
        // Dispatch event while subscribed
        dispatcher.dispatch(new ChangeEngineCloseEvent());

        // Listener should have been called
        assertTrue(listenerCalled);
    }
}
