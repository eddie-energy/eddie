package energy.eddie.regionconnector.at.eda.handlers.integration.outbound;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.at.api.AtPermissionRequestRepository;
import energy.eddie.regionconnector.at.eda.permission.request.EdaPermissionRequest;
import energy.eddie.regionconnector.at.eda.permission.request.events.SimpleEvent;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.EventBusImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConnectionStatusMessageHandlerTest {
    @Mock
    private AtPermissionRequestRepository repository;

    @Test
    void testAccept_emitsStatusMessage() {
        // Given
        Sinks.Many<ConnectionStatusMessage> messages = Sinks.many().multicast().onBackpressureBuffer();
        var start = ZonedDateTime.now(ZoneOffset.UTC);
        var end = start.plusDays(10);
        EdaPermissionRequest permissionRequest = new EdaPermissionRequest(
                "connectionId", "pid", "dnid", "cmRequestId", "conversationId", "mid", "dsoId", start, end,
                Granularity.PT15M, PermissionProcessStatus.VALIDATED, "", null,
                ZonedDateTime.now(ZoneOffset.UTC)
        );
        when(repository.findByPermissionId("pid")).thenReturn(Optional.of(permissionRequest));
        EventBusImpl eventBus = new EventBusImpl();
        new ConnectionStatusMessageHandler(eventBus, messages, repository);

        // When
        eventBus.emit(new SimpleEvent("pid", PermissionProcessStatus.VALIDATED));

        // Then
        StepVerifier.create(messages.asFlux())
                .then(messages::tryEmitComplete)
                .assertNext(csm -> assertAll(
                        () -> assertEquals(permissionRequest.connectionId(), csm.connectionId()),
                        () -> assertEquals(permissionRequest.permissionId(), csm.permissionId()),
                        () -> assertEquals(permissionRequest.dataNeedId(), csm.dataNeedId()),
                        () -> assertEquals(permissionRequest.dataSourceInformation(), csm.dataSourceInformation()),
                        () -> assertEquals(permissionRequest.status(), csm.status()),
                        () -> assertEquals(permissionRequest.message(), csm.message())
                ))
                .verifyComplete();
    }

    @Test
    void testAccept_doesNotEmitStatus_ifNoPermissionIsFound() {
        // Given
        Sinks.Many<ConnectionStatusMessage> messages = Sinks.many().multicast().onBackpressureBuffer();
        when(repository.findByPermissionId("pid")).thenReturn(Optional.empty());
        EventBus eventBus = new EventBusImpl();
        new ConnectionStatusMessageHandler(eventBus, messages, repository);

        // When
        eventBus.emit(new SimpleEvent("pid", PermissionProcessStatus.VALIDATED));

        // Then
        StepVerifier.create(messages.asFlux())
                .then(messages::tryEmitComplete)
                .verifyComplete();
    }
}