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

package io.github.totalschema.connector.gcp.bigquery.actions.scheduledquery.create;

import com.google.cloud.bigquery.datatransfer.v1.CreateTransferConfigRequest;
import com.google.cloud.bigquery.datatransfer.v1.DataTransferServiceClient;
import com.google.cloud.bigquery.datatransfer.v1.DeleteTransferConfigRequest;
import com.google.cloud.bigquery.datatransfer.v1.LocationName;
import com.google.cloud.bigquery.datatransfer.v1.TransferConfig;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import io.github.totalschema.config.Configuration;
import io.github.totalschema.connector.gcp.bigquery.BigQueryConnectorException;
import io.github.totalschema.connector.gcp.bigquery.actions.scheduledquery.AbstractScheduledQueryAction;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Action for creating a BigQuery scheduled query using the Data Transfer Service.
 *
 * <p>Example YAML:
 *
 * <pre>{@code
 * CreateScheduledQuery:
 *   displayName: Daily Sales Summary
 *   query: |
 *     SELECT DATE(order_date) as date, SUM(amount) as total
 *     FROM `project.dataset.orders`
 *     WHERE DATE(order_date) = CURRENT_DATE()
 *     GROUP BY date
 *   destination:
 *     datasetId: analytics
 *     tableId: daily_sales_summary
 *     writeDisposition: WRITE_APPEND  # WRITE_APPEND (default) or WRITE_TRUNCATE
 *     partitioningField: date_column    # Optional
 *   schedule: "every day 00:00"
 *   # Optional - defaults to connector configuration:
 *   # projectId: my-gcp-project
 *   # location: US
 *   # serviceAccount: my-sa@my-project.iam.gserviceaccount.com
 *   disabled: false
 *   replaceIfExists: false
 * }</pre>
 *
 * <p><strong>Configuration Defaults:</strong>
 *
 * <ul>
 *   <li>{@code projectId} - If not specified in YAML, uses {@code defaults.projectId} from
 *       connector configuration. Required.
 *   <li>{@code location} - If not specified in YAML, uses {@code defaults.location} from connector
 *       configuration. Defaults to "US" if neither is specified.
 *   <li>{@code serviceAccount} - Service account email to run the scheduled query. If not
 *       specified, uses {@code defaults.serviceAccount} from connector configuration. If neither is
 *       specified, defaults to the project's default compute service account.
 *   <li>{@code destination.writeDisposition} - Defaults to "WRITE_APPEND" if not specified.
 * </ul>
 *
 * <p>Schedule syntax follows the Data Transfer Service format. Common examples:
 *
 * <ul>
 *   <li>{@code "every day 00:00"} - Daily at midnight UTC
 *   <li>{@code "every 4 hours"} - Every 4 hours
 *   <li>{@code "every monday 09:00"} - Weekly on Mondays at 9 AM UTC
 *   <li>{@code "first sunday of quarter 00:00"} - Quarterly
 * </ul>
 *
 * <p>The query can reference parameters like {@code @run_time} and {@code @run_date}.
 */
public class CreateScheduledQuery extends AbstractScheduledQueryAction {
    private static final Logger logger = LoggerFactory.getLogger(CreateScheduledQuery.class);

    private String query;
    private ScheduledQueryDestination destination;
    private String schedule;
    private String serviceAccount;
    private Boolean disabled;
    private Boolean replaceIfExists;
    private Map<String, String> parameters;

    @Override
    public void execute(
            DataTransferServiceClient dataTransferServiceClient,
            Configuration connectorConfiguration) {
        logger.info("Creating scheduled query: {}", displayName);

        // Get effective configuration values
        String effectiveProjectId = getEffectiveProjectId(connectorConfiguration);
        String effectiveLocation = getEffectiveLocation(connectorConfiguration);
        String effectiveServiceAccount =
                getEffectiveServiceAccount(effectiveProjectId, connectorConfiguration);

        // Build transfer config
        Struct params = buildParametersStruct();
        TransferConfig transferConfig = buildTransferConfig(params);

        // Handle replaceIfExists logic
        if (replaceIfExists != null && replaceIfExists) {
            deleteExistingScheduledQuery(
                    dataTransferServiceClient, effectiveProjectId, effectiveLocation);
        }

        // Create the scheduled query
        createScheduledQuery(
                dataTransferServiceClient,
                effectiveProjectId,
                effectiveLocation,
                effectiveServiceAccount,
                transferConfig);
    }

