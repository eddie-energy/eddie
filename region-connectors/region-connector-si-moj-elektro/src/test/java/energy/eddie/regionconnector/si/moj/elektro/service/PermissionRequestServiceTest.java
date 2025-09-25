package energy.eddie.regionconnector.si.moj.elektro.service;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.*;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.si.moj.elektro.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.si.moj.elektro.permission.events.CreatedEvent;
import energy.eddie.regionconnector.si.moj.elektro.permission.events.MalformedEvent;
import energy.eddie.regionconnector.si.moj.elektro.permission.events.ValidatedEvent;
import energy.eddie.regionconnector.si.moj.elektro.permission.request.MojElektroPermissionRequest;
import energy.eddie.regionconnector.si.moj.elektro.persistence.SiPermissionRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionRequestServiceTest {

    @Mock
    private DataNeedCalculationService<DataNeed> calculationService;
    @Mock
    private Outbox outbox;
    @Mock
    private SiPermissionRequestRepository repository;
    @InjectMocks
    private PermissionRequestService permissionRequestService;
    @Captor
    private ArgumentCaptor<ValidatedEvent> validatedCaptor;
    private final PermissionRequestForCreation pr = new PermissionRequestForCreation(
            "cid",
            "dnid",
            "apitoken"
    );

    @Test
    void testCreatePermissionRequest_invalidDataNeed() {
        // Given
        when(calculationService.calculate("dnid")).thenReturn(
                new DataNeedNotSupportedResult("unsupported")
        );

        // When
        assertThrows(UnsupportedDataNeedException.class, () -> permissionRequestService.createPermissionRequest(pr));

        // Then
        verify(outbox).commit(isA(CreatedEvent.class));
        verify(outbox).commit(isA(MalformedEvent.class));
    }

    @Test
    void testCreatePermissionRequest_accountingPointDataNeed() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        when(calculationService.calculate("dnid")).thenReturn(
                new AccountingPointDataNeedResult(new Timeframe(now, now))
        );

        // When
        assertThrows(UnsupportedDataNeedException.class, () -> permissionRequestService.createPermissionRequest(pr));

        // Then
        verify(outbox).commit(isA(CreatedEvent.class));
        verify(outbox).commit(isA(MalformedEvent.class));
    }

    @Test
    void testCreatePermissionRequest_unknownDataNeed() {
        // Given
        when(calculationService.calculate("dnid")).thenReturn(new DataNeedNotFoundResult());

        // When
        assertThrows(DataNeedNotFoundException.class, () -> permissionRequestService.createPermissionRequest(pr));

        // Then
        verify(outbox).commit(isA(CreatedEvent.class));
        verify(outbox).commit(isA(MalformedEvent.class));
    }

    @Test
    void testCreatePermissionRequest_validatedResult() throws DataNeedNotFoundException, UnsupportedDataNeedException {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        when(calculationService.calculate("dnid"))
                .thenReturn(new ValidatedHistoricalDataDataNeedResult(
                        List.of(Granularity.PT15M),
                        new Timeframe(now, now.plusDays(1)),
                        new Timeframe(now.minusDays(10), now.minusDays(1))
                ));

        // When
        var res = permissionRequestService.createPermissionRequest(pr);

        // Then
        verify(outbox).commit(isA(CreatedEvent.class));
        verify(outbox).commit(validatedCaptor.capture());
        verify(outbox, times(2)).commit(any());
        var event = validatedCaptor.getValue();
        assertAll(
                () -> assertEquals(res.permissionId(), event.permissionId()),
                () -> assertEquals(now.minusDays(10), event.start()),
                () -> assertEquals(now.minusDays(1), event.end()),
                () -> assertEquals(Granularity.PT15M, event.granularity()),
                () -> assertEquals(pr.apiToken(), event.apiToken())
        );
    }

    @Test
    void testFindConnectionStatusMessageById_returnsValue() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        var request = new MojElektroPermissionRequest(
                "pid",
                "cid",
                "dnid",
                PermissionProcessStatus.CREATED,
                Granularity.PT15M,
                now.minusDays(10),
                now.minusDays(1),
                ZonedDateTime.now(ZoneOffset.UTC),
                null
        );
        when(repository.findByPermissionId("pid")).thenReturn(Optional.of(request));

        // When
        var result = permissionRequestService.findConnectionStatusMessageById("pid");

        // Then
        assertTrue(result.isPresent());
        var csm = result.get();
        assertAll(
                () -> assertEquals(request.permissionId(), csm.permissionId()),
                () -> assertEquals(request.connectionId(), csm.connectionId()),
                () -> assertEquals(request.dataNeedId(), csm.dataNeedId()),
                () -> assertEquals(request.status(), csm.status()),
                () -> assertEquals(request.start(), now.minusDays(10)),
                () -> assertEquals(request.end(), now.minusDays(1)),
                () -> assertEquals(Granularity.PT15M, request.granularity())
        );
    }

    @Test
    void testFindConnectionStatusMessageById_notFound() {
        // Given
        when(repository.findByPermissionId("pid")).thenReturn(Optional.empty());

        // When
        var result = permissionRequestService.findConnectionStatusMessageById("pid");

        // Then
        assertTrue(result.isEmpty());
    }
}
