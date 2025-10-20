package energy.eddie.regionconnector.at.eda.requests;

import energy.eddie.regionconnector.at.eda.config.AtConfiguration;
import energy.eddie.regionconnector.at.eda.requests.restricted.enums.AllowedGranularity;
import energy.eddie.regionconnector.at.eda.requests.restricted.enums.AllowedTransmissionCycle;
import jakarta.annotation.Nullable;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Optional;

public record CCMORequest(
        DsoIdAndMeteringPoint dsoIdAndMeteringPoint,
        CCMOTimeFrame timeframe,
        String cmRequestId,
        String messageId,
        RequestDataType requestDataType,
        AllowedGranularity granularity,
        AllowedTransmissionCycle transmissionCycle,
        AtConfiguration configuration,
        ZonedDateTime timestamp,
        @Nullable String purpose
) {

    public CCMORequest(
            DsoIdAndMeteringPoint dsoIdAndMeteringPoint,
            CCMOTimeFrame timeframe,
            String cmRequestId,
            String messageId,
            RequestDataType requestDataType,
            AllowedGranularity granularity,
            AllowedTransmissionCycle transmissionCycle,
            AtConfiguration configuration,
            ZonedDateTime timestamp
    ) {
        this(
                dsoIdAndMeteringPoint,
                timeframe,
                cmRequestId,
                messageId,
                requestDataType,
                granularity,
                transmissionCycle,
                configuration,
                timestamp,
                null
        );
    }

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
}
