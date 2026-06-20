/*
 * totalschema: tool for managing database versioning and schema changes with ease.
 * Copyright (C) 2025-2026 totalschema development team
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

package io.github.totalschema.cli;

import io.github.totalschema.engine.api.ChangeFileSelector;
import java.util.ArrayList;
import java.util.List;
import picocli.CommandLine;

/**
 * Picocli mixin that adds the {@code -l} / {@code --label} option and a helper method to build a
 * {@link ChangeFileSelector} from the collected options.
 */
public final class LabelFilterMixin {

    @CommandLine.Option(
            names = {"-l", "--label"},
            description =
                    "Select only change files whose effective labels contain KEY=VALUE."
                            + " Specify multiple times for AND semantics.",
            paramLabel = "KEY=VALUE")
    private List<String> labelFilters = new ArrayList<>();

    /**
     * Builds a {@link ChangeFileSelector} combining the given path filter expression with any label
     * filter expressions collected by this mixin.
     *
     * @param filterExpression path regex filter, or {@code null} for no path restriction
     * @return a selector encapsulating both filters
     */
    public ChangeFileSelector buildSelector(String filterExpression) {
        return ChangeFileSelector.builder()
                .filterExpression(filterExpression)
                .labels(labelFilters)
                .build();
    }
}
