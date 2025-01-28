package energy.eddie.regionconnector.us.green.button.permission.handlers;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.EventBusImpl;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.us.green.button.GreenButtonPermissionRequestBuilder;
import energy.eddie.regionconnector.us.green.button.api.GreenButtonApi;
import energy.eddie.regionconnector.us.green.button.client.dtos.authorization.Authorization;
import energy.eddie.regionconnector.us.green.button.client.dtos.authorization.Scope;
import energy.eddie.regionconnector.us.green.button.client.dtos.meter.Exports;
import energy.eddie.regionconnector.us.green.button.permission.events.UsSimpleEvent;
import energy.eddie.regionconnector.us.green.button.persistence.UsPermissionRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequiresExternalTerminationHandlerTest {
    @Spy
    private final EventBus eventBus = new EventBusImpl();
    @Mock
    private Outbox outbox;
    @Mock
    private UsPermissionRequestRepository repository;
    @Mock
    private GreenButtonApi api;
    @InjectMocks
    @SuppressWarnings("unused")
    private RequiresExternalTerminationHandler handler;


    @Test
    void testHandler_terminatesExternally_onSuccessfulAPICall() {
        // Given
        when(repository.getByPermissionId("pid"))
                .thenReturn(new GreenButtonPermissionRequestBuilder()
                                    .setPermissionId("pid")
                                    .setCompanyId("company")
                                    .setAuthUid("1111")
                                    .build());
        when(api.revoke("1111", "company"))
                .thenReturn(Mono.just(createAuthorization()));

        // When
        eventBus.emit(new UsSimpleEvent("pid", PermissionProcessStatus.REQUIRES_EXTERNAL_TERMINATION));

        // Then
        verify(outbox).commit(assertArg(event -> assertAll(
                () -> assertEquals("pid", event.permissionId()),
                () -> assertEquals(PermissionProcessStatus.EXTERNALLY_TERMINATED, event.status())
        )));
    }

    @Test
    void testHandler_emitsTerminationFailed_onError() {
        // Given
        when(repository.getByPermissionId("pid"))
                .thenReturn(new GreenButtonPermissionRequestBuilder().setPermissionId("pid")
                                                                     .setCompanyId("company")
                                                                     .setAuthUid("1111")
                                                                     .build());
        when(api.revoke("1111", "company"))
                .thenReturn(Mono.error(WebClientResponseException.create(402, "", null, null, null)));

        // When
        eventBus.emit(new UsSimpleEvent("pid", PermissionProcessStatus.REQUIRES_EXTERNAL_TERMINATION));

        // Then
        verify(outbox).commit(assertArg(event -> assertAll(
                () -> assertEquals("pid", event.permissionId()),
                () -> assertEquals(PermissionProcessStatus.FAILED_TO_TERMINATE, event.status())
        )));
    }

    @Test
    void testHandler_emitsExternallyTerminated_ifPermissionCannotBeFound() {
        // Given
        when(repository.getByPermissionId("pid"))
                .thenReturn(new GreenButtonPermissionRequestBuilder().setPermissionId("pid")
                                                                     .setCompanyId("company")
                                                                     .setAuthUid("1111")
                                                                     .build());
        when(api.revoke("1111", "company"))
                .thenReturn(Mono.error(WebClientResponseException.create(404, "", null, null, null)));

        // When
        eventBus.emit(new UsSimpleEvent("pid", PermissionProcessStatus.REQUIRES_EXTERNAL_TERMINATION));

        // Then
        verify(outbox).commit(assertArg(event -> assertAll(
                () -> assertEquals("pid", event.permissionId()),
                () -> assertEquals(PermissionProcessStatus.EXTERNALLY_TERMINATED, event.status())
        )));
    }

    private static Authorization createAuthorization() {
        return new Authorization(
                "uid",
                ZonedDateTime.now(ZoneOffset.UTC),
                "e@mail",
                null,
                null,
                false,
                "P1D",
                false,
                new Exports(null, null, null, null, null),
                null,
                null,
                null,
                null,
                false,
                false,
                List.of(),
                null,
                ZonedDateTime.now(ZoneOffset.UTC),
                true,
                new Scope("P1D", null),
                "revoked",
                "",
                ZonedDateTime.now(ZoneOffset.UTC),
                "e@mail",
                "uid",
                "",
                "DEMO-UTILITY"
        );
    }
}