package energy.eddie.regionconnector.fr.enedis.api;

import energy.eddie.api.v0.ConsumptionRecord;
import energy.eddie.api.v0.HealthState;
import energy.eddie.regionconnector.fr.enedis.invoker.ApiException;

import java.time.ZonedDateTime;
import java.util.Map;

public interface EnedisApi {
    /**
     * Request a bearer token and write it to the file
     *
     * @throws ApiException Something went wrong while retrieving data from the API
     */
    void postToken() throws ApiException;

    /**
     * Request daily consumption metering data
     *
     * @return Response with metering data
     * @throws ApiException Something went wrong while retrieving data from the API
     */
    ConsumptionRecord getDailyConsumption(String usagePointId, ZonedDateTime start, ZonedDateTime end) throws ApiException;

    /**
     * Request consumption load curve metering data
     *
     * @return Response with metering data
     * @throws ApiException Something went wrong while retrieving data from the API
     */
    ConsumptionRecord getConsumptionLoadCurve(String usagePointId, ZonedDateTime start, ZonedDateTime end) throws ApiException;

    default Map<String, HealthState> health() {
        throw new IllegalStateException("Not implemented yet");
    }
}
