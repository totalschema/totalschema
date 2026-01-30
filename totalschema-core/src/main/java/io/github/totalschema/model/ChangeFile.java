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

package io.github.totalschema.model;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Abstract base class representing a change file containing database modifications.
 *
 * <p>Change files are identified by their metadata and can be of various types (apply, revert,
 * etc.).
 */
public abstract class ChangeFile {

    /** Unique identifier for a change file containing all its metadata. */
    public static final class Id {

        private static final int INVALID_ORDER = -1;

        private final String parentDirectory;

        private final String orderString;

        private volatile int order = INVALID_ORDER;

        private final String description;

        private final String environment;
        private final ChangeType changeType;
        private final String connector;
        private final String extension;

        /**
         * Constructs a change file identifier with all metadata fields.
         *
         * @param parentDirectory the parent directory path
         * @param orderString the execution order as a string
         * @param description the change description
         * @param environment the target environment (nullable)
         * @param changeType the type of change
         * @param connector the connector name
         * @param extension the file extension
         */
        public Id(
                String parentDirectory,
                String orderString,
                String description,
                String environment,
                ChangeType changeType,
                String connector,
                String extension) {

            this.parentDirectory = parentDirectory;
            this.orderString = orderString;
            this.description = description;
            this.environment = environment;
            this.changeType = changeType;
            this.connector = connector;
            this.extension = extension;
        }

        /**
         * Returns the string representation of this identifier.
         *
         * @return the formatted string representation
         */
        public String toStringRepresentation() {

            final StringBuilder sb = new StringBuilder();
            sb.append(parentDirectory).append('/');
            sb.append(orderString).append('.');
            sb.append(description).append('.');

            if (environment != null) {
                sb.append(environment).append('.');
            }

            sb.append(changeType.name().toLowerCase(Locale.ENGLISH)).append('.');
            sb.append(connector).append('.');
            sb.append(extension);

            return sb.toString();
        }

        /**
         * Gets the parent directory path.
         *
         * @return the parent directory
         */
        public String getParentDirectory() {
            return parentDirectory;
        }

        /**
         * Gets the execution order as an integer.
         *
         * @return the execution order
         */
        public int getOrder() {
            if (order == INVALID_ORDER) {
                // no locking as order is volatile
                order = Integer.parseInt(orderString);
            }

            return order;
        }

        /**
         * Gets the change description.
         *
         * @return the description
         */
        public String getDescription() {
            return description;
        }

        /**
         * Gets the target environment if specified.
         *
         * @return an Optional containing the environment, or empty if none specified
         */
        public Optional<String> getEnvironment() {
            return Optional.ofNullable(environment);
        }

        /**
         * Gets the change type.
         *
         * @return the change type
         */
        public ChangeType getChangeType() {
            return changeType;
        }

        /**
         * Gets the connector name.
         *
         * @return the connector name
         */
        public String getConnector() {
            return connector;
        }

        /**
         * Gets the file extension.
         *
         * @return the file extension
         */
        public String getExtension() {
            return extension;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Id that = (Id) o;
            return Objects.equals(parentDirectory, that.parentDirectory)
                    && Objects.equals(orderString, that.orderString)
                    && Objects.equals(description, that.description)
                    && Objects.equals(environment, that.environment)
                    && changeType == that.changeType
                    && Objects.equals(connector, that.connector)
                    && Objects.equals(extension, that.extension);
        }

        @Override
        public int hashCode() {
            return Objects.hash(
                    parentDirectory,
                    orderString,
                    description,
                    environment,
                    changeType,
                    connector,
                    extension);
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("Id{");
            sb.append("parentDirectory='").append(parentDirectory).append('\'');
            sb.append(", orderString='").append(orderString).append('\'');
            sb.append(", description='").append(description).append('\'');
            sb.append(", environment='").append(environment).append('\'');
            sb.append(", changeType=").append(changeType);
            sb.append(", connector='").append(connector).append('\'');
            sb.append(", extension='").append(extension).append('\'');
            sb.append(", order=").append(getOrder());
            sb.append(", toStringRepresentation='").append(toStringRepresentation()).append('\'');
            sb.append('}');
            return sb.toString();
        }

        /**
         * Creates a new Id with a different change type.
         *
         * @param newChangeType the new change type
         * @return a new Id instance with the specified change type
         */
        public Id withChangeType(ChangeType newChangeType) {

            return new Id(
                    parentDirectory,
                    orderString,
                    description,
                    environment,
                    newChangeType,
                    connector,
                    extension);
        }
    }

    private final Id id;

    private final Path file;
    private final Path relativePath;

    /**
     * Constructs a ChangeFile with the specified properties.
     *
     * @param baseDirectory the base directory for resolving relative paths
     * @param file the absolute path to the change file
     * @param id the unique identifier for this change file
     */
    protected ChangeFile(Path baseDirectory, Path file, Id id) {
        this.id = id;
        this.file = file;
        this.relativePath = baseDirectory.relativize(file);
    }

    /**
     * Gets the absolute file path.
     *
     * @return the file path
     */
    public Path getFile() {
        return file;
    }

    /**
     * Gets the relative path from the base directory.
     *
     * @return the relative path
     */
    public Path getRelativePath() {
        return relativePath;
    }

    /**
     * Gets the change file identifier.
     *
     * @return the identifier
     */
    public Id getId() {
        return id;
    }

    /**
     * Gets the change type from the identifier.
     *
     * @return the change type
     */
    public ChangeType getChangeType() {
        return id.getChangeType();
    }

    /**
     * Gets the target environment from the identifier.
     *
     * @return an Optional containing the environment, or empty if none specified
     */
    public Optional<String> getEnvironment() {
        return id.getEnvironment();
    }

    /**
     * Gets the connector name from the identifier.
     *
     * @return the connector name
     */
    public String getConnector() {
        return id.getConnector();
    }

    public int getOrder() {
        return id.getOrder();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append("{");
        sb.append("id=").append(id);
        sb.append(", relativePath=").append(relativePath);
        sb.append(", file=").append(file);
        sb.append('}');
        return sb.toString();
    }
}
