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
import io.github.totalschema.config.Configuration;
import io.github.totalschema.engine.internal.lock.database.repository.spi.LockStateRepository;
import io.github.totalschema.model.LockRecord;
import io.github.totalschema.spi.lock.LockService;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Database-backed implementation of {@link LockService} with reentrant lock support.
 *
 * <p>This service coordinates distributed locking across multiple processes using a database table
 * as the synchronization mechanism. It provides:
 *
 * <ul>
 *   <li>Reentrant locking within the same {@code DefaultDatabaseLockService} instance (typically
 *       one instance per {@code ChangeEngine})
 *   <li>Automatic lock renewal for long-running operations
 *   <li>Thread-safe lock acquisition and release
 *   <li>Stale lock cleanup via TTL expiration
 * </ul>
 *
 * <p><strong>Important:</strong> Each {@code DefaultDatabaseLockService} instance is identified by
 * a unique UUID. Multiple {@code ChangeEngine} instances within the same JVM will have separate
 * {@code DefaultDatabaseLockService} instances with different UUIDs, and thus will compete for the
 * database lock rather than sharing reentrant state.
 *
 * <h2>Orchestration Architecture</h2>
 *
 * <p>This class acts as an orchestrator that coordinates multiple specialized components:
 *
 * <ul>
 *   <li><strong>{@link LockRenewalPolicy}</strong> - Determines when locks need renewal based on
 *       TTL and elapsed time.
 *   <li><strong>{@link DatabaseLockOperations}</strong> - Handles all database I/O operations
 *       including lock acquisition, renewal, and release.
 *   <li><strong>{@link ReentrantLockState}</strong> - Manages the reentrant lock state including
 *       the acquisition count and current expiration time.
 *   <li><strong>{@link LockStateRepository}</strong> - Low-level database access layer for the lock
 *       table.
 * </ul>
 *
 * <h2>Example Usage</h2>
 *
 * <pre>{@code
 * // Lock acquisition
 * if (lockService.tryLock(2, TimeUnit.MINUTES)) {
 *     try {
 *         // Perform protected operations
 *         // Nested calls to tryLock() are safe (reentrant)
 *     } finally {
 *         lockService.unlock();
 *     }
 * }
 * }</pre>
 *
 * @see LockService
 * @see LockRenewalPolicy
 * @see DatabaseLockOperations
 * @see ReentrantLockState
 * @see LockStateRepository
 */
public final class DefaultDatabaseLockService implements LockService {

    public static final String CONFIG_PREFIX = "database";

    private static final int DEFAULT_TTL_TIMEOUT = 1;
    private static final TimeUnit DEFAULT_TTL_TIME_UNIT = TimeUnit.HOURS;

    private final Logger logger = LoggerFactory.getLogger(DefaultDatabaseLockService.class);

    private final String lockId = UUID.randomUUID().toString();

    private final LockTemplate mutexLockTemplate =
            new LockTemplate(1, TimeUnit.MINUTES, new ReentrantLock());

    private final LockRenewalPolicy renewalPolicy;
    private final DatabaseLockOperations databaseOperations;
    private final ReentrantLockState lockState;
    private final LockStateRepository lockStateRepository;

    /**
     * Constructs a database lock service with the specified repository and configuration.
     *
     * @param lockStateRepository the repository for lock state persistence, must not be null
     * @param configuration the configuration containing TTL settings, must not be null
     * @throws NullPointerException if lockStateRepository or configuration is null
     */
    public DefaultDatabaseLockService(
            LockStateRepository lockStateRepository, Configuration configuration) {

        this.lockStateRepository =
                Objects.requireNonNull(lockStateRepository, "lockStateRepository must not be null");
        Objects.requireNonNull(configuration, "configuration must not be null");

        long timeToLiveTimeout =
                configuration.getInt("lock.ttl.timeout").orElse(DEFAULT_TTL_TIMEOUT);

        TimeUnit timeToLiveTimeUnit =
                configuration
                        .getEnumValue(TimeUnit.class, "lock.ttl.timeUnit")
                        .orElse(DEFAULT_TTL_TIME_UNIT);

        Duration lockTimeToLive = Duration.of(timeToLiveTimeout, timeToLiveTimeUnit.toChronoUnit());

        this.renewalPolicy = new LockRenewalPolicy(lockTimeToLive);
        this.databaseOperations = new DatabaseLockOperations(lockId, lockStateRepository);
        this.lockState = new ReentrantLockState();
    }

