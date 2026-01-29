// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.us.green.button.services;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.us.green.button.GreenButtonPermissionRequestBuilder;
import energy.eddie.regionconnector.us.green.button.dtos.WebhookEvent;
import energy.eddie.regionconnector.us.green.button.permission.events.UsAuthorizationUpdateFinishedEvent;
import energy.eddie.regionconnector.us.green.button.permission.events.UsUnfulfillableEvent;
import energy.eddie.regionconnector.us.green.button.permission.request.GreenButtonPermissionRequest;
import energy.eddie.regionconnector.us.green.button.persistence.UsPermissionRequestRepository;
import energy.eddie.regionconnector.us.green.button.services.utility.events.MeterEventCallbacks;
import energy.eddie.regionconnector.us.green.button.services.utility.events.UtilityEventService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
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
    private MeterEventCallbacks callbacks;
    @InjectMocks
    private UtilityEventService utilityEventService;
    @Captor
    private ArgumentCaptor<UsUnfulfillableEvent> unfulfillableEvent;

    @Test
    void testReceiveEvents_withAuthorizationExpiredWebhookEvent_emitsUnfulfillable() {
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
    void testReceiveEvents_withAuthorizationExpiredWebhookEvent_emitsNothingIfPermissionRequestAlreadyTerminated() {
        // Given
        var events = List.of(getWebhookEvent("authorization_expired", null));
        when(repository.findByAuthUid("0000")).thenReturn(getPermissionRequest(PermissionProcessStatus.EXTERNALLY_TERMINATED));

        // When
        utilityEventService.receiveEvents(events);

        // Then
        verify(outbox, never()).commit(any());
    }

    @Test
    void testReceiveEvents_withAuthorizationRevokedWebhookEvent_emitsRevoked() {
        // Given
        var events = List.of(getWebhookEvent("authorization_revoked", null));
        when(repository.findByAuthUid("0000")).thenReturn(getPermissionRequest(PermissionProcessStatus.ACCEPTED));

        // When
        utilityEventService.receiveEvents(events);

        // Then
        verify(outbox).commit(assertArg(res -> assertEquals(PermissionProcessStatus.REVOKED, res.status())));
    }

    @Test
    void testReceiveEvents_emitsNothing_forUnknownEventType() {
        // Given
        var events = List.of(getWebhookEvent("unknown_event_type", null));
        when(repository.findByAuthUid("0000")).thenReturn(getPermissionRequest(PermissionProcessStatus.EXTERNALLY_TERMINATED));

        // When
        utilityEventService.receiveEvents(events);

        // Then
        verify(outbox, never()).commit(any());
    }

    @Test
    void testReceiveEvents_emitsNothing_forUnknownPermissionRequest() {
        // Given
        var events = List.of(getWebhookEvent("unknown_event_type", null));
        when(repository.findByAuthUid("0000")).thenReturn(null);

        // When
        utilityEventService.receiveEvents(events);

        // Then
        verify(outbox, never()).commit(any());
        verify(callbacks, never()).onMeterCreatedEvent(any(), any());
        verify(callbacks, never()).onHistoricalCollectionFinishedEvent(any(), any());
    }

    @Test
    void testReceiveEvents_emitsNothing_forMissingAuthUid() {
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
    void testReceiveEvents_withHistoricalCollectionFinishedEvent_usesCallback() {
        // Given
        var event = getWebhookEvent("meter_historical_collection_finished_successful", "uid");
        var events = List.of(event);
        var permissionRequest = getPermissionRequest(PermissionProcessStatus.ACCEPTED);
        when(repository.findByAuthUid("0000")).thenReturn(permissionRequest);

        // When
        utilityEventService.receiveEvents(events);

        // Then
        verify(callbacks).onHistoricalCollectionFinishedEvent(event, permissionRequest);
    }


    @Test
    void testReceiveEvents_withMeterCreated_usesCallback() {
        // Given
        var event = getWebhookEvent("meter_created", "uid");
        var events = List.of(event);
        var permissionRequest = getPermissionRequest(PermissionProcessStatus.ACCEPTED);
        when(repository.findByAuthUid("0000")).thenReturn(permissionRequest);

        // When
        utilityEventService.receiveEvents(events);

        // Then
        verify(callbacks).onMeterCreatedEvent(event, permissionRequest);
    }

    @Test
    void testReceiveEvents_withAuthorizationUpdateFinishedSuccessful_emitsAuthorizationFinishedEvent() {
        // Given
        var events = List.of(getWebhookEvent("authorization_update_finished_successful", "uid"));
        when(repository.findByAuthUid("0000")).thenReturn(getPermissionRequest(PermissionProcessStatus.ACCEPTED));

        // When
        utilityEventService.receiveEvents(events);

        // Then
        verify(outbox).commit(isA(UsAuthorizationUpdateFinishedEvent.class));
    }

    @Test
    void testReceiveEvents_withAuthorizationUpdateFinishedSuccessful_forFulfilledPermissionRequest_emitsNothing() {
        // Given
        var events = List.of(getWebhookEvent("authorization_update_finished_successful", "uid"));
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
        return new GreenButtonPermissionRequestBuilder().setPermissionId("pid")
                                                        .setStatus(status)
                                                        .build();
    }
}