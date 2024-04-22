package energy.eddie.regionconnector.at.eda.handlers;

import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.at.api.AtPermissionRequestRepository;
import energy.eddie.regionconnector.at.eda.EdaAdapter;
import energy.eddie.regionconnector.at.eda.TransmissionException;
import energy.eddie.regionconnector.at.eda.config.PlainAtConfiguration;
import energy.eddie.regionconnector.at.eda.permission.request.EdaPermissionRequest;
import energy.eddie.regionconnector.at.eda.permission.request.events.SimpleEvent;
import energy.eddie.regionconnector.at.eda.requests.restricted.enums.AllowedGranularity;
import energy.eddie.regionconnector.shared.event.sourcing.EventBusImpl;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SendingEventHandlerTest {
    @Mock
    private EdaAdapter edaAdapter;
    @Mock
    private AtPermissionRequestRepository repository;
    @Mock
    private Outbox outbox;
    @Captor
    private ArgumentCaptor<PermissionEvent> captor;

    @Test
    void testAcceptSendsCmRequest_whenPontonAvailable() throws TransmissionException {
        // Given
        var start = LocalDate.now(ZoneOffset.UTC);
        var end = start.plusDays(10);
        var permissionRequest = new EdaPermissionRequest(
                "connectionId", "pid", "dnid", "cmRequestId", "conversationId", "mid", "dsoId", start, end,
                AllowedGranularity.PT15M, PermissionProcessStatus.VALIDATED, "", null,
                ZonedDateTime.now(ZoneOffset.UTC)
        );
        when(repository.findByPermissionId("pid"))
                .thenReturn(Optional.of(permissionRequest));
        var configuration = new PlainAtConfiguration("ep", null);
        var handler = new SendingEventHandler(new EventBusImpl(), edaAdapter, configuration, repository, outbox);
        var event = new SimpleEvent("pid", PermissionProcessStatus.VALIDATED);

        // When
        handler.accept(event);

        // Then
        verify(edaAdapter).sendCMRequest(any());
    }

    @Test
    void testAcceptCommitsUnableToSend_whenPontonUnavailable() throws TransmissionException {
        // Given
        var start = LocalDate.now(ZoneOffset.UTC);
        var end = start.plusDays(10);
        var permissionRequest = new EdaPermissionRequest(
                "connectionId", "pid", "dnid", "cmRequestId", "conversationId", "mid", "dsoId", start, end,
                AllowedGranularity.PT15M, PermissionProcessStatus.VALIDATED, "", null,
                ZonedDateTime.now(ZoneOffset.UTC)
        );
        when(repository.findByPermissionId("pid"))
                .thenReturn(Optional.of(permissionRequest));
        doThrow(new TransmissionException(null))
                .when(edaAdapter).sendCMRequest(any());

        var configuration = new PlainAtConfiguration("ep", null);
        var handler = new SendingEventHandler(new EventBusImpl(), edaAdapter, configuration, repository, outbox);
        var event = new SimpleEvent("pid", PermissionProcessStatus.VALIDATED);

        // When
        handler.accept(event);

        // Then
        verify(outbox).commit(captor.capture());
        assertThat(captor.getValue().status())
                .isEqualTo(PermissionProcessStatus.UNABLE_TO_SEND);
    }

    @Test
    void testAcceptDoesNotCommit_unknownPermissionRequest() {
        // Given
        when(repository.findByPermissionId("pid"))
                .thenReturn(Optional.empty());

        var configuration = new PlainAtConfiguration("ep", null);
        var handler = new SendingEventHandler(new EventBusImpl(), edaAdapter, configuration, repository, outbox);
        var event = new SimpleEvent("pid", PermissionProcessStatus.VALIDATED);

        // When
        handler.accept(event);

        // Then
        verify(outbox, never()).commit(any());
    }
}
