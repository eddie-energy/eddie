package energy.eddie.regionconnector.at.eda.handlers.integration.inbound;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.api.AtPermissionRequestRepository;
import energy.eddie.regionconnector.at.eda.dto.ResponseData;
import energy.eddie.regionconnector.at.eda.dto.SimpleResponseData;
import energy.eddie.regionconnector.at.eda.models.CMRequestStatus;
import energy.eddie.regionconnector.at.eda.models.ConsentData;
import energy.eddie.regionconnector.at.eda.models.ResponseCode;
import energy.eddie.regionconnector.at.eda.permission.request.EdaPermissionRequest;
import energy.eddie.regionconnector.at.eda.permission.request.events.*;
import energy.eddie.regionconnector.at.eda.ponton.messenger.NotificationMessageType;
import energy.eddie.regionconnector.at.eda.requests.restricted.enums.AllowedGranularity;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CCMOAcceptHandlerTest {
    @Captor
    private ArgumentCaptor<AcceptedEvent> acceptedEventArgumentCaptor;

    @Mock
    private AtPermissionRequestRepository repository;

    @Mock
    private Outbox outbox;

    @InjectMocks
    private CCMOAcceptHandler handler;

    @Test
    void handleCCMOAccept_oneConsent_oneRequest_acceptsRequest() {
        // Given
        CMRequestStatus cmRequestStatus = cmRequestStatus(
                List.of(responseData("consentId", "meteringPoint"))
        );
        when(repository.findByConversationIdOrCMRequestId(cmRequestStatus.conversationId(),
                                                          cmRequestStatus.cmRequestId()))
                .thenReturn(List.of(permissionRequest()));

        // When
        handler.handleCCMOAccept(cmRequestStatus);

        // Then
        verify(outbox).commit(acceptedEventArgumentCaptor.capture());
        assertEquals(PermissionProcessStatus.ACCEPTED, acceptedEventArgumentCaptor.getValue().status());
    }

    private static CMRequestStatus cmRequestStatus(
            List<ResponseData> responseData
    ) {
        return new CMRequestStatus(
                NotificationMessageType.CCMO_ACCEPT,
                "conversationId",
                "cmRequestId",
                responseData.stream().map(ConsentData::fromResponseData).toList()
        );
    }

    private static ResponseData responseData(String consentId, String meteringPoint) {
        return new SimpleResponseData(consentId, meteringPoint, List.of(ResponseCode.CmReqOnl.ACCEPTED));
    }

    private static EdaPermissionRequest permissionRequest() {
        return new EdaPermissionRequest("connectionId", "pid", "dnid", "cmRequestId",
                                        "conversationId", null, "dsoId", null, null,
                                        AllowedGranularity.PT15M,
                                        PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR,
                                        "", null, ZonedDateTime.now(ZoneOffset.UTC));
    }

    @Test
    void handleCCMOAccept_oneConsent_multipleRequests_acceptsOnlyFirstRequest() {
        CMRequestStatus cmRequestStatus = cmRequestStatus(
                List.of(responseData("consentId", "meteringPoint"))
        );
        when(repository.findByConversationIdOrCMRequestId(cmRequestStatus.conversationId(),
                                                          cmRequestStatus.cmRequestId()))
                .thenReturn(List.of(
                        permissionRequest(),
                        permissionRequest()
                ));

        // When
        handler.handleCCMOAccept(cmRequestStatus);

        // Then
        verify(outbox).commit(acceptedEventArgumentCaptor.capture());
        assertEquals(PermissionProcessStatus.ACCEPTED, acceptedEventArgumentCaptor.getValue().status());
    }

    @Test
    void handleCCMOAccept_oneConsent_noRequests_doesNothing() {
        CMRequestStatus cmRequestStatus = cmRequestStatus(
                List.of(responseData("consentId", "meteringPoint"))
        );
        when(repository.findByConversationIdOrCMRequestId(cmRequestStatus.conversationId(),
                                                          cmRequestStatus.cmRequestId()))
                .thenReturn(List.of());

        // When
        handler.handleCCMOAccept(cmRequestStatus);

        // Then
        verifyNoInteractions(outbox);
    }


    @Test
    void handleCCMOAccept_multipleConsent_oneRequests_acceptsFirstRequestCreatesNewForEveryConsent() {
        // Given
        ArgumentCaptor<PersistablePermissionEvent> eventCaptor = ArgumentCaptor.forClass(PersistablePermissionEvent.class);

        String meteringPoint1 = "meteringPoint1";
        String meteringPoint2 = "meteringPoint2";
        String meteringPoint3 = "meteringPoint3";
        CMRequestStatus cmRequestStatus = cmRequestStatus(
                List.of(
                        responseData("consentId1", meteringPoint1),
                        responseData("consentId2", meteringPoint2),
                        responseData("consentId3", meteringPoint3)
                )
        );
        EdaPermissionRequest permissionRequest = permissionRequest();
        when(repository.findByConversationIdOrCMRequestId(cmRequestStatus.conversationId(),
                                                          cmRequestStatus.cmRequestId()))
                .thenReturn(List.of(permissionRequest));

        // When
        handler.handleCCMOAccept(cmRequestStatus);

        // Then
        verify(outbox, times(9)).commit(eventCaptor.capture());
        assertAll(
                () -> {
                    assertInstanceOf(AcceptedEvent.class, eventCaptor.getAllValues().getFirst());
                    assertAcceptedEvent((AcceptedEvent) eventCaptor.getAllValues().getFirst(),
                                        meteringPoint1,
                                        "consentId1");
                },
                () -> {
                    assertInstanceOf(CreatedEvent.class, eventCaptor.getAllValues().get(1));
                    assertCreatedEvent((CreatedEvent) eventCaptor.getAllValues().get(1), permissionRequest,
                                       meteringPoint2);
                    assertInstanceOf(ValidatedEvent.class, eventCaptor.getAllValues().get(2));
                    assertValidatedEvent((ValidatedEvent) eventCaptor.getAllValues().get(2), permissionRequest);
                    assertInstanceOf(EdaAnswerEvent.class, eventCaptor.getAllValues().get(3));
                    assertSentEvent((EdaAnswerEvent) eventCaptor.getAllValues().get(3), permissionRequest);
                    assertInstanceOf(AcceptedEvent.class, eventCaptor.getAllValues().get(4));
                    assertAcceptedEvent((AcceptedEvent) eventCaptor.getAllValues().get(4),
                                        meteringPoint2,
                                        "consentId2");
                },
                () -> {
                    assertInstanceOf(CreatedEvent.class, eventCaptor.getAllValues().get(5));
                    assertCreatedEvent((CreatedEvent) eventCaptor.getAllValues().get(5), permissionRequest,
                                       meteringPoint3);
                    assertInstanceOf(ValidatedEvent.class, eventCaptor.getAllValues().get(6));
                    assertValidatedEvent((ValidatedEvent) eventCaptor.getAllValues().get(6), permissionRequest);
                    assertInstanceOf(EdaAnswerEvent.class, eventCaptor.getAllValues().get(7));
                    assertSentEvent((EdaAnswerEvent) eventCaptor.getAllValues().get(7), permissionRequest);
                    assertInstanceOf(AcceptedEvent.class, eventCaptor.getAllValues().getLast());
                    assertAcceptedEvent((AcceptedEvent) eventCaptor.getAllValues().getLast(),
                                        meteringPoint3,
                                        "consentId3");
                }
        );
    }

    private static void assertAcceptedEvent(AcceptedEvent event, String meteringPoint, String consentId) {
        assertAll(
                () -> assertEquals(PermissionProcessStatus.ACCEPTED, event.status()),
                () -> assertEquals(meteringPoint, event.meteringPointId()),
                () -> assertEquals(consentId, event.cmConsentId())
        );
    }

    private void assertCreatedEvent(CreatedEvent event, AtPermissionRequest permissionRequest, String meteringPoint) {
        assertAll(
                () -> assertNotEquals(permissionRequest.permissionId(), event.permissionId()),
                () -> assertEquals(permissionRequest.connectionId(), event.connectionId()),
                () -> assertEquals(permissionRequest.created(), event.created()),
                () -> assertEquals(permissionRequest.dataNeedId(), event.dataNeedId()),
                () -> assertEquals(meteringPoint, event.meteringPointId()),
                () -> assertEquals(permissionRequest.dataSourceInformation().meteredDataAdministratorId(),
                                   event.dataSourceInformation().meteredDataAdministratorId())
        );
    }

    private void assertValidatedEvent(ValidatedEvent event, AtPermissionRequest permissionRequest) {
        assertAll(
                () -> assertNotEquals(permissionRequest.permissionId(), event.permissionId()),
                () -> assertEquals(permissionRequest.start(), event.start()),
                () -> assertEquals(permissionRequest.end(), event.end()),
                () -> assertEquals(permissionRequest.granularity(), event.granularity()),
                () -> assertEquals(permissionRequest.cmRequestId(), event.cmRequestId()),
                () -> assertFalse(event.needsToBeSent())
        );
    }

    private void assertSentEvent(EdaAnswerEvent event, AtPermissionRequest permissionRequest) {
        assertAll(
                () -> assertNotEquals(permissionRequest.permissionId(), event.permissionId()),
                () -> assertEquals(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR, event.status())
        );
    }
}
