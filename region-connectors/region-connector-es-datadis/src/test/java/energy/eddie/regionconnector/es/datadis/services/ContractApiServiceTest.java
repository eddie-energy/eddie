// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis.services;

import energy.eddie.regionconnector.es.datadis.ContractDetailsProvider;
import energy.eddie.regionconnector.es.datadis.api.ContractApi;
import energy.eddie.regionconnector.es.datadis.dtos.ContractDetails;
import energy.eddie.regionconnector.es.datadis.dtos.exceptions.NoContractsException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContractApiServiceTest {
    @Mock
    private ContractApi contractApi;

    @Test
    void fetchDataForPermissionRequest_whenApiReturnsEmptyContractDetails_returnNoContractsException() {
        // Given
        var expectedNif = "nif";
        var expectedDistributorCode = "distributorCode";
        var expectedMeteringPointId = "meteringPointId";

        when(contractApi.getContractDetails(expectedNif, expectedDistributorCode, expectedMeteringPointId))
                .thenReturn(Mono.just(List.of()));

        var dataApiService = new ContractApiService(contractApi);

        // When
        var contractDetails = dataApiService.fetchContractDetails("permissionId",
                                                                  expectedNif,
                                                                  expectedDistributorCode,
                                                                  expectedMeteringPointId);
        // Then
        StepVerifier.create(contractDetails)
                    .expectError(NoContractsException.class)
                    .verify(Duration.ofSeconds(2));

        verify(contractApi).getContractDetails(expectedNif, expectedDistributorCode, expectedMeteringPointId);
        verifyNoMoreInteractions(contractApi);
    }

    @Test
    void fetchDataForPermissionRequest_whenApiReturnsSingleContractDetails_ContractDetails() throws IOException {
        // Given
        var expectedNif = "nif";
        var expectedDistributorCode = "distributorCode";
        var expectedMeteringPointId = "meteringPointId";

        List<ContractDetails> allContracts = ContractDetailsProvider.loadContractDetails();
        when(contractApi.getContractDetails(expectedNif, expectedDistributorCode, expectedMeteringPointId))
                .thenReturn(Mono.just(allContracts));

        var dataApiService = new ContractApiService(contractApi);

        // When
        var contractDetails = dataApiService.fetchContractDetails("permissionId",
                                                                  expectedNif,
                                                                  expectedDistributorCode,
                                                                  expectedMeteringPointId);
        // Then
        StepVerifier.create(contractDetails)
                    .expectNext(allContracts.getFirst())
                    .verifyComplete();
    }

    @Test
    void fetchDataForPermissionRequest_whenApiReturnsMultipleContractDetails_returnsMostRecentContractDetails() throws IOException {
        // Given
        var expectedNif = "nif";
        var expectedDistributorCode = "distributorCode";
        var expectedMeteringPointId = "meteringPointId";

        List<ContractDetails> allContracts = ContractDetailsProvider.loadMultipleContractDetails();
        when(contractApi.getContractDetails(expectedNif, expectedDistributorCode, expectedMeteringPointId))
                .thenReturn(Mono.just(allContracts));

        var dataApiService = new ContractApiService(contractApi);

        // When
        var contractDetails = dataApiService.fetchContractDetails("permissionId",
                                                                  expectedNif,
                                                                  expectedDistributorCode,
                                                                  expectedMeteringPointId);
        // Then
        StepVerifier.create(contractDetails)
                    .expectNext(allContracts.get(1)) // second contract is the most recent
                    .verifyComplete();
    }
}
