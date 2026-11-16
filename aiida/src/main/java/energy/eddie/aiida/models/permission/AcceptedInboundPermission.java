// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.models.permission;

import energy.eddie.aiida.models.datasource.DataSource;
import energy.eddie.aiida.models.permission.dataneed.InboundAiidaLocalDataNeed;

import java.time.Instant;
import java.util.UUID;

/**
 * Snapshot of an accepted {@link InboundPermission}, see {@link AcceptedPermission}.
 *
 * @param dataSource The inbound data source created for the permission.
 */
public record AcceptedInboundPermission(
        UUID id,
        UUID eddieId,
        UUID userId,
        String connectionId,
        Instant startTime,
        Instant expirationTime,
        InboundAiidaLocalDataNeed dataNeed,
        DataSource dataSource,
        MqttStreamingConfig mqttStreamingConfig
) implements AcceptedPermission {}
