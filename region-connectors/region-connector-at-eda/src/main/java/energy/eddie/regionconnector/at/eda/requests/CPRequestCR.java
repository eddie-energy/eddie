package energy.eddie.regionconnector.at.eda.requests;

import energy.eddie.regionconnector.at.eda.config.AtConfiguration;
import energy.eddie.regionconnector.at.eda.requests.restricted.enums.AllowedGranularity;
import jakarta.annotation.Nullable;

import java.time.LocalDate;

/**
 * CPRequestCR This can be used to request retransmission of consumption records in the specified timeframe
 */
public record CPRequestCR(
        String dsoId,
        String meteringPointId,
        String messageId,
        LocalDate start,
        LocalDate end,
        @Nullable
        AllowedGranularity granularity,
        AtConfiguration configuration
) {
    public String eligiblePartyId() {
        return configuration.eligiblePartyId();
    }
}
