package energy.eddie.regionconnector.us.green.button.permission.handlers;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.EventBusImpl;
import energy.eddie.regionconnector.us.green.button.api.GreenButtonApi;
import energy.eddie.regionconnector.us.green.button.api.Pages;
import energy.eddie.regionconnector.us.green.button.client.dtos.*;
import energy.eddie.regionconnector.us.green.button.config.GreenButtonConfiguration;
import energy.eddie.regionconnector.us.green.button.oauth.persistence.OAuthTokenRepository;
import energy.eddie.regionconnector.us.green.button.permission.events.UsSimpleEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AcceptedHandlerTest {
    @SuppressWarnings("unused")
    @Spy
    private final GreenButtonConfiguration config = new GreenButtonConfiguration(
            "token",
            "http://localhost",
            Map.of(),
            Map.of(),
            "http://localhost",
            1
    );
    @Spy
    private final EventBus eventBus = new EventBusImpl();
    @Mock
    private GreenButtonApi api;
    @Mock
    private OAuthTokenRepository repository;
    @InjectMocks
    @SuppressWarnings("unused")
    private AcceptedHandler handler;

    @Test
    void testAccept_activatesHistoricalCollection() {
        // Given
        when(repository.findAllByPermissionIdIn(List.of("pid")))
                .thenReturn(List.of(new SimplePermissionAuthId("pid", "1111")));
        var meter = new Meter(
                "uid",
                "1111",
                ZonedDateTime.now(ZoneOffset.UTC),
                "mail@mail.com",
                "userId",
                false,
                false,
                false,
                List.of(),
                "status",
                "",
                ZonedDateTime.now(ZoneOffset.UTC),
                new OngoingMonitoring("", null, null, null, null),
                "DEMO-UTILITY",
                0,
                List.of(),
                List.of(),
                0,
                List.of(),
                List.of(),
                new Exports(null, null, null, null, null),
                List.of(),
                List.of(),
                Map.of()
        );
        when(api.fetchInactiveMeters(Pages.NO_SLURP, List.of("1111")))
                .thenReturn(Flux.just(new MeterListing(List.of(meter), null)));
        when(api.collectHistoricalData(List.of("uid")))
                .thenReturn(Mono.just(new HistoricalCollectionResponse(true, List.of("uid"))));

        // When
        eventBus.emit(new UsSimpleEvent("pid", PermissionProcessStatus.ACCEPTED));

        // Then
        verify(api).collectHistoricalData(List.of("uid"));
    }

    private record SimplePermissionAuthId(String permissionId,
                                          String authUid) implements OAuthTokenRepository.PermissionAuthId {
        @Override
        public String getPermissionId() {
            return permissionId;
        }

        @Override
        public String getAuthUid() {
            return authUid;
        }
    }
}