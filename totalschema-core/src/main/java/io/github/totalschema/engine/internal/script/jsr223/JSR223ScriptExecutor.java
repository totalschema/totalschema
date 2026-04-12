package io.github.totalschema.engine.internal.script.jsr223;

import io.github.totalschema.config.Configuration;
import io.github.totalschema.config.environment.Environment;
import io.github.totalschema.engine.api.Context;
import io.github.totalschema.spi.script.ScriptExecutor;
import java.sql.Connection;
import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link ScriptExecutor} that delegates script evaluation to a JSR-223 compatible {@link
 * ScriptEngine}.
 *
 * <p>One instance is created per supported file extension (e.g. {@code js}, {@code py}). The
 * underlying engine is resolved at construction time from the classpath via {@link
 * ScriptEngineManager}. At execution time the following objects are bound into the script's {@link
 * Bindings} when present in the supplied {@link Context}:
 *
 * <ul>
 *   <li>{@code configuration} — the active {@link Configuration}
 *   <li>{@code connection} — the active JDBC {@link java.sql.Connection}
 *   <li>{@code environment} — the active {@link Environment}
 * </ul>
 *
 * <p>Instances should be obtained via {@link JSR223ScriptExecutorFactory#createExecutors()} rather
 * than constructed directly.
 */
public class JSR223ScriptExecutor implements ScriptExecutor {

    private static final Logger log = LoggerFactory.getLogger(JSR223ScriptExecutor.class);

    /** The file extension (without leading dot) this executor handles, e.g. {@code "js"}. */
    private final String extension;

    /** The JSR-223 engine used to evaluate scripts. */
    private final ScriptEngine engine;

    /**
     * Creates a new {@link JSR223ScriptExecutor} for the given file extension.
     *
     * <p>The corresponding {@link ScriptEngine} is resolved from the current thread's context class
     * loader via {@link ScriptEngineManager#getEngineByExtension(String)}.
     *
     * @param extension the file extension to associate with this executor (e.g. {@code "js"}); must
     *     not be {@code null}
     * @return a new {@link JSR223ScriptExecutor} bound to the resolved engine
     * @throws IllegalStateException if the engine cannot be loaded for the given extension
     */
    static JSR223ScriptExecutor newInstance(String extension) {

        try {
            ScriptEngineManager manager =
                    new ScriptEngineManager(Thread.currentThread().getContextClassLoader());

            ScriptEngine scriptEngine = manager.getEngineByExtension(extension);

            log.debug("Created ScriptEngine for extension: {}", extension);

            return new JSR223ScriptExecutor(extension, scriptEngine);

        } catch (RuntimeException ex) {
            throw new IllegalStateException("Failed to load JSR223 engine: " + extension, ex);
        }
    }

    private JSR223ScriptExecutor(String extension, ScriptEngine engine) {
        this.extension = extension;
        this.engine = engine;
    }

    /**
     * Returns the file extension handled by this executor.
     *
     * @return the file extension (without a leading dot), e.g. {@code "js"}; never {@code null}
     */
    public String getExtension() {
        return extension;
    }

    /**
     * Evaluates the supplied script using the underlying JSR-223 engine.
     *
     * <p>Before evaluation, the following objects are bound into the engine's {@link Bindings} when
     * present in {@code context}:
     *
     * <ul>
     *   <li>{@code "configuration"} → the active {@link Configuration}
     *   <li>{@code "connection"} → the active JDBC {@link java.sql.Connection}
     *   <li>{@code "environment"} → the active {@link Environment}
     * </ul>
     *
     * @param script the source code to evaluate; must not be {@code null}
     * @param context the execution context providing optional bindings; must not be {@code null}
     * @throws InterruptedException if the current thread is interrupted during execution
     * @throws RuntimeException if the script raises a {@link ScriptException}
     */
    @Override
    public void execute(String script, Context context) throws InterruptedException {

        try {

            Bindings bindings = engine.createBindings();

            context.getOptional(Configuration.class)
                    .ifPresent(configuration -> bindings.put("configuration", configuration));

            context.getOptional(Connection.class)
                    .ifPresent(connection -> bindings.put("connection", connection));

            context.getOptional(Environment.class)
                    .ifPresent(environment -> bindings.put("environment", environment));

            engine.eval(script, bindings);

        } catch (ScriptException ex) {
            throw new RuntimeException("Failure evaluating script", ex);
        }
    }

    @Override
    public String toString() {
        return "JSR223ScriptExecutor{" + "extension='" + extension + '\'' + '}';
    }
}
