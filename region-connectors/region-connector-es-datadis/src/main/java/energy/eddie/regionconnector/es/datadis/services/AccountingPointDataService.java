package energy.eddie.regionconnector.es.datadis.services;

import energy.eddie.regionconnector.es.datadis.dtos.AccountingPointData;
import energy.eddie.regionconnector.es.datadis.dtos.exceptions.NoContractsException;
import energy.eddie.regionconnector.es.datadis.dtos.exceptions.NoSuppliesException;
import energy.eddie.regionconnector.es.datadis.dtos.exceptions.NoSupplyForMeteringPointException;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class AccountingPointDataService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AccountingPointDataService.class);
    private final ContractApiService contractApiService;
    private final SupplyApiService supplyApiService;

    public AccountingPointDataService(ContractApiService contractApiService, SupplyApiService supplyApiService) {
        this.contractApiService = contractApiService;
        this.supplyApiService = supplyApiService;
    }

    /**
     * Fetches the {@link AccountingPointData} for a given permission request.
     *
     * @param permissionRequest the permission request that is used to fetch the accounting point data.
     *
     * @return A {@link AccountingPointData} object containing the accounting point details if successful.
     * <p>{@link NoSuppliesException} If no supplies are found for the provided NIF a nd distributor code.
     * <p>{@link NoSupplyForMeteringPointException} If no supply is found for the given metering point of the
     * permission request.
     * <p>{@link NoContractsException} If no contracts are found for permission request.
     */
    public Mono<AccountingPointData> fetchAccountingPointDataForPermissionRequest(
            EsPermissionRequest permissionRequest
    ) {
        LOGGER.atInfo()
              .addArgument(permissionRequest::permissionId)
              .log("Fetching supply and contract for permission request {}");

        return supplyApiService
                .fetchSupplyForPermissionRequest(permissionRequest)
                .flatMap(supply -> contractApiService
                        .fetchContractDetails(
                                permissionRequest.permissionId(),
                                permissionRequest.nif(),
                                supply.distributorCode(),
                                permissionRequest.meteringPointId()
                        )
                        .map(contractDetails -> new AccountingPointData(supply, contractDetails))
                );
    }
}
