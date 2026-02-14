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

package io.github.totalschema.engine.internal.state;

import io.github.totalschema.config.Configuration;
import io.github.totalschema.engine.core.command.api.CommandContext;
import io.github.totalschema.model.*;
import io.github.totalschema.spi.hash.HashService;
import io.github.totalschema.spi.state.StateRepository;
import io.github.totalschema.spi.state.StateService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DefaultStateService implements StateService {

    private final Logger logger = LoggerFactory.getLogger(DefaultStateService.class);

    private final StateRepository repository;
    private final HashService hashService;

    private final CommandContext context;

    DefaultStateService(StateRepository repository, CommandContext context) {
        this.repository = repository;
        this.context = context;

        if (context.has(HashService.class)) {
            hashService = context.get(HashService.class);
        } else {
            hashService = null;
        }
    }

    @Override
    public final List<ChangeFile.Id> getAppliedChanges() {

        return repository.getAllStateRecords().stream()
                .map(StateRecord::getChangeFileId)
                .collect(Collectors.toList());
    }

    @Override
    public List<StateRecord> getStateRecords() {
        return repository.getAllStateRecords();
    }

    @Override
    public void registerCompletion(ApplyFile applyFile) {

        if (applyFile.getChangeType() == ChangeType.APPLY_ON_CHANGE) {
            deleteStateRecordsByChangeId(applyFile.getId());
        }

        String fileHash = hashFileIfRequired(applyFile);

        StateRecord stateRecord = new StateRecord();

        stateRecord.setChangeFileId(applyFile.getId());

        stateRecord.setFileHash(fileHash);

        stateRecord.setAppliedBy(getAppliedByUserId());

        ZonedDateTime timestamp = ZonedDateTime.now().withZoneSameInstant(ZoneOffset.UTC);
        stateRecord.setApplyTimeStamp(timestamp);

        repository.saveStateRecord(stateRecord);
    }

    private String hashFileIfRequired(ApplyFile applyFile) {

        if (hashService != null) {

            Path file = applyFile.getFile();

            try {
                String fileContent = Files.readString(file);

                return hashService.hashToHexString(fileContent);

            } catch (IOException ex) {
                throw new RuntimeException("Failure reading file for hashing: " + file, ex);
            }
        }

        return null;
    }

    @Override
    public void registerCompletion(RevertFile revertFile) {

        ChangeFile.Id id = revertFile.getId();

        int recordsDeleted = deleteStateRecordsByChangeId(id);

        if (recordsDeleted != 1) {
            throw new IllegalStateException(
                    recordsDeleted + " records deleted from state for: " + revertFile);
        }
    }

    private int deleteStateRecordsByChangeId(ChangeFile.Id id) {
        Set<ChangeFile.Id> changeFileIdsToDelete =
                Stream.of(ChangeType.values()).map(id::withChangeType).collect(Collectors.toSet());

        int recordsDeleted = repository.deleteStateRecordByIds(changeFileIdsToDelete);
        return recordsDeleted;
    }

    private String getAppliedByUserId() {

        String appliedByUserId = System.getProperty("user.name");

        if (context.has(Configuration.class)) {
            String overrideAppliedByUserId =
                    context.get(Configuration.class)
                            .getString("state.override.appliedBy.userId")
                            .orElse(null);

            if (overrideAppliedByUserId != null) {
                logger.info(
                        "Overriding appliedByUserId from {} to {} due to configuration",
                        appliedByUserId,
                        overrideAppliedByUserId);

                appliedByUserId = overrideAppliedByUserId;
            }
        }
        return appliedByUserId;
    }

    @Override
    public void close() throws IOException {
        repository.close();
    }
}
