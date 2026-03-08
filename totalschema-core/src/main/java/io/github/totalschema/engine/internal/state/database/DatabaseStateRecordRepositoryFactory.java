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
    public Class<StateRepository> getConstructedClass() {
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

        SqlDialect sqlDialect = context.get(SqlDialect.class);
        ChangeFileFactory changeFileFactory = context.get(ChangeFileFactory.class);
        int changeFileNameMaxLength = changeFileFactory.getChangeFileNameMaxLength();

        Configuration dbConfig = stateConfig.getPrefixNamespace("database");

        // Create JdbcDatabase instance with logSql configuration
        boolean logSql = dbConfig.getBoolean("logSql").orElse(false);
        Configuration configWithLogSqlSet = dbConfig.withEntry("logSql", Boolean.toString(logSql));

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
}
