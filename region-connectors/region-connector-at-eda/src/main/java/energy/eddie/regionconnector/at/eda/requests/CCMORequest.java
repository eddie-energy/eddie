// SPDX-FileCopyrightText: 2023-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.requests;

import energy.eddie.dataneeds.needs.AccountingPointDataNeed;
import energy.eddie.dataneeds.needs.CESUJoinRequestDataNeed;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.needs.EnergyDirection;
import energy.eddie.regionconnector.at.eda.config.AtConfiguration;
import energy.eddie.regionconnector.at.eda.models.MessageCodes;
import energy.eddie.regionconnector.at.eda.requests.restricted.enums.AllowedGranularity;
import energy.eddie.regionconnector.at.eda.requests.restricted.enums.AllowedTransmissionCycle;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Optional;

public record CCMORequest(
        DsoIdAndMeteringPoint dsoIdAndMeteringPoint,
        CCMOTimeFrame timeframe,
        String cmRequestId,
        String messageId,
        AllowedGranularity granularity,
        AllowedTransmissionCycle transmissionCycle,
        AtConfiguration configuration,
        ZonedDateTime timestamp,
        DataNeed dataNeed
) {

    public String dsoId() {
        return dsoIdAndMeteringPoint.dsoId();
    }

    public LocalDate start() {
        return timeframe.start();
    }

    public Optional<LocalDate> end() {
        return timeframe.end();
    }

    public Optional<String> meteringPointId() {
        return dsoIdAndMeteringPoint.meteringPoint();
    }

    public String eligiblePartyId() {
        return configuration.eligiblePartyId();
    }

    @Nullable
    public String energyCommunityId() {
        return configuration.energyCommunityId();
    }

    /**
     * Returns either the eligible party ID or the energy community ID depending on whether this is a request for an energy community process or not.
     *
     * @return either the eligble party ID or the energy community ID depending on the market process.
     * @see CCMORequest#requestDataType()
     */
    public String senderId() {
        return requestDataType().equals(RequestDataType.ENERGY_COMMUNITY_REGISTRATION)
                ? configuration.partyIdFor(AtConfiguration.PartyIdType.ENERGY_COMMUNITY)
                : configuration.partyIdFor(AtConfiguration.PartyIdType.ELIGIBLE_PARTY);
    }

    /**
     * Returns the {@link RequestDataType} depending on the given data need.
     * The RequestDataType determines in which kind of ebutilities process the CCMO Request is going to be used in.
     *
     * @return the {@link RequestDataType} depending on the given data need.
     */
    public RequestDataType requestDataType() {
        if (dataNeed instanceof CESUJoinRequestDataNeed) {
            return RequestDataType.ENERGY_COMMUNITY_REGISTRATION;
        }
        if (dataNeed instanceof AccountingPointDataNeed) {
            return RequestDataType.MASTER_DATA;
        }
        return RequestDataType.METERING_DATA;
    }

    public String messageCode() {
        return requestDataType() == RequestDataType.ENERGY_COMMUNITY_REGISTRATION
                ? MessageCodes.EcRequest.CODE
                : MessageCodes.Request.CODE;
    }

    @Nullable
    public BigDecimal partFact() {
        if (dataNeed instanceof CESUJoinRequestDataNeed ec) {
            return BigDecimal.valueOf(ec.participationFactor());
        }
        return null;
    }

    @Nullable
    public EnergyDirection energyDirection() {
        if (dataNeed instanceof CESUJoinRequestDataNeed ec) {
            return ec.energyDirection();
        }
        return null;
    }
}
