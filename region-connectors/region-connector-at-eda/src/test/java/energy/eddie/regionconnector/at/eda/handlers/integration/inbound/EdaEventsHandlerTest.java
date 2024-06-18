package energy.eddie.regionconnector.at.eda.handlers.integration.inbound;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.DataNeedCalculation;
import energy.eddie.api.agnostic.data.needs.DataNeedCalculationService;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.EnergyType;
import energy.eddie.dataneeds.duration.RelativeDuration;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.at.api.AtPermissionRequestRepository;
import energy.eddie.regionconnector.at.eda.EdaAdapter;
import energy.eddie.regionconnector.at.eda.config.PlainAtConfiguration;
import energy.eddie.regionconnector.at.eda.models.CMRequestStatus;
import energy.eddie.regionconnector.at.eda.models.ResponseCode;
import energy.eddie.regionconnector.at.eda.permission.request.EdaPermissionRequest;
import energy.eddie.regionconnector.at.eda.permission.request.events.AcceptedEvent;
import energy.eddie.regionconnector.at.eda.permission.request.events.EdaAnswerEvent;
import energy.eddie.regionconnector.at.eda.permission.request.events.ValidatedEvent;
import energy.eddie.regionconnector.at.eda.permission.request.events.ValidatedEventFactory;
import energy.eddie.regionconnector.at.eda.ponton.messenger.NotificationMessageType;
import energy.eddie.regionconnector.at.eda.requests.restricted.enums.AllowedGranularity;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.publisher.TestPublisher;

