package io.github.totalschema.engine.internal.state.database;

import io.github.totalschema.config.Configuration;
import io.github.totalschema.engine.api.Context;
import io.github.totalschema.engine.internal.changefile.ChangeFileFactory;
import io.github.totalschema.jdbc.JdbcDatabase;
import io.github.totalschema.spi.ComponentFactory;
import io.github.totalschema.spi.sql.SqlDialect;
import io.github.totalschema.spi.state.StateConstants;
import io.github.totalschema.spi.state.StateRepository;
import java.util.List;

public class DatabaseStateRecordRepositoryFactory extends ComponentFactory<StateRepository> {

    @Override
    public boolean isLazy() {
        return true;
    }

    @Override
    public Class<StateRepository> getComponentType() {
        return StateRepository.class;
    }

    @Override
    public String getQualifier() {
        return "database";
    }

    @Override
    public List<Class<?>> getRequiredContextTypes() {
        return List.of(Configuration.class, SqlDialect.class, ChangeFileFactory.class);
    }

    @Override
    public StateRepository newComponent(Context context, Object... arguments) {

        Configuration stateConfig =
                context.get(Configuration.class)
                        .getPrefixNamespace(StateConstants.CONFIG_PROPERTY_NAMESPACE);

        Configuration dbConfig = stateConfig.getPrefixNamespace("database");

        // Get SQL dialect from configuration, or use "default"
        String dialectType = dbConfig.getString("dialect").orElse("default");
        SqlDialect sqlDialect = getSqlDialect(context, dialectType);

        ChangeFileFactory changeFileFactory = context.get(ChangeFileFactory.class);
        int changeFileNameMaxLength = changeFileFactory.getChangeFileNameMaxLength();

        // Create JdbcDatabase instance with logSql configuration defaulting to false
        Configuration configWithLogSqlSet =
                dbConfig.toBuilder().setIfAbsent("logSql", false).build();

        JdbcDatabase jdbcDatabase =
                context.get(JdbcDatabase.class, null, "state", configWithLogSqlSet);

        JdbcDatabaseStateRecordRepository repository =
                new JdbcDatabaseStateRecordRepository(
                        sqlDialect,
                        jdbcDatabase,
                        changeFileFactory,
                        changeFileNameMaxLength,
                        dbConfig);

        repository.init();

        return repository;
    }

    private static SqlDialect getSqlDialect(Context context, String dialectType) {
        try {
            return context.get(SqlDialect.class, dialectType);
        } catch (RuntimeException e) {
            throw new IllegalStateException(
                    String.format("SQL Dialect '%s' not found", dialectType), e);
        }
    }
}
