package energy.eddie.regionconnector.fr.enedis.api;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.regionconnector.fr.enedis.dto.readings.MeterReading;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

public interface EnedisMeterReadingApi {
    /**
     * Retrieves meter reading data for a specified usage point over a given period. This method supports different
     * granularities for the consumption data: {@link Granularity#PT30M} and {@link Granularity#P1D}.
     *
     * <p>Important Constraints:</p>
     * <ul>
     *   <li>When using {@link Granularity#PT30M}, the duration between the start and end dates must not exceed 7 days. Batch the requests</li>
     *   <li>The end date is treated as exclusive, meaning consumption on this date is not included in the returned data.</li>
     * </ul>
     *
     * <p>If the specified period or granularity does not meet these constraints, the method may throw an {@link IllegalArgumentException}.</p>
     *
     * @param usagePointId The unique identifier for the usage point. Must not be null or empty.
     * @param start        The start date of the period for which to retrieve consumption data. Must be before the end
     *                     date.
     * @param end          The end date of the period, exclusive. Consumption data up to but not including this date is
     *                     retrieved.
     * @param granularity  The granularity of the consumption data. Must be one of the supported {@link Granularity}
     *                     values.
     * @return A {@link Mono} that emits the {@link MeterReading} data for the specified usage point and period or an
     * error signal if the request fails.
     * @throws IllegalArgumentException                                                    if any parameter is invalid
     *                                                                                     or if the date range and
     *                                                                                     granularity combination is
     *                                                                                     not supported.
     * @throws org.springframework.web.reactive.function.client.WebClientResponseException if the request to the ENEDIS
     *                                                                                     API fails e.g. due to an
     *                                                                                     invalid token or a bad
     *                                                                                     request.
     */
    Mono<MeterReading> getConsumptionMeterReading(
            String usagePointId,
            LocalDate start,
            LocalDate end,
            Granularity granularity
    );

    /**
     * Retrieves meter reading data for a specified usage point over a given period. This method supports different
     * granularities for the consumption data: {@link Granularity#PT30M} and {@link Granularity#P1D}.
     *
     * <p>Important Constraints:</p>
     * <ul>
     *   <li>When using {@link Granularity#PT30M}, the duration between the start and end dates must not exceed 7 days. Batch the requests</li>
     *   <li>The end date is treated as exclusive, meaning consumption on this date is not included in the returned data.</li>
     * </ul>
     *
     * <p>If the specified period or granularity does not meet these constraints, the method may throw an {@link IllegalArgumentException}.</p>
     *
     * @param usagePointId The unique identifier for the usage point. Must not be null or empty.
     * @param start        The start date of the period for which to retrieve consumption data. Must be before the end
     *                     date.
     * @param end          The end date of the period, exclusive. Consumption data up to but not including this date is
     *                     retrieved.
     * @param granularity  The granularity of the consumption data. Must be one of the supported {@link Granularity}
     *                     values.
     * @return A {@link Mono} that emits the {@link MeterReading} data for the specified usage point and period or an
     * error signal if the request fails.
     * @throws IllegalArgumentException                                                    if any parameter is invalid
     *                                                                                     or if the date range and
     *                                                                                     granularity combination is
     *                                                                                     not supported.
     * @throws org.springframework.web.reactive.function.client.WebClientResponseException if the request to the ENEDIS
     *                                                                                     API fails e.g. due to an
     *                                                                                     invalid token or a bad
     *                                                                                     request.
     */
    Mono<MeterReading> getProductionMeterReading(
            String usagePointId,
            LocalDate start,
            LocalDate end,
            Granularity granularity
    );
}
