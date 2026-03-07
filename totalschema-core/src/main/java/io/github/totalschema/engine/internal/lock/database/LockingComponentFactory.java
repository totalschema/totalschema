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

import static io.github.totalschema.engine.internal.lock.database.LockingConstants.PROPERTY_NAMESPACE;

import io.github.totalschema.config.Configuration;
import io.github.totalschema.engine.core.container.FactorySpecification;
import io.github.totalschema.engine.core.container.ObjectSpecification;
import io.github.totalschema.spi.ComponentFactory;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class LockingComponentFactory<T> extends ComponentFactory<T> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public boolean isEnabled(
            Map<ObjectSpecification, Object> objects,
            Map<FactorySpecification, ComponentFactory<?>> factories) {

        ObjectSpecification configSpecification =
                ObjectSpecification.from(Configuration.class, null);

        Configuration configuration = (Configuration) objects.get(configSpecification);

        boolean enabled = configuration.getString(PROPERTY_NAMESPACE, "type").isPresent();

        logger.debug("Locking is {}, and so is {}", enabled ? "enabled" : "disabled", getClass());

        return enabled;
    }
}
