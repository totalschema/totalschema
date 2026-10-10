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

package io.github.totalschema.connector.gcp.bigtable.actions.table.createtable;

import com.google.cloud.bigtable.admin.v2.models.GCRules;

/**
 * Represents a single GC rule within a union or intersection.
 *
 * <p>Example YAML usage:
 *
 * <pre>
 * union:
 *   - maxVersions: 3
 *   - maxAge: 7d
 * </pre>
 */
public class GcRule {

    private Integer maxVersions;
    private String maxAge;

    /** Gets the maximum number of versions to retain. */
    public Integer getMaxVersions() {
        return maxVersions;
    }

    /** Sets the maximum number of versions to retain. */
    public void setMaxVersions(Integer maxVersions) {
        this.maxVersions = maxVersions;
    }

    /** Gets the maximum age as a duration string. */
    public String getMaxAge() {
        return maxAge;
    }

    /** Sets the maximum age as a duration string. */
    public void setMaxAge(String maxAge) {
        this.maxAge = maxAge;
    }

    /**
     * Converts this rule to a BigTable GCRule.
     *
     * @return the GCRule, or null if no rule is defined
     */
    public GCRules.GCRule toGcRule() {
        if (maxVersions != null && maxAge != null) {
            // Both specified = implicit intersection
            return GCRules.GCRULES
                    .intersection()
                    .rule(GCRules.GCRULES.maxVersions(maxVersions))
                    .rule(GCRules.GCRULES.maxAge(DurationParser.parse(maxAge)));
        }

        if (maxVersions != null) {
            return GCRules.GCRULES.maxVersions(maxVersions);
        }

        if (maxAge != null) {
            return GCRules.GCRULES.maxAge(DurationParser.parse(maxAge));
        }

        return null;
    }

    @Override
    public String toString() {
        return "GcRule{" + "maxVersions=" + maxVersions + ", maxAge='" + maxAge + '\'' + '}';
    }
}
