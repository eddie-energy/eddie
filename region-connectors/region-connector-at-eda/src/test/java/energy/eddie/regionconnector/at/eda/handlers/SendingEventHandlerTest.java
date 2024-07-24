package energy.eddie.regionconnector.at.eda.handlers;

import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.at.api.AtPermissionRequestRepository;
import energy.eddie.regionconnector.at.eda.EdaAdapter;
import energy.eddie.regionconnector.at.eda.TransmissionException;
import energy.eddie.regionconnector.at.eda.config.AtConfiguration;
import energy.eddie.regionconnector.at.eda.permission.request.EdaPermissionRequest;
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

import java.time.LocalDate;
import java.time.ZonedDateTime;

import static energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata.AT_ZONE_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SendingEventHandlerTest {
    private final EdaPermissionRequest permissionRequest = new EdaPermissionRequest(
            "connId",
            "pid",
            "did",
            "cmRequesId",
            "convId",
            null,
            "dsoId",
            LocalDate.now(AT_ZONE_ID),
            null,
            null,
            PermissionProcessStatus.VALIDATED,
            "",
            null,
            ZonedDateTime.now(AT_ZONE_ID)
    );
    @Mock
    private EdaAdapter edaAdapter;
    @Mock
    private Outbox outbox;
    @Captor
    private ArgumentCaptor<PermissionEvent> captor;
    @Captor
    private ArgumentCaptor<CCMORequest> ccmoRequestArgumentCaptor;
    @Mock
    private AtPermissionRequestRepository repository;
    @Mock
    private AtConfiguration configuration;

    @Test
    void testAcceptSendsCmRequest_whenPontonAvailable() throws TransmissionException {
        // Given
        when(repository.getByPermissionId("pid")).thenReturn(permissionRequest);
        var handler = new SendingEventHandler(new EventBusImpl(), repository, edaAdapter, outbox, configuration);
        var event = new ValidatedEvent("pid",
                                       permissionRequest.start(),
                                       permissionRequest.end(),
                                       permissionRequest.granularity(),
                                       permissionRequest.cmRequestId(),
                                       permissionRequest.conversationId(),
                                       ValidatedEvent.NeedsToBeSent.YES);

        // When
        handler.accept(event);

        // Then
        verify(edaAdapter).sendCMRequest(ccmoRequestArgumentCaptor.capture());
        var ccmoRequest = ccmoRequestArgumentCaptor.getValue();
        assertAll(
                () -> assertEquals(permissionRequest.cmRequestId(), ccmoRequest.cmRequestId()),
                () -> assertEquals(permissionRequest.conversationId(), ccmoRequest.messageId()),
                () -> assertEquals(permissionRequest.start(), ccmoRequest.start()),
                () -> assertTrue(ccmoRequest.end().isEmpty()),
                () -> assertEquals(permissionRequest.dataSourceInformation().permissionAdministratorId(),
                                   ccmoRequest.dsoIdAndMeteringPoint().dsoId()
                ),
                () -> assertTrue(ccmoRequest.dsoIdAndMeteringPoint().meteringPoint().isEmpty())
        );
    }

    @Test
    void testAcceptCommitsUnableToSend_whenPontonUnavailable() throws TransmissionException {
        // Given
        doThrow(new TransmissionException(null))
                .when(edaAdapter).sendCMRequest(any());

        when(repository.getByPermissionId("pid")).thenReturn(permissionRequest);
        var handler = new SendingEventHandler(new EventBusImpl(), repository, edaAdapter, outbox, configuration);
        var event = new ValidatedEvent("pid",
                                       permissionRequest.start(),
                                       permissionRequest.end(),
                                       permissionRequest.granularity(),
                                       permissionRequest.cmRequestId(),
                                       permissionRequest.conversationId(),
                                       ValidatedEvent.NeedsToBeSent.YES);

        // When
        handler.accept(event);

        // Then
        verify(outbox).commit(captor.capture());
        assertThat(captor.getValue().status())
                .isEqualTo(PermissionProcessStatus.UNABLE_TO_SEND);
    }
}
