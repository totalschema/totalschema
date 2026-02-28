package io.github.totalschema.spi;

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

        List<Class<?>> requiredContextTypes = getRequiredContextTypes();

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
