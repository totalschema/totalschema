/*
 * totalschema: tool for managing database versioning and schema changes with ease.
 * Copyright (C) 2025 totalschema development team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.github.totalschema.cli.state;

import io.github.totalschema.cli.CommonCliCommandBase;
import io.github.totalschema.engine.api.ChangeEngine;
import io.github.totalschema.engine.api.ChangeEngineFactory;
import io.github.totalschema.model.StateRecord;
import io.github.totalschema.spi.config.ConfigurationSupplier;
import io.github.totalschema.spi.secrets.SecretsManager;
import java.util.List;
import picocli.CommandLine;

@CommandLine.Command(
        name = "purge-orphaned",
        mixinStandardHelpOptions = true,
        description = "removes state records for change files that no longer exist on disk")
public class PurgeOrphanedStateCliCommand extends CommonCliCommandBase {

    @CommandLine.Option(
            names = {"-e", "--environment"},
            description =
                    "restrict to change files applicable to this environment; "
                            + "when omitted, all records are candidates regardless of environment")
    private String environment;

    @CommandLine.Option(
            names = {"--dry-run"},
            description =
                    "show which orphaned state records would be removed without making any changes")
    private boolean dryRun;

    @Override
    protected ChangeEngine getChangeEngine(
            ConfigurationSupplier configurationSupplier,
            ChangeEngineFactory changeEngineFactory,
            SecretsManager secretsManager) {

        return changeEngineFactory.getChangeEngine(
                configurationSupplier, secretsManager, environment);
    }

    @Override
    public void run(ChangeEngine changeEngine) {

        if (dryRun) {
            runDry(changeEngine);
        } else {
            runActual(changeEngine);
        }
    }

    private void runActual(ChangeEngine changeEngine) {

        List<StateRecord> purged = changeEngine.getStateManager().purgeOrphanedStateRecords();

        if (purged.isEmpty()) {
            System.out.println("No orphaned state records found.");
        } else {
            System.out.format("Purged %d orphaned state record(s):%n", purged.size());
            for (StateRecord stateRecord : purged) {
                System.out.format("  %s%n", stateRecord.getChangeFileId().toStringRepresentation());
            }
        }
    }

    private void runDry(ChangeEngine changeEngine) {

        List<StateRecord> wouldBePurged = changeEngine.getStateManager().getOrphanedStateRecords();

        if (wouldBePurged.isEmpty()) {
            System.out.println("[dry-run] No orphaned state records found.");
        } else {
            System.out.format(
                    "[dry-run] Would purge %d orphaned state record(s):%n", wouldBePurged.size());
            for (StateRecord stateRecord : wouldBePurged) {
                System.out.format("  %s%n", stateRecord.getChangeFileId().toStringRepresentation());
            }
        }
    }
}
