package energy.eddie.regionconnector.es.datadis.api;

import energy.eddie.regionconnector.es.datadis.dtos.ContractDetails;
import reactor.core.publisher.Mono;

import java.util.List;

public interface ContractApi {
    Mono<List<ContractDetails>> getContractDetails(String authorizedNif, String distributorCode, String cups);
}
