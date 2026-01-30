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

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

public final class LockTemplate {

    public interface VoidLockCallback<E extends Throwable> {
        void execute() throws E;
    }

    public interface LockCallback<R, E extends Throwable> {
        R execute() throws E;
    }

    public interface InterruptibleVoidLockCallback<E extends Throwable> {
        void execute() throws E, InterruptedException;
    }

    public interface InterruptibleLockCallback<R, E extends Throwable> {
        R execute() throws E, InterruptedException;
    }

    private final long defaultTimeout;
    private final TimeUnit defaultTimeUnit;

    private final Lock lock;

    public LockTemplate(long defaultTimeout, TimeUnit defaultTimeUnit, Lock lock) {

        Objects.requireNonNull(lock, "Argument lock cannot be null");
        this.lock = lock;

        if (defaultTimeout <= 0) {
            throw new RuntimeException(
                    "defaultTimeout cannot be negative or zero; was: " + defaultTimeout);
        }
        this.defaultTimeout = defaultTimeout;

        Objects.requireNonNull(defaultTimeUnit, "Argument defaultTimeUnit cannot be null");
        this.defaultTimeUnit = defaultTimeUnit;
    }

    public <E extends Throwable> void withTryLock(VoidLockCallback<E> lockCallback) throws E {
        withTryLock(
                defaultTimeout,
                defaultTimeUnit,
                new LockCallback<Void, E>() {
                    @Override
                    public Void execute() throws E {

                        lockCallback.execute();

                        return null;
                    }
                });
    }

    public <R, E extends Throwable> R withTryLock(LockCallback<R, E> lockCallback) throws E {
        return withTryLock(defaultTimeout, defaultTimeUnit, lockCallback);
    }

    public <R, E extends Throwable> R withTryLock(
            long timeout, TimeUnit timeUnit, LockCallback<R, E> lockCallback) throws E {
        try {
            boolean couldLock = lock.tryLock(timeout, timeUnit);
            if (!couldLock) {
                throw new RuntimeException("Failed to acquire lock");
            }

            try {
                return lockCallback.execute();

            } finally {
                lock.unlock();
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            throw new RuntimeException("interrupted while acquiring the lock", e);
        }
    }

    public <E extends Throwable> void withTryLockInterruptible(
            InterruptibleVoidLockCallback<E> lockCallback) throws E, InterruptedException {

        withTryLockInterruptible(
                defaultTimeout,
                defaultTimeUnit,
                new InterruptibleLockCallback<Void, E>() {
                    @Override
                    public Void execute() throws E, InterruptedException {

                        lockCallback.execute();

                        return null;
                    }
                });
    }

    public <R, E extends Throwable> R withTryLockInterruptible(
            InterruptibleLockCallback<R, E> lockCallback) throws E, InterruptedException {

        return withTryLockInterruptible(defaultTimeout, defaultTimeUnit, lockCallback);
    }

    public <R, E extends Throwable> R withTryLockInterruptible(
            long timeout, TimeUnit timeUnit, InterruptibleLockCallback<R, E> lockCallback)
            throws E, InterruptedException {

        boolean couldLock = lock.tryLock(timeout, timeUnit);
        if (!couldLock) {
            throw new RuntimeException("Failed to acquire lock");
        }

        try {
            return lockCallback.execute();

        } finally {
            lock.unlock();
        }
    }
}
