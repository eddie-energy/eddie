package energy.eddie.regionconnector.fr.enedis.client;

import energy.eddie.regionconnector.fr.enedis.api.AuthorizationApi;
import energy.eddie.regionconnector.fr.enedis.api.EnedisApi;
import energy.eddie.regionconnector.fr.enedis.api.MeteringDataApi;
import energy.eddie.regionconnector.fr.enedis.config.EnedisConfiguration;
import energy.eddie.regionconnector.fr.enedis.invoker.ApiClient;
import energy.eddie.regionconnector.fr.enedis.invoker.ApiException;
import energy.eddie.regionconnector.fr.enedis.model.*;

import java.time.*;
import java.util.Objects;

public class EnedisApiClient extends ApiClient implements EnedisApi {
    private static final String APPLICATION_JSON = "application/json";
    private static final String USER_AGENT = "eddie";
    private final AuthorizationApi authApi;
    private final MeteringDataApi meterApi;
    private final EnedisConfiguration configuration;
    private String bearerToken = "";

    public EnedisApiClient(EnedisConfiguration configuration) {
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
                .oauth2V3TokenPost(contentType, authorization, grantType, USER_AGENT, clientId, clientSecret);

        bearerToken = Objects.requireNonNull(tokenGenerationResponse.getAccessToken());
    }

    /**
     * Request daily consumption metering data
     * TODO: Fix null value
     *
     * @return Response with metering data
     * @throws ApiException Something went wrong while retrieving data from the API
     */
    @Override
    public DailyConsumptionMeterReading getDailyConsumption(String usagePointId, ZonedDateTime start, ZonedDateTime end) throws ApiException {
        throwIfInvalidTimeframe(start, end);
        // The end date is not in the response when requesting data, increment +1 day to prevent confusion
        end = end.plusDays(1);
        String authorization = "Bearer " + bearerToken;
        DailyConsumptionResponse dcResponse = meterApi.meteringDataDcV5DailyConsumptionGet(authorization, usagePointId, start.toLocalDate().toString(), end.toLocalDate().toString(), APPLICATION_JSON, APPLICATION_JSON, USER_AGENT, "");
        return dcResponse.getMeterReading();
    }

    /**
     * Request consumption load curve metering data
     * TODO: Fix null value
     *
     * @return Response with metering data
     * @throws ApiException Something went wrong while retrieving data from the API
     */
    @Override
    @SuppressWarnings("NullAway")
    public ConsumptionLoadCurveMeterReading getConsumptionLoadCurve(String usagePointId, ZonedDateTime start, ZonedDateTime end) throws ApiException {
        throwIfInvalidTimeframe(start, end);
        // The end date is not in the response when requesting data, increment +1 day to prevent confusion
        end = end.plusDays(1);

        String authorization = "Bearer " + bearerToken;
        ConsumptionLoadCurveResponse clcResponse = meterApi.meteringDataClcV5ConsumptionLoadCurveGet(
                authorization,
                usagePointId,
                start.toLocalDate().toString(),
                end.toLocalDate().toString(),
                APPLICATION_JSON,
                APPLICATION_JSON,
                USER_AGENT,
                null
        );

        return clcResponse.getMeterReading();
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