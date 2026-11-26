// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.dtos.permission;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import energy.eddie.aiida.models.datasource.DataSource;
import energy.eddie.aiida.models.permission.*;
import energy.eddie.aiida.models.permission.dataneed.AiidaLocalDataNeed;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;

import java.time.Instant;
import java.util.UUID;

/**
 * Representation of a persisted permission returned by the REST API. The {@code type} property tells inbound and
 * outbound permissions apart, as only they carry the fields of their direction.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = PermissionDto.TYPE_PROPERTY)
@JsonSubTypes({
        @JsonSubTypes.Type(value = InboundPermissionDto.class, name = InboundPermission.DISCRIMINATOR_VALUE),
        @JsonSubTypes.Type(value = OutboundPermissionDto.class, name = OutboundPermission.DISCRIMINATOR_VALUE)
})
@Schema(
        oneOf = {InboundPermissionDto.class, OutboundPermissionDto.class},
        discriminatorProperty = PermissionDto.TYPE_PROPERTY,
        discriminatorMapping = {
                @DiscriminatorMapping(value = InboundPermission.DISCRIMINATOR_VALUE, schema = InboundPermissionDto.class),
                @DiscriminatorMapping(value = OutboundPermission.DISCRIMINATOR_VALUE, schema = OutboundPermissionDto.class)
        }
)
public sealed interface PermissionDto permits InboundPermissionDto, OutboundPermissionDto {
    String TYPE_PROPERTY = "type";

    /**
     * Creates the representation of the passed permission matching its direction.
     *
     * @param permission       The persisted permission.
     * @param connectionStatus The connection status derived at runtime, or {@code null} if the permission is not
     *                         accepted.
     */
    static PermissionDto from(Permission permission, @Nullable ConnectionStatus connectionStatus) {
        return switch (permission) {
            case InboundPermission inboundPermission -> InboundPermissionDto.from(inboundPermission, connectionStatus);
            case OutboundPermission outboundPermission ->
                    OutboundPermissionDto.from(outboundPermission, connectionStatus);
            default -> throw new IllegalArgumentException("Unsupported permission type " + permission.getClass().getName());
        };
    }

    UUID permissionId();

    UUID eddieId();

    PermissionStatus status();

    @Nullable
    ConnectionStatus connectionStatus();

    String serviceName();

    String displayName();

    Instant startTime();

    Instant expirationTime();

    Instant grantTime();

    @Nullable
    Instant revokeTime();

    @Nullable
    String meterId();

    @Nullable
    MqttStreamingConfig mqttStreamingConfig();

    AiidaLocalDataNeed dataNeed();

    UUID userId();

    @Nullable
    DataSource dataSource();
}
