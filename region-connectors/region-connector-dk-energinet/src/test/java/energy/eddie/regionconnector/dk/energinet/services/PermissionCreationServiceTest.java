package energy.eddie.regionconnector.dk.energinet.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.DataNeedCalculation;
import energy.eddie.api.agnostic.data.needs.DataNeedCalculationService;
import energy.eddie.api.agnostic.data.needs.Timeframe;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.dataneeds.needs.AccountingPointDataNeed;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.needs.aiida.AiidaDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.dk.energinet.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.dk.energinet.permission.events.DKValidatedEvent;
import energy.eddie.regionconnector.dk.energinet.permission.events.DkCreatedEvent;
import energy.eddie.regionconnector.dk.energinet.permission.events.DkMalformedEvent;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionCreationServiceTest {
    @Mock
    private Outbox outbox;
    @Mock
    private DataNeedsService dataNeedsService;
    @Mock
    private AiidaDataNeed aiidaDataNeed;
    @Mock
    private ValidatedHistoricalDataDataNeed vhdDataNeed;
    @Mock
    private AccountingPointDataNeed accountingPointDataNeed;
    @Mock
    private DataNeedCalculationService<DataNeed> calculationService;
    @InjectMocks
    private PermissionCreationService service;
    @Captor
    private ArgumentCaptor<DkCreatedEvent> createdCaptor;
    @Captor
    private ArgumentCaptor<DKValidatedEvent> validatedCaptor;

    @Test
    void testCreatePermissionRequest_emitsMalformedOnUnknownDataNeed() {
        // Given
        var request = new PermissionRequestForCreation("cid",
                                                       "refreshToken",
                                                       "meteringPointId",
                                                       "dnid");
        when(dataNeedsService.findById(any()))
                .thenReturn(Optional.empty());

        // When
        // Then
        assertThrows(DataNeedNotFoundException.class, () -> service.createPermissionRequest(request));
        verify(outbox).commit(isA(DkCreatedEvent.class));
        verify(outbox).commit(isA(DkMalformedEvent.class));
    }

    @Test
    void testCreatePermissionRequest_emitsMalformedOnUnsupportedDataNeed() {
        // Given
        var request = new PermissionRequestForCreation("cid",
                                                       "refreshToken",
                                                       "meteringPointId",
                                                       "dnid");
        when(dataNeedsService.findById(any()))
                .thenReturn(Optional.of(aiidaDataNeed));
        when(calculationService.calculate(aiidaDataNeed))
                .thenReturn(new DataNeedCalculation(false));

        // When
        // Then
        assertThrows(UnsupportedDataNeedException.class, () -> service.createPermissionRequest(request));
        verify(outbox).commit(isA(DkCreatedEvent.class));
        verify(outbox).commit(isA(DkMalformedEvent.class));
    }

    @Test
    void testCreatePermissionRequest_emitsMalformedOnInvalidTimeframe() {
        // Given
        var request = new PermissionRequestForCreation("cid",
                                                       "refreshToken",
                                                       "meteringPointId",
                                                       "dnid");
        when(dataNeedsService.findById(any()))
                .thenReturn(Optional.of(vhdDataNeed));
        when(calculationService.calculate(vhdDataNeed))
                .thenReturn(new DataNeedCalculation(false));
        // When
        // Then
        assertThrows(UnsupportedDataNeedException.class, () -> service.createPermissionRequest(request));
        verify(outbox).commit(isA(DkCreatedEvent.class));
        verify(outbox).commit(isA(DkMalformedEvent.class));
    }

    @Test
    void testCreatePermissionRequest_emitsMalformedOnUnsupportedGranularity() {
        // Given
        var request = new PermissionRequestForCreation("cid",
                                                       "refreshToken",
                                                       "meteringPointId",
                                                       "dnid");
        when(dataNeedsService.findById(any()))
                .thenReturn(Optional.of(vhdDataNeed));

        var now = LocalDate.now(ZoneOffset.UTC);
        when(calculationService.calculate(vhdDataNeed))
                .thenReturn(new DataNeedCalculation(true, List.of(), null, new Timeframe(now, now)));

        // When
        // Then
        assertThrows(UnsupportedDataNeedException.class, () -> service.createPermissionRequest(request));
        verify(outbox).commit(isA(DkCreatedEvent.class));
        verify(outbox).commit(isA(DkMalformedEvent.class));
    }

    @Test
    void testCreatePermissionRequest_emitsValidated() throws DataNeedNotFoundException, UnsupportedDataNeedException {
        // Given
        var request = new PermissionRequestForCreation("cid",
                                                       "refreshToken",
                                                       "meteringPointId",
                                                       "dnid");
        var now = LocalDate.now(ZoneOffset.UTC);
        when(dataNeedsService.findById(any()))
                .thenReturn(Optional.of(vhdDataNeed));

        when(calculationService.calculate(vhdDataNeed))
                .thenReturn(new DataNeedCalculation(true,
                                                    List.of(Granularity.PT15M),
                                                    null,
                                                    new Timeframe(now.minusYears(2), now.plusYears(2))));

        // When
        service.createPermissionRequest(request);
        // Then
        verify(outbox).commit(createdCaptor.capture());
        verify(outbox).commit(validatedCaptor.capture());
        var created = createdCaptor.getValue();
        assertAll(
                () -> assertEquals("cid", created.connectionId()),
                () -> assertEquals("refreshToken", created.refreshToken()),
                () -> assertEquals("meteringPointId", created.meteringPointId()),
                () -> assertEquals("dnid", created.dataNeedId())
        );
        var validated = validatedCaptor.getValue();
        assertAll(
                () -> assertEquals(now.minusYears(2), validated.start()),
                () -> assertEquals(now.plusYears(2), validated.end())
        );
    }

    @Test
    void testCreatePermissionRequest_forAccountingPointDataNeed_emitsValidated() throws DataNeedNotFoundException, UnsupportedDataNeedException {
        // Given
        var request = new PermissionRequestForCreation("cid",
                                                       "refreshToken",
                                                       "meteringPointId",
                                                       "dnid");
        var now = LocalDate.now(ZoneOffset.UTC);
        when(dataNeedsService.findById(any()))
                .thenReturn(Optional.of(accountingPointDataNeed));

        when(calculationService.calculate(accountingPointDataNeed))
                .thenReturn(new DataNeedCalculation(true,
                                                    List.of(Granularity.PT15M),
                                                    null,
                                                    null));

        // When
        service.createPermissionRequest(request);
        // Then
        verify(outbox).commit(createdCaptor.capture());
        verify(outbox).commit(validatedCaptor.capture());
        var created = createdCaptor.getValue();
        assertAll(
                () -> assertEquals("cid", created.connectionId()),
                () -> assertEquals("refreshToken", created.refreshToken()),
                () -> assertEquals("meteringPointId", created.meteringPointId()),
                () -> assertEquals("dnid", created.dataNeedId())
        );
        var validated = validatedCaptor.getValue();
        assertAll(
                () -> assertEquals(now, validated.start()),
                () -> assertEquals(now, validated.end())
        );
    }
}
