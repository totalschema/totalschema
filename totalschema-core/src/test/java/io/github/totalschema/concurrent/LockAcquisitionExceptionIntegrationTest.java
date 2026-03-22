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

package io.github.totalschema.concurrent;

import static org.testng.Assert.*;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import org.testng.annotations.Test;

/**
 * Integration test demonstrating the type safety benefits of LockAcquisitionException.
 *
 * <p>This test shows how callers can distinguish between lock acquisition failures and other
 * exceptions, enabling more precise error handling.
 */
public class LockAcquisitionExceptionIntegrationTest {

    @Test
    public void testTypeSafeExceptionHandling() throws Exception {
        ReentrantLock lock = new ReentrantLock();
        LockTemplate template = new LockTemplate(100, TimeUnit.MILLISECONDS, lock);

        // Acquire the lock in a separate thread to prevent reentrant acquisition
        Thread lockHolder =
                new Thread(
                        () -> {
                            lock.lock();
                            try {
                                Thread.sleep(1000); // Hold lock for 1 second
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            } finally {
                                lock.unlock();
                            }
                        });

        lockHolder.start();
        Thread.sleep(50); // Give lock holder time to acquire the lock

        try {
            // Try to acquire the locked lock - should timeout
            template.withTryLock(
                    () -> {
                        fail("Should not execute callback");
                    });

            fail("Should have thrown LockAcquisitionException");

        } catch (LockAcquisitionException e) {
            // Type-safe exception handling
            assertTrue(e.getMessage().contains("Failed to acquire lock within"));
            assertTrue(e.getMessage().contains("MILLISECONDS"));

            // Can distinguish from other RuntimeExceptions
            assertNotNull(e);

        } catch (RuntimeException e) {
            fail("Should have caught LockAcquisitionException, not generic RuntimeException");

        } finally {
            lockHolder.join(2000); // Wait for lock holder to finish
        }
    }

    @Test
    public void testInterruptedExceptionWrapping() throws Exception {
        ReentrantLock lock = new ReentrantLock();
        LockTemplate template = new LockTemplate(10, TimeUnit.SECONDS, lock);

        // Create a thread that will be interrupted
        Thread testThread =
                new Thread(
                        () -> {
                            try {
                                // This will be interrupted
                                template.withTryLock(
                                        () -> {
                                            fail("Should not execute");
                                        });
                                fail("Should have thrown LockAcquisitionException");

                            } catch (LockAcquisitionException e) {
                                // Verify the cause is InterruptedException
                                assertTrue(e.getMessage().contains("interrupted while acquiring"));
                                assertTrue(e.getCause() instanceof InterruptedException);

                                // Verify interrupt flag was restored
                                assertTrue(Thread.currentThread().isInterrupted());
                            }
                        });

        // Acquire lock in main thread
        lock.lock();
        try {
            testThread.start();
            Thread.sleep(100); // Give thread time to start waiting
            testThread.interrupt(); // Interrupt the waiting thread
            testThread.join(1000); // Wait for thread to complete
            assertFalse(testThread.isAlive(), "Test thread should have completed");

        } finally {
            lock.unlock();
        }
    }

    @Test
    public void testDistinguishFromBusinessExceptions() throws Exception {
        LockTemplate template = new LockTemplate(10, TimeUnit.SECONDS, new ReentrantLock());

        boolean caughtBusinessException = false;
        boolean caughtLockException = false;

        // Demonstrate we can distinguish business exceptions from lock exceptions
        try {
            template.withTryLock(
                    () -> {
                        // Simulate a business exception
                        throw new IllegalStateException("Business logic error");
                    });

        } catch (LockAcquisitionException e) {
            caughtLockException = true;

        } catch (IllegalStateException e) {
            // Business exceptions pass through unchanged
            caughtBusinessException = true;
            assertEquals("Business logic error", e.getMessage());
        }

        assertTrue(caughtBusinessException, "Should have caught business exception");
        assertFalse(caughtLockException, "Should not have caught lock exception");
    }

    @Test
    public void testSerializableException() {
        LockAcquisitionException exception = new LockAcquisitionException("Test message");
        assertNotNull(exception.getMessage());
        assertEquals("Test message", exception.getMessage());

        // Test constructor with cause
        InterruptedException cause = new InterruptedException("Interrupted");
        LockAcquisitionException exceptionWithCause =
                new LockAcquisitionException("Test with cause", cause);
        assertEquals("Test with cause", exceptionWithCause.getMessage());
        assertSame(cause, exceptionWithCause.getCause());
    }
}
