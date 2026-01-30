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

import io.github.totalschema.cli.EnvironmentAwareCliCommand;
import io.github.totalschema.engine.api.ChangeEngine;
import io.github.totalschema.model.StateRecord;
import java.time.format.DateTimeFormatter;
import java.util.List;
import picocli.CommandLine;

@CommandLine.Command(
        name = "display",
        mixinStandardHelpOptions = true,
        description = "display state information")
public class DisplayStateCliCommand extends EnvironmentAwareCliCommand {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");

    public static final String CHANGE_FILE_ID = "%1$-70s";
    public static final String APPLY_TIMESTAMP = "%1$-30s";
    public static final String APPLIED_BY = "%1$-20s";

    @Override
    public void run(ChangeEngine changeEngine) {

        List<StateRecord> stateRecords = changeEngine.getStateManager().getStateRecords();

        System.out.format(CHANGE_FILE_ID, "Change File Id");
        System.out.format(APPLY_TIMESTAMP, "Apply Timestamp");
        System.out.format(APPLIED_BY, "Applied by");
        System.out.println();

        for (StateRecord stateRecord : stateRecords) {
            System.out.format(
                    CHANGE_FILE_ID, stateRecord.getChangeFileId().toStringRepresentation());
            System.out.format(
                    APPLY_TIMESTAMP, DATE_TIME_FORMATTER.format(stateRecord.getApplyTimeStamp()));
            System.out.format(APPLIED_BY, stateRecord.getAppliedBy());
        }
    }
}
