package io.github.totalschema.jdbc;

import static io.github.totalschema.spi.factory.ArgumentSpecification.*;

import io.github.totalschema.config.Configuration;
import io.github.totalschema.engine.api.Context;
import io.github.totalschema.spi.factory.ArgumentHandler;
import io.github.totalschema.spi.factory.ArgumentSpecification;
import io.github.totalschema.spi.factory.ComponentFactory;
import java.util.List;
import java.util.Optional;

public class JdbcDatabaseComponentFactory extends ComponentFactory<JdbcDatabase> {

    private static final ArgumentSpecification<String> NAME =
            string("name").withConstraint(notBlank());

    private static final ArgumentSpecification<Configuration> CONFIGURATION =
            configuration("configuration");

    private static final ArgumentHandler ARGUMENTS =
            ArgumentHandler.getInstance(JdbcDatabaseComponentFactory.class, NAME, CONFIGURATION);

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

        ARGUMENTS.validateStructure(arguments);

        String name = ARGUMENTS.getArgument(NAME, arguments);
        Configuration configuration = ARGUMENTS.getArgument(CONFIGURATION, arguments);

        return DefaultJdbcDatabase.newInstance(name, configuration);
    }
}
