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

import static org.easymock.EasyMock.*;
import static org.testng.Assert.*;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class LockTemplateTest {

    private Lock mockLock;
    private LockTemplate lockTemplate;

    @BeforeMethod
    public void setUp() {
        mockLock = createMock(Lock.class);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testConstructorWithNullLock() {
        new LockTemplate(10, TimeUnit.SECONDS, null);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testConstructorWithNullTimeUnit() {
        new LockTemplate(10, null, mockLock);
    }

    @Test(
            expectedExceptions = RuntimeException.class,
            expectedExceptionsMessageRegExp = ".*cannot be negative or zero.*")
    public void testConstructorWithZeroTimeout() {
        new LockTemplate(0, TimeUnit.SECONDS, mockLock);
    }

    @Test(
            expectedExceptions = RuntimeException.class,
            expectedExceptionsMessageRegExp = ".*cannot be negative or zero.*")
    public void testConstructorWithNegativeTimeout() {
        new LockTemplate(-5, TimeUnit.SECONDS, mockLock);
    }

    @Test
    public void testConstructorWithValidParameters() {
        LockTemplate template = new LockTemplate(10, TimeUnit.SECONDS, mockLock);
        assertNotNull(template);
    }

    @Test
    public void testWithTryLockVoidCallbackSuccess() throws Exception {
        AtomicBoolean callbackExecuted = new AtomicBoolean(false);

        expect(mockLock.tryLock(10, TimeUnit.SECONDS)).andReturn(true);
        mockLock.unlock();
        expectLastCall();

        replay(mockLock);

        lockTemplate = new LockTemplate(10, TimeUnit.SECONDS, mockLock);
        lockTemplate.withTryLock(() -> callbackExecuted.set(true));

        assertTrue(callbackExecuted.get());
        verify(mockLock);
    }

    @Test(
            expectedExceptions = RuntimeException.class,
            expectedExceptionsMessageRegExp = "Failed to acquire lock")
    public void testWithTryLockVoidCallbackFailedToAcquireLock() throws Exception {
        expect(mockLock.tryLock(10, TimeUnit.SECONDS)).andReturn(false);

        replay(mockLock);

        lockTemplate = new LockTemplate(10, TimeUnit.SECONDS, mockLock);
        lockTemplate.withTryLock(() -> {});

        verify(mockLock);
    }

    @Test
    public void testWithTryLockVoidCallbackInterrupted() throws Exception {
        expect(mockLock.tryLock(10, TimeUnit.SECONDS)).andThrow(new InterruptedException());

        replay(mockLock);

        lockTemplate = new LockTemplate(10, TimeUnit.SECONDS, mockLock);

        try {
            lockTemplate.withTryLock(() -> {});
            fail("Expected RuntimeException to be thrown");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("interrupted while acquiring the lock"));
            assertTrue(Thread.interrupted(), "Thread interrupt flag should be set");
        }

        verify(mockLock);
    }

    @Test
    public void testWithTryLockReturningValueSuccess() throws Exception {
        String expectedResult = "test-result";

        expect(mockLock.tryLock(10, TimeUnit.SECONDS)).andReturn(true);
        mockLock.unlock();
        expectLastCall();

        replay(mockLock);

        lockTemplate = new LockTemplate(10, TimeUnit.SECONDS, mockLock);
        String result = lockTemplate.withTryLock(() -> expectedResult);

        assertEquals(result, expectedResult);
        verify(mockLock);
    }

    @Test(
            expectedExceptions = RuntimeException.class,
            expectedExceptionsMessageRegExp = "Failed to acquire lock")
    public void testWithTryLockReturningValueFailedToAcquireLock() throws Exception {
        expect(mockLock.tryLock(10, TimeUnit.SECONDS)).andReturn(false);

        replay(mockLock);

        lockTemplate = new LockTemplate(10, TimeUnit.SECONDS, mockLock);
        lockTemplate.withTryLock(() -> "should not execute");

        verify(mockLock);
    }

    @Test
    public void testWithTryLockWithCustomTimeoutSuccess() throws Exception {
        Integer expectedResult = 42;

        expect(mockLock.tryLock(5, TimeUnit.MINUTES)).andReturn(true);
        mockLock.unlock();
        expectLastCall();

        replay(mockLock);

        lockTemplate = new LockTemplate(10, TimeUnit.SECONDS, mockLock);
        Integer result = lockTemplate.withTryLock(5, TimeUnit.MINUTES, () -> expectedResult);

        assertEquals(result, expectedResult);
        verify(mockLock);
    }

    @Test(
            expectedExceptions = RuntimeException.class,
            expectedExceptionsMessageRegExp = "Failed to acquire lock")
    public void testWithTryLockWithCustomTimeoutFailedToAcquireLock() throws Exception {
        expect(mockLock.tryLock(3, TimeUnit.MILLISECONDS)).andReturn(false);

        replay(mockLock);

        lockTemplate = new LockTemplate(10, TimeUnit.SECONDS, mockLock);
        lockTemplate.withTryLock(3, TimeUnit.MILLISECONDS, () -> "should not execute");

        verify(mockLock);
    }

    @Test
    public void testWithTryLockUnlocksOnException() throws Exception {
        expect(mockLock.tryLock(10, TimeUnit.SECONDS)).andReturn(true);
        mockLock.unlock();
        expectLastCall();

        replay(mockLock);

        lockTemplate = new LockTemplate(10, TimeUnit.SECONDS, mockLock);

        try {
            lockTemplate.withTryLock(
                    () -> {
                        throw new IllegalStateException("Test exception");
                    });
            fail("Expected IllegalStateException to be thrown");
        } catch (IllegalStateException e) {
            assertEquals(e.getMessage(), "Test exception");
        }

        verify(mockLock);
    }

    @Test
    public void testWithTryLockInterruptibleVoidCallbackSuccess() throws Exception {
        AtomicBoolean callbackExecuted = new AtomicBoolean(false);

        expect(mockLock.tryLock(10, TimeUnit.SECONDS)).andReturn(true);
        mockLock.unlock();
        expectLastCall();

        replay(mockLock);

        lockTemplate = new LockTemplate(10, TimeUnit.SECONDS, mockLock);
        lockTemplate.withTryLockInterruptible(() -> callbackExecuted.set(true));

        assertTrue(callbackExecuted.get());
        verify(mockLock);
    }

    @Test(
            expectedExceptions = RuntimeException.class,
            expectedExceptionsMessageRegExp = "Failed to acquire lock")
    public void testWithTryLockInterruptibleVoidCallbackFailedToAcquireLock() throws Exception {
        expect(mockLock.tryLock(10, TimeUnit.SECONDS)).andReturn(false);

        replay(mockLock);

        lockTemplate = new LockTemplate(10, TimeUnit.SECONDS, mockLock);
        lockTemplate.withTryLockInterruptible(() -> {});

        verify(mockLock);
    }

    @Test(expectedExceptions = InterruptedException.class)
    public void testWithTryLockInterruptibleVoidCallbackInterrupted() throws Exception {
        expect(mockLock.tryLock(10, TimeUnit.SECONDS)).andThrow(new InterruptedException());

        replay(mockLock);

        lockTemplate = new LockTemplate(10, TimeUnit.SECONDS, mockLock);
        lockTemplate.withTryLockInterruptible(() -> {});

        verify(mockLock);
    }

    @Test
    public void testWithTryLockInterruptibleReturningValueSuccess() throws Exception {
        Long expectedResult = 123L;

        expect(mockLock.tryLock(10, TimeUnit.SECONDS)).andReturn(true);
        mockLock.unlock();
        expectLastCall();

        replay(mockLock);

        lockTemplate = new LockTemplate(10, TimeUnit.SECONDS, mockLock);
        Long result = lockTemplate.withTryLockInterruptible(() -> expectedResult);

        assertEquals(result, expectedResult);
        verify(mockLock);
    }

    @Test(
            expectedExceptions = RuntimeException.class,
            expectedExceptionsMessageRegExp = "Failed to acquire lock")
    public void testWithTryLockInterruptibleReturningValueFailedToAcquireLock() throws Exception {
        expect(mockLock.tryLock(10, TimeUnit.SECONDS)).andReturn(false);

        replay(mockLock);

        lockTemplate = new LockTemplate(10, TimeUnit.SECONDS, mockLock);
        lockTemplate.withTryLockInterruptible(() -> "should not execute");

        verify(mockLock);
    }

    @Test
    public void testWithTryLockInterruptibleWithCustomTimeoutSuccess() throws Exception {
        Double expectedResult = 3.14;

        expect(mockLock.tryLock(2, TimeUnit.HOURS)).andReturn(true);
        mockLock.unlock();
        expectLastCall();

        replay(mockLock);

        lockTemplate = new LockTemplate(10, TimeUnit.SECONDS, mockLock);
        Double result =
                lockTemplate.withTryLockInterruptible(2, TimeUnit.HOURS, () -> expectedResult);

        assertEquals(result, expectedResult);
        verify(mockLock);
    }

    @Test(
            expectedExceptions = RuntimeException.class,
            expectedExceptionsMessageRegExp = "Failed to acquire lock")
    public void testWithTryLockInterruptibleWithCustomTimeoutFailedToAcquireLock()
            throws Exception {
        expect(mockLock.tryLock(100, TimeUnit.NANOSECONDS)).andReturn(false);

        replay(mockLock);

        lockTemplate = new LockTemplate(10, TimeUnit.SECONDS, mockLock);
        lockTemplate.withTryLockInterruptible(
                100, TimeUnit.NANOSECONDS, () -> "should not execute");

        verify(mockLock);
    }

    @Test
    public void testWithTryLockInterruptibleUnlocksOnException() throws Exception {
        expect(mockLock.tryLock(10, TimeUnit.SECONDS)).andReturn(true);
        mockLock.unlock();
        expectLastCall();

        replay(mockLock);

        lockTemplate = new LockTemplate(10, TimeUnit.SECONDS, mockLock);

        try {
            lockTemplate.withTryLockInterruptible(
                    () -> {
                        throw new IllegalArgumentException("Test exception in interruptible");
                    });
            fail("Expected IllegalArgumentException to be thrown");
        } catch (IllegalArgumentException e) {
            assertEquals(e.getMessage(), "Test exception in interruptible");
        }

        verify(mockLock);
    }

    @Test
    public void testWithTryLockInterruptibleUnlocksOnInterruptedException() throws Exception {
        expect(mockLock.tryLock(10, TimeUnit.SECONDS)).andReturn(true);
        mockLock.unlock();
        expectLastCall();

        replay(mockLock);

        lockTemplate = new LockTemplate(10, TimeUnit.SECONDS, mockLock);

        try {
            lockTemplate.withTryLockInterruptible(
                    () -> {
                        throw new InterruptedException("Interrupted during execution");
                    });
            fail("Expected InterruptedException to be thrown");
        } catch (InterruptedException e) {
            assertEquals(e.getMessage(), "Interrupted during execution");
        }

        verify(mockLock);
    }

    @Test
    public void testMultipleOperationsWithSameLockTemplate() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);

        expect(mockLock.tryLock(10, TimeUnit.SECONDS)).andReturn(true).times(3);
        mockLock.unlock();
        expectLastCall().times(3);

        replay(mockLock);

        lockTemplate = new LockTemplate(10, TimeUnit.SECONDS, mockLock);

        lockTemplate.withTryLock(() -> counter.incrementAndGet());
        lockTemplate.withTryLock(() -> counter.incrementAndGet());
        lockTemplate.withTryLock(() -> counter.incrementAndGet());

        assertEquals(counter.get(), 3);
        verify(mockLock);
    }

    @Test
    public void testWithTryLockVoidCallbackWithCheckedException() throws Exception {
        expect(mockLock.tryLock(10, TimeUnit.SECONDS)).andReturn(true);
        mockLock.unlock();
        expectLastCall();

        replay(mockLock);

        lockTemplate = new LockTemplate(10, TimeUnit.SECONDS, mockLock);

        try {
            lockTemplate.withTryLock(
                    () -> {
                        throw new java.io.IOException("Test IO exception");
                    });
            fail("Expected IOException to be thrown");
        } catch (java.io.IOException e) {
            assertEquals(e.getMessage(), "Test IO exception");
        }

        verify(mockLock);
    }

    @Test
    public void testWithTryLockReturningValueWithCheckedException() throws Exception {
        expect(mockLock.tryLock(10, TimeUnit.SECONDS)).andReturn(true);
        mockLock.unlock();
        expectLastCall();

        replay(mockLock);

        lockTemplate = new LockTemplate(10, TimeUnit.SECONDS, mockLock);

        try {
            lockTemplate.withTryLock(
                    () -> {
                        throw new java.sql.SQLException("Test SQL exception");
                    });
            fail("Expected SQLException to be thrown");
        } catch (java.sql.SQLException e) {
            assertEquals(e.getMessage(), "Test SQL exception");
        }

        verify(mockLock);
    }
}