import java.time.Period;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EdaEventsHandlerTest {
    private final ValidatedEventFactory validatedEventFactory = new ValidatedEventFactory(new PlainAtConfiguration(
            "test"));
    @Captor
    ArgumentCaptor<AcceptedEvent> acceptedEventCaptor;
    @Mock
    private AtPermissionRequestRepository repository;
    @Mock
    private EdaAdapter edaAdapter;
    @Mock
    private Outbox outbox;
    @Mock
    private DataNeedsService dataNeedsService;
    @Mock
    private DataNeedCalculationService<DataNeed> dataNeedCalculationService;
    @Captor
    private ArgumentCaptor<EdaAnswerEvent> edaAnswerEventCaptor;
    @Captor
    private ArgumentCaptor<ValidatedEvent> validatedEventCaptor;

    public static Stream<Arguments> testAcceptedMessage_doesNotEmitOnMissingPayload() {
        return Stream.of(
                Arguments.of("mid", null),
                Arguments.of(null, "consentId")
        );
    }

    public static Stream<Arguments> testCmRequestStatusMessage_emitsCorrectEvent() {
        return Stream.of(
                Arguments.of(NotificationMessageType.PONTON_ERROR, PermissionProcessStatus.INVALID, List.of()),
                Arguments.of(NotificationMessageType.CCMS_REJECT,
                             PermissionProcessStatus.EXTERNALLY_TERMINATED,
                             List.of(ResponseCode.CmRevSP.TERMINATION_SUCCESSFUL)),
                Arguments.of(NotificationMessageType.CCMS_REJECT,
                             PermissionProcessStatus.EXTERNALLY_TERMINATED,
                             List.of(ResponseCode.CmRevSP.CONSENT_AND_METERINGPOINT_DO_NOT_MATCH)),
                Arguments.of(NotificationMessageType.CCMS_REJECT,
                             PermissionProcessStatus.EXTERNALLY_TERMINATED,
                             List.of(ResponseCode.CmRevSP.INVALID_PROCESSDATE)),
                Arguments.of(NotificationMessageType.CCMS_REJECT,
                             PermissionProcessStatus.EXTERNALLY_TERMINATED,
                             List.of(ResponseCode.CmRevSP.CONSENT_ID_EXPIRED)),
                Arguments.of(NotificationMessageType.CCMS_REJECT,
                             PermissionProcessStatus.EXTERNALLY_TERMINATED,
                             List.of(ResponseCode.CmRevSP.NO_CONSENT_PRESENT)),
                Arguments.of(NotificationMessageType.CCMO_REJECT,
                             PermissionProcessStatus.REJECTED,
                             List.of(ResponseCode.CmReqOnl.REJECTED)),
                Arguments.of(NotificationMessageType.CCMO_REJECT,
                             PermissionProcessStatus.TIMED_OUT,
                             List.of(ResponseCode.CmReqOnl.TIMEOUT)),
                Arguments.of(NotificationMessageType.CCMO_REJECT,
                             PermissionProcessStatus.INVALID,
                             List.of(ResponseCode.CmReqOnl.INVALID_REQUEST)),
                Arguments.of(NotificationMessageType.CCMO_ANSWER,
                             PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR,
                             List.of(ResponseCode.CmReqOnl.RECEIVED))
        );
    }

    @Test
    void testAcceptedMessage_emitsAcceptedEvent() {
        // Given
        TestPublisher<CMRequestStatus> publisher = TestPublisher.create();
        when(edaAdapter.getCMRequestStatusStream()).thenReturn(publisher.flux());
        var permissionRequest = new EdaPermissionRequest("connectionId", "pid", "dnid", "cmRequestId",
                                                         "conversationId", null, "dsoId", null, null,
                                                         AllowedGranularity.PT15M,
                                                         PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR,
                                                         "", null, null);
        when(repository.findByConversationIdOrCMRequestId("conversationId", "cmRequestId"))
                .thenReturn(Optional.of(permissionRequest));
        CMRequestStatus cmRequestStatus = new CMRequestStatus(
                NotificationMessageType.CCMO_ACCEPT,
                "conversationId",
                List.of(),
                "cmRequestId",
                "consentId",
                "mid");
        new EdaEventsHandler(edaAdapter,
                             outbox,
                             repository,
                             dataNeedCalculationService,
                             dataNeedsService,
                             validatedEventFactory);

        // When
        publisher.emit(cmRequestStatus);

        // Then
        verify(outbox).commit(acceptedEventCaptor.capture());
        assertAll(
                () -> assertEquals("mid", acceptedEventCaptor.getValue().meteringPointId()),
                () -> assertEquals("consentId", acceptedEventCaptor.getValue().cmConsentId())
        );
    }

    @ParameterizedTest
    @MethodSource
    void testAcceptedMessage_doesNotEmitOnMissingPayload(String meteringPoint, String consentId) {
        // Given
        TestPublisher<CMRequestStatus> publisher = TestPublisher.create();
        when(edaAdapter.getCMRequestStatusStream()).thenReturn(publisher.flux());
        var permissionRequest = new EdaPermissionRequest("connectionId", "pid", "dnid", "cmRequestId",
                                                         "conversationId", null, "dsoId", null, null,
                                                         AllowedGranularity.PT15M,
                                                         PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR,
                                                         "", null, null);
        when(repository.findByConversationIdOrCMRequestId("conversationId", "cmRequestId"))
                .thenReturn(Optional.of(permissionRequest));
        CMRequestStatus cmRequestStatus = new CMRequestStatus(
                NotificationMessageType.CCMO_ACCEPT,
                "conversationId",
                List.of(),
                "cmRequestId",
                consentId,
                meteringPoint);
        new EdaEventsHandler(edaAdapter,
                             outbox,
                             repository,
                             dataNeedCalculationService,
                             dataNeedsService,
                             validatedEventFactory);

        // When
        publisher.emit(cmRequestStatus);

        // Then
        verify(outbox, never()).commit(any());
    }

    @Test
    void testErrorMessage_forNotAcknowledgedPermissionRequest_emitsAcknowledgmentAndInvalidEvent() {
        // Given
        TestPublisher<CMRequestStatus> publisher = TestPublisher.create();
        when(edaAdapter.getCMRequestStatusStream()).thenReturn(publisher.flux());
        var permissionRequest = new EdaPermissionRequest("connectionId", "pid", "dnid", "cmRequestId",
                                                         "conversationId", null, "dsoId", null, null,
                                                         AllowedGranularity.PT15M,
                                                         PermissionProcessStatus.VALIDATED,
                                                         "", null, null);
        when(repository.findByConversationIdOrCMRequestId("conversationId", null))
                .thenReturn(Optional.of(permissionRequest));
        CMRequestStatus cmRequestStatus = new CMRequestStatus(NotificationMessageType.PONTON_ERROR,
                                                              "conversationId",
                                                              "");
        new EdaEventsHandler(edaAdapter,
                             outbox,
                             repository,
                             dataNeedCalculationService,
                             dataNeedsService,
                             validatedEventFactory);

        // When
        publisher.emit(cmRequestStatus);

        // Then
        verify(outbox, times(2)).commit(edaAnswerEventCaptor.capture());
        assertAll(
                () -> assertEquals(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR,
                                   edaAnswerEventCaptor.getAllValues().getFirst().status()),
                () -> assertEquals(PermissionProcessStatus.INVALID, edaAnswerEventCaptor.getAllValues().get(1).status())
        );
    }

    @ParameterizedTest
    @MethodSource
    void testCmRequestStatusMessage_emitsCorrectEvent(
            NotificationMessageType notificationMessageType,
            PermissionProcessStatus status,
            List<Integer> statusCodes
    ) {
        // Given
        TestPublisher<CMRequestStatus> publisher = TestPublisher.create();
        when(edaAdapter.getCMRequestStatusStream()).thenReturn(publisher.flux());
        var permissionRequest = new EdaPermissionRequest("connectionId", "pid", "dnid", "cmRequestId",
                                                         "conversationId", null, "dsoId", null, null,
                                                         AllowedGranularity.PT15M,
                                                         PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR,
                                                         "", null, null);
        when(repository.findByConversationIdOrCMRequestId("conversationId", null))
                .thenReturn(Optional.of(permissionRequest));
        CMRequestStatus cmRequestStatus = new CMRequestStatus(
                notificationMessageType,
                "conversationId",
                statusCodes,
                null,
                null,
                null
        );
        new EdaEventsHandler(edaAdapter,
                             outbox,
                             repository,
                             dataNeedCalculationService,
                             dataNeedsService,
                             validatedEventFactory);

        // When
        publisher.emit(cmRequestStatus);

        // Then
        verify(outbox).commit(edaAnswerEventCaptor.capture());
        assertEquals(status, edaAnswerEventCaptor.getValue().status());
    }

    @Test
    void testUnknownCmRequestStatus_doesNotEmit() {
        // Given
        TestPublisher<CMRequestStatus> publisher = TestPublisher.create();
        when(edaAdapter.getCMRequestStatusStream()).thenReturn(publisher.flux());
        when(repository.findByConversationIdOrCMRequestId("conversationId", null))
                .thenReturn(Optional.empty());
        CMRequestStatus cmRequestStatus = new CMRequestStatus(NotificationMessageType.CCMO_ACCEPT,
                                                              "conversationId",
                                                              "");
        new EdaEventsHandler(edaAdapter,
                             outbox,
                             repository,
                             dataNeedCalculationService,
                             dataNeedsService,
                             validatedEventFactory);

        // When
        publisher.emit(cmRequestStatus);

        // Then
        verify(outbox, never()).commit(any());
    }

    @Test
    void testCmRequestStatusMessage_retriesWithHigherGranularity() {
        // Given
        TestPublisher<CMRequestStatus> publisher = TestPublisher.create();
        when(edaAdapter.getCMRequestStatusStream()).thenReturn(publisher.flux());
        var permissionRequest = new EdaPermissionRequest("connectionId", "pid", "dnid", "cmRequestId",
                                                         "conversationId", null, "dsoId", null, null,
                                                         AllowedGranularity.PT15M,
                                                         PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR,
                                                         "", null, null);
        when(repository.findByConversationIdOrCMRequestId("conversationId", null))
                .thenReturn(Optional.of(permissionRequest));
        CMRequestStatus cmRequestStatus = new CMRequestStatus(
                NotificationMessageType.CCMO_REJECT,
                "conversationId",
                List.of(ResponseCode.CmReqOnl.REQUESTED_DATA_NOT_DELIVERABLE),
                null,
                null,
                null
        );
        when(dataNeedsService.getById("dnid"))
                .thenReturn(new ValidatedHistoricalDataDataNeed(
                        new RelativeDuration(Period.ofDays(-10), Period.ofDays(-1), null),
                        EnergyType.ELECTRICITY,
                        Granularity.PT15M,
                        Granularity.P1Y
                ));
        when(dataNeedCalculationService.calculate(any()))
                .thenReturn(new DataNeedCalculation(true, List.of(Granularity.PT15M, Granularity.P1D), null, null));
        new EdaEventsHandler(edaAdapter,
                             outbox,
                             repository,
                             dataNeedCalculationService,
                             dataNeedsService,
                             validatedEventFactory);

        // When
        publisher.emit(cmRequestStatus);

        // Then
        verify(outbox).commit(validatedEventCaptor.capture());
        assertEquals(AllowedGranularity.P1D, validatedEventCaptor.getValue().granularity());
    }

    @Test
    void testCmRequestStatusMessage_doesNotRetryOnUnsupportedGranularity() {
        // Given
        TestPublisher<CMRequestStatus> publisher = TestPublisher.create();
        when(edaAdapter.getCMRequestStatusStream()).thenReturn(publisher.flux());
        var permissionRequest = new EdaPermissionRequest("connectionId", "pid", "dnid", "cmRequestId",
                                                         "conversationId", null, "dsoId", null, null,
                                                         AllowedGranularity.PT15M,
                                                         PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR,
                                                         "", null, null);
        when(repository.findByConversationIdOrCMRequestId("conversationId", null))
                .thenReturn(Optional.of(permissionRequest));
        CMRequestStatus cmRequestStatus = new CMRequestStatus(
                NotificationMessageType.CCMO_REJECT,
                "conversationId",
                List.of(ResponseCode.CmReqOnl.REQUESTED_DATA_NOT_DELIVERABLE),
                null,
                null,
                null
        );
        when(dataNeedsService.getById("dnid"))
                .thenReturn(new ValidatedHistoricalDataDataNeed(
                        new RelativeDuration(Period.ofDays(-10), Period.ofDays(-1), null),
                        EnergyType.ELECTRICITY,
                        Granularity.PT15M,
                        Granularity.PT1H
                ));
        when(dataNeedCalculationService.calculate(any()))
                .thenReturn(new DataNeedCalculation(true, List.of(Granularity.PT15M), null, null));
        new EdaEventsHandler(edaAdapter,
                             outbox,
                             repository,
                             dataNeedCalculationService,
                             dataNeedsService,
                             validatedEventFactory);

        // When
        publisher.emit(cmRequestStatus);

        // Then
        verify(outbox).commit(edaAnswerEventCaptor.capture());
        assertEquals(PermissionProcessStatus.INVALID, edaAnswerEventCaptor.getValue().status());
    }

    @Test
    void testCmRequestStatusMessage_doesNotRetryOnHighestGranularity() {
        // Given
        TestPublisher<CMRequestStatus> publisher = TestPublisher.create();
        when(edaAdapter.getCMRequestStatusStream()).thenReturn(publisher.flux());
        var permissionRequest = new EdaPermissionRequest("connectionId", "pid", "dnid", "cmRequestId",
                                                         "conversationId", null, "dsoId", null, null,
                                                         AllowedGranularity.P1D,
                                                         PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR,
                                                         "", null, null);
        when(repository.findByConversationIdOrCMRequestId("conversationId", null))
                .thenReturn(Optional.of(permissionRequest));
        CMRequestStatus cmRequestStatus = new CMRequestStatus(
                NotificationMessageType.CCMO_REJECT,
                "conversationId",
                List.of(ResponseCode.CmReqOnl.REQUESTED_DATA_NOT_DELIVERABLE),
                null,
                null,
                null
        );
        new EdaEventsHandler(edaAdapter,
                             outbox,
                             repository,
                             dataNeedCalculationService,
                             dataNeedsService,
                             validatedEventFactory);

        // When
        publisher.emit(cmRequestStatus);

        // Then
        verify(outbox).commit(edaAnswerEventCaptor.capture());
        assertEquals(PermissionProcessStatus.INVALID, edaAnswerEventCaptor.getValue().status());
    }
}
