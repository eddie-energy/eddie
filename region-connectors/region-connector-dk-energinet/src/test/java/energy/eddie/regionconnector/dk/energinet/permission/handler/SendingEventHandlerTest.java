package energy.eddie.regionconnector.dk.energinet.permission.handler;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.dk.energinet.customer.api.EnerginetCustomerApi;
import energy.eddie.regionconnector.dk.energinet.permission.events.DKValidatedEvent;
import energy.eddie.regionconnector.dk.energinet.permission.events.DkAcceptedEvent;
import energy.eddie.regionconnector.dk.energinet.permission.events.DkSimpleEvent;
import energy.eddie.regionconnector.dk.energinet.permission.request.EnerginetPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.persistence.DkPermissionRequestRepository;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.EventBusImpl;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SendingEventHandlerTest {
    @Spy
    private final EventBus eventBus = new EventBusImpl();
    @Mock
    private Outbox outbox;
    @Mock
    private EnerginetCustomerApi customerApi;
    @Mock
    private DkPermissionRequestRepository repository;
    @InjectMocks
    @SuppressWarnings("unused")
    private SendingEventHandler sendingEventHandler;
    @Captor
    private ArgumentCaptor<DkAcceptedEvent> acceptedCaptor;
    @Captor
    private ArgumentCaptor<PermissionEvent> eventCaptor;

    public static Stream<Arguments> testAccept_emitsErrorEvent() {
        return Stream.of(
                Arguments.of(HttpClientErrorException.create(HttpStatus.TOO_MANY_REQUESTS,
                                                             "too many requests",
                                                             null,
                                                             null,
                                                             StandardCharsets.UTF_8),
                             PermissionProcessStatus.UNABLE_TO_SEND),
                Arguments.of(HttpClientErrorException.create(HttpStatus.UNAUTHORIZED,
                                                             "unauthorized",
                                                             null,
                                                             null,
                                                             StandardCharsets.UTF_8),
                             PermissionProcessStatus.INVALID),
                Arguments.of(WebClientResponseException.create(HttpStatus.UNAUTHORIZED,
                                                               "unauthorized",
                                                               null,
                                                               null,
                                                               null,
                                                               null),
                             PermissionProcessStatus.INVALID),
                Arguments.of(new WebClientRequestException(new UnknownHostException(), HttpMethod.GET,
                                                           URI.create("http://example.com"), HttpHeaders.EMPTY),
                             PermissionProcessStatus.UNABLE_TO_SEND),
                Arguments.of(new WebClientRequestException(new RuntimeException(), HttpMethod.GET,
                                                           URI.create("http://example.com"), HttpHeaders.EMPTY),
                             PermissionProcessStatus.INVALID),
                Arguments.of(new Throwable(), PermissionProcessStatus.INVALID)
        );
    }

    @Test
    void testAccept_emitsAcceptedOnSuccessResponse() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        var pr = new EnerginetPermissionRequest("pid",
                                                "cid",
                                                "dnid",
                                                "mid",
                                                "refresh",
                                                now,
                                                now,
                                                Granularity.PT1H,
                                                null,
                                                PermissionProcessStatus.VALIDATED,
                                                ZonedDateTime.now(ZoneOffset.UTC));
        when(repository.getByPermissionId("pid"))
                .thenReturn(pr);
        when(customerApi.accessToken("refresh"))
                .thenReturn(Mono.just("access"));

        // When
        eventBus.emit(new DKValidatedEvent("pid", Granularity.PT1H, now, now));

        // Then
        verify(outbox).commit(isA(DkSimpleEvent.class));
        verify(outbox).commit(acceptedCaptor.capture());
        var res = acceptedCaptor.getValue();
        assertEquals("access", res.accessToken());
    }

    @ParameterizedTest
    @MethodSource
    void testAccept_emitsErrorEvent(Throwable throwable, PermissionProcessStatus status) {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        var pr = new EnerginetPermissionRequest("pid",
                                                "cid",
                                                "dnid",
                                                "mid",
                                                "refresh",
                                                now,
                                                now,
                                                Granularity.PT1H,
                                                null,
                                                PermissionProcessStatus.VALIDATED,
                                                ZonedDateTime.now(ZoneOffset.UTC));
        when(repository.getByPermissionId("pid"))
                .thenReturn(pr);
        when(customerApi.accessToken("refresh"))
                .thenReturn(Mono.error(throwable));

        // When
        eventBus.emit(new DKValidatedEvent("pid", Granularity.PT1H, now, now));

        // Then
        verify(outbox, atMost(2)).commit(eventCaptor.capture());
        var event = eventCaptor.getValue();
        assertEquals(status, event.status());
    }
}