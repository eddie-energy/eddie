package energy.eddie.regionconnector.dk.energinet.customer.api;

import energy.eddie.api.v0.ConsumptionRecord;
import energy.eddie.regionconnector.dk.energinet.customer.model.MeteringPointsRequest;

import java.time.ZonedDateTime;

public interface EnerginetCustomerApi {

    Boolean isAlive();

    String apiToken();


    ConsumptionRecord getTimeSeries(ZonedDateTime dateFrom, ZonedDateTime dateTo, MeteringPointsRequest meteringPointsRequest);
}
