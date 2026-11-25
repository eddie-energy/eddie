// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.models.permission;

import energy.eddie.aiida.models.datasource.DataSource;
import energy.eddie.aiida.models.permission.dataneed.AiidaLocalDataNeed;
import jakarta.annotation.Nullable;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable snapshot of an {@link PermissionStatus#ACCEPTED} permission with all fields that are required to stream
 * data. In contrast to {@link Permission}, none of the fields are nullable.
 *
 * @see AcceptedInboundPermission
 * @see AcceptedOutboundPermission
 */
public sealed interface AcceptedPermission permits AcceptedInboundPermission, AcceptedOutboundPermission {
    /**
     * Creates a snapshot of the passed permission matching its direction.
     *
     * @throws IllegalStateException If the permission is not {@link PermissionStatus#ACCEPTED} or a field required for
     *                               streaming is missing.
     */
    static AcceptedPermission from(Permission permission) throws IllegalStateException {
        return switch (permission) {
            case InboundPermission inboundPermission -> from(inboundPermission);
            case OutboundPermission outboundPermission -> from(outboundPermission);
            default -> throw new IllegalStateException("Unsupported permission type " + permission.getClass().getName());
        };
    }

    /**
     * Creates a snapshot of the passed inbound permission.
     *
     * @throws IllegalStateException If the permission is not {@link PermissionStatus#ACCEPTED} or a field required for
     *                               streaming is missing.
     */
    static AcceptedInboundPermission from(InboundPermission permission) throws IllegalStateException {
        requireAccepted(permission);
        return new AcceptedInboundPermission(permission.id(),
                                             permission.eddieId(),
                                             permission.userId(),
                                             permission.connectionId(),
                                             permission.startTime(),
                                             permission.expirationTime(),
                                             permission.dataNeed(),
                                             required(permission, permission.dataSource(), "data source"),
                                             required(permission,
                                                      permission.mqttStreamingConfig(),
                                                      "MQTT streaming config"));
    }

    /**
     * Creates a snapshot of the passed outbound permission.
     *
     * @throws IllegalStateException If the permission is not {@link PermissionStatus#ACCEPTED} or a field required for
     *                               streaming is missing.
     */
    static AcceptedOutboundPermission from(OutboundPermission permission) throws IllegalStateException {
        requireAccepted(permission);
        return new AcceptedOutboundPermission(permission.id(),
                                              permission.eddieId(),
                                              permission.userId(),
                                              permission.connectionId(),
                                              permission.startTime(),
                                              permission.expirationTime(),
                                              permission.dataNeed(),
                                              required(permission, permission.dataSource(), "data source"),
                                              required(permission,
                                                       permission.mqttStreamingConfig(),
                                                       "MQTT streaming config"),
                                              permission.effectiveTransmissionSchedule(),
                                              permission.transmissionEnabled());
    }

    /**
     * {@return the ID of the permission}
     */
    UUID id();

    /**
     * {@return the ID of the EDDIE application that created the permission}
     */
    UUID eddieId();

    /**
     * {@return the ID of the user that owns the permission}
     */
    UUID userId();

    /**
     * {@return the connection ID that is sent along with each message to the EP}
     */
    String connectionId();

    /**
     * {@return the UTC timestamp when the data sharing starts}
     */
    Instant startTime();

    /**
     * {@return the UTC timestamp when the data sharing stops}
     */
    Instant expirationTime();

    /**
     * {@return the data need of the permission}
     */
    AiidaLocalDataNeed dataNeed();

    /**
     * {@return the data source of the permission}
     */
    DataSource dataSource();

    /**
     * {@return the MQTT configuration used to stream data to the EP}
     */
    MqttStreamingConfig mqttStreamingConfig();

    private static void requireAccepted(Permission permission) {
        if (!permission.status().isAccepted()) {
            throw new IllegalStateException("Permission %s is not accepted, its status is %s".formatted(
                    permission.id(),
                    permission.status()));
        }
    }

    private static <T> T required(Permission permission, @Nullable T value, String fieldName) {
        if (value == null) {
            throw new IllegalStateException("Accepted permission %s is missing its %s".formatted(permission.id(),
                                                                                                fieldName));
        }
        return value;
    }
}
