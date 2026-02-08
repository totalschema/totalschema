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

package io.github.totalschema.spi;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ServiceLoaderFactory {

    private static final Logger logger = LoggerFactory.getLogger(ServiceLoaderFactory.class);

    private static final ConcurrentHashMap<Class<?>, List<Object>> SERVICE_CACHE =
            new ConcurrentHashMap<>();

    private ServiceLoaderFactory() {
        throw new AssertionError("static utility class, no instances allowed");
    }

    public static <S> Optional<S> getSingleService(Class<S> serviceClass) {

        List<S> services = getAllServices(serviceClass);

        switch (services.size()) {
            case 0:
                logger.debug("Found no loadable implementation for {}", serviceClass);
                return Optional.empty();
            case 1:
                S service = services.get(0);
                logger.debug(
                        "Found single loadable implementation for {}: {}",
                        serviceClass,
                        service.getClass());
                return Optional.of(service);

            default:
                logger.error(
                        "Found multiple loadable implementations for {}: {}",
                        serviceClass,
                        services);
                throw new IllegalStateException(
                        String.format(
                                "Multiple ServiceLoader services found for %s: %s",
                                serviceClass, services));
        }
    }

    @SuppressWarnings("unchecked") // code ensures this cast is possible
    public static <S> List<S> getAllServices(Class<S> serviceClass) {

        logger.debug("Loading {}", serviceClass);

        return (List<S>)
                SERVICE_CACHE.computeIfAbsent(serviceClass, ServiceLoaderFactory::loadServices);
    }

    private static <S> List<Object> loadServices(Class<S> serviceClass) {

        ServiceLoader<S> loadedServices = ServiceLoader.load(serviceClass);

        LinkedList<S> services = new LinkedList<>();

        for (S service : loadedServices) {
            logger.debug("Found loadable {} implementation: {}", serviceClass, service.getClass());
            services.add(service);
        }

        return Collections.unmodifiableList(services);
    }
}
