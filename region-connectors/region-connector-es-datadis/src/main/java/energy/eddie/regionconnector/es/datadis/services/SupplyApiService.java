package energy.eddie.regionconnector.es.datadis.services;

import energy.eddie.regionconnector.es.datadis.api.SupplyApi;
import energy.eddie.regionconnector.es.datadis.dtos.Supply;
import energy.eddie.regionconnector.es.datadis.dtos.exceptions.InvalidPointAndMeasurementTypeCombinationException;
import energy.eddie.regionconnector.es.datadis.dtos.exceptions.NoSuppliesException;
import energy.eddie.regionconnector.es.datadis.dtos.exceptions.NoSupplyForMeteringPointException;
import energy.eddie.regionconnector.es.datadis.filter.SupplyMeteringPointFilter;
import energy.eddie.regionconnector.es.datadis.filter.SupplyPointTypeAndMeasurementTypeCombinationFilter;
import energy.eddie.regionconnector.es.datadis.filter.SupplyRetryFilter;
import energy.eddie.regionconnector.es.datadis.permission.request.DistributorCode;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

import java.time.Duration;

@Service
public class SupplyApiService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SupplyApiService.class);
    private static final RetryBackoffSpec SUPPLY_RETRY_BACKOFF_SPEC = Retry
            .backoff(20, Duration.ofMinutes(2))
            .maxBackoff(Duration.ofHours(5))
            .filter(ex -> new SupplyRetryFilter(ex).filter());
    private final SupplyApi supplyApi;

    public SupplyApiService(SupplyApi supplyApi) {
        this.supplyApi = supplyApi;
    }

    /**
     * Fetches the supply information for a given permission request.
     * This method retrieves supply details based on the NIF
     * and distributor code provided in the permission request. It performs a series of
     * validations including checking for correct metering point and ensuring the point type
     * supports the requested measurement type. If these conditions are not met, appropriate
     * exceptions are thrown.
     *
     * @param permissionRequest The permission request containing the NIF, distributor code,
     *                          metering point ID, and measurement type.
     * @return A {@link Supply} object containing the supply details if successful.
     * {@link NoSuppliesException}                                If no supplies are found for the provided NIF a nd distributor code.
     * {@link NoSupplyForMeteringPointException}                  If no supply is found for the given metering point ID.
     * {@link InvalidPointAndMeasurementTypeCombinationException} If the point type does not support
     * the requested measurement type.
     */
    public Mono<Supply> fetchSupplyForPermissionRequest(EsPermissionRequest permissionRequest) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Fetching supply for permission request {}", permissionRequest.permissionId());
        }
        return supplyApi.getSupplies(permissionRequest.nif(), permissionRequest.distributorCode().map(DistributorCode::getCode).orElse(null))
                .flatMap(supplies -> new SupplyMeteringPointFilter(supplies, permissionRequest.meteringPointId()).filter())
                .retryWhen(SUPPLY_RETRY_BACKOFF_SPEC) // Supplies are unavailable if a distributor is down, so we retry
                .flatMap(supply -> new SupplyPointTypeAndMeasurementTypeCombinationFilter(supply, permissionRequest.measurementType()).filter());
    }
}
