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

package io.github.totalschema.spi.lock;

import io.github.totalschema.model.LockRecord;
import java.util.concurrent.TimeUnit;

/**
 * Service Provider Interface for distributed locking mechanisms.
 *
 * <p>Implementations provide <strong>reentrant</strong> locking to prevent concurrent change
 * execution. A lock that is already held by the current process can be acquired again without
 * blocking or deadlock. Each successful acquisition must be paired with a corresponding release.
 *
 * <h2>Critical Usage Contract</h2>
 *
 * <p><strong>The return value of {@link #tryLock(long, TimeUnit)} MUST be checked.</strong> If it
 * returns {@code false}, the lock was NOT acquired and protected operations MUST NOT proceed.
 * Ignoring this return value can lead to race conditions, data corruption, and inconsistent state.
 *
 * <h2>Reentrant Behavior</h2>
 *
 * <p>The lock is reentrant: if {@code tryLock()} is called multiple times on the same {@code
 * LockService} instance (typically one per {@code ChangeEngine}), the subsequent calls will succeed
 * immediately without blocking. The lock will only be fully released when {@code unlock()} has been
 * called the same number of times as successful {@code tryLock()} calls.
 *
 * <h2>Example Usage</h2>
 *
 * <pre>{@code
 * // Basic lock acquisition pattern - ALWAYS check return value
 * if (lockService.tryLock(2, TimeUnit.MINUTES)) {
 *     try {
 *         // Lock acquired - safe to perform protected operations
 *         applyChangeScripts();
 *     } finally {
 *         lockService.unlock();
 *     }
 * } else {
 *     // Lock NOT acquired - must not proceed with protected operations
 *     throw new LockException("Could not acquire lock within 2 minutes");
 * }
 * }</pre>
 *
 * <h3>Reentrant Lock Example</h3>
 *
 * <pre>{@code
 * // First acquisition
 * if (lockService.tryLock(2, TimeUnit.MINUTES)) {
 *     try {
 *         // Nested operation also needs the lock
 *         if (lockService.tryLock(2, TimeUnit.MINUTES)) {  // Succeeds immediately (reentrant)
 *             try {
 *                 // Perform nested operations
 *                 applyAdditionalChanges();
 *             } finally {
 *                 lockService.unlock();  // Decrements reentrant count
 *             }
 *         }
 *     } finally {
 *         lockService.unlock();  // Fully releases the lock
 *     }
 * }
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 *
 * <p>Implementations must be thread-safe and support concurrent calls from multiple threads within
 * the same process.
 *
 * @see io.github.totalschema.model.LockRecord
 */
public interface LockService {

    /**
     * Attempts to acquire a lock within the specified timeout.
     *
     * <p><strong>Important:</strong> The return value indicates whether the lock was successfully
     * acquired. If this method returns {@code false}, the lock was NOT acquired and the calling
     * code MUST NOT proceed with operations that depend on holding the lock. Failure to check this
     * return value can result in race conditions and data corruption.
     *
     * @param timeout the maximum time to wait for the lock
     * @param timeUnit the time unit of the timeout
     * @return {@code true} if the lock was successfully acquired, {@code false} if the lock could
     *     not be acquired within the specified timeout
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    boolean tryLock(long timeout, TimeUnit timeUnit) throws InterruptedException;

    /** Releases the lock held by the current thread. */
    void unlock();

    /**
     * Gets the current lock record information.
     *
     * @return the lock record
     */
    LockRecord getLock();
}
