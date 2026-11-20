// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.models.permission;

import energy.eddie.aiida.models.datasource.DataSource;
import energy.eddie.aiida.models.permission.dataneed.OutboundAiidaLocalDataNeed;
import org.springframework.scheduling.support.CronExpression;

import java.time.Instant;
import java.util.UUID;

/**
 * Snapshot of an accepted {@link OutboundPermission}, see {@link AcceptedPermission}.
 *
 * @param dataSource           The data source selected by the user, whose records are streamed.
 * @param transmissionSchedule The schedule that is used to stream data, see
 *                             {@link OutboundPermission#effectiveTransmissionSchedule()}.
 * @param transmissionEnabled  Whether data transmission is enabled.
 */
public record AcceptedOutboundPermission(
        UUID id,
        UUID eddieId,
        UUID userId,
        String connectionId,
        Instant startTime,
        Instant expirationTime,
        OutboundAiidaLocalDataNeed dataNeed,
        DataSource dataSource,
        MqttStreamingConfig mqttStreamingConfig,
        CronExpression transmissionSchedule,
        boolean transmissionEnabled
) implements AcceptedPermission {}
