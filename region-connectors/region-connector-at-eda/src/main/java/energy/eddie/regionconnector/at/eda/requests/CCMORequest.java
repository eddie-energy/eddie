package energy.eddie.regionconnector.at.eda.requests;

import energy.eddie.regionconnector.at.eda.config.AtConfiguration;
import energy.eddie.regionconnector.at.eda.requests.restricted.enums.AllowedGranularity;
import energy.eddie.regionconnector.at.eda.requests.restricted.enums.AllowedTransmissionCycle;
import energy.eddie.regionconnector.at.eda.utils.CMRequestId;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Optional;

public record CCMORequest(
        DsoIdAndMeteringPoint dsoIdAndMeteringPoint,
        CCMOTimeFrame timeframe,
        RequestDataType requestDataType,
        AllowedGranularity granularity,
        AllowedTransmissionCycle transmissionCycle,
        AtConfiguration configuration,
        ZonedDateTime timestamp
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

    public String cmRequestId() {
        return new CMRequestId(messageId()).toString();
    }

    public String messageId() {
        return new MessageId(configuration.eligiblePartyId(), timestamp).toString();
    }

    public Optional<String> meteringPointId() {
        return dsoIdAndMeteringPoint.meteringPoint();
    }

    public String eligiblePartyId() {
        return configuration.eligiblePartyId();
    }

    public Optional<String> conversationIdPrefix() {
        return configuration.conversationIdPrefix();
    }
}
