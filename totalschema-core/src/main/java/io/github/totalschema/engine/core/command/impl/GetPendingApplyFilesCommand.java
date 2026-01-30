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

package io.github.totalschema.engine.core.command.impl;

import io.github.totalschema.engine.core.command.api.Command;
import io.github.totalschema.engine.core.command.api.CommandContext;
import io.github.totalschema.model.ApplyFile;
import io.github.totalschema.model.ChangeFile;
import io.github.totalschema.model.ChangeType;
import io.github.totalschema.model.StateRecord;
import io.github.totalschema.spi.hash.HashService;
import io.github.totalschema.spi.state.StateService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Command to determine which apply files are pending execution. Filters out files that have already
 * been applied, except for APPLY_ON_CHANGE files that have been modified since their last
 * application.
 *
 * <h2>Defensive Copying</h2>
 *
 * <p>This class creates a defensive copy of the List passed to its constructor to prevent external
 * modification of its internal state. Changes to the original list after construction will not
 * affect this command's behavior.
 */
public final class GetPendingApplyFilesCommand implements Command<List<ApplyFile>> {

    private final List<ApplyFile> applyFiles;

    /**
     * Constructs a new GetPendingApplyFilesCommand with a defensive copy of the provided list.
     *
     * @param applyFiles the list of apply files to process (will be copied, must not be null)
     * @throws NullPointerException if applyFiles is null
     */
    public GetPendingApplyFilesCommand(List<ApplyFile> applyFiles) {
        this.applyFiles = new ArrayList<>(applyFiles);
    }

    @Override
    public List<ApplyFile> execute(CommandContext context) {

        StateService stateService = context.get(StateService.class);

        List<StateRecord> stateRecords = stateService.getStateRecords();

        Predicate<ChangeFile> filterPredicate = stateRecordsDoNotContainIdPredicate(stateRecords);

        if (stateRecords.stream()
                .anyMatch(
                        it -> it.getChangeFileId().getChangeType() == ChangeType.APPLY_ON_CHANGE)) {

            filterPredicate =
                    filterPredicate.or(isApplyOnChangeAndChangedPredicate(stateRecords, context));
        }

        return applyFiles.stream().filter(filterPredicate).collect(Collectors.toList());
    }

    private static Predicate<ChangeFile> stateRecordsDoNotContainIdPredicate(
            List<StateRecord> stateRecords) {

        Set<ChangeFile.Id> appliedChangeIds =
                stateRecords.stream().map(StateRecord::getChangeFileId).collect(Collectors.toSet());

        return new AppliedSetDoesNotContainPredicate(appliedChangeIds);
    }

    private static final class AppliedSetDoesNotContainPredicate implements Predicate<ChangeFile> {

        private final Set<ChangeFile.Id> appliedChangeIds;

        private AppliedSetDoesNotContainPredicate(Set<ChangeFile.Id> appliedChangeIds) {
            this.appliedChangeIds = appliedChangeIds;
        }

        @Override
        public boolean test(ChangeFile changeFile) {
            return !appliedChangeIds.contains(changeFile.getId());
        }
    }

    private static Predicate<ChangeFile> isApplyOnChangeAndChangedPredicate(
            List<StateRecord> stateRecords, CommandContext context) {

        if (!context.has(HashService.class)) {
            throw new IllegalStateException(
                    "HashService not registered in context: Hashing must be set as validation mode!");
        }

        HashService hashService = context.get(HashService.class);

        return new IsApplyOnChangeAndChanged(stateRecords, hashService);
    }

    private static final class IsApplyOnChangeAndChanged implements Predicate<ChangeFile> {

        private final HashService hashService;

        private final Map<ChangeFile.Id, String> idToHash;

        private IsApplyOnChangeAndChanged(List<StateRecord> stateRecords, HashService hashService) {
            this.hashService = hashService;
            this.idToHash =
                    stateRecords.stream()
                            .filter(
                                    it ->
                                            it.getChangeFileId().getChangeType()
                                                    == ChangeType.APPLY_ON_CHANGE)
                            .collect(
                                    Collectors.toMap(
                                            StateRecord::getChangeFileId,
                                            StateRecord::getFileHash));
        }

        @Override
        public boolean test(ChangeFile changeFile) {

            ChangeFile.Id id = changeFile.getId();
            ChangeType changeType = id.getChangeType();

            boolean result;

            if (changeType != ChangeType.APPLY_ON_CHANGE) {
                result = false;

            } else {

                Path path = changeFile.getFile();

                try {
                    String fileContent = Files.readString(path);

                    String fileHashContent = hashService.hashToHexString(fileContent);

                    String existingHash = idToHash.get(changeFile.getId());

                    result = !Objects.equals(existingHash, fileHashContent);

                } catch (IOException e) {
                    throw new RuntimeException("Failure reading: " + path, e);
                }
            }

            return result;
        }
    }
}
