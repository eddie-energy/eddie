package energy.eddie.regionconnector.de.eta.permission.handlers;

import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequest;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequestBuilder;
import energy.eddie.regionconnector.de.eta.permission.request.events.LatestMeterReadingEvent;
import energy.eddie.regionconnector.de.eta.permission.request.events.SimpleEvent;
import energy.eddie.regionconnector.de.eta.persistence.DePermissionRequestRepository;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LatestMeterReadingEventHandlerTest {

    @Mock
    private EventBus eventBus;

    @Mock
    private DePermissionRequestRepository repository;

    @Mock
    private Outbox outbox;

    private LatestMeterReadingEventHandler handler;

    @BeforeEach
    void setUp() {
        when(eventBus.filteredFlux(LatestMeterReadingEvent.class)).thenReturn(Flux.empty());
        handler = new LatestMeterReadingEventHandler(eventBus, outbox, repository);
    }

    @Test
    @DisplayName("Should emit FULFILLED event when latest reading equals end date")
    void shouldEmitFulfilledEventWhenLatestReadingEqualsEndDate() {
        LocalDate endDate = LocalDate.of(2024, 12, 31);
        String permissionId = "perm-fulfilled-exact";

        DePermissionRequest permissionRequest = new DePermissionRequestBuilder()
                .permissionId(permissionId)
                .end(endDate)
                .build();

        when(repository.findByPermissionId(permissionId)).thenReturn(Optional.of(permissionRequest));

        handler.accept(new LatestMeterReadingEvent(permissionId, endDate));

        ArgumentCaptor<PermissionEvent> eventCaptor = ArgumentCaptor.forClass(PermissionEvent.class);
        verify(outbox).commit(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(SimpleEvent.class);
        assertThat(eventCaptor.getValue().status()).isEqualTo(PermissionProcessStatus.FULFILLED);
        assertThat(eventCaptor.getValue().permissionId()).isEqualTo(permissionId);
    }

    @Test
    @DisplayName("Should emit FULFILLED event when latest reading is after end date")
    void shouldEmitFulfilledEventWhenLatestReadingIsAfterEndDate() {
        LocalDate endDate = LocalDate.of(2024, 12, 31);
        LocalDate latestReading = endDate.plusDays(1);
        String permissionId = "perm-fulfilled-after";

        DePermissionRequest permissionRequest = new DePermissionRequestBuilder()
                .permissionId(permissionId)
                .end(endDate)
                .build();

        when(repository.findByPermissionId(permissionId)).thenReturn(Optional.of(permissionRequest));

        handler.accept(new LatestMeterReadingEvent(permissionId, latestReading));

        ArgumentCaptor<PermissionEvent> eventCaptor = ArgumentCaptor.forClass(PermissionEvent.class);
        verify(outbox).commit(eventCaptor.capture());
        assertThat(eventCaptor.getValue().status()).isEqualTo(PermissionProcessStatus.FULFILLED);
        assertThat(eventCaptor.getValue().permissionId()).isEqualTo(permissionId);
    }

    @Test
    @DisplayName("Should not emit any event when latest reading is before end date")
    void shouldNotEmitEventWhenLatestReadingIsBeforeEndDate() {
        LocalDate endDate = LocalDate.of(2024, 12, 31);
        LocalDate latestReading = endDate.minusDays(1);
        String permissionId = "perm-not-yet-fulfilled";

        DePermissionRequest permissionRequest = new DePermissionRequestBuilder()
                .permissionId(permissionId)
                .end(endDate)
                .build();

        when(repository.findByPermissionId(permissionId)).thenReturn(Optional.of(permissionRequest));

        handler.accept(new LatestMeterReadingEvent(permissionId, latestReading));

        verifyNoInteractions(outbox);
    }

    @Test
    @SuppressWarnings("NullAway")
    @DisplayName("Should not emit any event when latest reading is null")
    void shouldNotEmitEventWhenLatestReadingIsNull() {
        LocalDate endDate = LocalDate.of(2024, 12, 31);
        String permissionId = "perm-null-reading";

        DePermissionRequest permissionRequest = new DePermissionRequestBuilder()
                .permissionId(permissionId)
                .end(endDate)
                .build();

        when(repository.findByPermissionId(permissionId)).thenReturn(Optional.of(permissionRequest));

        handler.accept(new LatestMeterReadingEvent(permissionId, null));

        verifyNoInteractions(outbox);
    }

    @Test
    @DisplayName("Should not emit any event when permission request is not found")
    void shouldNotEmitEventWhenPermissionRequestNotFound() {
        String unknownPermissionId = "unknown-id";
        when(repository.findByPermissionId(unknownPermissionId)).thenReturn(Optional.empty());

        handler.accept(new LatestMeterReadingEvent(unknownPermissionId, LocalDate.of(2024, 12, 31)));

        verifyNoInteractions(outbox);
    }
}
