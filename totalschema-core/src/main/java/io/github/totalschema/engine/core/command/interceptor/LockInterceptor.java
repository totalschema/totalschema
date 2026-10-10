/*
 * totalschema: tool for managing database versioning and schema changes with ease.
 * Copyright (C) 2025-2026 totalschema development team
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

package io.github.totalschema.engine.core.command.interceptor;

import io.github.totalschema.config.Configuration;
import io.github.totalschema.engine.api.Context;
import io.github.totalschema.engine.core.command.api.*;
import io.github.totalschema.model.LockRecord;
import io.github.totalschema.spi.lock.LockService;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LockInterceptor extends CommandInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(LockInterceptor.class);

    private static final long DEFAULT_TIMEOUT = 2;
    private static final TimeUnit DEFAULT_TIME_UNIT = TimeUnit.MINUTES;

    public LockInterceptor(CommandExecutor next) {
        super(next);
    }

    @Override
    public <R> R intercept(Context context, Command<R> command, CommandExecutor next) {

        try {
            R result;

            if (context.has(LockService.class)) {

                LockService lockService = context.get(LockService.class);

                result = executeWithLockService(lockService, context, command, next);

            } else {
                result = next.execute(context, command);
            }

            return result;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    private <R> R executeWithLockService(
            LockService lockService, Context context, Command<R> command, CommandExecutor next)
            throws InterruptedException {

        R result;

        Configuration lockConfig = context.get(Configuration.class).getPrefixNamespace("lock");

        long lockTimeout = lockConfig.getLong("timeout").orElse(DEFAULT_TIMEOUT);
        TimeUnit lockTimeoutTimeUnit =
                lockConfig.getEnumValue(TimeUnit.class, "timeUnit").orElse(DEFAULT_TIME_UNIT);

        logger.info(
                "Attempting to acquire change lock for command '{}' with {} {} timeout",
                command.getClass().getSimpleName(),
                lockTimeout,
                lockTimeoutTimeUnit.name().toLowerCase(Locale.ENGLISH));

        long lockAcquisitionStart = System.currentTimeMillis();
        boolean couldLock = lockService.tryLock(lockTimeout, lockTimeoutTimeUnit);

        if (couldLock) {
            long lockAcquisitionTime = System.currentTimeMillis() - lockAcquisitionStart;
            logger.info("Change lock acquired successfully in {} ms", lockAcquisitionTime);

            try {
                result = next.execute(context, command);
                logger.debug("Command execution completed successfully under lock");

            } finally {
                lockService.unlock();
                logger.info("Change lock released");
            }

        } else {
            logger.warn(
                    "Failed to acquire change lock within {} {}", lockTimeout, lockTimeoutTimeUnit);

            LockRecord lockRecord = lockService.getLock();
            logger.warn(
                    "Lock is currently held by user '{}' with lock ID '{}' and will expire at {}",
                    lockRecord.getLockedByUserId(),
                    lockRecord.getLockId(),
                    lockRecord.getLockExpiration());

            throw new IllegalStateException(
                    String.format(
                            "Could not acquire change lock within %d %s. "
                                    + "Lock is held by user '%s' (ID: '%s') and expires at '%s'. Please retry later or manually release the stale lock.",
                            lockTimeout,
                            lockTimeoutTimeUnit,
                            lockRecord.getLockedByUserId(),
                            lockRecord.getLockId(),
                            lockRecord.getLockExpiration()));
        }

        return result;
    }
}
