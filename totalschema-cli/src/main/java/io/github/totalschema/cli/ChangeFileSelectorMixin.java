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
import picocli.CommandLine;

/**
 * Picocli mixin that combines the {@code -f} / {@code --filterExpression} path-filter option with
 * the {@code -l} / {@code --label} label-filter options (via {@link LabelFilterMixin}) and exposes
 * a single {@link #buildSelector()} method to construct the resulting {@link ChangeFileSelector}.
 *
 * <p>Use this mixin in any command that accepts both a path filter and label filters:
 *
 * <pre>{@code
 * @CommandLine.Mixin private ChangeFileSelectorMixin selectorMixin = new ChangeFileSelectorMixin();
 *
 * // inside run():
 * ChangeFileSelector selector = selectorMixin.buildSelector();
 * }</pre>
 */
public final class ChangeFileSelectorMixin {

    @CommandLine.Option(
            names = {"-f", "--filterExpression"},
            description = "Include change files matching this expression only")
    private String filterExpression;

    @CommandLine.Mixin private LabelFilterMixin labelFilterMixin = new LabelFilterMixin();

    /**
     * Builds a {@link ChangeFileSelector} from the path filter expression and any label filter
     * expressions collected by this mixin.
     *
     * @return a selector encapsulating both filters
     */
    public ChangeFileSelector buildSelector() {
        return labelFilterMixin.buildSelector(filterExpression);
    }
}
