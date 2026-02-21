package io.github.totalschema.engine.internal.state.csv;

import io.github.totalschema.config.Configuration;
import io.github.totalschema.engine.api.Context;
import io.github.totalschema.spi.ComponentFactory;
import io.github.totalschema.spi.state.StateConstants;
import io.github.totalschema.spi.state.StateRepository;
import java.util.List;

public class CsvFileStateRecordRepositoryFactory implements ComponentFactory<StateRepository> {

    @Override
    public boolean isLazy() {
        return true;
    }

    @Override
    public Class<StateRepository> getConstructedClass() {
        return StateRepository.class;
    }

    @Override
    public String getQualifier() {
        return "csv";
    }

    @Override
    public List<Class<?>> getRequiredContextTypes() {
        return List.of(Configuration.class);
    }

    @Override
    public List<Class<?>> getArgumentTypes() {
        return List.of();
    }

    @Override
    public StateRepository newComponent(Context context, Object... arguments) {

        Configuration stateConfig =
                context.get(Configuration.class)
                        .getPrefixNamespace(StateConstants.CONFIG_PROPERTY_NAMESPACE);

        return CsvFileStateRecordRepository.newInstance(
                context, stateConfig.getPrefixNamespace("csv"));
    }
}