    /**
     * Gets the effective service account from action config, connector defaults, or generates
     * default.
     *
     * @param projectId the GCP project ID, used to generate default service account if needed
     * @param connectorConfiguration the connector configuration containing default values
     * @return the effective service account email to use for the scheduled query
     */
    private String getEffectiveServiceAccount(
            String projectId, Configuration connectorConfiguration) {
        String effectiveServiceAccount = serviceAccount;
        if (effectiveServiceAccount == null || effectiveServiceAccount.isEmpty()) {
            effectiveServiceAccount =
                    connectorConfiguration.getString("defaults.serviceAccount").orElse(null);
            if (effectiveServiceAccount == null || effectiveServiceAccount.isEmpty()) {
                // Fallback to default compute service account:
                // {project-id}@cloudservices.gserviceaccount.com
                effectiveServiceAccount = projectId + "@cloudservices.gserviceaccount.com";
                logger.debug(
                        "Using default service account for project: {}", effectiveServiceAccount);
            } else {
                logger.debug(
                        "Using default service account from connector config: {}",
                        effectiveServiceAccount);
            }
        }
        return effectiveServiceAccount;
    }

    /**
     * Builds the parameters struct for the transfer config.
     *
     * <p>Includes the query and any destination-specific parameters (write disposition, table
     * template, partitioning field) along with custom parameters provided by the user.
     *
     * @return a {@link Struct} containing all transfer config parameters
     */
    private Struct buildParametersStruct() {
        Struct.Builder paramsBuilder = Struct.newBuilder();
        paramsBuilder.putFields("query", Value.newBuilder().setStringValue(query).build());

        // Configure destination parameters if provided
        if (destination != null) {
            addDestinationParameters(paramsBuilder, destination);
        }

        // Add any custom parameters
        if (parameters != null) {
            for (Map.Entry<String, String> entry : parameters.entrySet()) {
                paramsBuilder.putFields(
                        entry.getKey(),
                        Value.newBuilder().setStringValue(entry.getValue()).build());
            }
        }

        return paramsBuilder.build();
    }

    /**
     * Adds destination-specific parameters to the params builder.
     *
     * <p>Configures write disposition (default WRITE_APPEND), destination table name template, and
     * partitioning field based on the destination configuration.
     *
     * @param paramsBuilder the Struct builder to add parameters to
     * @param destination the destination configuration (never null)
     * @throws NullPointerException if destination is null
     */
    private static void addDestinationParameters(
            Struct.Builder paramsBuilder, ScheduledQueryDestination destination) {
        Objects.requireNonNull(destination, "destination must not be null");

        // Set write disposition (default to WRITE_APPEND)
        String writeDisposition =
                getValueOrDefault(destination.getWriteDisposition(), "WRITE_APPEND");
        putField(paramsBuilder, "write_disposition", writeDisposition);

        // Set destination table name template if tableId is specified
        if (isPresent(destination.getTableId())) {
            putField(paramsBuilder, "destination_table_name_template", destination.getTableId());
        }

        // Set partitioning field if specified, otherwise empty string
        String partitioningField = getValueOrDefault(destination.getPartitioningField(), "");
        putField(paramsBuilder, "partitioning_field", partitioningField);
    }

    /**
     * Gets a value or returns a default if the value is null or empty.
     *
     * @param value the value to check
     * @param defaultValue the default value to use if value is null or empty
     * @return the value if present, otherwise the defaultValue
     */
    private static String getValueOrDefault(String value, String defaultValue) {
        return (value != null && !value.isEmpty()) ? value : defaultValue;
    }

    /**
     * Checks if a string value is present (not null and not empty).
     *
     * @param value the value to check
     * @return true if value is not null and not empty, false otherwise
     */
    private static boolean isPresent(String value) {
        return value != null && !value.isEmpty();
    }

    /**
     * Puts a string field into the Struct builder.
     *
     * @param builder the Struct builder
     * @param fieldName the field name
     * @param fieldValue the field value
     */
    private static void putField(Struct.Builder builder, String fieldName, String fieldValue) {
        builder.putFields(fieldName, Value.newBuilder().setStringValue(fieldValue).build());
    }

