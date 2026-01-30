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

package io.github.totalschema.engine.internal.state.csv;

import io.github.totalschema.ProjectConventions;
import io.github.totalschema.concurrent.LockTemplate;
import io.github.totalschema.config.Configuration;
import io.github.totalschema.config.environment.Environment;
import io.github.totalschema.engine.core.command.api.CommandContext;
import io.github.totalschema.engine.internal.changefile.ChangeFileFactory;
import io.github.totalschema.model.ChangeFile;
import io.github.totalschema.model.StateRecord;
import io.github.totalschema.spi.expression.evaluator.ExpressionEvaluator;
import io.github.totalschema.spi.state.StateRepository;
import java.io.*;
import java.nio.file.*;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

/**
 * CSV file-based implementation of {@link StateRepository} that stores migration state records in a
 * CSV file.
 *
 * <h2>CSV State Storage Overview</h2>
 *
 * <p>This implementation stores the migration state in CSV files which can be committed to the same
 * source code repository as the change files themselves. This approach offers version control
 * benefits, allowing teams to track state changes alongside schema changes.
 *
 * <h2>Benefits</h2>
 *
 * <ul>
 *   <li>State is versioned alongside change files in source control
 *   <li>Simple, human-readable format (CSV) that can be inspected and modified if needed
 *   <li>No external database dependency for state tracking
 *   <li>Easy to backup and restore as part of repository backups
 * </ul>
 *
 * <h2>Critical Limitations</h2>
 *
 * <p><strong>This approach has significant limitations that must be understood:</strong>
 *
 * <ul>
 *   <li><strong>Manual State Management:</strong> Relies on developers to properly commit and push
 *       the state file after migrations. Forgotten commits lead to state inconsistencies.
 *   <li><strong>No Concurrent Execution Protection:</strong> Does not provide any solution against
 *       concurrent migration execution. If multiple developers or CI/CD pipelines run migrations
 *       simultaneously, state corruption can occur.
 *   <li><strong>Merge Conflicts:</strong> In distributed teams, the state file can experience merge
 *       conflicts when multiple branches apply different migrations.
 *   <li><strong>File System Locks Only:</strong> Uses local file locks (ReadWriteLock) which only
 *       protect against concurrent access on the same machine, not across distributed systems.
 * </ul>
 *
 * <h2>Recommended Usage</h2>
 *
 * <ul>
 *   <li><strong>Suitable for:</strong> Single developer projects or very small teams (2-3
 *       developers) with disciplined workflow and sequential migration execution
 *   <li><strong>NOT recommended for:</strong> Larger teams, distributed development, CI/CD
 *       environments with parallel execution, or production environments requiring high reliability
 *   <li><strong>Alternative:</strong> For larger teams, consider using a database-backed state
 *       repository with proper transaction support and distributed locking mechanisms
 * </ul>
 *
 * <h2>File Format</h2>
 *
 * <p>The CSV file contains the following columns:
 *
 * <ul>
 *   <li>CHANGE_FILE_ID - Unique identifier of the applied change file
 *   <li>FILE_HASH - Hash of the change file content for integrity verification
 *   <li>APPLY_TIMESTAMP - ISO 8601 formatted timestamp of when the change was applied
 *   <li>APPLIED_BY - User or system that applied the change
 * </ul>
 *
 * @see StateRepository
 */
public final class CsvFileStateRecordRepository implements StateRepository {

    private static final int FILE_OPERATION_TIMEOUT = 30;
    private static final TimeUnit FILE_OPERATION_TIMEOUT_UNIT = TimeUnit.SECONDS;

    private enum CsvHeaders {
        CHANGE_FILE_ID,
        FILE_HASH,
        APPLY_TIMESTAMP,
        APPLIED_BY;

        private static String[] asStrings() {
            return Stream.of(CsvHeaders.values()).map(Enum::name).toArray(String[]::new);
        }
    }

    private static final DateTimeFormatter CSV_DATE_TIME_FORMATTER =
            DateTimeFormatter.ISO_DATE_TIME;

    private final ChangeFileFactory changeFileFactory;

    private final Path stateFile;

    private final Path tempFilePath;

    private static final CSVFormat CSV_FORMAT =
            CSVFormat.DEFAULT
                    .builder()
                    .setHeader(CsvHeaders.asStrings())
                    .setSkipHeaderRecord(true)
                    .get();

    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final LockTemplate readLockTemplate =
            new LockTemplate(
                    FILE_OPERATION_TIMEOUT, FILE_OPERATION_TIMEOUT_UNIT, readWriteLock.readLock());
    private final LockTemplate writeLockTemplate =
            new LockTemplate(
                    FILE_OPERATION_TIMEOUT, FILE_OPERATION_TIMEOUT_UNIT, readWriteLock.writeLock());

    public static CsvFileStateRecordRepository newInstance(
            CommandContext context, Configuration configuration) {

        try {
            CsvFileStateRecordRepository repository =
                    new CsvFileStateRecordRepository(context, configuration);

            repository.init();

            return repository;

        } catch (IOException | RuntimeException ex) {
            throw new RuntimeException("Initialization failure", ex);
        }
    }

