package energy.eddie.regionconnector.fr.enedis.client;

import eddie.energy.regionconnector.api.v0.models.ConsumptionRecord;
import energy.eddie.regionconnector.fr.enedis.ApiClient;
import energy.eddie.regionconnector.fr.enedis.ApiException;
import energy.eddie.regionconnector.fr.enedis.ConsumptionRecordMapper;
import energy.eddie.regionconnector.fr.enedis.api.AuthorizationApi;
import energy.eddie.regionconnector.fr.enedis.api.MeteringDataApi;
import energy.eddie.regionconnector.fr.enedis.model.ConsumptionLoadCurveResponse;
import energy.eddie.regionconnector.fr.enedis.model.DailyConsumptionResponse;
import energy.eddie.regionconnector.fr.enedis.model.TokenGenerationResponse;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.*;

import static java.nio.charset.StandardCharsets.UTF_8;

public class EnedisApiClient extends ApiClient {
    private final static String bearerTokenFile = "region-connectors/region-connector-fr-enedis/bearer.txt";
    private final AuthorizationApi authApi;
    private final MeteringDataApi meterApi;
    private final EnedisApiClientConfiguration configuration;

    public EnedisApiClient(EnedisApiClientConfiguration configuration) {
        this.configuration = configuration;

        this.setBasePath(configuration.getBasePath());
        this.authApi = new AuthorizationApi(this);
        this.meterApi = new MeteringDataApi(this);
    }

    /**
     * Request a bearer token and write it to the file
     *
     * @throws ApiException Something went wrong while retrieving data from the API
     * @throws IOException  Something went wrong when accessing the file
     */
    public void postToken() throws ApiException, IOException {
        String grantType = "client_credentials";
        String clientId = configuration.getClientId();
        String clientSecret = configuration.getClientSecret();
        String contentType = "application/x-www-form-urlencoded";
        String host = configuration.getHostname();
        String authorization = "No auth";

        TokenGenerationResponse tokenGenerationResponse = authApi
                .oauth2V3TokenPost(grantType, clientId, clientSecret, contentType, host, authorization, null);

        try (BufferedWriter br = Files.newBufferedWriter(Paths.get(bearerTokenFile), UTF_8)) {
            br.write(tokenGenerationResponse.getAccessToken());
        }
    }

    /**
     * Request daily consumption metering data
     *
     * @return Response with metering data
     * @throws ApiException Something went wrong while retrieving data from the API
     * @throws IOException  Something went wrong when accessing the file
     */
    public ConsumptionRecord getDailyConsumption(String usagePointId, ZonedDateTime start, ZonedDateTime end) throws ApiException, IOException {
        checkDate(start, end);
        // The end date is not in the response when requesting data, increment +1 day to prevent confusion
        end = end.plusDays(1);

        String accept = "application/json";
        String host = configuration.getHostname();
        ConsumptionRecord dcRecord;

        try (BufferedReader br = Files.newBufferedReader(Paths.get(bearerTokenFile), UTF_8)) {
            String authorization = "Bearer " + br.readLine();
            DailyConsumptionResponse dcResponse = meterApi.meteringDataDcV5DailyConsumptionGet(authorization, usagePointId, start.toLocalDate().toString(), end.toLocalDate().toString(), accept, null, null, host, null);
            dcRecord = ConsumptionRecordMapper.dcReadingToCIM(dcResponse.getMeterReading());
        }

        return dcRecord;
    }

    /**
     * Request consumption load curve metering data
     *
     * @return Response with metering data
     * @throws ApiException Something went wrong while retrieving data from the API
     * @throws IOException  Something went wrong when accessing the file
     */
    public ConsumptionRecord getConsumptionLoadCurve(String usagePointId, ZonedDateTime start, ZonedDateTime end) throws ApiException, IOException {
        checkDate(start, end);
        // The end date is not in the response when requesting data, increment +1 day to prevent confusion
        end = end.plusDays(1);

        String accept = "application/json";
        String host = configuration.getHostname();
        ConsumptionRecord clcRecord;

        try (BufferedReader br = Files.newBufferedReader(Paths.get(bearerTokenFile), UTF_8)) {
            String authorization = "Bearer " + br.readLine();
            ConsumptionLoadCurveResponse clcResponse = meterApi.meteringDataClcV5ConsumptionLoadCurveGet(authorization, usagePointId, start.toLocalDate().toString(), end.toLocalDate().toString(), accept, null, null, host, null);
            clcRecord = ConsumptionRecordMapper.clcReadingToCIM(clcResponse.getMeterReading());
        }

        return clcRecord;
    }

    private void checkDate(ZonedDateTime start, ZonedDateTime end) throws DateTimeException {
        LocalDate currentDate = LocalDate.ofInstant(Instant.now(), ZoneId.of("Europe/Paris"));

        if (start.isAfter(end)) {
            throw new DateTimeException("Start date should be before end date.");
        }
        if (end.toLocalDate().isEqual(currentDate) || end.toLocalDate().isAfter(currentDate)) {
            throw new DateTimeException("The end date parameter must be earlier than the current date.");
        }
    }
}
