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
import io.github.totalschema.engine.api.ChangeFileSelector;
import io.github.totalschema.engine.core.command.api.Command;
import io.github.totalschema.engine.core.command.api.CommandContext;
import io.github.totalschema.engine.internal.changefile.ChangeFileFactory;
import io.github.totalschema.engine.internal.changefile.ChangeFileIgnorePatterns;
import io.github.totalschema.engine.internal.changefile.labels.ChangeFileLabels;
import io.github.totalschema.engine.internal.changefile.labels.ChangeFileLabelsCascade;
import io.github.totalschema.engine.internal.changefile.labels.LabelFilter;
import io.github.totalschema.engine.internal.changefile.labels.LabelInheritanceMode;
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
    private final LabelFilter labelFilter;
    private final EnumSet<ChangeType> includedChangeTypes;

    public GetChangeFilesCommand(
            ChangeFileSelector selector,
            ChangeType firstChangeType,
            ChangeType... additionalChangeTypes) {

        includedChangeTypes = EnumSet.of(firstChangeType, additionalChangeTypes);

        String filterExpression = selector != null ? selector.getFilterExpression() : null;
        if (filterExpression != null) {
            filterExpressionPattern = Pattern.compile(filterExpression);
        } else {
            filterExpressionPattern = null;
        }

        this.labelFilter =
                selector != null ? LabelFilter.of(selector.getLabelFilters()) : LabelFilter.empty();
    }

    @Override
    public List<T> execute(CommandContext context) {

        ChangeFileFactory changeFileFactory = context.get(ChangeFileFactory.class);

        String environmentName =
                context.getOptional(Environment.class).map(Environment::getName).orElse(null);

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

        LabelInheritanceMode inheritanceMode =
                LabelInheritanceMode.fromConfig(
                        config.getString("labels", "inheritance").orElse(null));

        ChangeFileIgnorePatterns rootIgnorePatterns = ChangeFileIgnorePatterns.load(rootDirectory);

        LinkedList<T> changeFiles = new LinkedList<>();

        try {
            // Each deque entry pairs a directory with its accumulated label cascade
            Deque<DirectoryEntry> directoriesToVisit = new LinkedList<>();
            ChangeFileLabelsCascade rootCascade =
                    ChangeFileLabelsCascade.empty().withDirectory(rootDirectory, inheritanceMode);
            directoriesToVisit.add(new DirectoryEntry(rootDirectory, rootCascade));

            while (!directoriesToVisit.isEmpty()) {
                DirectoryEntry entry = directoriesToVisit.poll();
                Path directoryToProcess = entry.directory;
                ChangeFileLabelsCascade cascade = entry.cascade;

                ChangeFileIgnorePatterns effectiveIgnorePatterns =
                        getEffectiveIgnorePatterns(
                                directoryToProcess, rootDirectory, rootIgnorePatterns);

                List<Path> directSubDirectories =
                        getDirectSubDirectories(
                                directoryToProcess, rootDirectory, effectiveIgnorePatterns);

                // Load labels for this directory once, then build child cascades
                ChangeFileLabels dirLabels = ChangeFileLabels.load(directoryToProcess);

                for (Path subDir : directSubDirectories) {
                    ChangeFileLabelsCascade childCascade =
                            cascade.withDirectory(dirLabels, inheritanceMode)
                                    .withDirectory(subDir, inheritanceMode);
                    directoriesToVisit.add(new DirectoryEntry(subDir, childCascade));
                }

                List<T> changeFilesInTheDirectory =
                        getChangeFilesInDirectory(
                                directoryToProcess,
                                environmentName,
                                rootDirectory,
                                changeFileFactory,
                                effectiveIgnorePatterns,
                                cascade,
                                dirLabels);

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

    /** Pairs a directory path with its accumulated label cascade. */
    private static final class DirectoryEntry {
        final Path directory;
        final ChangeFileLabelsCascade cascade;

        DirectoryEntry(Path directory, ChangeFileLabelsCascade cascade) {
            this.directory = directory;
            this.cascade = cascade;
        }
    }

    private static ChangeFileIgnorePatterns getEffectiveIgnorePatterns(
            Path currentDirectory,
            Path rootDirectory,
            ChangeFileIgnorePatterns rootIgnorePatterns) {
        // Load per-directory ignore file and combine with root patterns.
        // When visiting the root itself we skip loading to avoid applying
        // root patterns twice (they are already in rootIgnorePatterns).
        ChangeFileIgnorePatterns effectiveIgnorePatterns;
        if (currentDirectory.equals(rootDirectory)) {
            effectiveIgnorePatterns = rootIgnorePatterns;
        } else {
            effectiveIgnorePatterns =
                    rootIgnorePatterns.combine(ChangeFileIgnorePatterns.load(currentDirectory));
        }
        return effectiveIgnorePatterns;
    }

    private List<T> getChangeFilesInDirectory(
            Path directoryToProcess,
            String environmentName,
            Path rootDirectory,
            ChangeFileFactory changeFileFactory,
            ChangeFileIgnorePatterns ignorePatterns,
            ChangeFileLabelsCascade cascade,
            ChangeFileLabels dirLabels)
            throws IOException {

        List<T> changeFilesInDirectory;

        try (Stream<Path> directoryContents = Files.list(directoryToProcess)) {

            changeFilesInDirectory =
                    directoryContents
                            .filter(Files::isRegularFile)
                            .filter(p -> !ignorePatterns.isIgnoredFile(rootDirectory.relativize(p)))
                            .filter(isIncludedType(rootDirectory, changeFileFactory))
                            .map(
                                    changeFile ->
                                            getChangeFile(
                                                    rootDirectory, changeFile, changeFileFactory))
                            .map(it -> resolveAndSetEffectiveLabels(it, cascade, dirLabels))
                            .filter(it -> matchesDesiredConfig(environmentName, it))
                            .sorted(getChangeFileSortComparator())
                            .collect(Collectors.toList());
        }

        return changeFilesInDirectory;
    }

    protected Comparator<T> getChangeFileSortComparator() {
        return Comparator.comparing(ChangeFile::getOrder);
    }

    private List<Path> getDirectSubDirectories(
            Path directoryToProcess, Path rootDirectory, ChangeFileIgnorePatterns ignorePatterns)
            throws IOException {

        List<Path> subDirectories;
        try (Stream<Path> directoryContents = Files.list(directoryToProcess)) {
            subDirectories =
                    directoryContents
                            .filter(Files::isDirectory)
                            .filter(
                                    p ->
                                            !ignorePatterns.isIgnoredDirectory(
                                                    rootDirectory.relativize(p)))
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

    private T resolveAndSetEffectiveLabels(
            T file, ChangeFileLabelsCascade cascade, ChangeFileLabels dirLabels) {

        Map<String, List<String>> effectiveLabels = cascade.resolve(dirLabels, file.getFile());
        logger.debug(
                "Resolved effective labels for file {}: {}",
                file.getRelativePath(),
                effectiveLabels);

        // withEffectiveLabels will always return the same type as the file was itself
        @SuppressWarnings("unchecked")
        T fileWithLabels = (T) file.withEffectiveLabels(effectiveLabels);

        return fileWithLabels;
    }

    private boolean matchesDesiredConfig(String environmentName, T file) {

        if (environmentName != null && file.getEnvironment().isPresent()) {
            if (!file.getEnvironment().get().equalsIgnoreCase(environmentName)) {
                return false;
            }
        }

        if (filterExpressionPattern != null) {
            if (!filterExpressionPattern.matcher(file.getRelativePath().toString()).matches()) {
                return false;
            }
        }

        if (!labelFilter.isEmpty()) {
            return labelFilter.matches(file.getEffectiveLabels());
        }

        return true;
    }
}
