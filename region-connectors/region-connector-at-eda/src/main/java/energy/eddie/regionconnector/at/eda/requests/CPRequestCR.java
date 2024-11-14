package energy.eddie.regionconnector.at.eda.requests;

import energy.eddie.regionconnector.at.eda.config.AtConfiguration;
import energy.eddie.regionconnector.at.eda.requests.restricted.enums.AllowedGranularity;
import jakarta.annotation.Nullable;

import java.time.LocalDate;

/**
 * CPRequestCR This can be used to request retransmission of consumption records in the specified timeframe
 *
 * @param dsoId           The ID of the distribution system operator.
 * @param meteringPointId The ID of the metering point.
 * @param messageId       The ID of the message.
 * @param start           The start date of the timeframe of the metering data that is re-requested.
 * @param end             The end date of the timeframe of the metering data that is re-requested.
 * @param granularity     The granularity of the re-requested metering data.
 * @param configuration   The configuration that contains the eligible party ID.
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
