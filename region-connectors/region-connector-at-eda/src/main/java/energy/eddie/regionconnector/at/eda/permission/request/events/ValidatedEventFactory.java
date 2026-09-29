// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.permission.request.events;

import energy.eddie.api.agnostic.data.needs.EnergyDirection;
import energy.eddie.regionconnector.at.eda.requests.EdaGroupingIdFactory;
import energy.eddie.regionconnector.at.eda.requests.restricted.enums.AllowedGranularity;
import energy.eddie.regionconnector.at.eda.utils.CMRequestId;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZonedDateTime;

import static energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata.AT_ZONE_ID;

@Component
public class ValidatedEventFactory {
    private final EdaGroupingIdFactory groupingIdFactory;

    public ValidatedEventFactory(EdaGroupingIdFactory groupingIdFactory) {
        this.groupingIdFactory = groupingIdFactory;
    }

    public ValidatedEvent createValidatedEvent(
            String permissionId,
            LocalDate start,
            @Nullable LocalDate end,
            @Nullable AllowedGranularity granularity
    ) {
        return createValidatedEvent(permissionId, start, end, granularity, null, null);
    }

    public ValidatedEvent createValidatedEvent(
            String permissionId,
            LocalDate start,
            @Nullable LocalDate end,
            @Nullable AllowedGranularity granularity,
            @Nullable EnergyDirection energyDirection,
            @Nullable Integer participationFactor
    ) {
        ZonedDateTime created = ZonedDateTime.now(AT_ZONE_ID);
        var messageId = groupingIdFactory.create(created);
        var cmRequestId = new CMRequestId(messageId).toString();

        return new ValidatedEvent(
                permissionId,
                start,
                end,
                granularity,
                cmRequestId,
                messageId,
                energyDirection,
                participationFactor,
                ValidatedEvent.NeedsToBeSent.YES
        );
    }
}
