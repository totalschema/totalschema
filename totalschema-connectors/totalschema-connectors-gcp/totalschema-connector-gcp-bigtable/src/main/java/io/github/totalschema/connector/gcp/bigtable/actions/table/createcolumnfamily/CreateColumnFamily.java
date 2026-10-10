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

package io.github.totalschema.connector.gcp.bigtable.actions.table.createcolumnfamily;

import com.google.cloud.bigtable.admin.v2.BigtableTableAdminClient;
import com.google.cloud.bigtable.admin.v2.models.GCRules;
import io.github.totalschema.connector.gcp.bigtable.actions.table.TableAction;
import io.github.totalschema.connector.gcp.bigtable.actions.table.createtable.DurationParser;
import io.github.totalschema.connector.gcp.bigtable.actions.table.createtable.GcRule;
import java.util.List;

/**
 * Adds a new column family to an existing BigTable table without recreation.
 *
 * <p>Example YAML configurations:
 *
 * <pre>
 * # Create family without GC rules
 * CreateColumnFamily:
 *   tableId: users
 *   familyName: analytics
 *
 * # Create family with max versions rule
 * CreateColumnFamily:
 *   tableId: users
 *   familyName: analytics
 *   maxVersions: 5
 *
 * # Create family with max age rule
 * CreateColumnFamily:
 *   tableId: users
 *   familyName: cache
 *   maxAge: 24h
 *
 * # Create family with both rules (intersection)
 * CreateColumnFamily:
 *   tableId: users
 *   familyName: audit
 *   maxVersions: 100
 *   maxAge: 365d
 *
 * # Create family with union rule
 * CreateColumnFamily:
 *   tableId: users
 *   familyName: mixed
 *   union:
 *     - maxVersions: 3
 *     - maxAge: 24h
 * </pre>
 *
 * <p><strong>Note:</strong> The table must exist before adding a column family. Schema evolution
 * with column families allows non-disruptive schema changes.
 */
public class CreateColumnFamily implements TableAction {

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

        // Build the GC rule if any rules are specified
        GCRules.GCRule gcRule = null;
        if (hasGcRules()) {
            gcRule = buildGcRule();
        }

        // Create ModifyColumnFamiliesRequest to add the family
        com.google.cloud.bigtable.admin.v2.models.ModifyColumnFamiliesRequest request =
                com.google.cloud.bigtable.admin.v2.models.ModifyColumnFamiliesRequest.of(tableId);

        if (gcRule != null) {
            request.addFamily(familyName, gcRule);
        } else {
            request.addFamily(familyName);
        }

        adminClient.modifyFamilies(request);
    }

    /** Checks if this family has any GC rules defined. */
    private boolean hasGcRules() {
        return maxVersions != null || maxAge != null || union != null || intersection != null;
    }

    /**
     * Builds a BigTable GCRule from this family's configuration.
     *
     * @return the constructed GCRule, or null if no rules are defined
     * @throws IllegalArgumentException if the configuration is invalid
     */
    private GCRules.GCRule buildGcRule() {
        if (!hasGcRules()) {
            return null;
        }

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

        return null;
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
        return "CreateColumnFamily{"
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
