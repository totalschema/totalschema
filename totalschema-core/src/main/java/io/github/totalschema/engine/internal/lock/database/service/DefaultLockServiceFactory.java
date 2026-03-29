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

import io.github.totalschema.config.Configuration;
import io.github.totalschema.engine.api.Context;
import io.github.totalschema.engine.internal.lock.database.LockingComponentFactory;
import io.github.totalschema.engine.internal.lock.database.LockingConstants;
import io.github.totalschema.engine.internal.lock.database.repository.spi.LockStateRepository;
import io.github.totalschema.spi.ArgumentSpecification;
import io.github.totalschema.spi.lock.LockService;
import java.util.List;
import java.util.Optional;

public class DefaultLockServiceFactory extends LockingComponentFactory<LockService> {

    @Override
    public boolean isLazy() {
        return false;
    }

    @Override
    public Class<LockService> getComponentType() {
        return LockService.class;
    }

    @Override
    public Optional<String> getQualifier() {
        return Optional.empty();
    }

    @Override
    public List<Class<?>> getRequiredContextTypes() {
        return List.of(Configuration.class);
    }

    @Override
    public List<ArgumentSpecification<?>> getArgumentSpecifications() {
        return List.of();
    }

    @Override
    public LockService createComponent(Context context, List<Object> arguments) {

        Configuration lockConfig =
                context.get(Configuration.class)
                        .getPrefixNamespace(LockingConstants.PROPERTY_NAMESPACE);

        LockStateRepository lockStateRepository = context.get(LockStateRepository.class);

        Configuration configuration =
                lockConfig.getPrefixNamespace(DefaultDatabaseLockService.CONFIG_PREFIX);

        return new DefaultDatabaseLockService(lockStateRepository, configuration);
    }
}
