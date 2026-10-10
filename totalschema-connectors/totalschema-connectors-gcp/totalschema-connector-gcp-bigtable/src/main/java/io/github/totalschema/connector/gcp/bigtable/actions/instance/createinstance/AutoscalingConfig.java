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

package io.github.totalschema.connector.gcp.bigtable.actions.instance.createinstance;

/**
 * Autoscaling configuration for a BigTable cluster.
 *
 * <p>Example YAML:
 *
 * <pre>
 * autoscaling:
 *   minNodes: 3
 *   maxNodes: 10
 *   cpuTargetPercent: 70
 * </pre>
 */
public class AutoscalingConfig {

    private Integer minNodes;
    private Integer maxNodes;
    private Integer cpuTargetPercent;

    public Integer getMinNodes() {
        return minNodes;
    }

    public void setMinNodes(Integer minNodes) {
        this.minNodes = minNodes;
    }

    public Integer getMaxNodes() {
        return maxNodes;
    }

    public void setMaxNodes(Integer maxNodes) {
        this.maxNodes = maxNodes;
    }

    public Integer getCpuTargetPercent() {
        return cpuTargetPercent;
    }

    public void setCpuTargetPercent(Integer cpuTargetPercent) {
        this.cpuTargetPercent = cpuTargetPercent;
    }

    @Override
    public String toString() {
        return "AutoscalingConfig{"
                + "minNodes="
                + minNodes
                + ", maxNodes="
                + maxNodes
                + ", cpuTargetPercent="
                + cpuTargetPercent
                + '}';
    }
}
