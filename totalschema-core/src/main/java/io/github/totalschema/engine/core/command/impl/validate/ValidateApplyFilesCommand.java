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

package io.github.totalschema.engine.core.command.impl.validate;

import io.github.totalschema.engine.api.ChangeEngine;
import io.github.totalschema.engine.core.command.api.Command;
import io.github.totalschema.engine.core.command.api.CommandContext;
import io.github.totalschema.model.ApplyFile;
import io.github.totalschema.model.ChangeFile;
import io.github.totalschema.model.StateRecord;
import io.github.totalschema.spi.hash.HashService;
import io.github.totalschema.spi.state.StateService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ValidateApplyFilesCommand implements Command<List<Exception>> {

    private final Logger log = LoggerFactory.getLogger(ValidateApplyFilesCommand.class);

    private final String filterExpression;

    public ValidateApplyFilesCommand(String filterExpression) {
        this.filterExpression = filterExpression;
    }

    @Override
    public List<Exception> execute(CommandContext context) throws InterruptedException {

        LinkedList<Exception> validationFailures = new LinkedList<>();

        HashService hashService = context.get(HashService.class);
        ChangeEngine changeEngine = context.get(ChangeEngine.class);
        StateService stateService = context.get(StateService.class);

        List<ApplyFile> allApplyFiles =
                changeEngine.getChangeManager().getAllApplyFiles(filterExpression);

        Map<ChangeFile.Id, ApplyFile> idToChangeFile =
                allApplyFiles.stream()
                        .collect(Collectors.toMap(ChangeFile::getId, Function.identity()));

        List<StateRecord> stateRecords = stateService.getStateRecords();

        log.info(
                "{} applied changes found in state, validating them against the files",
                stateRecords.size());

        for (StateRecord stateRecord : stateRecords) {

            ChangeFile.Id id = stateRecord.getChangeFileId();

            try {
                ApplyFile applyFile = idToChangeFile.get(id);

                validateFile(applyFile, stateRecord, hashService);

            } catch (RuntimeException ex) {

                log.error("Validation failed of : " + id.toStringRepresentation(), ex);

                validationFailures.add(ex);
            }
        }

        return validationFailures;
    }

    private void validateFile(
            ApplyFile applyFile, StateRecord stateRecord, HashService hashService) {

        ChangeFile.Id id = stateRecord.getChangeFileId();

        String expectedHash = stateRecord.getFileHash();
        if (expectedHash == null || expectedHash.isBlank()) {

            throw new RuntimeException("Cannot validate state record, as hash is null in: " + id);
        } else {

            if (applyFile != null) {

                Path changeFilePath = applyFile.getFile();
                try {
                    String fileContent = Files.readString(changeFilePath);

                    String actualHash = hashService.hashToHexString(fileContent);

                    if (!actualHash.equalsIgnoreCase(expectedHash)) {

                        log.error("Validation FAILED: hash mismatch for: {}", changeFilePath);

                        throw new RuntimeException(
                                "File hash does not match hash from state: "
                                        + applyFile.getId().toStringRepresentation());
                    }

                    log.info("Validation successful for: {}", changeFilePath);

                } catch (IOException ex) {
                    throw new RuntimeException("Failed to read file: " + changeFilePath);
                }

            } else {
                throw new RuntimeException("No change file found for for state record: " + id);
            }
        }
    }
}
