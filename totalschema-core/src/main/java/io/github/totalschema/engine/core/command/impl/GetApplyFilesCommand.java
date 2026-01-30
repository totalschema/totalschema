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
import io.github.totalschema.model.ApplyFile;
import io.github.totalschema.model.ChangeFile;
import io.github.totalschema.model.ChangeType;
import java.nio.file.Path;
import java.util.Comparator;

/**
 * Command to retrieve all apply files matching specified change types. Returns files sorted by
 * their order number.
 */
public final class GetApplyFilesCommand extends GetChangeFilesCommand<ApplyFile> {

    public GetApplyFilesCommand(String filterExpression) {
        super(
                filterExpression,
                ChangeType.APPLY,
                ChangeType.APPLY_ALWAYS,
                ChangeType.APPLY_ON_CHANGE);
    }

    @Override
    protected ApplyFile getChangeFile(
            Path changesDirectory, Path changeFile, ChangeFileFactory changeFileFactory) {

        return changeFileFactory.getApplyFile(changesDirectory, changeFile);
    }

    protected Comparator<ApplyFile> getChangeFileSortComparator() {
        return Comparator.comparing(ChangeFile::getOrder);
    }
}
