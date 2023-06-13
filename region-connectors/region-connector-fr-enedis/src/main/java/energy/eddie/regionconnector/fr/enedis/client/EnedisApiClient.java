package energy.eddie.regionconnector.fr.enedis.client;

import energy.eddie.api.v0.ConsumptionRecord;
import energy.eddie.regionconnector.fr.enedis.api.AuthorizationApi;
import energy.eddie.regionconnector.fr.enedis.api.EnedisApi;
import energy.eddie.regionconnector.fr.enedis.api.MeteringDataApi;
import energy.eddie.regionconnector.fr.enedis.invoker.ApiClient;
import energy.eddie.regionconnector.fr.enedis.invoker.ApiException;
import energy.eddie.regionconnector.fr.enedis.model.ConsumptionLoadCurveResponse;
import energy.eddie.regionconnector.fr.enedis.model.DailyConsumptionResponse;
import energy.eddie.regionconnector.fr.enedis.model.TokenGenerationResponse;
import energy.eddie.regionconnector.fr.enedis.utils.ConsumptionRecordMapper;

import java.time.*;
import java.util.Objects;

public class EnedisApiClient extends ApiClient implements EnedisApi {
    private final AuthorizationApi authApi;
    private final MeteringDataApi meterApi;
    private final EnedisApiClientConfiguration configuration;
    private String bearerToken = "";

    public EnedisApiClient(EnedisApiClientConfiguration configuration) {
        this.configuration = configuration;

        this.updateBaseUri(configuration.basePath());
        this.authApi = new AuthorizationApi(this);
        this.meterApi = new MeteringDataApi(this);
    }

    /**
     * Request a bearer token and write it to the file
     *
     * @throws ApiException Something went wrong while retrieving data from the API
     */
    @Override
    public void postToken() throws ApiException {
        String grantType = "client_credentials";
        String clientId = configuration.clientId();
        String clientSecret = configuration.clientSecret();
        String contentType = "application/x-www-form-urlencoded";
        String authorization = "No auth";

        TokenGenerationResponse tokenGenerationResponse = authApi
                .oauth2V3TokenPost(contentType, authorization, grantType, "eddie", clientId, clientSecret);

        bearerToken = Objects.requireNonNull( tokenGenerationResponse.getAccessToken());
    }

    /**
     * Request daily consumption metering data
     * TODO: Fix null value
     *
     * @return Response with metering data
     * @throws ApiException Something went wrong while retrieving data from the API
     */
    @Override
    public ConsumptionRecord getDailyConsumption(String usagePointId, ZonedDateTime start, ZonedDateTime end) throws ApiException {
        throwIfInvalidTimeframe(start, end);
        // The end date is not in the response when requesting data, increment +1 day to prevent confusion
        end = end.plusDays(1);

        String accept = "application/json";
        ConsumptionRecord dcRecord;

        String authorization = "Bearer " + bearerToken;

        DailyConsumptionResponse dcResponse = meterApi.meteringDataDcV5DailyConsumptionGet(authorization, usagePointId, start.toLocalDate().toString(), end.toLocalDate().toString(), accept, "application/json", "eddie", null);
        dcRecord = ConsumptionRecordMapper.dcReadingToCIM(dcResponse.getMeterReading());

        return dcRecord;
    }

    /**
     * Request consumption load curve metering data
     * TODO: Fix null value
     *
     * @return Response with metering data
     * @throws ApiException Something went wrong while retrieving data from the API
     */
    @Override
    public ConsumptionRecord getConsumptionLoadCurve(String usagePointId, ZonedDateTime start, ZonedDateTime end) throws ApiException {
        throwIfInvalidTimeframe(start, end);
        // The end date is not in the response when requesting data, increment +1 day to prevent confusion
        end = end.plusDays(1);

        String accept = "application/json";
        ConsumptionRecord clcRecord;

        String authorization = "Bearer " + bearerToken;
        ConsumptionLoadCurveResponse clcResponse = meterApi.meteringDataClcV5ConsumptionLoadCurveGet(authorization, usagePointId, start.toLocalDate().toString(), end.toLocalDate().toString(), accept, "application/json", "eddie", null);
        clcRecord = ConsumptionRecordMapper.clcReadingToCIM(clcResponse.getMeterReading());

        return clcRecord;
    }

    private void throwIfInvalidTimeframe(ZonedDateTime start, ZonedDateTime end) throws DateTimeException {
        LocalDate currentDate = LocalDate.ofInstant(Instant.now(), ZoneId.of("Europe/Paris"));

        if (start.isAfter(end)) {
            throw new DateTimeException("Start date should be before end date.");
        }
        if (end.toLocalDate().isEqual(currentDate) || end.toLocalDate().isAfter(currentDate)) {
            throw new DateTimeException("The end date parameter must be earlier than the current date.");
        }
    }
}
