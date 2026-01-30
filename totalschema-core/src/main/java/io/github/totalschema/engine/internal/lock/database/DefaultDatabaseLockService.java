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

package io.github.totalschema.engine.internal.lock.database;

import io.github.totalschema.concurrent.LockTemplate;
import io.github.totalschema.config.Configuration;
import io.github.totalschema.engine.internal.lock.database.repository.spi.LockStateRepository;
import io.github.totalschema.model.LockRecord;
import io.github.totalschema.spi.lock.LockService;
import java.sql.SQLException;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DefaultDatabaseLockService implements LockService {

    public static final String CONFIG_PREFIX = "database";

    private static final int DEFAULT_TTL_TIMEOUT = 1;
    private static final TimeUnit DEFAULT_TTL_TIME_UNIT = TimeUnit.HOURS;

    private final Logger logger = LoggerFactory.getLogger(DefaultDatabaseLockService.class);

    private final String lockId = UUID.randomUUID().toString();

    private final LockTemplate mutexLockTemplate =
            new LockTemplate(1, TimeUnit.MINUTES, new ReentrantLock());

    private final Duration lockTimeToLive;
    private final Duration reAcquireLockAfterDuration;

    private int acquiredCount = 0;

    private final LockStateRepository lockStateRepository;

    private ZonedDateTime acquiredLockExpiration;

    public static DefaultDatabaseLockService newInstance(
            LockStateRepository lockStateRepository, Configuration configuration) {

        return new DefaultDatabaseLockService(lockStateRepository, configuration);
    }

    DefaultDatabaseLockService(
            LockStateRepository lockStateRepository, Configuration configuration) {

        this.lockStateRepository = lockStateRepository;

        long timeToLiveTimeout =
                configuration.getInt("lock.ttl.timeout").orElse(DEFAULT_TTL_TIMEOUT);

        TimeUnit timeToLiveTimeUnit =
                configuration
                        .getEnumValue(TimeUnit.class, "lock.ttl.timeUnit")
                        .orElse(DEFAULT_TTL_TIME_UNIT);

        lockTimeToLive = Duration.of(timeToLiveTimeout, timeToLiveTimeUnit.toChronoUnit());
        reAcquireLockAfterDuration = lockTimeToLive.dividedBy(4);
    }

    @Override
    public boolean tryLock(long timeout, TimeUnit timeUnit) throws InterruptedException {

        logger.debug("tryLock({},{})", timeout, timeUnit);

        return mutexLockTemplate.withTryLock(
                timeout, timeUnit, this::tryLockWithLocalMutexAcquired);
    }

    private boolean tryLockWithLocalMutexAcquired() throws InterruptedException {

        boolean couldLock;

        if (acquiredCount == 0) {
            couldLock = acquireLockInDatabase();

        } else {

            ZonedDateTime lockAcquiredDateTime = acquiredLockExpiration.minus(lockTimeToLive);
            ZonedDateTime pointInTimeAfterLockShouldBeReNewed =
                    lockAcquiredDateTime.plus(reAcquireLockAfterDuration);

            if (ZonedDateTime.now().isAfter(pointInTimeAfterLockShouldBeReNewed)) {

                logger.trace(
                        "Renewing lock in the remote database as requestedCount={}, acquiredLockExpiration={}",
                        acquiredCount,
                        acquiredLockExpiration);

                this.acquiredLockExpiration = renewLockInDatabase();

            } else {
                logger.trace(
                        "NOT locking the remote database as requestedCount={}, acquiredLockExpiration={}",
                        acquiredCount,
                        acquiredLockExpiration);
            }

            couldLock = true;
        }

        if (couldLock) {
            // only increment if lock could be acquired!
            ++acquiredCount;
        }

        logger.debug("acquiredCount={}; couldLock={} ", acquiredCount, couldLock);

        return couldLock;
    }

    private boolean acquireLockInDatabase() throws InterruptedException {

        try {
            boolean couldLock;

            ZonedDateTime lockExpiration = getLockExpiration();

            logger.trace(
                    "Attempting to lock the remote database as acquiredCount={}", acquiredCount);
            couldLock =
                    lockStateRepository.updateIdAndExpirationIfOwnerIsNullOrExpirationIsReached(
                            lockId, lockExpiration);

            if (couldLock) {
                this.acquiredLockExpiration = lockExpiration;
            }

            return couldLock;

        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    private ZonedDateTime renewLockInDatabase() throws InterruptedException {
        try {
            ZonedDateTime lockExpiration = getLockExpiration();

            logger.info("Renewing lock expiration to {} for: {}", lockExpiration, lockId);
            boolean couldRenew = lockStateRepository.updateLockExpiration(lockId, lockExpiration);

            if (!couldRenew) {
                throw new IllegalStateException(
                        "Failure renewing lock in database for lock Id: " + lockId);
            }

            return lockExpiration;

        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    private ZonedDateTime getLockExpiration() {
        return ZonedDateTime.now().plus(this.lockTimeToLive).withZoneSameInstant(ZoneOffset.UTC);
    }

    @Override
    public void unlock() {

        mutexLockTemplate.withTryLock(this::unlockWithLocalMutexAcquired);
    }

    private void unlockWithLocalMutexAcquired() {

        try {
            if (acquiredCount == 0) {
                throw new IllegalStateException("The lock is not held!");
            }

            --acquiredCount;

            if (acquiredCount == 0) {
                logger.debug("Unlocking the remote database as acquiredCount={}", acquiredCount);

                acquiredLockExpiration = null;

                lockStateRepository.updateIdToNull(lockId);

            } else {
                logger.debug(
                        "NOT unlocking the remote database as acquiredCount={}", acquiredCount);
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    @Override
    public LockRecord getLock() {
        return lockStateRepository.getLockRecord();
    }
}
