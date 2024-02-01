package energy.eddie.regionconnector.es.datadis.api;

import energy.eddie.regionconnector.es.datadis.dtos.MeteringData;
import energy.eddie.regionconnector.es.datadis.dtos.MeteringDataRequest;
import reactor.core.publisher.Mono;

import java.util.List;

public interface DataApi {

    Mono<List<MeteringData>> getConsumptionKwh(MeteringDataRequest meteringDataRequest);
}