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

package io.github.totalschema.spi.factory;

import io.github.totalschema.engine.core.container.FactorySpecification;
import io.github.totalschema.engine.core.container.ObjectSpecification;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ConditionalComponentFactory<T> extends ComponentFactory<T> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public boolean isEnabled(
            Map<ObjectSpecification, Object> objects,
            Map<FactorySpecification, ComponentFactory<?>> factories) {

        Set<Class<?>> providedObjectsClasses =
                objects.keySet().stream()
                        .map(ObjectSpecification::getType)
                        .collect(Collectors.toSet());

        Set<Class<?>> factoryContributedClasses =
                factories.keySet().stream()
                        .map(FactorySpecification::getConstructedClass)
                        .collect(Collectors.toSet());

        HashSet<Class<?>> classesKnownInContext = new HashSet<>();
        classesKnownInContext.addAll(providedObjectsClasses);
        classesKnownInContext.addAll(factoryContributedClasses);

        List<Class<?>> requiredContextTypes = getDependencies();

        for (Class<?> requiredClass : requiredContextTypes) {
            if (!classesKnownInContext.contains(requiredClass)) {
                logger.debug(
                        "Factory {} is NOT enabled as required type {} is not provided.",
                        getClass().getName(),
                        requiredClass.getName());
                return false;
            }
        }

        logger.debug(
                "Factory {} is enabled as all required context types {} are provided.",
                getClass().getName(),
                requiredContextTypes);

        return true;
    }
}
