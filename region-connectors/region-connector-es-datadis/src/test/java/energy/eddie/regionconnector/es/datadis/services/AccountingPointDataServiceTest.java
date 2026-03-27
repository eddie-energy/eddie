// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis.services;


import energy.eddie.cim.agnostic.PermissionProcessStatus;
import energy.eddie.regionconnector.es.datadis.AuthorizedCupsProvider;
import energy.eddie.regionconnector.es.datadis.DatadisPermissionRequestBuilder;
import energy.eddie.regionconnector.es.datadis.PointType;
import energy.eddie.regionconnector.es.datadis.api.AuthorizationApi;
import energy.eddie.regionconnector.es.datadis.api.MeasurementType;
import energy.eddie.regionconnector.es.datadis.dtos.ContractDetails;
import energy.eddie.regionconnector.es.datadis.dtos.Supply;
import energy.eddie.regionconnector.es.datadis.dtos.exceptions.InvalidPointAndMeasurementTypeCombinationException;
import energy.eddie.regionconnector.es.datadis.dtos.exceptions.NoContractsException;
import energy.eddie.regionconnector.es.datadis.dtos.exceptions.NoSuppliesException;
import energy.eddie.regionconnector.es.datadis.dtos.exceptions.NoSupplyForMeteringPointException;
import energy.eddie.regionconnector.es.datadis.permission.request.DistributorCode;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static energy.eddie.regionconnector.es.datadis.DatadisRegionConnectorMetadata.ZONE_ID_SPAIN;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountingPointDataServiceTest {
    @Mock
    private ContractApiService contractApiService;
    @Mock
    private SupplyApiService supplyApiService;
    @Mock
    private AuthorizationApi authorizationApi;

    static Stream<Exception> supplyApiServiceExceptions() {
        return Stream.of(new NoSuppliesException("No supplies found"),
                         new NoSupplyForMeteringPointException("No supply found for metering point"),
                         new InvalidPointAndMeasurementTypeCombinationException(4, MeasurementType.QUARTER_HOURLY));
    }

    @Test
    void fetchAccountingPointDataForPermissionRequest_everythingSucceeds_returnsExpected() throws IOException {
        // Given
        var permissionRequest = acceptedPermissionRequest();
        Supply supply = createSupply(permissionRequest);
        ContractDetails contractDetails = createContractDetails(permissionRequest);
        var accountingPointDataService = new AccountingPointDataService(contractApiService,
                                                                        supplyApiService,
                                                                        authorizationApi);

        when(supplyApiService.fetchSupplyForPermissionRequest(permissionRequest))
                .thenReturn(Mono.just(supply));
        when(contractApiService.fetchContractDetails(permissionRequest.permissionId(),
                                                     permissionRequest.nif(),
                                                     supply.distributorCode(),
                                                     permissionRequest.meteringPointId()))
                .thenReturn(Mono.just(contractDetails));
        when(authorizationApi.getThirdPartyAuthorizedUsersCups()).thenReturn(Mono.just(AuthorizedCupsProvider.loadUserAuthorizationResponse()));

        // When
        var accountingPointDataMono = accountingPointDataService
                .fetchAccountingPointDataForPermissionRequest(permissionRequest);

        // Then
        StepVerifier.create(accountingPointDataMono)
                    .assertNext(accountingPointData -> assertAll(
                            () -> assertEquals(supply, accountingPointData.supply()),
                            () -> assertEquals(contractDetails, accountingPointData.contractDetails())
                    ))
                    .verifyComplete();
    }

    @ParameterizedTest
    @MethodSource("supplyApiServiceExceptions")
    void fetchAccountingPointDataForPermissionRequest_supplyServiceReturnsError_propagatesError(Exception exception) {
        // Given
        var permissionRequest = acceptedPermissionRequest();
        var accountingPointDataService = new AccountingPointDataService(contractApiService,
                                                                        supplyApiService,
                                                                        authorizationApi);

        when(supplyApiService.fetchSupplyForPermissionRequest(permissionRequest))
                .thenReturn(Mono.error(exception));
        // When
        var accountingPointDataMono = accountingPointDataService
                .fetchAccountingPointDataForPermissionRequest(permissionRequest);

        // Then
        StepVerifier.create(accountingPointDataMono)
                    .expectError(exception.getClass())
                    .verify();
    }

    @Test
    void fetchAccountingPointDataForPermissionRequest_contractApiServiceReturnsError_propagatesError() {
        // Given
        var permissionRequest = acceptedPermissionRequest();
        Supply supply = createSupply(permissionRequest);
        var accountingPointDataService = new AccountingPointDataService(contractApiService,
                                                                        supplyApiService,
                                                                        authorizationApi);

        when(supplyApiService.fetchSupplyForPermissionRequest(permissionRequest))
                .thenReturn(Mono.just(supply));
        when(contractApiService.fetchContractDetails(permissionRequest.permissionId(),
                                                     permissionRequest.nif(),
                                                     supply.distributorCode(),
                                                     permissionRequest.meteringPointId()))
                .thenReturn(Mono.error(new NoContractsException("")));
        // When
        var accountingPointDataMono = accountingPointDataService
                .fetchAccountingPointDataForPermissionRequest(permissionRequest);

        // Then
        StepVerifier.create(accountingPointDataMono)
                    .expectError(NoContractsException.class)
                    .verify();
    }


    @Test
    void fetchAccountingPointDataForPermissionRequest_withoutAuthorizationForNif_returnsNothing() throws IOException {
        // Given
        var permissionRequest = (EsPermissionRequest) new DatadisPermissionRequestBuilder()
                .setNif("G11111111")
                .setMeteringPointId("E00000000000000000GG")
                .setPointType(PointType.TYPE_1)
                .setDistributorCode(DistributorCode.CIDE)
                .build();
        Supply supply = createSupply(permissionRequest);
        ContractDetails contractDetails = createContractDetails(permissionRequest);
        var accountingPointDataService = new AccountingPointDataService(contractApiService,
                                                                        supplyApiService,
                                                                        authorizationApi);

        when(supplyApiService.fetchSupplyForPermissionRequest(permissionRequest))
                .thenReturn(Mono.just(supply));
        when(contractApiService.fetchContractDetails(permissionRequest.permissionId(),
                                                     permissionRequest.nif(),
                                                     supply.distributorCode(),
                                                     permissionRequest.meteringPointId()))
                .thenReturn(Mono.just(contractDetails));
        when(authorizationApi.getThirdPartyAuthorizedUsersCups()).thenReturn(Mono.just(AuthorizedCupsProvider.loadUserAuthorizationResponse()));

        // When
        var accountingPointDataMono = accountingPointDataService
                .fetchAccountingPointDataForPermissionRequest(permissionRequest);

        // Then
        StepVerifier.create(accountingPointDataMono)
                    .verifyComplete();
    }

    @Test
    void fetchAccountingPointDataForPermissionRequest_withoutAuthorizationForCUPS_returnsNothing() throws IOException {
        // Given
        var permissionRequest = (EsPermissionRequest) new DatadisPermissionRequestBuilder()
                .setNif("G00000000")
                .setMeteringPointId("E11111111111111111GG")
                .setPointType(PointType.TYPE_1)
                .setDistributorCode(DistributorCode.CIDE)
                .build();
        Supply supply = createSupply(permissionRequest);
        ContractDetails contractDetails = createContractDetails(permissionRequest);
        var accountingPointDataService = new AccountingPointDataService(contractApiService,
                                                                        supplyApiService,
                                                                        authorizationApi);

        when(supplyApiService.fetchSupplyForPermissionRequest(permissionRequest))
                .thenReturn(Mono.just(supply));
        when(contractApiService.fetchContractDetails(permissionRequest.permissionId(),
                                                     permissionRequest.nif(),
                                                     supply.distributorCode(),
                                                     permissionRequest.meteringPointId()))
                .thenReturn(Mono.just(contractDetails));
        when(authorizationApi.getThirdPartyAuthorizedUsersCups()).thenReturn(Mono.just(AuthorizedCupsProvider.loadUserAuthorizationResponse()));

        // When
        var accountingPointDataMono = accountingPointDataService
                .fetchAccountingPointDataForPermissionRequest(permissionRequest);

        // Then
        StepVerifier.create(accountingPointDataMono)
                    .verifyComplete();
    }

    private static EsPermissionRequest acceptedPermissionRequest() {
        return new DatadisPermissionRequestBuilder()
                .setNif("G00000000")
                .setMeteringPointId("E00000000000000000GG")
                .setStart(LocalDate.now(ZONE_ID_SPAIN))
                .setEnd(LocalDate.now(ZONE_ID_SPAIN))
                .setDistributorCode(DistributorCode.ASEME)
                .setPointType(PointType.TYPE_1)
                .setStatus(PermissionProcessStatus.ACCEPTED)
                .build();
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    private Supply createSupply(EsPermissionRequest permissionRequest) {
        return new Supply(
                "",
                permissionRequest.meteringPointId(),
                "",
                "",
                "",
                "",
                LocalDate.now(ZONE_ID_SPAIN),
                null,
                permissionRequest.pointType().get(),
                permissionRequest.distributorCode().get().getCode()
        );
    }

    private ContractDetails createContractDetails(EsPermissionRequest permissionRequest) {
        return new ContractDetails(
                permissionRequest.meteringPointId(),
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                List.of(0.0),
                "",
                "",
                LocalDate.now(ZONE_ID_SPAIN),
                Optional.empty(),
                "",
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty()

        );
    }
}