    /**
     * Builds the transfer config with display name, data source, schedule, and params.
     *
     * @param params the parameters struct containing query and destination configuration
     * @return a configured {@link TransferConfig} ready to be sent to the Data Transfer Service
     */
    private TransferConfig buildTransferConfig(Struct params) {
        TransferConfig.Builder transferConfigBuilder =
                TransferConfig.newBuilder()
                        .setDisplayName(displayName)
                        .setDataSourceId(DATA_SOURCE_ID)
                        .setSchedule(schedule)
                        .setParams(params);

        if (destination != null
                && destination.getDatasetId() != null
                && !destination.getDatasetId().isEmpty()) {
            transferConfigBuilder.setDestinationDatasetId(destination.getDatasetId());
        }

        if (disabled != null) {
            transferConfigBuilder.setDisabled(disabled);
        }

        return transferConfigBuilder.build();
    }

    /**
     * Deletes any existing scheduled query with the same display name.
     *
     * @param dataTransferServiceClient the BigQuery Data Transfer Service client
     * @param projectId the GCP project ID
     * @param location the GCP location (e.g., "US", "EU")
     * @throws BigQueryConnectorException if deletion fails
     */
    private void deleteExistingScheduledQuery(
            DataTransferServiceClient dataTransferServiceClient,
            String projectId,
            String location) {
        try {
            findTransferConfigByDisplayName(
                            dataTransferServiceClient, projectId, location, displayName)
                    .ifPresent(
                            existingConfig -> {
                                logger.info(
                                        "Deleting existing scheduled query with display name: {}",
                                        displayName);
                                DeleteTransferConfigRequest deleteRequest =
                                        DeleteTransferConfigRequest.newBuilder()
                                                .setName(existingConfig.getName())
                                                .build();
                                dataTransferServiceClient.deleteTransferConfig(deleteRequest);
                            });
        } catch (Exception e) {
            throw new BigQueryConnectorException(
                    "Failed to delete existing scheduled query with display name: " + displayName,
                    e);
        }
    }

    /**
     * Creates the scheduled query using the Data Transfer Service.
     *
     * @param dataTransferServiceClient the BigQuery Data Transfer Service client
     * @param projectId the GCP project ID
     * @param location the GCP location (e.g., "US", "EU")
     * @param serviceAccount the service account email to run the scheduled query
     * @param transferConfig the configured transfer config to create
     * @throws BigQueryConnectorException if creation fails
     */
    private void createScheduledQuery(
            DataTransferServiceClient dataTransferServiceClient,
            String projectId,
            String location,
            String serviceAccount,
            TransferConfig transferConfig) {
        try {
            LocationName parent = LocationName.of(projectId, location);
            CreateTransferConfigRequest.Builder requestBuilder =
                    CreateTransferConfigRequest.newBuilder()
                            .setParent(parent.toString())
                            .setTransferConfig(transferConfig)
                            .setServiceAccountName(serviceAccount);

            CreateTransferConfigRequest request = requestBuilder.build();
            TransferConfig response = dataTransferServiceClient.createTransferConfig(request);

            logger.info(
                    "Scheduled query created successfully: {} (ID: {})",
                    displayName,
                    response.getName());
        } catch (Exception e) {
            throw new BigQueryConnectorException("Failed to create scheduled query", e);
        }
    }

    // Jackson requires getters and setters

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public ScheduledQueryDestination getDestination() {
        return destination;
    }

    public void setDestination(ScheduledQueryDestination destination) {
        this.destination = destination;
    }

    public String getSchedule() {
        return schedule;
    }

    public void setSchedule(String schedule) {
        this.schedule = schedule;
    }

    public String getServiceAccount() {
        return serviceAccount;
    }

    public void setServiceAccount(String serviceAccount) {
        this.serviceAccount = serviceAccount;
    }

    public Boolean getDisabled() {
        return disabled;
    }

    public void setDisabled(Boolean disabled) {
        this.disabled = disabled;
    }

    public Boolean getReplaceIfExists() {
        return replaceIfExists;
    }

    public void setReplaceIfExists(Boolean replaceIfExists) {
        this.replaceIfExists = replaceIfExists;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    @Override
    public String toString() {
        return "CreateScheduledQuery{"
                + "displayName='"
                + displayName
                + '\''
                + ", schedule='"
                + schedule
                + '\''
                + ", destination="
                + destination
                + ", serviceAccount='"
                + serviceAccount
                + '\''
                + ", replaceIfExists="
                + replaceIfExists
                + '}';
    }
}
