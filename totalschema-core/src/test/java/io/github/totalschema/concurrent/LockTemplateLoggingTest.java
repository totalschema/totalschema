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
 * Test demonstrating the trace logging functionality of LockTemplate.
 *
 * <p>Note: These tests verify that logging code executes without errors. To see actual log output,
 * configure the logging level for {@code io.github.totalschema.concurrent.LockTemplate} to TRACE in
 * your logging configuration (e.g., logback.xml or log4j2.xml).
 *
 * <p>Example logback configuration:
 *
 * <pre>{@code
 * <logger name="io.github.totalschema.concurrent.LockTemplate" level="TRACE"/>
 * }</pre>
 */
public class LockTemplateLoggingTest {

    @Test
    public void testSuccessfulLockAcquisitionWithLogging() throws Exception {
        ReentrantLock lock = new ReentrantLock();
        LockTemplate template = new LockTemplate(5, TimeUnit.SECONDS, lock);

        // This should log: attempt, success, callback completion, and release
        String result =
                template.withTryLock(
                        () -> {
                            return "test-result";
                        });

        assertEquals(result, "test-result");
    }

    @Test
    public void testFailedLockAcquisitionWithLogging() throws Exception {
        ReentrantLock lock = new ReentrantLock();
        LockTemplate template = new LockTemplate(50, TimeUnit.MILLISECONDS, lock);

        // Acquire lock in another thread
        Thread lockHolder =
                new Thread(
                        () -> {
                            lock.lock();
                            try {
                                Thread.sleep(500); // Hold for 500ms
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            } finally {
                                lock.unlock();
                            }
                        });

        lockHolder.start();
        Thread.sleep(20); // Give lock holder time to acquire

        try {
            // This should log: attempt and failure
            template.withTryLock(
                    () -> {
                        fail("Should not execute");
                    });
            fail("Should have thrown LockAcquisitionException");

        } catch (LockAcquisitionException e) {
            assertTrue(e.getMessage().contains("Failed to acquire lock within"));

        } finally {
            lockHolder.join(1000);
        }
    }

    @Test
    public void testInterruptibleLockWithLogging() throws Exception {
        ReentrantLock lock = new ReentrantLock();
        LockTemplate template = new LockTemplate(5, TimeUnit.SECONDS, lock);

        // This should log with "(interruptible)" markers
        Integer result = template.withTryLockInterruptible(() -> 42);

        assertEquals(result, Integer.valueOf(42));
    }

    @Test
    public void testMultipleOperationsShowIndividualLogs() throws Exception {
        ReentrantLock lock = new ReentrantLock();
        LockTemplate template = new LockTemplate(1, TimeUnit.SECONDS, lock);

        // Each operation should have its own set of log entries
        template.withTryLock(() -> "first");
        template.withTryLock(() -> "second");
        template.withTryLock(() -> "third");

        // If TRACE logging is enabled, you should see 3 complete cycles:
        // - 3 acquisition attempts
        // - 3 successful acquisitions
        // - 3 callback completions
        // - 3 lock releases
    }

    @Test
    public void testLockHoldTimeIsLogged() throws Exception {
        ReentrantLock lock = new ReentrantLock();
        LockTemplate template = new LockTemplate(5, TimeUnit.SECONDS, lock);

        // Execute a callback that takes some time
        template.withTryLock(
                () -> {
                    Thread.sleep(100); // Simulate work
                    return null;
                });

        // If TRACE logging is enabled, the release message should show
        // "total hold time" of approximately 100+ ms
    }

    @Test
    public void testCustomTimeoutIsLoggedCorrectly() throws Exception {
        ReentrantLock lock = new ReentrantLock();
        LockTemplate template = new LockTemplate(1, TimeUnit.SECONDS, lock);

        // Use custom timeout - should be reflected in logs
        template.withTryLock(10, TimeUnit.MILLISECONDS, () -> "custom-timeout");

        // The log should show "10 MILLISECONDS" as the timeout, not "1 SECONDS"
    }
}
