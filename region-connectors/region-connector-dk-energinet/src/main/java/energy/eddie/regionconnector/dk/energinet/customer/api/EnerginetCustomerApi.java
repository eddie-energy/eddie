package energy.eddie.regionconnector.dk.energinet.customer.api;

import energy.eddie.api.v0.ConsumptionRecord;
import energy.eddie.api.v0.HealthState;
import energy.eddie.regionconnector.dk.energinet.customer.model.MeteringPointsRequest;
import energy.eddie.regionconnector.dk.energinet.enums.PeriodResolutionEnum;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;

public interface EnerginetCustomerApi {
    /**
     * Call this endpoint to verify whether Energinet is currently operating normally.
     * If Energinet is experiencing more traffic than we can handle, this endpoint may return HTTP 503.  The status currently refreshes every 60 seconds.
     *
     * @return Boolean
     */
    Boolean isAlive();

    /**
     * Returns a data access token.
     * In order to get an access token you will need  a valid refresh token. This token can be fetched from the portal.  The token is a JWT token. There are tools that can read the content eg: https://jwt.io/  The token must be submittet in the request header like \&quot;Authorization: Bearer eyJhbGciOi...\&quot;
     */
    void apiToken();

    /**
     * Returns ConsumptionRecord out of a time series for each metering point in list.
     *
     * @param dateFrom              Date from filter in format: &#39;YYYY-MM-DD&#39; (required)
     * @param dateTo                Date to filter in format: &#39;YYYY-MM-DD&#39; (required)
     * @param periodResolution      Period Resolution. Possible values are: &#39;PT15M&#39;, &#39;PT1H&#39;, &#39;PT1D&#39;, &#39;P1M&#39;, &#39;P1Y&#39; (required)
     * @param meteringPointsRequest List of metering point ids. (optional)
     * @return MyEnergyDataMarketDocumentResponseListApiResponse
     */
    ConsumptionRecord getTimeSeries(ZonedDateTime dateFrom, ZonedDateTime dateTo, PeriodResolutionEnum periodResolution, MeteringPointsRequest meteringPointsRequest);

    void setUserCorrelationId(UUID userCorrelationId);

    Map<String, HealthState> health();
}
