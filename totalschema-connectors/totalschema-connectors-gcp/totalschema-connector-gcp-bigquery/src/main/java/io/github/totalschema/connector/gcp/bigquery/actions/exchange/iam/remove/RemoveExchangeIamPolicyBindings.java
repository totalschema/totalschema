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

package io.github.totalschema.connector.gcp.bigquery.actions.exchange.iam.remove;

import com.google.cloud.Identity;
import com.google.cloud.Role;
import com.google.cloud.bigquery.dataexchange.v1beta1.AnalyticsHubServiceClient;
import com.google.iam.v1.Binding;
import com.google.iam.v1.Policy;
import io.github.totalschema.config.Configuration;
import io.github.totalschema.connector.gcp.bigquery.BigQueryConnectorException;
import io.github.totalschema.connector.gcp.bigquery.actions.exchange.iam.AbstractExchangeIamAction;
import io.github.totalschema.connector.gcp.core.model.iam.IamBinding;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contains logic to remove IAM policy bindings from a BigQuery Analytics Hub (sharing feature) Data
 * Exchange Object.
 *
 * <p>Fetches the current IAM policy for the data exchange, removes the configured members from the
 * matching role's binding, and writes the resulting policy back. If removing the configured members
 * leaves a binding with no members left, the binding itself is removed from the policy.
 *
 * <p>Mirrors the semantics of {@code gcloud ... remove-iam-policy-binding}: this action is
 * idempotent. If the configured role has no binding on the policy, or a configured member isn't
 * present in that role's binding, it is silently ignored rather than treated as an error.
 *
 * <p>Example YAML:
 *
 * <pre>{@code
 * RemoveExchangeIamPolicyBindings:
 *   dataExchangeId: my_exchange
 *   bindings:
 *     - role: roles/analyticshub.subscriber
 *       members:
 *         - serviceAccount:analytics@project.iam.gserviceaccount.com
 *         - user:analyst@company.com
 *     - role: roles/analyticshub.viewer
 *       members:
 *         - group:viewers@company.com
 *   # Optional - defaults to connector configuration:
 *   # project: my-project
 *   # location: US
 * }</pre>
 */
public class RemoveExchangeIamPolicyBindings extends AbstractExchangeIamAction {

    private static final Logger logger =
            LoggerFactory.getLogger(RemoveExchangeIamPolicyBindings.class);

    @Override
    public void execute(AnalyticsHubServiceClient client, Configuration connectorConfiguration) {

        validate(connectorConfiguration);

        logger.info(
                "Removing {} IAM binding(s) from Analytics Hub data exchange: {}",
                bindings.size(),
                dataExchangeId);

        try {
            Policy currentPolicy = getCurrentPolicy(client, connectorConfiguration);

            Policy.Builder policyBuilder = currentPolicy.toBuilder();

            // Remove members (and empty bindings) for each configured binding
            for (IamBinding bindingConfig : bindings) {
                removeBinding(bindingConfig, policyBuilder);
            }

            Policy updatedPolicy = policyBuilder.build();

            setIamPolicy(updatedPolicy, client, connectorConfiguration);

            logger.info(
                    "IAM policy updated successfully for Analytics Hub data exchange: {}",
                    dataExchangeId);

        } catch (RuntimeException e) {
            throw new BigQueryConnectorException(
                    "Failed to remove IAM policy binding from Analytics Hub data exchange: "
                            + dataExchangeId,
                    e);
        }
    }

    /**
     * Removes a single binding configuration's members from the policy being built.
     *
     * <p>If the role has no matching binding, this is a no-op. Members not present in the existing
     * binding are silently ignored. If the binding has no members left after removal, the whole
     * binding is removed from the policy.
     *
     * @param bindingConfig the role/member configuration to remove
     * @param policyBuilder the policy builder to update
     */
    private void removeBinding(IamBinding bindingConfig, Policy.Builder policyBuilder) {

        String role = Role.of(bindingConfig.getRole()).getValue();

        Set<String> membersToRemove = new LinkedHashSet<>();
        for (String member : bindingConfig.getMembers()) {
            membersToRemove.add(Identity.valueOf(member).strValue());
        }

        Optional<Integer> existingBindingIndex = findBindingIndex(role, policyBuilder);

        if (existingBindingIndex.isEmpty()) {
            logger.debug(
                    "No existing binding found for role '{}', nothing to remove for data exchange:"
                            + " {}",
                    role,
                    dataExchangeId);
            return;
        }

        int index = existingBindingIndex.get();
        Binding.Builder bindingsBuilder = policyBuilder.getBindingsBuilder(index);

        Set<String> remainingMembers = new LinkedHashSet<>(bindingsBuilder.getMembersList());
        remainingMembers.removeAll(membersToRemove);

        if (remainingMembers.isEmpty()) {
            // Nothing left for this role: drop the binding entirely.
            policyBuilder.removeBindings(index);
        } else {
            bindingsBuilder.clearMembers().addAllMembers(remainingMembers);
        }
    }

    /**
     * Looks up the index of the existing binding for the given role in the policy being built.
     *
     * <p>Defensive against unexpected {@code null} values that may appear in the nested protobuf
     * structures (e.g. a {@code null} bindings list, or a binding with a {@code null} role) rather
     * than throwing a {@link NullPointerException}.
     *
     * @param role the fully qualified role (e.g. {@code roles/analyticshub.subscriber})
     * @param policyBuilder the policy builder to search
     * @return the index of the matching binding, or {@link Optional#empty()} if none exists
     */
    private Optional<Integer> findBindingIndex(String role, Policy.Builder policyBuilder) {
        if (policyBuilder == null || role == null) {
            return Optional.empty();
        }

        int bindingsCount = policyBuilder.getBindingsCount();

        for (int i = 0; i < bindingsCount; i++) {
            Binding binding = policyBuilder.getBindings(i);
            if (binding != null && role.equals(binding.getRole())) {
                return Optional.of(i);
            }
        }
        return Optional.empty();
    }

    // Jackson requires getters and setters

    @Override
    public String toString() {
        return "RemoveExchangeIamPolicyBindings{"
                + "project='"
                + project
                + '\''
                + ", location='"
                + location
                + '\''
                + ", dataExchangeId='"
                + dataExchangeId
                + '\''
                + ", bindings="
                + bindings
                + '}';
    }
}
