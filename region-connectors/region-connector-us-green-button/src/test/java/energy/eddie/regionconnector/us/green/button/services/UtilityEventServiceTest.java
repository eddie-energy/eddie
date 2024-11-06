package energy.eddie.regionconnector.us.green.button.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import energy.eddie.regionconnector.us.green.button.dtos.WebhookEvent;
import energy.eddie.regionconnector.us.green.button.permission.events.MeterReading;
import energy.eddie.regionconnector.us.green.button.permission.events.PollingStatus;
import energy.eddie.regionconnector.us.green.button.permission.events.UsStartPollingEvent;
import energy.eddie.regionconnector.us.green.button.permission.events.UsUnfulfillableEvent;
import energy.eddie.regionconnector.us.green.button.permission.request.GreenButtonPermissionRequest;
import energy.eddie.regionconnector.us.green.button.persistence.MeterReadingRepository;
import energy.eddie.regionconnector.us.green.button.persistence.UsPermissionRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UtilityEventServiceTest {
    @Mock
    private Outbox outbox;
    @Mock
    private UsPermissionRequestRepository repository;
    @Mock
    private MeterReadingRepository meterReadingRepository;
    @InjectMocks
    private UtilityEventService utilityEventService;
    @Captor
    private ArgumentCaptor<UsUnfulfillableEvent> unfulfillableEvent;

    @Test
    void testReceiveEvents_withAuthorizationExpiredWebhookEvent_emitsUnfulfillable() throws PermissionNotFoundException {
        // Given
        var events = List.of(getWebhookEvent("authorization_expired", null));
        when(repository.findByAuthUid("0000")).thenReturn(getPermissionRequest(PermissionProcessStatus.ACCEPTED));

        // When
        utilityEventService.receiveEvents(events);

        // Then
        verify(outbox).commit(unfulfillableEvent.capture());
        var res = unfulfillableEvent.getValue();
        assertAll(
                () -> assertEquals("pid", res.permissionId()),
                () -> assertFalse(res.requiresExternalTermination())
        );
    }

    @Test
    void testReceiveEvents_withAuthorizationExpiredWebhookEvent_emitsNothingIfPermissionRequestAlreadyTerminated() throws PermissionNotFoundException {
        // Given
        var events = List.of(getWebhookEvent("authorization_expired", null));
        when(repository.findByAuthUid("0000")).thenReturn(getPermissionRequest(PermissionProcessStatus.EXTERNALLY_TERMINATED));

        // When
        utilityEventService.receiveEvents(events);

        // Then
        verify(outbox, never()).commit(any());
    }

    @Test
    void testReceiveEvents_withAuthorizationRevokedWebhookEvent_emitsRevoked() throws PermissionNotFoundException {
        // Given
        var events = List.of(getWebhookEvent("authorization_revoked", null));
        when(repository.findByAuthUid("0000")).thenReturn(getPermissionRequest(PermissionProcessStatus.ACCEPTED));

        // When
        utilityEventService.receiveEvents(events);

        // Then
        verify(outbox).commit(assertArg(res -> assertEquals(PermissionProcessStatus.REVOKED, res.status())));
    }

    @Test
    void testReceiveEvents_withUnknownPermission_throwsPermissionNotFound() {
        // Given
        var events = List.of(getWebhookEvent("authorization_revoked", null));
        when(repository.findByAuthUid("0000")).thenReturn(null);

        // When & Then
        assertThrows(PermissionNotFoundException.class, () -> utilityEventService.receiveEvents(events));
    }

    @Test
    void testReceiveEvents_emitsNothing_forUnknownEventType() throws PermissionNotFoundException {
        // Given
        var events = List.of(getWebhookEvent("unknown_event_type", null));
        when(repository.findByAuthUid("0000")).thenReturn(getPermissionRequest(PermissionProcessStatus.EXTERNALLY_TERMINATED));

        // When
        utilityEventService.receiveEvents(events);

        // Then
        verify(outbox, never()).commit(any());
    }

    @Test
    void testReceiveEvents_emitsNothing_forMissingAuthUid() throws PermissionNotFoundException {
        // Given
        var events = List.of(
                new WebhookEvent(
                        "0000",
                        "authorization_revoked",
                        ZonedDateTime.now(ZoneOffset.UTC),
                        "webhook",
                        URI.create("https://localhost:8080/region-connectors/us-green-button/webhook/"),
                        false,
                        null,
                        null
                )
        );

        // When
        utilityEventService.receiveEvents(events);

        // Then
        verify(outbox, never()).commit(any());
        verify(repository, never()).findByAuthUid(any());
    }

    @Test
    void testReceiveEvents_withHistoricalCollectionFinishedEvent_emitsNothing_ifCollectionIsNotFinishedForAllMeters() throws PermissionNotFoundException {
        // Given
        var events = List.of(getWebhookEvent("meter_historical_collection_finished_successful", "uid"));
        when(repository.findByAuthUid("0000")).thenReturn(getPermissionRequest(PermissionProcessStatus.ACCEPTED));
        when(meterReadingRepository.findAllByPermissionId("pid")).thenReturn(List.of(
                new MeterReading("pid", "uid", null, PollingStatus.DATA_READY),
                new MeterReading("pid", "other-uid", null, PollingStatus.DATA_NOT_READY)
        ));

        // When
        utilityEventService.receiveEvents(events);

        // Then
        verify(outbox, never()).commit(any());
    }

    @Test
    void testReceiveEvents_withHistoricalCollectionFinishedEvent_withoutMeterUid_emitsNothing() throws PermissionNotFoundException {
        // Given
        var events = List.of(getWebhookEvent("meter_historical_collection_finished_successful", null));
        when(repository.findByAuthUid("0000")).thenReturn(getPermissionRequest(PermissionProcessStatus.ACCEPTED));

        // When
        utilityEventService.receiveEvents(events);

        // Then
        verify(outbox, never()).commit(any());
    }

    @Test
    void testReceiveEvents_withHistoricalCollectionFinishedEvent_emitsStartPollingEvent_ifCollectionFinishedAndPermissionRequestAccepted() throws PermissionNotFoundException {
        // Given
        var events = List.of(getWebhookEvent("meter_historical_collection_finished_successful", "uid"));
        when(repository.findByAuthUid("0000")).thenReturn(getPermissionRequest(PermissionProcessStatus.ACCEPTED));
        when(meterReadingRepository.findAllByPermissionId("pid")).thenReturn(List.of(
                new MeterReading("pid", "uid", null, PollingStatus.DATA_READY)
        ));

        // When
        utilityEventService.receiveEvents(events);

        // Then
        verify(outbox).commit(isA(UsStartPollingEvent.class));
    }

    @Test
    void testReceiveEvents_withHistoricalCollectionFinishedEvent_emitsNothing_ifPermissionRequestNotAccepted() throws PermissionNotFoundException {
        // Given
        var events = List.of(getWebhookEvent("meter_historical_collection_finished_successful", "uid"));
        when(repository.findByAuthUid("0000")).thenReturn(getPermissionRequest(PermissionProcessStatus.FULFILLED));

        // When
        utilityEventService.receiveEvents(events);

        // Then
        verify(outbox, never()).commit(any());
    }

    private static WebhookEvent getWebhookEvent(String type, String meterUid) {
        return new WebhookEvent(
                "0000",
                type,
                ZonedDateTime.now(ZoneOffset.UTC),
                "webhook",
                URI.create("https://localhost:8080/region-connectors/us-green-button/webhook/"),
                false,
                "0000",
                meterUid
        );
    }

    private static GreenButtonPermissionRequest getPermissionRequest(PermissionProcessStatus status) {
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        var today = LocalDate.now(ZoneOffset.UTC);
        return new GreenButtonPermissionRequest(
                "pid",
                "cid",
                "dnid",
                today,
                today,
                Granularity.PT15M,
                status,
                now,
                "US",
                "company",
                "http://localhost",
                "scope",
                "0000"
        );
    }
}