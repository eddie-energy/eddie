package energy.eddie.regionconnector.fi.fingrid.permission.handlers;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.fi.fingrid.permission.events.UpdateGranularityEvent;
import energy.eddie.regionconnector.fi.fingrid.permission.request.FingridPermissionRequestBuilder;
import energy.eddie.regionconnector.fi.fingrid.persistence.FiPermissionRequestRepository;
import energy.eddie.regionconnector.fi.fingrid.services.PollingService;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.EventBusImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateGranularityEventHandlerTest {
    @Spy
    private final EventBus eventBus = new EventBusImpl();
    @Mock
    private PollingService pollingService;
    @Mock
    private FiPermissionRequestRepository repository;
    @InjectMocks
    @SuppressWarnings("unused")
    private UpdateGranularityEventHandler handler;

    @Test
    void updateGranularityEvent_triggersPolling() {
        // Given
        var pr = new FingridPermissionRequestBuilder().setPermissionId("pid")
                                                      .setConnectionId("cid")
                                                      .setDataNeedId("dnid")
                                                      .setStatus(PermissionProcessStatus.ACCEPTED)
                                                      .setCreated(ZonedDateTime.now(ZoneOffset.UTC))
                                                      .setStart(LocalDate.now(ZoneOffset.UTC))
                                                      .setEnd(LocalDate.now(ZoneOffset.UTC))
                                                      .setCustomerIdentification("cid")
                                                      .setGranularity(Granularity.P1D)
                                                      .setLastMeterReadings(null)
                                                      .build();
        when(repository.getByPermissionId("pid"))
                .thenReturn(pr);
        // When
        eventBus.emit(new UpdateGranularityEvent("pid", Granularity.P1Y));

        // Then
        verify(pollingService).pollTimeSeriesData(pr, Granularity.P1Y);
    }
}