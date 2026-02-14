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

package io.github.totalschema.engine.internal.changefile;

import io.github.totalschema.config.Configuration;
import io.github.totalschema.model.ApplyFile;
import io.github.totalschema.model.ChangeFile;
import io.github.totalschema.model.ChangeType;
import io.github.totalschema.model.RevertFile;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;

public class ChangeFileFactory {

    private static final class FileNameConvention {

        private FileNameConvention() {
            throw new AssertionError("static constant class, no instances allowed");
        }

        public static final String FILENAME_PARTS_SEPARATOR = "\\.";

        private static final int MAX_LENGTH_ALLOWED = 256;

        private static final int NO_ENVIRONMENT_SPECIFIED_FILENAME_PART_COUNT = 5;
        private static final int ENVIRONMENT_SPECIFIED_FILENAME_PART_COUNT = 6;
    }

    private final Integer changeFileNameMaxLength;

    public ChangeFileFactory(Configuration configuration) {

        changeFileNameMaxLength =
                configuration
                        .getInt(ChangeFileFactoryConfiguration.CHANGE_FILE_ID_PATH_MAX_LENGTH)
                        .orElse(FileNameConvention.MAX_LENGTH_ALLOWED);
    }

    public Integer getChangeFileNameMaxLength() {
        return changeFileNameMaxLength;
    }

    public ApplyFile getApplyFile(Path changesDirectory, Path changeFile) {
        ChangeFile.Id id = getIdFromPaths(changesDirectory, changeFile);

        return new ApplyFile(changesDirectory, changeFile, id);
    }

    public RevertFile getRevertFile(Path changesDirectory, Path changeFile) {

        ChangeFile.Id id = getIdFromPaths(changesDirectory, changeFile);

        return new RevertFile(changesDirectory, changeFile, id);
    }

    private ChangeFile.Id getIdFromPaths(Path baseDirectory, Path file) {
        Path relativePath = baseDirectory.relativize(file);
        return getIdFromPath(relativePath);
    }

    public ChangeFile.Id getIdFromPath(Path relativePath) {

        return getIdFromString(relativePath.toString());
    }

    public ChangeFile.Id getIdFromString(String string) {

        Objects.requireNonNull(string, "argument string cannot be null");

        checkLength(string);

        Path filePath = getPath(string);

        String parentDirectoryString = getParentDirectory(string, filePath);

        String fileName = getFileName(filePath);

        String[] fileNameParts = fileName.split(FileNameConvention.FILENAME_PARTS_SEPARATOR);
        if (fileNameParts.length != FileNameConvention.NO_ENVIRONMENT_SPECIFIED_FILENAME_PART_COUNT
                && fileNameParts.length
                        != FileNameConvention.ENVIRONMENT_SPECIFIED_FILENAME_PART_COUNT) {
            throw new IllegalArgumentException(String.format("File name is illegal: '%s'", string));
        }

        Iterator<String> parts = Arrays.asList(fileNameParts).iterator();

        String orderString = parseOrder(parts, string);
        String description = extractDescription(parts, string);
        String environment = extractEnvironment(fileNameParts.length, parts, string);
        ChangeType changeType = extractChangeType(parts, string);
        String connector = extractConnector(parts, string);
        String extension = extractExtension(parts, string);

        return new ChangeFile.Id(
                parentDirectoryString,
                orderString,
                description,
                environment,
                changeType,
                connector,
                extension);
    }

    private void checkLength(String string) {
        if (string.length() > changeFileNameMaxLength) {
            throw new IllegalArgumentException(
                    String.format(
                            "Maximum allowed length limit for parent directory "
                                    + "name and file name combined is %s, was %s for %s. Adjustable via config: %s",
                            changeFileNameMaxLength,
                            string.length(),
                            string,
                            changeFileNameMaxLength));
        }
    }

    private static Path getPath(String string) {
        Path filePath = Paths.get(string);
        Objects.requireNonNull(filePath, "filePath was null for input: " + string);

        if (filePath.isAbsolute()) {
            throw new IllegalStateException(
                    "A relative path is expected here, but was: " + filePath);
        }
        return filePath;
    }

    private static String getParentDirectory(String string, Path filePath) {
        Objects.requireNonNull(string, "string was null");
        Objects.requireNonNull(filePath, "filePath was null for input: " + string);

        Path parentDirectory = filePath.getParent();
        Objects.requireNonNull(
                parentDirectory, String.format("parentDirectory was null for input: '%s'", string));

        // Normalize path to a common format, replacing Windows path separators with forward slash
        return parentDirectory.toString().replace("\\", "/");
    }

    private static String getFileName(Path filePath) {
        Path fileNamePath = filePath.getFileName();
        if (fileNamePath == null) {
            throw new NullPointerException("File name cannot be null for: " + filePath);
        }

        return fileNamePath.toString();
    }

    private static String parseOrder(Iterator<String> parts, String path) {
        try {
            String value = parts.next();

            int order = Integer.parseInt(value);
            if (order < 0) {
                throw new IllegalArgumentException("Order cannot be a negative number");
            }

            return value;

        } catch (NumberFormatException nfe) {
            throw new RuntimeException("Failed to extract order from: " + path);
        }
    }

    private static String extractDescription(Iterator<String> parts, String path) {
        String value = parts.next();

        if (value.isBlank()) {
            throw new IllegalArgumentException("Found blank description in: " + path);
        }

        return value;
    }

    private static String extractEnvironment(
            int partCount, Iterator<String> fileNameParts, String path) {
        try {

            String environment;
            if (partCount == FileNameConvention.ENVIRONMENT_SPECIFIED_FILENAME_PART_COUNT) {
                environment = fileNameParts.next();
            } else {
                environment = null;
            }
            return environment;

        } catch (RuntimeException ex) {
            throw new RuntimeException("Failed to extract environment from: " + path);
        }
    }

    private static ChangeType extractChangeType(Iterator<String> fileNameParts, String path) {

        try {
            String value = fileNameParts.next();

            ChangeType changeType;

            if (value.equalsIgnoreCase("null")) {
                changeType = null;
            } else {
                changeType = ChangeType.getChangeType(value);
            }

            return changeType;

        } catch (RuntimeException ex) {
            throw new RuntimeException("Failed to extract ChangeType from: " + path, ex);
        }
    }

    private static String extractConnector(Iterator<String> parts, String path) {
        String value = parts.next();

        if (value.isBlank()) {
            throw new IllegalArgumentException("Found blank connector in: " + path);
        }

        return value;
    }

    private static String extractExtension(Iterator<String> parts, String path) {
        String value = parts.next();

        if (value.isBlank()) {
            throw new IllegalArgumentException("Found blank extension in: " + path);
        }

        return value;
    }
}
