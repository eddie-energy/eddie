package energy.eddie.regionconnector.es.datadis.consumer;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.es.datadis.api.DatadisApiException;
import energy.eddie.regionconnector.es.datadis.dtos.AccountingPointData;
import energy.eddie.regionconnector.es.datadis.dtos.ContractDetails;
import energy.eddie.regionconnector.es.datadis.dtos.Supply;
import energy.eddie.regionconnector.es.datadis.permission.events.EsAcceptedEvent;
import energy.eddie.regionconnector.es.datadis.permission.events.EsSimpleEvent;
import energy.eddie.regionconnector.es.datadis.permission.request.DatadisPermissionRequest;
import energy.eddie.regionconnector.es.datadis.permission.request.DistributorCode;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static energy.eddie.regionconnector.es.datadis.DatadisRegionConnectorMetadata.ZONE_ID_SPAIN;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionRequestConsumerTest {
    private final EsPermissionRequest permissionRequest = new DatadisPermissionRequest(
            "pid", null, null, null, null, null, null, null, null, null, null, null, null, false, null
    );
    @Mock
    private Outbox outbox;
    @Captor
    private ArgumentCaptor<EsAcceptedEvent> acceptedCaptor;
    @Captor
    private ArgumentCaptor<EsSimpleEvent> simpleCaptor;
    @InjectMocks
    private PermissionRequestConsumer permissionRequestConsumer;

    @Test
    void acceptPermission_setsDistributorCodeAndPointType_AcceptsRequest_FetchesHistoricalData() {
        // Given
        Supply supply = mock(Supply.class);
        when(supply.pointType()).thenReturn(1);
        when(supply.distributorCode()).thenReturn("1");

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
                new double[0],
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
}
