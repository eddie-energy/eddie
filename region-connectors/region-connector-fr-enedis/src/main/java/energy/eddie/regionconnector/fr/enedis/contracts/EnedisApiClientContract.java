package energy.eddie.regionconnector.fr.enedis.contracts;

import eddie.energy.regionconnector.api.v0.models.ConsumptionRecord;
import energy.eddie.regionconnector.fr.enedis.ApiException;

import java.time.ZonedDateTime;

public interface EnedisApiClientContract {
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
}
