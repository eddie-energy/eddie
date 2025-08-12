package energy.eddie.regionconnector.us.green.button.permission.handlers;

import energy.eddie.api.agnostic.data.needs.*;
import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.EventBusImpl;
import energy.eddie.regionconnector.us.green.button.GreenButtonPermissionRequestBuilder;
import energy.eddie.regionconnector.us.green.button.permission.events.UsMeterReadingUpdateEvent;
import energy.eddie.regionconnector.us.green.button.permission.events.UsSimpleEvent;
import energy.eddie.regionconnector.us.green.button.permission.events.UsStartPollingEvent;
import energy.eddie.regionconnector.us.green.button.persistence.UsPermissionRequestRepository;
import energy.eddie.regionconnector.us.green.button.services.PollingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StartPollingEventHandlerTest {
    @Spy
    private final EventBus eventBus = new EventBusImpl();
    @Mock
    private PollingService pollingService;
    @Mock
    private DataNeedCalculationService<DataNeed> calculationService;
    @Mock
    private UsPermissionRequestRepository repository;
    @InjectMocks
    @SuppressWarnings("unused")
    private StartPollingEventHandler startPollingEventHandler;

    public static Stream<Arguments> testOtherEvents_doNotTriggerPolling() {
        return Stream.of(
                Arguments.of(new UsMeterReadingUpdateEvent("pid", List.of())),
                Arguments.of(new UsSimpleEvent("pid", PermissionProcessStatus.ACCEPTED))
        );
    }

    @Test
    void startPollingEvent_forValidatedHistoricalData_triggersPolling() {
        // Given
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        var today = now.toLocalDate();
        when(repository.getByPermissionId("pid"))
                .thenReturn(new GreenButtonPermissionRequestBuilder()
                                    .setPermissionId("pid")
                                    .setDataNeedId("dnid")
                                    .setCreated(now)
                                    .build());
        var timeframe = new Timeframe(today, today);
        when(calculationService.calculate("dnid", now))
                .thenReturn(new ValidatedHistoricalDataDataNeedResult(List.of(), timeframe, timeframe));

        // When
        eventBus.emit(new UsStartPollingEvent("pid"));

        // Then
        verify(pollingService).pollValidatedHistoricalData("pid");
    }

    @Test
    void startPollingEvent_forAccountingPointData_triggersPolling() {
        // Given
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        var today = now.toLocalDate();
        var pr = new GreenButtonPermissionRequestBuilder()
                .setPermissionId("pid")
                .setDataNeedId("dnid")
                .setCreated(now)
                .build();
        when(repository.getByPermissionId("pid"))
                .thenReturn(pr);
        var timeframe = new Timeframe(today, today);
        when(calculationService.calculate("dnid", now))
                .thenReturn(new AccountingPointDataNeedResult(timeframe));

        // When
        eventBus.emit(new UsStartPollingEvent("pid"));

        // Then
        verify(pollingService).pollAccountingPointData(pr);
    }

    @Test
    void startPollingEvent_forInvalidDataNeed_doesNotTriggerPolling() {
        // Given
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        var pr = new GreenButtonPermissionRequestBuilder()
                .setPermissionId("pid")
                .setDataNeedId("dnid")
                .setCreated(now)
                .build();
        when(repository.getByPermissionId("pid"))
                .thenReturn(pr);
        when(calculationService.calculate("dnid", now))
                .thenReturn(new DataNeedNotFoundResult());

        // When
        eventBus.emit(new UsStartPollingEvent("pid"));

        // Then
        verify(pollingService, never()).pollAccountingPointData(any());
    }

    @ParameterizedTest
    @MethodSource
    void testOtherEvents_doNotTriggerPolling(PermissionEvent event) {
        // Given
        // When
        eventBus.emit(event);

        // Then
        verify(pollingService, never()).pollValidatedHistoricalData(any());
    }
}