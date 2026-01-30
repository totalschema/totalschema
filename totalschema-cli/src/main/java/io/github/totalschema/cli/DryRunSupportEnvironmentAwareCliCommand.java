package io.github.totalschema.cli;

import io.github.totalschema.engine.api.ChangeEngine;
import picocli.CommandLine;

public abstract class DryRunSupportEnvironmentAwareCliCommand extends EnvironmentAwareCliCommand {

    @CommandLine.Option(
            names = {"--dry-run"},
            description = "Show what would be applied without executing")
    protected boolean dryRun;

    @Override
    protected final void run(ChangeEngine changeEngine) {
        if (dryRun) {
            runDry(changeEngine);
        } else {
            runActual(changeEngine);
        }
    }

    protected abstract void runActual(ChangeEngine changeEngine);

    protected abstract void runDry(ChangeEngine changeEngine);
}
