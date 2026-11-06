// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.models.permission;

import energy.eddie.aiida.models.datasource.DataSource;
import energy.eddie.aiida.models.permission.dataneed.AiidaLocalDataNeed;
import jakarta.annotation.Nullable;
import org.springframework.scheduling.support.CronExpression;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable snapshot of an {@link PermissionStatus#ACCEPTED} permission with all fields that are required to stream
 * data. In contrast to {@link Permission}, none of the fields are nullable.
 *
 * @param id                   The ID of the permission.
 * @param eddieId              The ID of the EDDIE application that created the permission.
 * @param userId               The ID of the user that owns the permission.
 * @param connectionId         The connection ID that is sent along with each message to the EP.
 * @param startTime            The UTC timestamp when the data sharing starts.
 * @param expirationTime       The UTC timestamp when the data sharing stops.
 * @param dataNeed             The data need of the permission.
 * @param dataSource           The data source of the permission.
 * @param mqttStreamingConfig  The MQTT configuration used to stream data to the EP.
 * @param transmissionSchedule The schedule that is used to stream data, see
 *                             {@link Permission#effectiveTransmissionSchedule()}.
 * @param transmissionEnabled  Whether data transmission is enabled.
 */
public record ActivePermission(
        UUID id,
        UUID eddieId,
        UUID userId,
        String connectionId,
        Instant startTime,
        Instant expirationTime,
        AiidaLocalDataNeed dataNeed,
        DataSource dataSource,
        MqttStreamingConfig mqttStreamingConfig,
        CronExpression transmissionSchedule,
        boolean transmissionEnabled
) {
    /**
     * Creates a snapshot of the passed permission.
     *
     * @throws IllegalStateException If the permission is not {@link PermissionStatus#ACCEPTED} or a field required for
     *                               streaming is missing.
     */
    public static ActivePermission from(Permission permission) throws IllegalStateException {
        if (!permission.status().isAccepted()) {
            throw new IllegalStateException("Permission %s is not accepted, its status is %s".formatted(
                    permission.id(),
                    permission.status()));
        }

        return new ActivePermission(permission.id(),
                                    required(permission, permission.eddieId(), "eddie id"),
                                    required(permission, permission.userId(), "user id"),
                                    required(permission, permission.connectionId(), "connection id"),
                                    required(permission, permission.startTime(), "start time"),
                                    required(permission, permission.expirationTime(), "expiration time"),
                                    required(permission, permission.dataNeed(), "data need"),
                                    required(permission, permission.dataSource(), "data source"),
                                    required(permission, permission.mqttStreamingConfig(), "MQTT streaming config"),
                                    required(permission,
                                             permission.effectiveTransmissionSchedule(),
                                             "transmission schedule"),
                                    permission.transmissionEnabled());
    }

    private static <T> T required(Permission permission, @Nullable T value, String fieldName) {
        if (value == null) {
            throw new IllegalStateException("Accepted permission %s is missing its %s".formatted(permission.id(),
                                                                                                fieldName));
        }
        return value;
    }
}
