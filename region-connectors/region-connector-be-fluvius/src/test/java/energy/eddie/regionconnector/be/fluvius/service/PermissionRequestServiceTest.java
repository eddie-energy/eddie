package energy.eddie.regionconnector.be.fluvius.service;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.*;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.regionconnector.be.fluvius.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.be.fluvius.permission.events.CreatedEvent;
import energy.eddie.regionconnector.be.fluvius.permission.events.MalformedEvent;
import energy.eddie.regionconnector.be.fluvius.permission.events.ValidatedEvent;
import energy.eddie.regionconnector.be.fluvius.permission.request.Flow;
import energy.eddie.regionconnector.be.fluvius.persistence.BePermissionRequestRepository;
import energy.eddie.regionconnector.be.fluvius.util.DefaultFluviusPermissionRequestBuilder;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionRequestServiceTest {
    @InjectMocks
    private PermissionRequestService permissionRequestService;
    @Mock
    private DataNeedCalculationService<DataNeed> calculationService;
    @Mock
    private Outbox outbox;
    @Mock
    private BePermissionRequestRepository repository;
    @Captor
    private ArgumentCaptor<ValidatedEvent> validatedCaptor;

    public static Stream<Arguments> testCreatePermissionRequest_withInvalidDataNeed_throwsUnsupportedDataNeedException() {
        var now = LocalDate.now(ZoneOffset.UTC);
        return Stream.of(
                Arguments.of(new AccountingPointDataNeedResult(new Timeframe(now, now))),
                Arguments.of(new DataNeedNotSupportedResult("unsupported"))
        );
    }

    @ParameterizedTest
    @MethodSource
    void testCreatePermissionRequest_withInvalidDataNeed_throwsUnsupportedDataNeedException(DataNeedCalculationResult result) {
        // Given
        when(calculationService.calculate("dnid")).thenReturn(result);
        var pr = new PermissionRequestForCreation("cid", "dnid", Flow.B2B);

        // When & Then
        assertThrows(UnsupportedDataNeedException.class, () -> permissionRequestService.createPermissionRequest(pr));
        verify(outbox).commit(isA(CreatedEvent.class));
        verify(outbox).commit(isA(MalformedEvent.class));
    }

    @Test
    void testCreatePermissionRequest_withUnknownDataNeed_throwsDataNeedNotFoundException() {
        // Given
        when(calculationService.calculate("dnid")).thenReturn(new DataNeedNotFoundResult());
        var pr = new PermissionRequestForCreation("cid", "dnid", Flow.B2B);

        // When & Then
        assertThrows(DataNeedNotFoundException.class, () -> permissionRequestService.createPermissionRequest(pr));
        verify(outbox).commit(isA(CreatedEvent.class));
        verify(outbox).commit(isA(MalformedEvent.class));
    }

    @Test
    void testCreatePermissionRequest_emitsValidatedEvent() throws DataNeedNotFoundException, UnsupportedDataNeedException {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        when(calculationService.calculate("dnid"))
                .thenReturn(new ValidatedHistoricalDataDataNeedResult(
                        List.of(Granularity.PT15M),
                        new Timeframe(now, now.plusDays(1)),
                        new Timeframe(now.minusDays(10), now.minusDays(1))
                ));
        var pr = new PermissionRequestForCreation("cid", "dnid", Flow.B2B);

        // When
        var res = permissionRequestService.createPermissionRequest(pr);

        // Then
        assertNotNull(res.permissionId());
        verify(outbox).commit(isA(CreatedEvent.class));
        verify(outbox).commit(validatedCaptor.capture());
        var event = validatedCaptor.getValue();
        assertAll(
                () -> assertEquals(res.permissionId(), event.permissionId()),
                () -> assertEquals(now.minusDays(10), event.start()),
                () -> assertEquals(now.minusDays(1), event.end()),
                () -> assertEquals(Granularity.PT15M, event.granularity()),
                () -> assertEquals(Flow.B2B, event.flow())
        );
    }

    @Test
    void testFindConnectionStatusMessage_mapsCorrectly() {
        // Given
        when(repository.findByPermissionId("pid"))
                .thenReturn(Optional.of(
                                DefaultFluviusPermissionRequestBuilder.create()
                                        .status(PermissionProcessStatus.ACCEPTED)
                                        .build()
                        )
                );

        // When
        var res = permissionRequestService.findConnectionStatusMessageById("pid");

        // Then
        assertTrue(res.isPresent());
        var csm = res.get();
        assertAll(
                () -> assertEquals("cid", csm.connectionId()),
                () -> assertEquals("pid", csm.permissionId()),
                () -> assertEquals("did", csm.dataNeedId()),
                () -> assertEquals(PermissionProcessStatus.ACCEPTED, csm.status())
        );
    }

    @Test
    void testAcceptOrRejectPermissionRequest_throwsOnInvalidStatus() {
        // Given
        // When & Then
        assertThrows(
                IllegalArgumentException.class,
                () -> permissionRequestService.acceptOrRejectPermissionRequest(
                        "pid",
                        PermissionProcessStatus.VALIDATED
                )
        );
    }

    @Test
    void testAcceptOrRejectPermissionRequest_throwsOnUnknownPermissionId() {
        // Given
        when(repository.findByPermissionId("pid")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(
                PermissionNotFoundException.class,
                () -> permissionRequestService.acceptOrRejectPermissionRequest(
                        "pid",
                        PermissionProcessStatus.ACCEPTED
                )
        );
    }

    @Test
    void testAcceptOrRejectPermissionRequest_returnsAcceptOnAlreadyAcceptedPermissionRequest() throws PermissionNotFoundException {
        // Given
        when(repository.findByPermissionId("pid"))
                .thenReturn(Optional.of(getPermissionRequest(Granularity.P1D, PermissionProcessStatus.ACCEPTED)));

        // When
        var res = permissionRequestService.acceptOrRejectPermissionRequest("pid", PermissionProcessStatus.ACCEPTED);

        // Then
        assertTrue(res);
    }

    @Test
    void testAcceptOrRejectPermissionRequest_returnsRejectOnAlreadyRejectedPermissionRequest() throws PermissionNotFoundException {
        // Given
        when(repository.findByPermissionId("pid"))
                .thenReturn(Optional.of(getPermissionRequest(Granularity.P1D, PermissionProcessStatus.REJECTED)));

        // When
        var res = permissionRequestService.acceptOrRejectPermissionRequest("pid", PermissionProcessStatus.REJECTED);

        // Then
        assertFalse(res);
    }

    @Test
    void testAcceptOrRejectPermissionRequest_returnsAcceptForAcceptedPermissionRequest() throws PermissionNotFoundException {
        // Given
        when(repository.findByPermissionId("pid"))
                .thenReturn(Optional.of(getPermissionRequest(Granularity.P1D,
                                                             PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR)));

        // When
        var res = permissionRequestService.acceptOrRejectPermissionRequest("pid", PermissionProcessStatus.ACCEPTED);

        // Then
        assertTrue(res);
        verify(outbox).commit(assertArg(e -> assertEquals(PermissionProcessStatus.ACCEPTED, e.status())));
    }

    @Test
    void testAcceptOrRejectPermissionRequest_returnsRejectForRejectedPermissionRequest() throws PermissionNotFoundException {
        // Given
        when(repository.findByPermissionId("pid"))
                .thenReturn(Optional.of(getPermissionRequest(Granularity.P1D,
                                                             PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR)));

        // When
        var res = permissionRequestService.acceptOrRejectPermissionRequest("pid", PermissionProcessStatus.REJECTED);

        // Then
        assertFalse(res);
        verify(outbox).commit(assertArg(e -> assertEquals(PermissionProcessStatus.REJECTED, e.status())));
    }

    private static FluviusPermissionRequest getPermissionRequest(Granularity granularity, PermissionProcessStatus status
    ) {
        var now = LocalDate.now(ZoneOffset.UTC);
        return new FluviusPermissionRequest(
                "pid",
                "cid",
                "dnid",
                status,
                granularity,
                now,
                now,
                ZonedDateTime.now(ZoneOffset.UTC),
                Flow.B2B
        );
    }
}