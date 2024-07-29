package energy.eddie.regionconnector.fr.enedis.api;

import energy.eddie.regionconnector.fr.enedis.dto.contract.CustomerContract;
import reactor.core.publisher.Mono;

public interface EnedisAccountingPointDataApi {

    /**
     * Retrieves the contract data for a specified usage point.
     *
     * @param usagePointId The unique identifier for the usage point. Must not be null or empty.
     * @return A {@link Mono} that emits the {@link CustomerContract} data for the specified usage point or an error
     */
    Mono<CustomerContract> getContract(String usagePointId);
}
