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

import io.github.totalschema.engine.internal.lock.database.repository.spi.LockStateRepository;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles database operations for lock management.
 *
 * <p>This class wraps the {@link LockStateRepository} and provides higher-level operations with
 * proper exception handling and logging.
 */
final class DatabaseLockOperations {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseLockOperations.class);

    private final String lockId;
    private final LockStateRepository lockStateRepository;

    /**
     * Creates database lock operations for the specified lock ID.
     *
     * @param lockId the unique identifier for this lock, must not be null
     * @param lockStateRepository the repository for lock state persistence, must not be null
     * @throws NullPointerException if lockId or lockStateRepository is null
     */
    DatabaseLockOperations(String lockId, LockStateRepository lockStateRepository) {
        this.lockId = Objects.requireNonNull(lockId, "lockId must not be null");
        this.lockStateRepository =
                Objects.requireNonNull(lockStateRepository, "lockStateRepository must not be null");
    }

    /**
     * Attempts to acquire the lock in the database.
     *
     * @param expiration the expiration time for the lock, must not be null
     * @return true if the lock was acquired, false if it's held by another process
     * @throws NullPointerException if expiration is null
     */
    boolean tryAcquire(ZonedDateTime expiration) {
        Objects.requireNonNull(expiration, "expiration must not be null");

        try {
            logger.trace("Attempting to acquire lock in database: lockId={}", lockId);

            boolean acquired =
                    lockStateRepository.updateIdAndExpirationIfOwnerIsNullOrExpirationIsReached(
                            lockId, expiration);

            if (acquired) {
                logger.debug(
                        "Lock acquired in database: lockId={}, expiration={}", lockId, expiration);
            } else {
                logger.debug("Lock acquisition failed - held by another process");
            }

            return acquired;

        } catch (SQLException ex) {
            throw new RuntimeException("Failed to acquire lock in database", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while acquiring lock", ex);
        }
    }

    /**
     * Renews the lock expiration time in the database.
     *
     * @param expiration the new expiration time, must not be null
     * @throws NullPointerException if expiration is null
     */
    void renew(ZonedDateTime expiration) {
        Objects.requireNonNull(expiration, "expiration must not be null");

        try {
            logger.info("Renewing lock: lockId={}, newExpiration={}", lockId, expiration);

            boolean renewed = lockStateRepository.updateLockExpiration(lockId, expiration);

            if (!renewed) {
                throw new IllegalStateException(
                        "Failed to renew lock in database - lock may have been stolen: lockId="
                                + lockId);
            }

        } catch (SQLException ex) {
            throw new RuntimeException("Failed to renew lock in database", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while renewing lock", ex);
        }
    }

    /** Releases the lock in the database. */
    void release() {
        try {
            logger.debug("Releasing lock in database: lockId={}", lockId);

            lockStateRepository.updateIdToNull(lockId);

        } catch (SQLException ex) {
            throw new RuntimeException("Failed to release lock in database", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while releasing lock", ex);
        }
    }
}
