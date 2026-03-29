package io.github.totalschema.engine.internal.state.csv;

import io.github.totalschema.config.Configuration;
import io.github.totalschema.engine.api.Context;
import io.github.totalschema.spi.ArgumentSpecification;
import io.github.totalschema.spi.ComponentFactory;
import io.github.totalschema.spi.state.StateConstants;
import io.github.totalschema.spi.state.StateRepository;
import java.util.List;
import java.util.Optional;

public class CsvFileStateRecordRepositoryFactory extends ComponentFactory<StateRepository> {

    @Override
    public boolean isLazy() {
        return true;
    }

    @Override
    public Class<StateRepository> getComponentType() {
        return StateRepository.class;
    }

    @Override
    public Optional<String> getQualifier() {
        return Optional.of("csv");
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
    public StateRepository createComponent(Context context, List<Object> arguments) {

        Configuration stateConfig =
                context.get(Configuration.class)
                        .getPrefixNamespace(StateConstants.CONFIG_PROPERTY_NAMESPACE);

        return CsvFileStateRecordRepository.newInstance(
                context, stateConfig.getPrefixNamespace("csv"));
    }
}
