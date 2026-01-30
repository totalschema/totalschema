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
 * <p>Implementations provide locking to prevent concurrent change execution.
 */
public interface LockService {

    /**
     * Attempts to acquire a lock within the specified timeout.
     *
     * @param timeout the maximum time to wait for the lock
     * @param timeUnit the time unit of the timeout
     * @return true if the lock was acquired, false otherwise
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
