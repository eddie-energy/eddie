package energy.eddie.regionconnector.es.datadis.services;

import energy.eddie.regionconnector.es.datadis.api.ContractApi;
import energy.eddie.regionconnector.es.datadis.api.DatadisApiException;
import energy.eddie.regionconnector.es.datadis.api.SupplyApi;
import energy.eddie.regionconnector.es.datadis.dtos.ContractDetails;
import energy.eddie.regionconnector.es.datadis.dtos.exceptions.NoContractsException;
import energy.eddie.regionconnector.es.datadis.filter.DatadisApiRetryFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

import java.time.Duration;
import java.util.List;

@Service
public class ContractApiService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContractApiService.class);
    private static final DatadisApiRetryFilter DATADIS_API_RETRY_FILTER = new DatadisApiRetryFilter(
            SupplyApi.class,
            List.of()
    );
    private static final RetryBackoffSpec RETRY_BACKOFF_SPEC = Retry
            .backoff(20, Duration.ofMinutes(2))
            .maxBackoff(Duration.ofHours(5))
            .filter(DATADIS_API_RETRY_FILTER::filter);
    private final ContractApi contractApi;

    public ContractApiService(ContractApi contractApi) {
        this.contractApi = contractApi;
    }

    /**
     * This method retrieves contract details based on the provided nif, distributor code and metering point. It
     * performs a series of validations including checking for correct metering point and ensuring the point type
     * supports the requested measurement type. If these conditions are not met, appropriate exceptions are thrown.
     *
     * @return A {@link ContractDetails} object containing the most recent contract details if successful.
     * <p>{@link NoContractsException} If no contracts are found for the provided NIF and distributor code.
     * <p>{@link DatadisApiException} If the API call * fails.
     */
    public Mono<ContractDetails> fetchContractDetails(
            String permissionId,
            String nif,
            String distributorCode,
            String meteringPointId
    ) {
        LOGGER.atInfo()
              .addArgument(permissionId)
              .log("Fetching contract details for permission request {}");

        return contractApi
                .getContractDetails(nif, distributorCode, meteringPointId)
                .retryWhen(RETRY_BACKOFF_SPEC)
                .flatMap(contracts -> mapContracts(permissionId, contracts));
    }

    private static Mono<ContractDetails> mapContracts(String permissionId, List<ContractDetails> contracts) {
        if (contracts.isEmpty()) {
            return Mono.error(
                    new NoContractsException("No contracts found, distributor might be unavailable")
            );
        }
        LOGGER.atInfo()
              .addArgument(permissionId)
              .addArgument(contracts::size)
              .log("Got {} contracts for permission request {}");
        if (contracts.size() == 1) {
            return Mono.just(contracts.getFirst());
        }

        ContractDetails mostRecentContract = contracts.getFirst();
        for (ContractDetails current : contracts) {
            if (current.startDate().isAfter(mostRecentContract.startDate())) {
                mostRecentContract = current;
            }
        }

        return Mono.just(mostRecentContract);
    }
}
