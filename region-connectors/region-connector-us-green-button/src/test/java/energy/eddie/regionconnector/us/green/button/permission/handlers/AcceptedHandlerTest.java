package energy.eddie.regionconnector.us.green.button.permission.handlers;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.EventBusImpl;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.us.green.button.api.GreenButtonApi;
import energy.eddie.regionconnector.us.green.button.api.Pages;
import energy.eddie.regionconnector.us.green.button.client.dtos.MeterListing;
import energy.eddie.regionconnector.us.green.button.client.dtos.meter.*;
import energy.eddie.regionconnector.us.green.button.config.GreenButtonConfiguration;
import energy.eddie.regionconnector.us.green.button.permission.events.UsAcceptedEvent;
import energy.eddie.regionconnector.us.green.button.permission.events.UsMeterReadingUpdateEvent;
import energy.eddie.regionconnector.us.green.button.persistence.MeterReadingRepository;
import energy.eddie.regionconnector.us.green.button.services.DataNeedMatcher;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

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
            2
    );
    @Spy
    private final EventBus eventBus = new EventBusImpl();
    @Mock
    private GreenButtonApi api;
    @Mock
    private DataNeedMatcher dataNeedMatcher;
    @Mock
    private Outbox outbox;
    @Mock
    private MeterReadingRepository meterReadingRepository;
    @InjectMocks
    @SuppressWarnings("unused")
    private AcceptedHandler handler;
    @Captor
    private ArgumentCaptor<UsMeterReadingUpdateEvent> meterReadingEventCaptor;

    @Test
    void testAccept_addsActivatedMeters_toPermissionRequest() {
        // Given
        var meter1 = createMeter("uid1", "1111");
        meter1.setMeterBlock("basic", createMeterBlock());
        var data1 = new MeterListing(List.of(meter1), null);
        var meter2 = createMeter("uid2", "1111");
        meter2.setMeterBlock("basic", createMeterBlock());
        var data2 = new MeterListing(List.of(meter2), null);
        var meter3 = createMeter("uid3", "2222");
        meter3.setMeterBlock("basic", createMeterBlock());
        var data3 = new MeterListing(List.of(meter3), null);
        when(api.fetchInactiveMeters(Pages.NO_SLURP, List.of("1111", "2222")))
                .thenReturn(Flux.just(data1, data2, data3));
        when(api.collectHistoricalData(List.of("uid1", "uid2", "uid3")))
                .thenReturn(Mono.just(new HistoricalCollectionResponse(true, List.of("uid1", "uid3"))));
        when(dataNeedMatcher.filterMetersNotMeetingDataNeedCriteria(data1))
                .thenReturn(List.of(meter1, meter2, meter3));
        when(meterReadingRepository.saveAll(any())).thenAnswer(i -> i.getArguments()[0]);

        // When
        eventBus.emit(new UsAcceptedEvent("pid1", "1111"));
        eventBus.emit(new UsAcceptedEvent("pid2", "2222"));

        // Then
        verify(outbox, times(2)).commit(meterReadingEventCaptor.capture());
        var res1 = meterReadingEventCaptor.getValue();
        assertEquals(Set.of("uid3"), res1.allowedMeters());
        var res2 = meterReadingEventCaptor.getAllValues().getFirst();
        assertEquals(Set.of("uid1"), res2.allowedMeters());
    }

    @Test
    void testAccept_emitsUnfulfillable_onPermissionRequestWithoutMeters() {
        // Given
        var meter1 = createMeter("uid1", "1111");
        meter1.setMeterBlock("basic", createMeterBlock());
        var data1 = new MeterListing(List.of(meter1, meter1), null);
        var meter2 = createMeter("uid2", "2222");
        meter2.setMeterBlock("basic", createMeterBlock());
        var data2 = new MeterListing(List.of(meter2), null);
        when(api.fetchInactiveMeters(Pages.NO_SLURP, List.of("1111", "2222")))
                .thenReturn(Flux.just(data1, data2));
        when(api.collectHistoricalData(List.of("uid1", "uid1")))
                .thenReturn(Mono.just(new HistoricalCollectionResponse(true, List.of())));
        when(dataNeedMatcher.filterMetersNotMeetingDataNeedCriteria(data1))
                .thenReturn(List.of(meter1, meter1));

        // When
        eventBus.emit(new UsAcceptedEvent("pid1", "1111"));
        eventBus.emit(new UsAcceptedEvent("pid2", "2222"));

        // Then
        verify(outbox, times(2))
                .commit(assertArg(event -> assertEquals(PermissionProcessStatus.UNFULFILLABLE, event.status())));
    }

    private static Meter createMeter(String uid, String authUid) {
        return new Meter(
                uid,
                authUid,
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
                List.of()
        );
    }

    private static @NotNull MeterBlock createMeterBlock() {
        return new MeterBlock("", "", "electric", "", List.of(), "", "", "");
    }
}