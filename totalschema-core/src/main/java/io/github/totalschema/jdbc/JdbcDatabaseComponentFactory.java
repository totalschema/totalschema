package io.github.totalschema.jdbc;

import static io.github.totalschema.spi.ArgumentSpecification.*;

import io.github.totalschema.config.Configuration;
import io.github.totalschema.engine.api.Context;
import io.github.totalschema.spi.ArgumentSpecification;
import io.github.totalschema.spi.ComponentFactory;
import java.util.List;

public class JdbcDatabaseComponentFactory extends ComponentFactory<JdbcDatabase> {

    private static final ArgumentSpecification<String> NAME_ARGUMENT = string("name");
    private static final ArgumentSpecification<Configuration> CONFIGURATION_ARGUMENT =
            configuration("configuration");

    @Override
    public boolean isLazy() {
        return false;
    }

    @Override
    public Class<JdbcDatabase> getComponentType() {
        return JdbcDatabase.class;
    }

    @Override
    public String getQualifier() {
        return null;
    }

    @Override
    public List<Class<?>> getRequiredContextTypes() {
        return List.of();
    }

    @Override
    public List<ArgumentSpecification<?>> getArgumentSpecifications() {
        return List.of(NAME_ARGUMENT, CONFIGURATION_ARGUMENT);
    }

    @Override
    public JdbcDatabase newComponent(Context context, Object... arguments) {

        String name = getArgument(NAME_ARGUMENT, arguments, 0);
        Configuration configuration = getArgument(CONFIGURATION_ARGUMENT, arguments, 1);

        return DefaultJdbcDatabase.newInstance(name, configuration);
    }
}
