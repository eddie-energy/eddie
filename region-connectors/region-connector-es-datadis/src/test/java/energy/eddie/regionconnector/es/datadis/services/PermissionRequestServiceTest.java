package energy.eddie.regionconnector.es.datadis.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.DataNeedCalculation;
import energy.eddie.api.agnostic.data.needs.DataNeedCalculationService;
import energy.eddie.api.agnostic.data.needs.Timeframe;
import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.es.datadis.api.DatadisApiException;
import energy.eddie.regionconnector.es.datadis.consumer.PermissionRequestConsumer;
import energy.eddie.regionconnector.es.datadis.dtos.*;
import energy.eddie.regionconnector.es.datadis.permission.events.EsCreatedEvent;
import energy.eddie.regionconnector.es.datadis.permission.events.EsMalformedEvent;
import energy.eddie.regionconnector.es.datadis.permission.request.DatadisPermissionRequest;
import energy.eddie.regionconnector.es.datadis.persistence.EsPermissionRequestRepository;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static energy.eddie.regionconnector.es.datadis.DatadisRegionConnectorMetadata.ZONE_ID_SPAIN;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionRequestServiceTest {
    @Mock
    private EsPermissionRequestRepository repository;
    @Mock
    private AccountingPointDataService accountingPointDataService;
    @Mock
    private PermissionRequestConsumer permissionRequestConsumer;
    @Mock
    private DataNeedsService dataNeedsService;
    @Mock
    private ValidatedHistoricalDataDataNeed validatedHistoricalDataDataNeed;
    @Mock
    private Outbox outbox;
    @Mock
    private DataNeedCalculationService<DataNeed> calculationService;
    @InjectMocks
    private PermissionRequestService service;
    @Captor
    private ArgumentCaptor<PermissionEvent> eventCaptor;

    @Test
    void findConnectionStatusMessageById_nonExistingId_returnsEmptyOptional() {
        // Given
        var permissionId = "nonExisting";
        when(repository.findByPermissionId(permissionId)).thenReturn(Optional.empty());

        // When
        Optional<ConnectionStatusMessage> statusMessage = service.findConnectionStatusMessageById(permissionId);

        // Then
        assertTrue(statusMessage.isEmpty());
    }

    @Test
    void findConnectionStatusMessageById_existingId_returnsPopulatedStatusMessage() {
        // Given
        var permissionId = "Existing";
        var connectionId = "bar";
        var dataNeedId = "luu";
        var nif = "muh";
        var meteringPointId = "kuh";
        var now = LocalDate.now(ZONE_ID_SPAIN);
        var requestDataFrom = now.minusDays(10);
        var requestDataTo = now.minusDays(5);
        var permissionRequest = new DatadisPermissionRequest(permissionId,
                                                             connectionId,
                                                             dataNeedId,
                                                             Granularity.PT15M,
                                                             nif,
                                                             meteringPointId,
                                                             requestDataFrom,
                                                             requestDataTo,
                                                             null,
                                                             null,
                                                             null,
                                                             PermissionProcessStatus.CREATED,
                                                             null,
                                                             false,
                                                             ZonedDateTime.now(ZoneOffset.UTC),
                                                             AllowedGranularity.PT15M_OR_PT1H);
        when(repository.findByPermissionId(permissionId)).thenReturn(Optional.of(permissionRequest));

        // When
        Optional<ConnectionStatusMessage> optional = service.findConnectionStatusMessageById(permissionId);

        // Then
        assertTrue(optional.isPresent());
        var statusMessage = optional.get();
        assertEquals(permissionId, statusMessage.permissionId());
        assertEquals(connectionId, statusMessage.connectionId());
        assertEquals(dataNeedId, statusMessage.dataNeedId());
        assertEquals(PermissionProcessStatus.CREATED, statusMessage.status());
    }

    @Test
    void acceptPermission_nonExistingId_throws() {
        // Given
        var permissionId = "nonExisting";
        when(repository.findByPermissionId(permissionId)).thenReturn(Optional.empty());

        // When, Then
        assertThrows(PermissionNotFoundException.class, () -> service.acceptPermission(permissionId));
    }

    @Test
    void acceptPermission_existingId_callsPermissionRequestConsumer() {
        // Given
        var permissionId = "Existing";
        var permissionRequest = mock(DatadisPermissionRequest.class);
        Supply supply = new Supply("", "", "", "", "", "1", LocalDate.now(ZONE_ID_SPAIN), null, 1, "1");
        AccountingPointData accountingPointData = new AccountingPointData(supply, createContractDetails());
        when(permissionRequest.permissionId()).thenReturn(permissionId);
        when(repository.findByPermissionId(permissionId)).thenReturn(Optional.of(permissionRequest));
        when(accountingPointDataService.fetchAccountingPointDataForPermissionRequest(permissionRequest)).thenReturn(
                Mono.just(accountingPointData)
        );

        // When
        assertDoesNotThrow(() -> service.acceptPermission(permissionId));

        // Then
        verify(permissionRequestConsumer).acceptPermission(permissionRequest, accountingPointData);
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
    void acceptPermission_existingId_whenSupplyApiServiceReturnsException_callsErrorConsumer() {
        // Given
        var permissionId = "Existing";
        var permissionRequest = mock(DatadisPermissionRequest.class);
        when(permissionRequest.permissionId()).thenReturn(permissionId);
        when(repository.findByPermissionId(permissionId)).thenReturn(Optional.of(permissionRequest));
        when(accountingPointDataService.fetchAccountingPointDataForPermissionRequest(permissionRequest)).thenReturn(
                Mono.error(new DatadisApiException("", HttpResponseStatus.FORBIDDEN, ""))
        );

        // When
        assertDoesNotThrow(() -> service.acceptPermission(permissionId));

        // Then
        verify(permissionRequestConsumer).consumeError(any(), eq(permissionRequest));
    }

    @Test
    void rejectPermission_nonExistingId_throws() {
        // Given
        var permissionId = "nonExisting";
        when(repository.findByPermissionId(permissionId)).thenReturn(Optional.empty());

        // When, Then
        assertThrows(PermissionNotFoundException.class, () -> service.rejectPermission(permissionId));
    }

    @Test
    void rejectPermission_existingId_callsReject() {
        // Given
        var permissionId = "Existing";
        var permissionRequest = mock(DatadisPermissionRequest.class);
        when(repository.findByPermissionId(permissionId)).thenReturn(Optional.of(permissionRequest));

        // When
        assertDoesNotThrow(() -> service.rejectPermission(permissionId));

        // Then
        verify(outbox).commit(assertArg(event -> assertEquals(PermissionProcessStatus.REJECTED, event.status())));
    }

    @Test
    void createAndSendPermissionRequest_emitsCreatedAndValidated() throws DataNeedNotFoundException, UnsupportedDataNeedException {
        // Given
        var creationRequest = new PermissionRequestForCreation("cid", "dnid", "nif", "mid");
        when(dataNeedsService.findById(any())).thenReturn(Optional.of(validatedHistoricalDataDataNeed));
        var now = LocalDate.now(ZONE_ID_SPAIN);
        when(calculationService.calculate(validatedHistoricalDataDataNeed))
                .thenReturn(new DataNeedCalculation(
                        true,
                        List.of(Granularity.PT15M, Granularity.PT1H),
                        new Timeframe(now, now),
                        new Timeframe(now, now)
                ));

        // When
        service.createAndSendPermissionRequest(creationRequest);

        // Then
        verify(dataNeedsService).findById(any());
        verify(outbox, times(2)).commit(eventCaptor.capture());
        var first = eventCaptor.getAllValues().getFirst();
        assertEquals(PermissionProcessStatus.CREATED, first.status());

        var second = eventCaptor.getAllValues().get(1);
        assertEquals(PermissionProcessStatus.VALIDATED, second.status());
    }

    @ParameterizedTest
    @NullAndEmptySource
    void createAndSendPermissionRequest_withInvalidGranularities_throws(List<Granularity> granularities) {
        // Given
        var mockCreationRequest = new PermissionRequestForCreation("cid", "dnid", "nif", "mid");
        when(dataNeedsService.findById(any())).thenReturn(Optional.of(validatedHistoricalDataDataNeed));
        var now = LocalDate.now(ZONE_ID_SPAIN);
        when(calculationService.calculate(validatedHistoricalDataDataNeed))
                .thenReturn(new DataNeedCalculation(true,
                                                    granularities,
                                                    new Timeframe(now, now),
                                                    new Timeframe(now, now)));
        // When
        // Then
        assertThrows(UnsupportedDataNeedException.class,
                     () -> service.createAndSendPermissionRequest(mockCreationRequest));
        verify(outbox).commit(isA(EsCreatedEvent.class));
        verify(outbox).commit(isA(EsMalformedEvent.class));
    }

    @Test
    void createAndSendPermissionRequest_withInvalidTimeframe_throws() {
        // Given
        var mockCreationRequest = new PermissionRequestForCreation("cid", "dnid", "nif", "mid");
        when(dataNeedsService.findById(any())).thenReturn(Optional.of(validatedHistoricalDataDataNeed));
        when(calculationService.calculate(validatedHistoricalDataDataNeed))
                .thenReturn(new DataNeedCalculation(true, List.of(Granularity.PT1H), null, null));
        // When
        // Then
        assertThrows(UnsupportedDataNeedException.class,
                     () -> service.createAndSendPermissionRequest(mockCreationRequest));
        verify(outbox).commit(isA(EsCreatedEvent.class));
        verify(outbox).commit(isA(EsMalformedEvent.class));
    }

    @Test
    void createAndSendPermissionRequest_withUnknownDataNeed_throws() {
        // Given
        var mockCreationRequest = new PermissionRequestForCreation("cid", "dnid", "nif", "mid");
        when(dataNeedsService.findById(any())).thenReturn(Optional.empty());

        // When
        // Then
        assertThrows(DataNeedNotFoundException.class,
                     () -> service.createAndSendPermissionRequest(mockCreationRequest));
        verify(outbox).commit(isA(EsCreatedEvent.class));
        verify(outbox).commit(isA(EsMalformedEvent.class));
    }

    @Test
    void createAndSendPermissionRequest_withInvalidDataNeed_throws() {
        // Given
        var mockCreationRequest = new PermissionRequestForCreation("cid", "dnid", "nif", "mid");
        when(dataNeedsService.findById(any())).thenReturn(Optional.of(validatedHistoricalDataDataNeed));
        when(calculationService.calculate(validatedHistoricalDataDataNeed))
                .thenReturn(new DataNeedCalculation(false));
        // When
        // Then
        assertThrows(UnsupportedDataNeedException.class,
                     () -> service.createAndSendPermissionRequest(mockCreationRequest));
        verify(outbox).commit(isA(EsCreatedEvent.class));
        verify(outbox).commit(isA(EsMalformedEvent.class));
    }

    @Test
    void terminatePermission_nonExistingId_throws() {
        // Given
        var permissionId = "nonExisting";
        when(repository.findByPermissionId(permissionId)).thenReturn(Optional.empty());

        // When, Then
        assertThrows(PermissionNotFoundException.class, () -> service.terminatePermission(permissionId));
    }

    @Test
    void terminatePermission_existingId_callsTerminate() {
        // Given
        var permissionId = "Existing";
        var permissionRequest = mock(DatadisPermissionRequest.class);
        when(repository.findByPermissionId(permissionId)).thenReturn(Optional.of(permissionRequest));

        // When
        assertDoesNotThrow(() -> service.terminatePermission(permissionId));

        // Then
        verify(outbox).commit(assertArg(event -> assertEquals(PermissionProcessStatus.TERMINATED, event.status())));
    }
}
