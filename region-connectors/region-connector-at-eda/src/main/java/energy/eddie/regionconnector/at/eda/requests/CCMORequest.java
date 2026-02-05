// SPDX-FileCopyrightText: 2023-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.requests;

import energy.eddie.dataneeds.needs.AccountingPointDataNeed;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.needs.EnergyCommunityDataNeed;
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

    public String purpose() {
        return dataNeed.purpose();
    }

    public RequestDataType requestDataType() {
        if (dataNeed instanceof EnergyCommunityDataNeed) {
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
        if (dataNeed instanceof EnergyCommunityDataNeed ec) {
            return ec.participationFactor();
        }
        return null;
    }
}
