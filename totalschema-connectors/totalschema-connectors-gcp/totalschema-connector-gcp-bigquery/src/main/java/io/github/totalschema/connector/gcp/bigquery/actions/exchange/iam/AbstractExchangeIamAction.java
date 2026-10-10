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

package io.github.totalschema.connector.gcp.bigquery.actions.exchange.iam;

import com.google.cloud.bigquery.dataexchange.v1beta1.AnalyticsHubServiceClient;
import com.google.iam.v1.GetIamPolicyRequest;
import com.google.iam.v1.GetPolicyOptions;
import com.google.iam.v1.Policy;
import com.google.iam.v1.SetIamPolicyRequest;
import io.github.totalschema.config.Configuration;
import io.github.totalschema.connector.gcp.bigquery.actions.exchange.AbstractExchangeAction;
import io.github.totalschema.connector.gcp.core.model.iam.IamBinding;
import java.util.List;

/**
 * Abstract base class for actions that manage the IAM policy of a BigQuery Analytics Hub data
 * exchange.
 *
 * <p>Provides common functionality shared by the {@code add}, {@code remove}, and {@code set} IAM
 * policy actions, including:
 *
 * <ul>
 *   <li>The {@code bindings} configuration (a list of role/member entries) and its validation
 *   <li>Fetching the current IAM {@link Policy} for the data exchange via {@link #getCurrentPolicy}
 *   <li>Writing back an updated {@link Policy} via {@link #setIamPolicy}
 * </ul>
 *
 * <p>Subclasses are responsible for deciding how to combine the configured {@code bindings} with
 * the current policy (e.g. merging, removing, or wholesale replacing).
 */
public abstract class AbstractExchangeIamAction extends AbstractExchangeAction {

    /** The role/member bindings to apply, as configured in YAML. */
    protected List<IamBinding> bindings;

    /**
     * Validates that a non-empty {@code bindings} list has been configured, and that each
     * individual binding is itself valid.
     *
     * @param connectorConfiguration the connector configuration containing default values
     * @throws IllegalArgumentException if {@code bindings} is {@code null}/empty, or if any
     *     individual binding fails validation
     */
    @Override
    protected void validate(Configuration connectorConfiguration) {

        super.validate(connectorConfiguration);

        if (bindings == null || bindings.isEmpty()) {
            throw new IllegalArgumentException("bindings is required and must not be empty");
        }

        // Validate all bindings, too
        for (IamBinding binding : bindings) {
            binding.validate();
        }
    }

    /**
     * Fetches the current IAM policy for the data exchange.
     *
     * @param client the Analytics Hub Service client
     * @param connectorConfiguration the connector configuration containing default values, used to
     *     resolve the data exchange's resource name
     * @return the current {@link Policy} attached to the data exchange
     */
    protected final Policy getCurrentPolicy(
            AnalyticsHubServiceClient client, Configuration connectorConfiguration) {

        String resourceName = buildExchangeResourceName(connectorConfiguration);

        GetPolicyOptions getPolicyOptions = GetPolicyOptions.newBuilder().build();

        GetIamPolicyRequest request =
                GetIamPolicyRequest.newBuilder()
                        .setResource(resourceName)
                        .setOptions(getPolicyOptions)
                        .build();

        return client.getIamPolicy(request);
    }

    /**
     * Writes the given IAM policy back to the data exchange, replacing whatever policy is currently
     * attached to it.
     *
     * @param policy the policy to set on the data exchange
     * @param client the Analytics Hub Service client
     * @param connectorConfiguration the connector configuration containing default values, used to
     *     resolve the data exchange's resource name
     */
    protected final void setIamPolicy(
            Policy policy, AnalyticsHubServiceClient client, Configuration connectorConfiguration) {

        String resourceName = buildExchangeResourceName(connectorConfiguration);

        SetIamPolicyRequest.Builder builder = SetIamPolicyRequest.newBuilder();

        builder.setResource(resourceName);
        builder.setPolicy(policy);

        SetIamPolicyRequest setIamPolicyRequest = builder.build();

        client.setIamPolicy(setIamPolicyRequest);
    }

    public final List<IamBinding> getBindings() {
        return bindings;
    }

    public final void setBindings(List<IamBinding> bindings) {
        this.bindings = bindings;
    }
}
