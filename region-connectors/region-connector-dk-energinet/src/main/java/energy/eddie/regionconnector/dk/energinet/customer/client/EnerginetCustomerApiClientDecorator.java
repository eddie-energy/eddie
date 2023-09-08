package energy.eddie.regionconnector.dk.energinet.customer.client;

import energy.eddie.api.v0.ConsumptionRecord;
import energy.eddie.regionconnector.dk.energinet.customer.api.EnerginetCustomerApi;
import energy.eddie.regionconnector.dk.energinet.customer.model.MeteringPointsRequest;

import java.time.ZonedDateTime;

public class EnerginetCustomerApiClientDecorator implements EnerginetCustomerApi {
    private final EnerginetCustomerApi energinetCustomerApi;

    public EnerginetCustomerApiClientDecorator(EnerginetCustomerApi energinetCustomerApi) {
        this.energinetCustomerApi = energinetCustomerApi;
    }

    @Override
    public Boolean isAlive() {
       return energinetCustomerApi.isAlive();
    }

    @Override
    public String apiToken() {
        return energinetCustomerApi.apiToken();
    }

    @Override
    public ConsumptionRecord getTimeSeries(ZonedDateTime dateFrom, ZonedDateTime dateTo, MeteringPointsRequest meteringPointsRequest) {
        return energinetCustomerApi.getTimeSeries(dateFrom, dateTo, meteringPointsRequest);
    }
}
