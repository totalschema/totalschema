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

import io.github.totalschema.engine.internal.changefile.ChangeFileFactory;
import io.github.totalschema.model.ChangeType;
import io.github.totalschema.model.RevertFile;
import java.nio.file.Path;
import java.util.Comparator;

public final class GetRevertFilesCommand extends GetChangeFilesCommand<RevertFile> {

    public GetRevertFilesCommand(String filterExpression) {
        super(filterExpression, ChangeType.REVERT);
    }

    @Override
    protected RevertFile getChangeFile(
            Path changesDirectory, Path changeFile, ChangeFileFactory changeFileFactory) {

        return changeFileFactory.getRevertFile(changesDirectory, changeFile);
    }

    protected Comparator<RevertFile> getChangeFileSortComparator() {
        return super.getChangeFileSortComparator().reversed();
    }

    @Override
    protected Comparator<Path> getDirectoryOrderComparator() {
        return super.getDirectoryOrderComparator().reversed();
    }
}