    @Override
    public boolean tryLock(long timeout, TimeUnit timeUnit) throws InterruptedException {
        logger.debug("tryLock({}, {})", timeout, timeUnit);

        if (timeout <= 0) {
            throw new IllegalArgumentException("timeout must be greater than 0");
        }
        Objects.requireNonNull(timeUnit, "timeUnit must not be null");

        return mutexLockTemplate.withTryLock(
                timeout, timeUnit, this::tryLockWithLocalMutexAcquired);
    }

    /**
     * Attempts to acquire the lock while holding the local mutex.
     *
     * <p>This method implements the reentrant locking logic with automatic renewal.
     *
     * @return true if the lock was acquired or already held
     * @throws InterruptedException if interrupted while acquiring
     */
    private boolean tryLockWithLocalMutexAcquired() throws InterruptedException {
        boolean couldLock;

        if (lockState.isNotHeld()) {
            // First acquisition - try to get lock from database
            couldLock = tryAcquireNewLock();
        } else {
            // Already holding the lock - check if renewal is needed
            couldLock = tryReentrantLock();
        }

        if (couldLock) {
            logger.debug(
                    "Lock acquired: acquiredCount={}, expiration={}",
                    lockState.getAcquiredCount(),
                    lockState.getLockExpiration());
        }

        return couldLock;
    }

    /**
     * Attempts to acquire a new lock from the database.
     *
     * @return true if the lock was acquired
     */
    private boolean tryAcquireNewLock() {
        ZonedDateTime expiration = renewalPolicy.calculateExpiration();
        boolean acquired = databaseOperations.tryAcquire(expiration);

        if (acquired) {
            lockState.acquire(expiration);
        }

        return acquired;
    }

    /**
     * Handles reentrant lock acquisition with optional renewal.
     *
     * @return always true (lock is already held)
     */
    private boolean tryReentrantLock() {
        ZonedDateTime currentExpiration = lockState.getLockExpiration();

        if (renewalPolicy.shouldRenew(currentExpiration)) {
            logger.trace(
                    "Renewing lock: acquiredCount={}, currentExpiration={}",
                    lockState.getAcquiredCount(),
                    currentExpiration);

            ZonedDateTime newExpiration = renewalPolicy.calculateExpiration();
            databaseOperations.renew(newExpiration);
            lockState.updateExpiration(newExpiration);
        } else {
            logger.trace(
                    "Reentrant lock acquisition without renewal: acquiredCount={}",
                    lockState.getAcquiredCount());
        }

        lockState.acquire(currentExpiration); // Increment count
        return true;
    }

    @Override
    public void unlock() {
        mutexLockTemplate.withTryLock(this::unlockWithLocalMutexAcquired);
    }

    /**
     * Releases the lock while holding the local mutex.
     *
     * <p>Decrements the reentrant counter and releases the database lock only when the count
     * reaches zero.
     */
    private void unlockWithLocalMutexAcquired() {
        lockState.release(); // Will throw if not held

        if (lockState.isNotHeld()) {
            logger.debug(
                    "Releasing lock in database: acquiredCount={}", lockState.getAcquiredCount());
            databaseOperations.release();
        } else {
            logger.debug(
                    "Lock still held (reentrant): acquiredCount={}", lockState.getAcquiredCount());
        }
    }

    @Override
    public LockRecord getLock() {
        return lockStateRepository.getLockRecord();
    }

    @Override
    public String toString() {
        return "DefaultDatabaseLockService{"
                + "lockId='"
                + lockId
                + '\''
                + ", renewalPolicy="
                + renewalPolicy
                + ", databaseOperations="
                + databaseOperations
                + ", lockStateRepository="
                + lockStateRepository
                + ", lockState="
                + lockState
                + '}';
    }
}
