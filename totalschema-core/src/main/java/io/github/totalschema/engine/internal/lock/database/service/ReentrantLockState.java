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

package io.github.totalschema.engine.internal.lock.database.service;

import io.github.totalschema.concurrent.LockTemplate;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Tracks the reentrant state of a lock acquisition.
 *
 * <p>This class manages the count of nested lock acquisitions and the expiration time of the
 * currently held lock. It ensures that locks can be acquired multiple times by the same process
 * without deadlock.
 *
 * <p><strong>Thread Safety:</strong> This class is thread-safe and can be used independently.
 */
final class ReentrantLockState {

    private final LockTemplate lockTemplate =
            new LockTemplate(1, TimeUnit.MINUTES, new ReentrantLock());

    private final AtomicInteger acquiredCount = new AtomicInteger(0);
    private final AtomicReference<ZonedDateTime> lockExpiration = new AtomicReference<>(null);

    /**
     * Checks if the lock is not currently held.
     *
     * @return true if the lock is not held
     */
    boolean isNotHeld() {
        return acquiredCount.get() == 0;
    }

    /**
     * Checks if the lock is currently held (at least once).
     *
     * @return true if the lock is held
     */
    boolean isHeld() {
        return acquiredCount.get() > 0;
    }

    /**
     * Gets the current acquired count.
     *
     * @return the number of times the lock has been acquired
     */
    int getAcquiredCount() {
        return acquiredCount.get();
    }

    /**
     * Gets the current lock expiration time.
     *
     * @return the expiration time, or null if not held
     */
    ZonedDateTime getLockExpiration() {
        return lockExpiration.get();
    }

    /**
     * Increments the acquired count and updates the expiration time.
     *
     * @param newExpiration the new expiration time, must not be null
     * @throws NullPointerException if newExpiration is null
     * @throws RuntimeException if unable to acquire lock within timeout
     */
    void acquire(ZonedDateTime newExpiration) {
        Objects.requireNonNull(newExpiration, "newExpiration must not be null");

        lockTemplate.withTryLock(
                () -> {
                    acquiredCount.incrementAndGet();
                    lockExpiration.set(newExpiration);
                });
    }

    /**
     * Decrements the acquired count.
     *
     * @throws IllegalStateException if the lock is not currently held
     * @throws RuntimeException if unable to acquire lock within timeout
     */
    void release() {
        lockTemplate.withTryLock(
                () -> {
                    if (acquiredCount.get() == 0) {
                        throw new IllegalStateException("The lock is not held!");
                    }
                    acquiredCount.decrementAndGet();

                    if (acquiredCount.get() == 0) {
                        lockExpiration.set(null);
                    }
                });
    }

    /**
     * Updates the expiration time for an already-held lock (used during renewal).
     *
     * @param newExpiration the new expiration time, must not be null
     * @throws NullPointerException if newExpiration is null
     * @throws RuntimeException if unable to acquire lock within timeout
     */
    void updateExpiration(ZonedDateTime newExpiration) {
        Objects.requireNonNull(newExpiration, "newExpiration must not be null");

        lockTemplate.withTryLock(() -> lockExpiration.set(newExpiration));
    }
}
