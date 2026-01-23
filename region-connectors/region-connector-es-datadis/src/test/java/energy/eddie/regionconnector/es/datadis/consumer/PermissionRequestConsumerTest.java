// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis.consumer;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.needs.AccountingPointDataNeed;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.es.datadis.ContractDetailsProvider;
import energy.eddie.regionconnector.es.datadis.DatadisPermissionRequestBuilder;
import energy.eddie.regionconnector.es.datadis.SupplyProvider;
import energy.eddie.regionconnector.es.datadis.api.DatadisApiException;
import energy.eddie.regionconnector.es.datadis.dtos.AccountingPointData;
import energy.eddie.regionconnector.es.datadis.dtos.ContractDetails;
import energy.eddie.regionconnector.es.datadis.dtos.Supply;
import energy.eddie.regionconnector.es.datadis.permission.events.EsAcceptedEventForAPD;
import energy.eddie.regionconnector.es.datadis.permission.events.EsAcceptedEventForVHD;
import energy.eddie.regionconnector.es.datadis.permission.events.EsSimpleEvent;
import energy.eddie.regionconnector.es.datadis.permission.request.DistributorCode;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import energy.eddie.regionconnector.es.datadis.providers.EnergyDataStreams;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static energy.eddie.regionconnector.es.datadis.DatadisRegionConnectorMetadata.ZONE_ID_SPAIN;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionRequestConsumerTest {
    private final EsPermissionRequest permissionRequest = new DatadisPermissionRequestBuilder()
            .setPermissionId("pid")
            .setDataNeedId("did")
            .build();
    @Mock
    private Outbox outbox;
    @Captor
    private ArgumentCaptor<EsAcceptedEventForVHD> acceptedCaptor;
    @Captor
    private ArgumentCaptor<EsSimpleEvent> simpleCaptor;
    @Captor
    private ArgumentCaptor<EsAcceptedEventForAPD> acceptedAccountingPointDataCaptor;
    @InjectMocks
    private PermissionRequestConsumer permissionRequestConsumer;
    @Mock
    private DataNeedsService dataNeedsService;
    @Mock
    private AccountingPointDataNeed accountingPointDataNeed;
    @Mock
    private ValidatedHistoricalDataDataNeed validatedHistoricalDataDataNeed;
    @Spy
    private EnergyDataStreams streams;


    @Test
    void acceptPermission_setsDistributorCodeAndPointType_AcceptsRequest_FetchesHistoricalData() {
        // Given
        Supply supply = mock(Supply.class);
        when(supply.pointType()).thenReturn(1);
        when(supply.distributorCode()).thenReturn("1");
        when(dataNeedsService.getById(permissionRequest.dataNeedId())).thenReturn(validatedHistoricalDataDataNeed);

        AccountingPointData accountingPointData = new AccountingPointData(supply, createContractDetails());

        // When
        permissionRequestConsumer.acceptPermission(permissionRequest, accountingPointData);

        // Then
        verify(outbox).commit(acceptedCaptor.capture());
        var res = acceptedCaptor.getValue();
        assertAll(
                () -> assertEquals(DistributorCode.fromCode("1"), res.distributorCode()),
                () -> assertEquals(1, res.supplyPointType()),
                () -> assertFalse(res.isProductionSupport())
        );
    }

    @Test
    void consumeError_ifForbidden_callsTimeOut() {
        // Given
        DatadisApiException exception = new DatadisApiException("", HttpResponseStatus.FORBIDDEN, "");

        // When
        permissionRequestConsumer.consumeError(exception, permissionRequest);

        // Then
        verify(outbox).commit(simpleCaptor.capture());
        var res = simpleCaptor.getValue();
        assertEquals(PermissionProcessStatus.TIMED_OUT, res.status());
    }

    @Test
    void consumeError_ifInternalServerError_callsInvalid() {
        // Given
        DatadisApiException exception = new DatadisApiException("", HttpResponseStatus.INTERNAL_SERVER_ERROR, "");

        // When
        permissionRequestConsumer.consumeError(exception, permissionRequest);

        // Then
        verify(outbox).commit(assertArg(event -> assertEquals(PermissionProcessStatus.INVALID, event.status())));
    }

    @Test
    void consumeError_ifRuntimeException_callsInvalid() {
        // Given
        RuntimeException exception = new RuntimeException(new RuntimeException("error"));

        // When
        permissionRequestConsumer.consumeError(exception, permissionRequest);

        // Then
        verify(outbox).commit(assertArg(event -> assertEquals(PermissionProcessStatus.INVALID, event.status())));
    }

    @Test
    void acceptPermission_forAccountingPointDataNeed_emitsAccountingPointDataAndFulfill() throws IOException {
        // Given
        when(dataNeedsService.getById(permissionRequest.dataNeedId())).thenReturn(accountingPointDataNeed);

        // When
        AccountingPointData accountingPointData = new AccountingPointData(
                SupplyProvider.loadSupply().getFirst(),
                ContractDetailsProvider.loadContractDetails().getFirst()
        );
        permissionRequestConsumer.acceptPermission(permissionRequest, accountingPointData);

        // Then
        StepVerifier.create(streams.getAccountingPointData())
                    .assertNext(acp -> assertAll(
                            () -> assertEquals(permissionRequest, acp.permissionRequest()),
                            () -> assertEquals(accountingPointData, acp.accountingPointData())
                    ))
                    .then(() -> streams.close())
                    .verifyComplete();
        verify(outbox).commit(acceptedAccountingPointDataCaptor.capture());
        verify(outbox).commit(simpleCaptor.capture());
        assertAll(
                () -> assertEquals("pid", acceptedAccountingPointDataCaptor.getValue().permissionId()),
                () -> assertEquals(PermissionProcessStatus.ACCEPTED,
                                   acceptedAccountingPointDataCaptor.getValue().status()),
                () -> assertEquals("pid", simpleCaptor.getValue().permissionId()),
                () -> assertEquals(PermissionProcessStatus.FULFILLED, simpleCaptor.getValue().status())
        );
    }

    private ContractDetails createContractDetails() {
        return new ContractDetails(
                "",
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
