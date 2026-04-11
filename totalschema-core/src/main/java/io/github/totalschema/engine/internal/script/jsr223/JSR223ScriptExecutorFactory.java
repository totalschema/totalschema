package io.github.totalschema.engine.internal.script.jsr223;

import io.github.totalschema.config.Configuration;
import java.util.LinkedList;
import java.util.List;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;

/**
 * Factory for creating {@link JSR223ScriptExecutor} instances backed by the Java Scripting API
 * (JSR-223).
 *
 * <p>At runtime, this factory discovers all {@link javax.script.ScriptEngineFactory}
 * implementations available on the classpath via {@link ScriptEngineManager} and produces one
 * {@link JSR223ScriptExecutor} per registered file extension. JSR-223 support must be explicitly
 * opted in via the {@code scripting.jsr223.enabled} configuration flag.
 */
public final class JSR223ScriptExecutorFactory {

    /**
     * Returns {@code true} if JSR-223 scripting is enabled in the supplied configuration.
     *
     * <p>The feature is controlled by the {@code scripting.jsr223.enabled} boolean property. When
     * the property is absent it defaults to {@code false}.
     *
     * @param configuration the runtime configuration to inspect; must not be {@code null}
     * @return {@code true} if JSR-223 scripting is enabled, {@code false} otherwise
     */
    public static boolean isEnabled(Configuration configuration) {
        return configuration.getBoolean("scripting.jsr223.enabled").orElse(false);
    }

    /**
     * Creates a {@link JSR223ScriptExecutor} for every file extension advertised by the {@link
     * ScriptEngineFactory} instances discovered on the current classpath.
     *
     * <p>The discovery is performed via {@link ScriptEngineManager#getEngineFactories()}. Factories
     * that report a {@code null} or empty extension list are silently skipped.
     *
     * @return an immutable list of {@link JSR223ScriptExecutor} instances, one per supported file
     *     extension; never {@code null}, may be empty if no JSR-223 engines are present
     */
    public static List<JSR223ScriptExecutor> createExecutors() {

        List<JSR223ScriptExecutor> results = new LinkedList<>();

        ScriptEngineManager scriptEngineManager = new ScriptEngineManager();

        for (ScriptEngineFactory scriptEngineFactory : scriptEngineManager.getEngineFactories()) {
            List<String> extensions = scriptEngineFactory.getExtensions();
            if (extensions != null) {
                for (String extension : extensions) {
                    results.add(JSR223ScriptExecutor.newInstance(extension));
                }
            }
        }

        return List.copyOf(results);
    }
}
