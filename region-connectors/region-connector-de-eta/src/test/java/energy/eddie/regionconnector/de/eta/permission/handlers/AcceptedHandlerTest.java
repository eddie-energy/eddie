package energy.eddie.regionconnector.de.eta.permission.handlers;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequest;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequestBuilder;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequestRepository;
import energy.eddie.regionconnector.de.eta.permission.request.events.AcceptedEvent;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import static org.assertj.core.api.Assertions.assertThat;
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
import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AcceptedHandlerTest {

    @Spy
    private final EventBus eventBus = new EventBusImpl();

    @Mock
    private DePermissionRequestRepository repository;


    @InjectMocks
    private AcceptedHandler handler;

    @Test
    void initialization_isSuccessful() {
        assertThat(handler).isNotNull();
    }

    @Test
    void acceptedEvent_processesRequest_loggingOnly() {
        DePermissionRequest pr = new DePermissionRequestBuilder()
                .permissionId("pid")
                .dataNeedId("dnid")
                .status(PermissionProcessStatus.ACCEPTED)
                .start(LocalDate.now(ZoneOffset.UTC).minusDays(10))
                .end(LocalDate.now(ZoneOffset.UTC))
                .granularity(Granularity.PT15M)
                .energyType(EnergyType.ELECTRICITY)
                .created(ZonedDateTime.now(ZoneOffset.UTC))
                .build();

        when(repository.findByPermissionId("pid")).thenReturn(Optional.of(pr));

        eventBus.emit(new AcceptedEvent("pid"));

        verify(repository).findByPermissionId("pid");
    }

    @Test
    void acceptedEvent_withFutureStartDate_processesWithoutError() {
        DePermissionRequest pr = new DePermissionRequestBuilder()
                .permissionId("pid_future")
                .status(PermissionProcessStatus.ACCEPTED)
                .start(LocalDate.now(ZoneOffset.UTC).plusDays(5)) // Future start
                .end(LocalDate.now(ZoneOffset.UTC).plusDays(10))
                .created(ZonedDateTime.now(ZoneOffset.UTC))
                .build();

        when(repository.findByPermissionId("pid_future")).thenReturn(Optional.of(pr));

        eventBus.emit(new AcceptedEvent("pid_future"));

        verify(repository).findByPermissionId("pid_future");
    }

    @Test
    void acceptedEvent_withMissingPermissionRequest_doesNotCrash() {
        when(repository.findByPermissionId("pid_missing")).thenReturn(Optional.empty());

        eventBus.emit(new AcceptedEvent("pid_missing"));

        verify(repository).findByPermissionId("pid_missing");
    }
}