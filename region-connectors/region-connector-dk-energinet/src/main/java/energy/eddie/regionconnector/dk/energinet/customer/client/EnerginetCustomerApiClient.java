package energy.eddie.regionconnector.dk.energinet.customer.client;

import energy.eddie.api.v0.ConsumptionRecord;
import energy.eddie.regionconnector.dk.energinet.config.EnerginetConfiguration;
import energy.eddie.regionconnector.dk.energinet.config.PropertiesEnerginetConfiguration;
import energy.eddie.regionconnector.dk.energinet.customer.api.EnerginetCustomerApi;
import energy.eddie.regionconnector.dk.energinet.customer.api.IsAliveApi;
import energy.eddie.regionconnector.dk.energinet.customer.api.MeterDataApi;
import energy.eddie.regionconnector.dk.energinet.customer.api.TokenApi;
import energy.eddie.regionconnector.dk.energinet.customer.invoker.ApiClient;
import energy.eddie.regionconnector.dk.energinet.customer.model.MeteringPointsRequest;
import energy.eddie.regionconnector.dk.energinet.util.ConsumptionRecordMapper;

import java.time.*;
import java.util.Objects;

public class EnerginetCustomerApiClient extends ApiClient implements EnerginetCustomerApi {
    private final TokenApi tokenApi;
    private final MeterDataApi meterDataApi;
    private final IsAliveApi isAliveApi;
    private String refreshToken = "";
    private String accessToken = "";

    public EnerginetCustomerApiClient(String refreshToken, PropertiesEnerginetConfiguration propertiesEnerginetConfiguration) {
        super("Bearer");

        this.refreshToken = refreshToken;
        setBasePath(((EnerginetConfiguration) propertiesEnerginetConfiguration).customerBasePath());

        tokenApi = this.buildClient(TokenApi.class);
        meterDataApi = this.buildClient(MeterDataApi.class);
        isAliveApi = this.buildClient(IsAliveApi.class);
    }

    private void throwIfInvalidTimeframe(ZonedDateTime start, ZonedDateTime end) throws DateTimeException {
        LocalDate currentDate = LocalDate.ofInstant(Instant.now(), ZoneId.of("Europe/Copenhagen"));

        if (start.isAfter(end)) {
            throw new DateTimeException("Start date should be before end date.");
        }
        if (end.toLocalDate().isEqual(currentDate) || end.toLocalDate().isAfter(currentDate)) {
            throw new DateTimeException("The end date parameter must be earlier than the current date.");
        }
    }

    @Override
    public Boolean isAlive() {
        return isAliveApi.apiIsaliveGet();
    }

    @Override
    public String apiToken() {
        setApiKey(refreshToken);
        accessToken = tokenApi.apiTokenGet().getResult();

        return accessToken;
    }

    @Override
    public ConsumptionRecord getTimeSeries(ZonedDateTime dateFrom,
                                           ZonedDateTime dateTo,
                                           MeteringPointsRequest meteringPointsRequest) {
        throwIfInvalidTimeframe(dateFrom, dateTo);
        setApiKey(accessToken);

        return ConsumptionRecordMapper.timeSeriesToCIM(
                Objects.requireNonNull(
                        meterDataApi.apiMeterdataGettimeseriesDateFromDateToAggregationPost(
                                dateFrom.toLocalDate().toString(),
                                dateTo.toLocalDate().toString(),
                                "Actual",
                                meteringPointsRequest
                        ).getResult()
                )
        );
    }

    @Override
    public void setApiKey(String token) {
        super.setApiKey("Bearer " + token);
    }
}