    public CsvFileStateRecordRepository(CommandContext context, Configuration configuration) {

        changeFileFactory = context.get(ChangeFileFactory.class);

        Environment environment = context.get(Environment.class);
        ExpressionEvaluator expressionEvaluator = context.get(ExpressionEvaluator.class);

        String filePathPattern =
                configuration
                        .getString("file.path.pattern")
                        .orElse(
                                String.format(
                                        "%s/state/${environment}/state-${environment}.csv",
                                        ProjectConventions.PROJECT_SYSTEM_NAME));

        Map<String, String> templateValue =
                Collections.singletonMap("environment", environment.getName());

        String filePath = expressionEvaluator.evaluate(filePathPattern, templateValue);

        stateFile = Paths.get(".", filePath);
        Path absolutePath = stateFile.toAbsolutePath();
        Objects.requireNonNull(absolutePath, "Absolute Path path cannot be null for: " + stateFile);

        Path parentPath = absolutePath.getParent();
        Objects.requireNonNull(parentPath, "Parent path cannot be null for: " + absolutePath);

        Path fileNamePath = stateFile.getFileName();
        Objects.requireNonNull(fileNamePath, "File name cannot be null for: " + stateFile);

        tempFilePath =
                Paths.get(
                        parentPath.toAbsolutePath().toString(),
                        fileNamePath + ".pending-commit.tmp");
    }

    private void init() throws IOException {

        try {
            if (Files.exists(tempFilePath)) {

                if (Files.exists(stateFile)) {
                    Files.delete(stateFile);
                }

                Files.move(tempFilePath, stateFile);
            }
        } catch (IOException e) {
            throw new IOException(
                    String.format(
                            "Failure recovering an inconsistent CSV state file. "
                                    + "This might require a MANUAL recovery of %s using %s or from BACKUP",
                            stateFile, tempFilePath),
                    e);
        }
    }

    @Override
    public void saveStateRecord(StateRecord stateRecord) {

        writeLockTemplate.withTryLock(
                () -> {
                    saveStateRecordsWithWriteLockHeld(List.of(stateRecord));

                    return null;
                });
    }

    private void saveStateRecordsWithWriteLockHeld(List<StateRecord> stateRecords) {

        try {
            Path parentPath = stateFile.getParent();
            if (parentPath != null) {
                Files.createDirectories(parentPath);
            }

            if (!Files.exists(stateFile)) {
                try (CSVPrinter printer =
                        new CSVPrinter(
                                Files.newBufferedWriter(stateFile, StandardOpenOption.CREATE),
                                CSV_FORMAT)) {

                    printer.printRecord(Arrays.asList(CsvHeaders.asStrings()));
                }
            }

            try (CSVPrinter printer =
                    new CSVPrinter(
                            Files.newBufferedWriter(stateFile, StandardOpenOption.APPEND),
                            CSV_FORMAT)) {

                for (StateRecord stateRecord : stateRecords) {
                    ChangeFile.Id id = stateRecord.getChangeFileId();
                    if (id == null) {
                        throw new IllegalArgumentException("id cannot be null");
                    }

                    printer.printRecord(
                            id.toStringRepresentation(),
                            stateRecord.getFileHash(),
                            stateRecord.getApplyTimeStamp().format(DateTimeFormatter.ISO_DATE_TIME),
                            stateRecord.getAppliedBy());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(
                    String.format("Failure appending %s to %s", stateRecords, stateFile), e);
        }
    }

    @Override
    public List<StateRecord> getAllStateRecords() {

        return readLockTemplate.withTryLock(
                () -> {
                    try {
                        LinkedList<StateRecord> stateRecords = new LinkedList<>();

                        if (Files.exists(stateFile)) {

                            Reader in = Files.newBufferedReader(stateFile);

                            Iterable<CSVRecord> csvRecords = CSV_FORMAT.parse(in);

                            for (CSVRecord csvRecord : csvRecords) {
                                StateRecord stateRecord = new StateRecord();

                                ChangeFile.Id id =
                                        changeFileFactory.getIdFromString(
                                                csvRecord.get(CsvHeaders.CHANGE_FILE_ID));
                                stateRecord.setChangeFileId(id);

                                stateRecord.setFileHash(csvRecord.get(CsvHeaders.FILE_HASH));

                                String applyTimestampString =
                                        csvRecord.get(CsvHeaders.APPLY_TIMESTAMP);
                                if (applyTimestampString != null) {
                                    ZonedDateTime applyTimeStamp =
                                            ZonedDateTime.parse(
                                                    applyTimestampString, CSV_DATE_TIME_FORMATTER);

                                    stateRecord.setApplyTimeStamp(applyTimeStamp);
                                }

                                stateRecord.setAppliedBy(csvRecord.get(CsvHeaders.APPLIED_BY));

                                stateRecords.add(stateRecord);
                            }
                        }

                        return Collections.unmodifiableList(stateRecords);

                    } catch (IOException e) {
                        throw new RuntimeException("I/O error reading: " + stateFile, e);
                    }
                });
    }

    @Override
    public int deleteStateRecordByIds(Set<ChangeFile.Id> changeFileMetadata) {

        Objects.requireNonNull(changeFileMetadata, "Argument changeFileMetadata cannot be null");

        return writeLockTemplate.withTryLock(
                () -> deleteByIdsWithWriteLockHeld(changeFileMetadata));
    }

    private int deleteByIdsWithWriteLockHeld(Set<ChangeFile.Id> idsToDelete) {

        try {
            List<StateRecord> stateRecords = getAllStateRecords();

            List<StateRecord> filteredState =
                    stateRecords.stream()
                            .filter(it -> !idsToDelete.contains(it.getChangeFileId()))
                            .collect(Collectors.toList());

            int changes = stateRecords.size() - filteredState.size();

            if (Files.exists(stateFile)) {
                Files.copy(stateFile, tempFilePath);

                Files.delete(stateFile);
            }

            saveStateRecordsWithWriteLockHeld(filteredState);

            if (Files.exists(tempFilePath)) {
                Files.delete(tempFilePath);
            }

            return changes;

        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
