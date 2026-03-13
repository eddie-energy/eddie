// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.permission.request.events;

import energy.eddie.api.agnostic.data.needs.CESUJoinRequestDataNeedResult;
import energy.eddie.api.agnostic.data.needs.DataNeedCalculationResult;
import energy.eddie.regionconnector.at.eda.config.AtConfiguration;
import energy.eddie.regionconnector.at.eda.requests.MessageId;
import energy.eddie.regionconnector.at.eda.requests.restricted.enums.AllowedGranularity;
import energy.eddie.regionconnector.at.eda.utils.CMRequestId;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZonedDateTime;

import static energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata.AT_ZONE_ID;

@Component
public class ValidatedEventFactory {
    private final AtConfiguration configuration;

    public ValidatedEventFactory(AtConfiguration configuration) {this.configuration = configuration;}

    public ValidatedEvent createValidatedEvent(
            String permissionId,
            LocalDate start,
            @Nullable LocalDate end,
            @Nullable AllowedGranularity granularity,
            DataNeedCalculationResult dataNeedCalculation
    ) {
        ZonedDateTime created = ZonedDateTime.now(AT_ZONE_ID);
        var type = dataNeedCalculation instanceof CESUJoinRequestDataNeedResult
                ? AtConfiguration.PartyIdType.ENERGY_COMMUNITY
                : AtConfiguration.PartyIdType.ELIGIBLE_PARTY;
        var messageId = new MessageId(configuration.partyIdFor(type), created).toString();
        var cmRequestId = new CMRequestId(messageId).toString();

        return new ValidatedEvent(
                permissionId,
                start,
                end,
                granularity,
                cmRequestId,
                messageId,
                ValidatedEvent.NeedsToBeSent.YES
        );
    }
}
