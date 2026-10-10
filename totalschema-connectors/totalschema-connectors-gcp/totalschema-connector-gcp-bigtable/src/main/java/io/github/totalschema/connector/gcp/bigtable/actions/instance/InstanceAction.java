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

package io.github.totalschema.connector.gcp.bigtable.actions.instance;

import com.google.cloud.bigtable.admin.v2.BigtableInstanceAdminClient;
import io.github.totalschema.connector.gcp.bigtable.actions.Action;

/**
 * Actions that operate on BigTable instances.
 *
 * <p>Examples: CreateInstance, DeleteInstance, EnableTableReplication, SetTableAutoscaling, etc.
 *
 * <p>These actions require {@link BigtableInstanceAdminClient} and operate at the infrastructure
 * level (instance and cluster management) rather than within a specific instance.
 *
 * <p><strong>Note:</strong> Instance-level operations are typically handled by
 * infrastructure-as-code tools (Terraform, Pulumi) in production environments. TotalSchema's
 * instance actions are designed for development/testing workflows, automated CI/CD pipelines, and
 * programmatic SaaS provisioning.
 */
public interface InstanceAction extends Action<BigtableInstanceAdminClient> {
    /**
     * Executes the instance-level action with type-safe client.
     *
     * @param adminClient the BigTable instance admin client
     */
    void execute(BigtableInstanceAdminClient adminClient);
}
