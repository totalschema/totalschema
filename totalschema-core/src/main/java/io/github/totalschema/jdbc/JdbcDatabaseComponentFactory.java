package io.github.totalschema.jdbc;

import static io.github.totalschema.spi.ArgumentSpecification.*;

import io.github.totalschema.config.Configuration;
import io.github.totalschema.engine.api.Context;
import io.github.totalschema.spi.ArgumentHandler;
import io.github.totalschema.spi.ArgumentSpecification;
import io.github.totalschema.spi.ComponentFactory;
import java.util.List;
import java.util.Optional;

public class JdbcDatabaseComponentFactory extends ComponentFactory<JdbcDatabase> {

    /**
     * ArgumentHandler for JdbcDatabase creation arguments. Encapsulates argument specifications and
     * provides type-safe accessors.
     */
    static class Arguments extends ArgumentHandler {

        private static final ArgumentSpecification<String> NAME =
                string("name").withConstraint(notBlank());

        private static final ArgumentSpecification<Configuration> CONFIGURATION =
                configuration("configuration");

        Arguments() {
            super(NAME, CONFIGURATION);
        }

        String getName(List<Object> args) {
            return getArgument(NAME, args);
        }

        Configuration getConfiguration(List<Object> args) {
            return getArgument(CONFIGURATION, args);
        }
    }

    private static final Arguments ARGUMENTS = new Arguments();

    @Override
    public boolean isLazy() {
        return false;
    }

    @Override
    public Class<JdbcDatabase> getComponentType() {
        return JdbcDatabase.class;
    }

    @Override
    public Optional<String> getQualifier() {
        return Optional.empty();
    }

    @Override
    public List<Class<?>> getRequiredContextTypes() {
        return List.of();
    }

    @Override
    public List<ArgumentSpecification<?>> getArgumentSpecifications() {
        return ARGUMENTS.getSpecifications();
    }

    @Override
    public JdbcDatabase createComponent(Context context, List<Object> arguments) {

        ARGUMENTS.validateStructure(arguments, getClass().getSimpleName());

        String name = ARGUMENTS.getName(arguments);
        Configuration configuration = ARGUMENTS.getConfiguration(arguments);

        return DefaultJdbcDatabase.newInstance(name, configuration);
    }
}
