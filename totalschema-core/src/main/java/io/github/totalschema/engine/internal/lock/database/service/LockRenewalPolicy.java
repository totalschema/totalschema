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

import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * Policy for determining when a lock should be renewed.
 *
 * <p>This class encapsulates the logic for calculating lock expiration times and determining when
 * renewal is needed based on time-to-live (TTL) settings.
 */
final class LockRenewalPolicy {

    private final Duration lockTimeToLive;
    private final Duration renewalThreshold;

    /**
     * Creates a renewal policy with the specified TTL.
     *
     * @param lockTimeToLive how long the lock remains valid
     */
    LockRenewalPolicy(Duration lockTimeToLive) {

        Objects.requireNonNull(lockTimeToLive, "lockTimeToLive");

        this.lockTimeToLive = lockTimeToLive;
        // Renew after 1/4 of TTL has elapsed
        this.renewalThreshold = lockTimeToLive.dividedBy(4);
    }

    /**
     * Calculates the expiration time for a new lock.
     *
     * @return the expiration time (now + TTL)
     */
    ZonedDateTime calculateExpiration() {
        return ZonedDateTime.now().plus(lockTimeToLive).withZoneSameInstant(ZoneOffset.UTC);
    }

    /**
     * Determines if a lock should be renewed based on its current expiration time.
     *
     * @param currentExpiration when the lock currently expires
     * @return true if renewal is needed
     */
    boolean shouldRenew(ZonedDateTime currentExpiration) {
        if (currentExpiration == null) {
            return false;
        }

        // Calculate when the lock was originally acquired
        ZonedDateTime acquisitionTime = currentExpiration.minus(lockTimeToLive);

        // Calculate when renewal should happen (acquisition + threshold)
        ZonedDateTime renewalTime = acquisitionTime.plus(renewalThreshold);

        // Renew if we've passed the renewal time
        return ZonedDateTime.now().isAfter(renewalTime);
    }

    Duration getLockTimeToLive() {
        return lockTimeToLive;
    }

    Duration getRenewalThreshold() {
        return renewalThreshold;
    }
}
