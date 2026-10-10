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

package io.github.totalschema.connector.gcp.bigtable.actions.table.modifycolumnfamily;

import com.google.cloud.bigtable.admin.v2.BigtableTableAdminClient;
import com.google.cloud.bigtable.admin.v2.models.GCRules;
import io.github.totalschema.connector.gcp.bigtable.actions.table.TableAction;
import io.github.totalschema.connector.gcp.bigtable.actions.table.createtable.DurationParser;
import io.github.totalschema.connector.gcp.bigtable.actions.table.createtable.GcRule;
import java.util.List;

/**
 * Updates GC rules on an existing column family without recreation.
 *
 * <p>This action modifies the garbage collection rules for an existing column family. Use this to
 * change retention policies (e.g., extend from 7d to 30d) or adjust version limits for compliance
 * and performance tuning.
 *
 * <p>Example YAML configurations:
 *
 * <pre>
 * # Modify family to add max versions rule
 * ModifyColumnFamily:
 *   tableId: users
 *   familyName: profile
 *   maxVersions: 10
 *
 * # Modify family to add max age rule
 * ModifyColumnFamily:
 *   tableId: users
 *   familyName: cache
 *   maxAge: 48h
 *
 * # Modify family with both rules (intersection)
 * ModifyColumnFamily:
 *   tableId: users
 *   familyName: audit
 *   maxVersions: 100
 *   maxAge: 365d
 *
 * # Modify family with union rule
 * ModifyColumnFamily:
 *   tableId: users
 *   familyName: mixed
 *   union:
 *     - maxVersions: 5
 *     - maxAge: 30d
 *
 * # Modify family with intersection rule
 * ModifyColumnFamily:
 *   tableId: users
 *   familyName: sensitive
 *   intersection:
 *     - maxVersions: 3
 *     - maxAge: 90d
 * </pre>
 *
 * <p><strong>Note:</strong> This action replaces the existing GC rules for the column family. If
 * you want to revert to no GC rules (keep all versions indefinitely), use {@code ResetColumnFamily}
 * instead.
 *
 * <p><strong>Use Cases:</strong>
 *
 * <ul>
 *   <li>Changing retention policies (e.g., extend from 7d to 30d)
 *   <li>Compliance requirement updates
 *   <li>Performance tuning (adjust version limits)
 * </ul>
 */
public class ModifyColumnFamily implements TableAction {

    private String tableId;
    private String familyName;
    private Integer maxVersions;
    private String maxAge;
    private List<GcRule> union;
    private List<GcRule> intersection;

    @Override
    public void execute(BigtableTableAdminClient adminClient) {
        if (tableId == null || tableId.isEmpty()) {
            throw new IllegalArgumentException("tableId is required");
        }
        if (familyName == null || familyName.isEmpty()) {
            throw new IllegalArgumentException("familyName is required");
        }

        // Build the GC rule - at least one rule must be specified for modify
        if (!hasGcRules()) {
            throw new IllegalArgumentException(
                    "At least one GC rule must be specified for ModifyColumnFamily. "
                            + "Use ResetColumnFamily to remove all GC rules.");
        }

        GCRules.GCRule gcRule = buildGcRule();

        // Create ModifyColumnFamiliesRequest to update the family
        com.google.cloud.bigtable.admin.v2.models.ModifyColumnFamiliesRequest request =
                com.google.cloud.bigtable.admin.v2.models.ModifyColumnFamiliesRequest.of(tableId);

        request.updateFamily(familyName, gcRule);

        adminClient.modifyFamilies(request);
    }

    /** Checks if this family has any GC rules defined. */
    private boolean hasGcRules() {
        return maxVersions != null || maxAge != null || union != null || intersection != null;
    }

    /**
     * Builds a BigTable GCRule from this family's configuration.
     *
     * @return the constructed GCRule
     * @throws IllegalArgumentException if the configuration is invalid
     */
    private GCRules.GCRule buildGcRule() {
        // Handle explicit union/intersection
        if (union != null && intersection != null) {
            throw new IllegalArgumentException(
                    "Cannot specify both 'union' and 'intersection' for family: " + familyName);
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

        // This should never happen due to hasGcRules() check in execute()
        throw new IllegalStateException("No GC rules defined");
    }

    /** Builds a union GC rule from a list of rules. */
    private GCRules.GCRule buildUnionRule(List<GcRule> rules) {
        if (rules == null || rules.isEmpty()) {
            throw new IllegalArgumentException(
                    "Union rule must have at least one rule for family: " + familyName);
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
                    "Intersection rule must have at least one rule for family: " + familyName);
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

    public String getTableId() {
        return tableId;
    }

    public void setTableId(String tableId) {
        this.tableId = tableId;
    }

    public String getFamilyName() {
        return familyName;
    }

    public void setFamilyName(String familyName) {
        this.familyName = familyName;
    }

    public Integer getMaxVersions() {
        return maxVersions;
    }

    public void setMaxVersions(Integer maxVersions) {
        this.maxVersions = maxVersions;
    }

    public String getMaxAge() {
        return maxAge;
    }

    public void setMaxAge(String maxAge) {
        this.maxAge = maxAge;
    }

    public List<GcRule> getUnion() {
        return union;
    }

    public void setUnion(List<GcRule> union) {
        this.union = union;
    }

    public List<GcRule> getIntersection() {
        return intersection;
    }

    public void setIntersection(List<GcRule> intersection) {
        this.intersection = intersection;
    }

    @Override
    public String toString() {
        return "ModifyColumnFamily{"
                + "tableId='"
                + tableId
                + '\''
                + ", familyName='"
                + familyName
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
