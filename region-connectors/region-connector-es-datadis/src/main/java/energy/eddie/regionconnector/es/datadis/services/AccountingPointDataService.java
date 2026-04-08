// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis.services;

import energy.eddie.regionconnector.es.datadis.api.AuthorizationApi;
import energy.eddie.regionconnector.es.datadis.dtos.AccountingPointData;
import energy.eddie.regionconnector.es.datadis.dtos.ContractDetails;
import energy.eddie.regionconnector.es.datadis.dtos.Supply;
import energy.eddie.regionconnector.es.datadis.dtos.authorizations.AuthorizedCups;
import energy.eddie.regionconnector.es.datadis.dtos.authorizations.UserAuthorizationsResponse;
import energy.eddie.regionconnector.es.datadis.dtos.exceptions.NoContractsException;
import energy.eddie.regionconnector.es.datadis.dtos.exceptions.NoSuppliesException;
import energy.eddie.regionconnector.es.datadis.dtos.exceptions.NoSupplyForMeteringPointException;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

@Service
public class AccountingPointDataService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AccountingPointDataService.class);
    private final ContractApiService contractApiService;
    private final SupplyApiService supplyApiService;
    private final AuthorizationApi authorizationApi;

    public AccountingPointDataService(
            ContractApiService contractApiService, SupplyApiService supplyApiService,
            AuthorizationApi authorizationApi
    ) {
        this.contractApiService = contractApiService;
        this.supplyApiService = supplyApiService;
        this.authorizationApi = authorizationApi;
    }

    /**
     * Fetches the {@link AccountingPointData} for a given permission request.
     *
     * @param permissionRequest the permission request that is used to fetch the accounting point data.
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
              .log("Fetching supply, contract, and authorization for permission request {}");

        return supplyApiService
                .fetchSupplyForPermissionRequest(permissionRequest)
                .zipWhen(supply -> contractApiService
                        .fetchContractDetails(
                                permissionRequest.permissionId(),
                                permissionRequest.nif(),
                                supply.distributorCode(),
                                permissionRequest.meteringPointId()
                        ))
                .zipWith(Mono.defer(() -> authorizationApi.getThirdPartyAuthorizedUsersCups()
                                                          .flatMap(users -> filterCups(permissionRequest, users))),
                         AccountingPointDataService::mapToAccountingPointData);
    }

    private static AccountingPointData mapToAccountingPointData(
            Tuple2<Supply, ContractDetails> tuple,
            AuthorizedCups users
    ) {
        return new AccountingPointData(tuple.getT1(), tuple.getT2(), users);
    }

    private Mono<AuthorizedCups> filterCups(
            EsPermissionRequest permissionRequest,
            UserAuthorizationsResponse userAuthorizationsResponse
    ) {
        for (var authorizedCup : userAuthorizationsResponse.authorizedCups()) {
            if (permissionRequest.meteringPointId().equals(authorizedCup.cups())
                && permissionRequest.nif().equals(authorizedCup.ownerDocument())) {
                return Mono.just(authorizedCup);
            }
        }
        LOGGER.atWarn()
              .addArgument(permissionRequest::permissionId)
              .log("Could not find authorized cups for permission request {}");
        return Mono.empty();
    }
}
