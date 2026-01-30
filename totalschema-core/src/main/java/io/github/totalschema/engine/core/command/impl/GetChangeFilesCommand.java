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

import io.github.totalschema.ProjectConventions;
import io.github.totalschema.config.Configuration;
import io.github.totalschema.config.environment.Environment;
import io.github.totalschema.engine.core.command.api.Command;
import io.github.totalschema.engine.core.command.api.CommandContext;
import io.github.totalschema.engine.internal.changefile.ChangeFileFactory;
import io.github.totalschema.model.ChangeFile;
import io.github.totalschema.model.ChangeType;
import io.github.totalschema.spi.hash.HashService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class GetChangeFilesCommand<T extends ChangeFile> implements Command<List<T>> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final Comparator<Path> DIRECTORY_COMPARATOR =
            new Comparator<>() {

                @Override
                public int compare(Path leftPath, Path rightPath) {

                    Path leftFileNamePath = leftPath.getFileName();
                    Path rightFileNamePath = rightPath.getFileName();

                    if (leftFileNamePath == null || rightFileNamePath == null) {
                        if (leftFileNamePath == null && rightFileNamePath == null) {
                            return 0;
                        }
                        return leftFileNamePath == null ? -1 : 1;
                    }

                    String leftFileName = leftFileNamePath.toString();
                    String rightFileName = rightFileNamePath.toString();

                    if (leftFileName.matches(".*\\d.*") && rightFileName.matches(".*\\d.*")) {

                        return Long.compare(
                                getDirectoryOrder(leftFileName), getDirectoryOrder(rightFileName));

                    } else {
                        return leftFileName.compareTo(rightFileName);
                    }
                }

                private long getDirectoryOrder(String directoryName) {
                    List<Integer> versionNumberParts =
                            Stream.of(directoryName.split("\\D"))
                                    .filter(Predicate.not(String::isEmpty))
                                    .filter(Predicate.not(String::isBlank))
                                    .map(
                                            it -> {
                                                try {
                                                    return Integer.parseInt(it);
                                                } catch (NumberFormatException nfe) {
                                                    throw new RuntimeException(
                                                            String.format(
                                                                    "Could not parse number [%s] part in %s",
                                                                    it, directoryName));
                                                }
                                            })
                                    .collect(Collectors.toList());

                    long accumulator = 0;

                    int partNumber = versionNumberParts.size();

                    final int offset = 10;

                    for (int i = 0; i < partNumber; i++) {

                        int partIndex = partNumber - 1 - i;
                        int digit = versionNumberParts.get(partIndex);

                        long multiplier = (long) Math.pow(10, offset - i);

                        accumulator += multiplier * digit;
                    }

                    return accumulator;
                }
            };

    private final Pattern filterExpressionPattern;
    private final EnumSet<ChangeType> includedChangeTypes;

    public GetChangeFilesCommand(
            String filterExpression,
            ChangeType firstChangeType,
            ChangeType... additionalChangeTypes) {

        includedChangeTypes = EnumSet.of(firstChangeType, additionalChangeTypes);

        if (filterExpression != null) {
            filterExpressionPattern = Pattern.compile(filterExpression);
        } else {
            filterExpressionPattern = null;
        }
    }

    @Override
    public List<T> execute(CommandContext context) {

        ChangeFileFactory changeFileFactory = context.get(ChangeFileFactory.class);

        String environmentName = context.get(Environment.class).getName();

        Configuration config = context.get(Configuration.class);

        String changeDirectoryConfig =
                config.getString("changes", "directory")
                        .orElse(ProjectConventions.CHANGE_DIRECTORY_PATH);
        Path rootDirectory = Paths.get(changeDirectoryConfig).toAbsolutePath();

        if (!Files.exists(rootDirectory) || !Files.isDirectory(rootDirectory)) {
            throw new IllegalStateException(
                    String.format(
                            "Changes directory does not exist or is not a directory: %s",
                            rootDirectory));
        }

        logger.info("Searching for change files in: {}", rootDirectory.toAbsolutePath());

        LinkedList<T> changeFiles = new LinkedList<>();

        try {
            Deque<Path> directoriesToVisit = new LinkedList<>();
            directoriesToVisit.add(rootDirectory);

            while (!directoriesToVisit.isEmpty()) {
                Path directoryToProcess = directoriesToVisit.poll();

                List<Path> directSubDirectories = getDirectSubDirectories(directoryToProcess);
                directoriesToVisit.addAll(directSubDirectories);

                List<T> changeFilesInTheDirectory =
                        getChangeFilesInDirectory(
                                directoryToProcess,
                                environmentName,
                                rootDirectory,
                                changeFileFactory);

                changeFiles.addAll(changeFilesInTheDirectory);
            }

            if (changeFiles.stream()
                    .anyMatch(it -> it.getChangeType() == ChangeType.APPLY_ON_CHANGE)) {
                if (!context.has(HashService.class)) {
                    throw new IllegalStateException(
                            String.format(
                                    "Hashing must be configured if %s files are used",
                                    ChangeType.APPLY_ON_CHANGE));
                }
            }

            return Collections.unmodifiableList(changeFiles);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<T> getChangeFilesInDirectory(
            Path directoryToProcess,
            String environmentName,
            Path rootDirectory,
            ChangeFileFactory changeFileFactory)
            throws IOException {

        List<T> changeFilesInDirectory;

        try (Stream<Path> directoryContents = Files.list(directoryToProcess)) {

            changeFilesInDirectory =
                    directoryContents
                            .filter(Files::isRegularFile)
                            .filter(isIncludedType(rootDirectory, changeFileFactory))
                            .map(
                                    changeFile ->
                                            getChangeFile(
                                                    rootDirectory, changeFile, changeFileFactory))
                            .filter(it -> matchesDesiredConfig(environmentName, it))
                            .sorted(getChangeFileSortComparator())
                            .collect(Collectors.toList());
        }

        return changeFilesInDirectory;
    }

    protected Comparator<T> getChangeFileSortComparator() {
        return Comparator.comparing(ChangeFile::getOrder);
    }

    private List<Path> getDirectSubDirectories(Path directoryToProcess) throws IOException {
        List<Path> subDirectories;
        try (Stream<Path> directoryContents = Files.list(directoryToProcess)) {
            subDirectories =
                    directoryContents
                            .filter(Files::isDirectory)
                            .sorted(getDirectoryOrderComparator())
                            .collect(Collectors.toList());
        }
        return subDirectories;
    }

    protected Comparator<Path> getDirectoryOrderComparator() {
        return DIRECTORY_COMPARATOR;
    }

    protected abstract T getChangeFile(
            Path changesDirectory, Path changeFile, ChangeFileFactory changeFileFactory);

    private Predicate<Path> isIncludedType(Path rootDirectory, ChangeFileFactory factory) {
        return path -> {
            Path relativePath = rootDirectory.relativize(path);

            ChangeFile.Id id = factory.getIdFromPath(relativePath);

            ChangeType changeType = id.getChangeType();

            return includedChangeTypes.contains(changeType);
        };
    }

    private boolean matchesDesiredConfig(String environmentName, ChangeFile file) {

        boolean matches = true;

        if (file.getEnvironment().isPresent()) {
            String fileNameEnvironment = file.getEnvironment().get();

            matches = fileNameEnvironment.equalsIgnoreCase(environmentName);
        }

        if (matches && filterExpressionPattern != null) {
            // short-circuit evaluation: if previous did not match, skip this check altogether
            matches = filterExpressionPattern.matcher(file.getRelativePath().toString()).matches();
        }

        return matches;
    }
}
