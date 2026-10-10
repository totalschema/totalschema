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
import java.util.List;

/**
 * Represents a BigTable column family with optional garbage collection rules.
 *
 * <p>Example YAML configurations:
 *
 * <pre>
 * # Simple family without GC rules
 * - name: profile
 *
 * # Family with max versions rule
 * - name: settings
 *   maxVersions: 3
 *
 * # Family with max age rule
 * - name: logs
 *   maxAge: 7d
 *
 * # Family with intersection rule (both must be satisfied)
 * - name: audit
 *   maxVersions: 10
 *   maxAge: 90d
 *
 * # Family with explicit intersection
 * - name: important
 *   intersection:
 *     - maxVersions: 100
 *     - maxAge: 365d
 *
 * # Family with union rule (any rule can be satisfied)
 * - name: cache
 *   union:
 *     - maxVersions: 3
 *     - maxAge: 24h
 * </pre>
 */
public class ColumnFamily {

    private String name;
    private Integer maxVersions;
    private String maxAge;
    private List<GcRule> union;
    private List<GcRule> intersection;

    /** Gets the column family name. */
    public String getName() {
        return name;
    }

    /** Sets the column family name. */
    public void setName(String name) {
        this.name = name;
    }

    /** Gets the maximum number of versions to retain. */
    public Integer getMaxVersions() {
        return maxVersions;
    }

    /** Sets the maximum number of versions to retain. */
    public void setMaxVersions(Integer maxVersions) {
        this.maxVersions = maxVersions;
    }

    /** Gets the maximum age as a duration string (e.g., "7d", "24h", "90d"). */
    public String getMaxAge() {
        return maxAge;
    }

    /** Sets the maximum age as a duration string (e.g., "7d", "24h", "90d"). */
    public void setMaxAge(String maxAge) {
        this.maxAge = maxAge;
    }

    /** Gets the union rules (keep if ANY rule matches). */
    public List<GcRule> getUnion() {
        return union;
    }

    /** Sets the union rules (keep if ANY rule matches). */
    public void setUnion(List<GcRule> union) {
        this.union = union;
    }

    /** Gets the intersection rules (keep if ALL rules match). */
    public List<GcRule> getIntersection() {
        return intersection;
    }

    /** Sets the intersection rules (keep if ALL rules match). */
    public void setIntersection(List<GcRule> intersection) {
        this.intersection = intersection;
    }

    /** Checks if this family has any GC rules defined. */
    public boolean hasGcRules() {
        return maxVersions != null || maxAge != null || union != null || intersection != null;
    }

    /**
     * Builds a BigTable GCRule from this family's configuration.
     *
     * @return the constructed GCRule, or null if no rules are defined
     * @throws IllegalArgumentException if the configuration is invalid
     */
    public GCRules.GCRule buildGcRule() {
        if (!hasGcRules()) {
            return null;
        }

        // Handle explicit union/intersection
        if (union != null && intersection != null) {
            throw new IllegalArgumentException(
                    "Cannot specify both 'union' and 'intersection' for family: " + name);
        }

        if (union != null) {
            return buildUnionRule(union);
        }

        if (intersection != null) {
            return buildIntersectionRule(intersection);
        }

        // Handle implicit intersection (maxVersions AND maxAge at top level)
        if (maxVersions != null && maxAge != null) {
            return GCRules.GCRULES
                    .intersection()
                    .rule(GCRules.GCRULES.maxVersions(maxVersions))
                    .rule(GCRules.GCRULES.maxAge(DurationParser.parse(maxAge)));
        }

        // Handle single rule
        if (maxVersions != null) {
            return GCRules.GCRULES.maxVersions(maxVersions);
        }

        if (maxAge != null) {
            return GCRules.GCRULES.maxAge(DurationParser.parse(maxAge));
        }

        return null;
    }

    /** Builds a union GC rule from a list of rules. */
    private GCRules.GCRule buildUnionRule(List<GcRule> rules) {
        if (rules == null || rules.isEmpty()) {
            throw new IllegalArgumentException(
                    "Union rule must have at least one rule for family: " + name);
        }

        GCRules.UnionRule unionRule = GCRules.GCRULES.union();
        for (GcRule rule : rules) {
            GCRules.GCRule gcRule = rule.toGcRule();
            if (gcRule != null) {
                unionRule.rule(gcRule);
            }
        }
        return unionRule;
    }

    /** Builds an intersection GC rule from a list of rules. */
    private GCRules.GCRule buildIntersectionRule(List<GcRule> rules) {
        if (rules == null || rules.isEmpty()) {
            throw new IllegalArgumentException(
                    "Intersection rule must have at least one rule for family: " + name);
        }

        GCRules.IntersectionRule intersectionRule = GCRules.GCRULES.intersection();
        for (GcRule rule : rules) {
            GCRules.GCRule gcRule = rule.toGcRule();
            if (gcRule != null) {
                intersectionRule.rule(gcRule);
            }
        }
        return intersectionRule;
    }

    @Override
    public String toString() {
        return "ColumnFamily{"
                + "name='"
                + name
                + '\''
                + ", maxVersions="
                + maxVersions
                + ", maxAge='"
                + maxAge
                + '\''
                + ", union="
                + union
                + ", intersection="
                + intersection
                + '}';
    }
}
