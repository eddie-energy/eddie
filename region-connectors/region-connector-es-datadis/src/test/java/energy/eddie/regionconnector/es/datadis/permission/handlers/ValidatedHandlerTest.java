package energy.eddie.regionconnector.es.datadis.permission.handlers;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.es.datadis.api.AuthorizationApi;
import energy.eddie.regionconnector.es.datadis.api.DatadisApiException;
import energy.eddie.regionconnector.es.datadis.dtos.AuthorizationRequestFactory;
import energy.eddie.regionconnector.es.datadis.dtos.AuthorizationRequestResponse;
import energy.eddie.regionconnector.es.datadis.permission.events.EsValidatedEvent;
import energy.eddie.regionconnector.es.datadis.permission.request.DatadisPermissionRequest;
import energy.eddie.regionconnector.es.datadis.persistence.EsPermissionRequestRepository;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.EventBusImpl;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unused")
class ValidatedHandlerTest {
    @Mock
    private Outbox outbox;
    @Mock
    private AuthorizationApi authorizationApi;
    @Mock
    private EsPermissionRequestRepository repository;
    @Spy
    private AuthorizationRequestFactory authorizationRequestFactory = new AuthorizationRequestFactory();
    @Spy
    private EventBus eventBus = new EventBusImpl();
    @InjectMocks
    private ValidatedHandler validatedHandler;

    @Test
    void testAccept_withUnknownPermissionRequest_doesNothing() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        when(repository.findByPermissionId("pid")).thenReturn(Optional.empty());

        // When
        eventBus.emit(new EsValidatedEvent("pid", now, now, Granularity.PT1H));

        // Then
        verifyNoInteractions(outbox);
    }

    @Test
    void testAccept_withSuccessResponse_emitsSentToPA() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        var pr = new DatadisPermissionRequest(
                "pid",
                "cid",
                "dnid",
                Granularity.PT1H,
                "NIF",
                "mid",
                now,
                now,
                null,
                null,
                null,
                PermissionProcessStatus.VALIDATED,
                null,
                false,
                ZonedDateTime.now(ZoneOffset.UTC)
        );
        when(repository.findByPermissionId("pid")).thenReturn(Optional.of(pr));
        when(authorizationApi.postAuthorizationRequest(any()))
                .thenReturn(Mono.just(AuthorizationRequestResponse.fromResponse("ok")));

        // When
        eventBus.emit(new EsValidatedEvent("pid", now, now, Granularity.PT1H));

        // Then
        verify(outbox).commit(assertArg(event -> assertEquals(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR,
                                                              event.status())));
    }

    @Test
    void testAccept_withErrorResponse_emitsUnableToSend() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        var pr = new DatadisPermissionRequest(
                "pid",
                "cid",
                "dnid",
                Granularity.PT1H,
                "NIF",
                "mid",
                now,
                now,
                null,
                null,
                null,
                PermissionProcessStatus.VALIDATED,
                null,
                false,
                ZonedDateTime.now(ZoneOffset.UTC)
        );
        when(repository.findByPermissionId("pid")).thenReturn(Optional.of(pr));
        when(authorizationApi.postAuthorizationRequest(any()))
                .thenReturn(Mono.error(new DatadisApiException("error", HttpResponseStatus.BAD_REQUEST, "blb")));

        // When
        eventBus.emit(new EsValidatedEvent("pid", now, now, Granularity.PT1H));

        // Then
        verify(outbox).commit(assertArg(event -> assertEquals(PermissionProcessStatus.UNABLE_TO_SEND, event.status())));
    }
}