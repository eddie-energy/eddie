package energy.eddie.regionconnector.at.eda.handlers;

import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.at.eda.EdaAdapter;
import energy.eddie.regionconnector.at.eda.TransmissionException;
import energy.eddie.regionconnector.at.eda.permission.request.events.ValidatedEvent;
import energy.eddie.regionconnector.at.eda.requests.CCMORequest;
import energy.eddie.regionconnector.shared.event.sourcing.EventBusImpl;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SendingEventHandlerTest {
    @Mock
    private EdaAdapter edaAdapter;
    @Mock
    private Outbox outbox;
    @Mock
    private CCMORequest ccmoRequest;
    @Captor
    private ArgumentCaptor<PermissionEvent> captor;

    @Test
    void testAcceptSendsCmRequest_whenPontonAvailable() throws TransmissionException {
        // Given
        var handler = new SendingEventHandler(new EventBusImpl(), edaAdapter, outbox);
        var event = new ValidatedEvent("pid", ccmoRequest);

        // When
        handler.accept(event);

        // Then
        verify(edaAdapter).sendCMRequest(ccmoRequest);
    }

    @Test
    void testAcceptCommitsUnableToSend_whenPontonUnavailable() throws TransmissionException {
        // Given
        doThrow(new TransmissionException(null))
                .when(edaAdapter).sendCMRequest(any());

        var handler = new SendingEventHandler(new EventBusImpl(), edaAdapter, outbox);
        var event = new ValidatedEvent("pid", ccmoRequest);

        // When
        handler.accept(event);

        // Then
        verify(outbox).commit(captor.capture());
        assertThat(captor.getValue().status())
                .isEqualTo(PermissionProcessStatus.UNABLE_TO_SEND);
    }
}
