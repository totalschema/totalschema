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

package io.github.totalschema.connector.gcp.bigquery.actions.exchange.iam.add;

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
 * Contains logic to add IAM policy bindings to a BigQuery Analytics Hub (sharing feature) Data
 * Exchange Object - required for subscription.
 *
 * <p>Fetches the current IAM policy for the data exchange, merges the configured role/member
 * bindings into it (adding new members to an existing role's binding, or creating a new binding if
 * the role isn't present yet), and writes the resulting policy back.
 *
 * <p>Example YAML:
 *
 * <pre>{@code
 * AddExchangeIamPolicyBindings:
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
public class AddExchangeIamPolicyBindings extends AbstractExchangeIamAction {

    private static final Logger logger =
            LoggerFactory.getLogger(AddExchangeIamPolicyBindings.class);

    @Override
    public void execute(AnalyticsHubServiceClient client, Configuration connectorConfiguration) {

        validate(connectorConfiguration);

        logger.info(
                "Adding {} IAM binding(s) to Analytics Hub data exchange: {}",
                bindings.size(),
                dataExchangeId);

        try {
            Policy currentPolicy = getCurrentPolicy(client, connectorConfiguration);

            Policy.Builder policyBuilder = currentPolicy.toBuilder();

            // Add or update bindings
            for (IamBinding bindingConfig : bindings) {
                applyBinding(bindingConfig, policyBuilder);
            }

            Policy updatedPolicy = policyBuilder.build();

            setIamPolicy(updatedPolicy, client, connectorConfiguration);

            logger.info(
                    "IAM policy updated successfully for Analytics Hub data exchange: {}",
                    dataExchangeId);

        } catch (RuntimeException e) {
            throw new BigQueryConnectorException(
                    "Failed to add IAM policy to Analytics Hub data exchange: " + dataExchangeId,
                    e);
        }
    }

    /**
     * Applies a single binding configuration to the policy being built: merges the configured
     * members into the existing binding for the same role, or creates a new binding if the role
     * isn't present yet.
     *
     * @param bindingConfig the role/member configuration to apply
     * @param policyBuilder the policy builder to update
     */
    private void applyBinding(IamBinding bindingConfig, Policy.Builder policyBuilder) {

        String role = Role.of(bindingConfig.getRole()).getValue();

        // Convert string members to their canonical IAM member representation and
        // deduplicate against each other while preserving order.
        Set<String> members = new LinkedHashSet<>();
        for (String member : bindingConfig.getMembers()) {
            members.add(Identity.valueOf(member).strValue());
        }

        Optional<Binding.Builder> existingBindingBuilder = findBindingBuilder(role, policyBuilder);

        if (existingBindingBuilder.isPresent()) {
            // Merge in only the members that aren't already present for this role.
            Binding.Builder builder = existingBindingBuilder.get();

            Set<String> mergedMembers = new LinkedHashSet<>(builder.getMembersList());
            mergedMembers.addAll(members);

            builder.clearMembers().addAllMembers(mergedMembers);
        } else {
            Binding newBinding = Binding.newBuilder().setRole(role).addAllMembers(members).build();
            policyBuilder.addBindings(newBinding);
        }
    }

    /**
     * Looks up the existing binding builder for the given role in the policy being built.
     *
     * <p>Defensive against unexpected {@code null} values that may appear in the nested protobuf
     * structures (e.g. a {@code null} bindings list, or a binding with a {@code null} role) rather
     * than throwing a {@link NullPointerException}.
     *
     * @param role the fully qualified role (e.g. {@code roles/analyticshub.subscriber})
     * @param policyBuilder the policy builder to search
     * @return the matching binding builder, or {@link Optional#empty()} if none exists
     */
    private Optional<Binding.Builder> findBindingBuilder(
            String role, Policy.Builder policyBuilder) {
        if (policyBuilder == null || role == null) {
            return Optional.empty();
        }

        int bindingsCount = policyBuilder.getBindingsCount();

        for (int i = 0; i < bindingsCount; i++) {
            Binding binding = policyBuilder.getBindings(i);
            if (binding != null && role.equals(binding.getRole())) {
                return Optional.of(policyBuilder.getBindingsBuilder(i));
            }
        }
        return Optional.empty();
    }

    @Override
    public String toString() {
        return "AddExchangeIamPolicyBindings{"
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
