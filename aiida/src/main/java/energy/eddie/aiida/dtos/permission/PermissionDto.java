// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.dtos.permission;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.aiida.models.datasource.DataSource;
import energy.eddie.aiida.models.permission.*;
import energy.eddie.aiida.models.permission.dataneed.AiidaLocalDataNeed;
import energy.eddie.dataneeds.utils.cron.CronExpressionSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import org.springframework.scheduling.support.CronExpression;
import tools.jackson.databind.annotation.JsonSerialize;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Representation of a persisted permission returned by the REST API.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PermissionDto(
        @JsonProperty
        @Schema(description = "Unique ID of this permission.", requiredMode = Schema.RequiredMode.REQUIRED, example = "a4dc1bad-b9fe-47ae-9336-690cfb4aada9")
        UUID permissionId,

        @JsonProperty
        @Schema(description = "Unique ID of the EDDIE application that created this permission.", requiredMode = Schema.RequiredMode.REQUIRED, example = "a4dc1bad-b9fe-47ae-9336-690cfb4aada9")
        UUID eddieId,

        @JsonProperty
        @Schema(description = "Status of this permission.", requiredMode = Schema.RequiredMode.REQUIRED, example = "ACCEPTED")
        PermissionStatus status,

        @JsonProperty
        @Schema(description = "Dynamic connection state, derived at runtime. Only present for accepted permissions.", example = "CONNECTED")
        @Nullable
        ConnectionStatus connectionStatus,

        @JsonProperty
        @Schema(description = "Name of the EP service that requested near real-time data.", example = "My Energy Visualization Service")
        @Nullable
        String serviceName,

        @JsonProperty
        @Schema(description = "User-chosen display name for this permission.", example = "My smart meter")
        @Nullable
        String displayName,

        @JsonProperty
        @Schema(description = "UTC timestamp when the data sharing should start.")
        @Nullable
        Instant startTime,

        @JsonProperty
        @Schema(description = "UTC timestamp the data sharing should automatically stop. Must be after startTime.")
        @Nullable
        Instant expirationTime,

        @JsonProperty
        @Schema(description = "UTC timestamp when the customer granted the sharing permission.")
        @Nullable
        Instant grantTime,

        @JsonProperty
        @Schema(description = "UTC timestamp when the permission was revoked or terminated. See status for the exact reason.")
        @Nullable
        Instant revokeTime,

        @JsonProperty
        @Schema(description = "ID of a physical smart meter asset used to map flexible connection agreements and control data source selection.", example = "003114735")
        @Nullable
        String meterId,

        @JsonProperty
        @Schema(description = "Optional current default minimum energy consumption or production limit in kilowatts for controllable units.", example = "1.0")
        @Nullable
        BigDecimal minLimitKw,

        @JsonProperty
        @Schema(description = "Optional current default maximum energy consumption or production limit in kilowatts for controllable units.", example = "10.0")
        @Nullable
        BigDecimal maxLimitKw,

        @JsonProperty
        @Schema(description = "The data source monitored for connection limit violations.")
        @Nullable
        UUID monitoringDataSourceId,

        @JsonProperty
        @Nullable
        MqttStreamingConfig mqttStreamingConfig,

        @JsonProperty
        @Nullable
        AiidaLocalDataNeed dataNeed,

        @JsonProperty
        @Schema(description = "UUID of the user that owns the permission.")
        UUID userId,

        @JsonProperty
        @Nullable
        DataSource dataSource,

        @JsonProperty
        @Schema(description = "Selected format for inbound permission messages.", example = "CIM_1_12")
        @Nullable
        InboundMessageFormat inboundMessageFormat,

        @JsonProperty
        @Schema(description = "Whether data transmission is currently enabled for this permission.")
        boolean transmissionEnabled,

        @JsonProperty
        @JsonSerialize(using = CronExpressionSerializer.class)
        @Schema(description = "The schedule used to stream data: the permission's own transmission schedule if set, otherwise the data need's schedule.", type = "string", example = "*/5 * * * * *")
        @Nullable
        CronExpression effectiveTransmissionSchedule,

        @JsonProperty
        @Schema(description = "Whether the permission supports connection limit features such as monitoring.", example = "true")
        boolean supportsConnectionLimits
) {
    /**
     * Creates the representation of the passed permission.
     *
     * @param permission       The persisted permission.
     * @param connectionStatus The connection status derived at runtime, or {@code null} if the permission is not
     *                         accepted.
     */
    public static PermissionDto from(Permission permission, @Nullable ConnectionStatus connectionStatus) {
        return new PermissionDto(permission.id(),
                                 permission.eddieId(),
                                 permission.status(),
                                 connectionStatus,
                                 permission.serviceName(),
                                 permission.displayName(),
                                 permission.startTime(),
                                 permission.expirationTime(),
                                 permission.grantTime(),
                                 permission.revokeTime(),
                                 permission.meterId(),
                                 permission.minLimitKw(),
                                 permission.maxLimitKw(),
                                 permission.monitoringDataSourceId(),
                                 permission.mqttStreamingConfig(),
                                 permission.dataNeed(),
                                 permission.userId(),
                                 permission.dataSource(),
                                 permission.inboundMessageFormat(),
                                 permission.transmissionEnabled(),
                                 permission.effectiveTransmissionSchedule(),
                                 permission.supportsConnectionLimits());
    }
}
