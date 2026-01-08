package energy.eddie.regionconnector.at.eda.handlers.integration.inbound;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.DataNeedCalculationService;
import energy.eddie.api.agnostic.data.needs.Timeframe;
import energy.eddie.api.agnostic.data.needs.ValidatedHistoricalDataDataNeedResult;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.regionconnector.at.api.AtPermissionRequestProjection;
import energy.eddie.regionconnector.at.api.AtPermissionRequestRepository;
import energy.eddie.regionconnector.at.eda.config.AtConfiguration;
import energy.eddie.regionconnector.at.eda.dto.SimpleResponseData;
import energy.eddie.regionconnector.at.eda.models.CMRequestStatus;
import energy.eddie.regionconnector.at.eda.models.ConsentData;
import energy.eddie.regionconnector.at.eda.models.ResponseCode;
import energy.eddie.regionconnector.at.eda.permission.request.events.EdaAnswerEvent;
import energy.eddie.regionconnector.at.eda.permission.request.events.ValidatedEvent;
import energy.eddie.regionconnector.at.eda.permission.request.events.ValidatedEventFactory;
import energy.eddie.regionconnector.at.eda.ponton.messenger.NotificationMessageType;
import energy.eddie.regionconnector.at.eda.requests.restricted.enums.AllowedGranularity;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CCMORejectHandlerTest {
    @InjectMocks
    CCMORejectHandler handler;
    @Mock
    private DataNeedCalculationService<DataNeed> dataNeedCalculationService;
    @Spy
    @SuppressWarnings("unused") // injected
    private ValidatedEventFactory validatedEventFactory = new ValidatedEventFactory(new AtConfiguration("test"));
    @Mock
    private Outbox outbox;

    @Mock
    private AtPermissionRequestRepository repository;

    @Captor
    private ArgumentCaptor<ValidatedEvent> validatedEventCaptor;
    @Captor
    private ArgumentCaptor<EdaAnswerEvent> edaAnswerEventCaptor;

    @Test
    void testCmRequestStatusMessage_retriesOnConsentRequestIdAlreadyExists() {
        // Given
        var permissionRequest = projection(AllowedGranularity.PT15M);

        when(repository.findByConversationIdOrCMRequestId(any(), any()))
                .thenReturn(List.of(permissionRequest));
        CMRequestStatus cmRequestStatus = cmRequestStatus(ResponseCode.CmReqOnl.CONSENT_REQUEST_ID_ALREADY_EXISTS);

        // When
        handler.handleCCMOReject(cmRequestStatus);

        // Then
        verify(outbox).commit(validatedEventCaptor.capture());
        assertAll(
                () -> assertEquals(permissionRequest.getPermissionStart(), validatedEventCaptor.getValue().start()),
                () -> assertEquals(permissionRequest.getPermissionEnd(), validatedEventCaptor.getValue().end()),
                () -> assertEquals(permissionRequest.getGranularity(), validatedEventCaptor.getValue().granularity().name())
        );
    }

    private static CMRequestStatus cmRequestStatus(int requestedDataNotDeliverable) {
        return new CMRequestStatus(
                NotificationMessageType.CCMO_REJECT,
                "conversationId",
                null,
                List.of(ConsentData.fromResponseData(
                        new SimpleResponseData("", "", List.of(requestedDataNotDeliverable)
                        )))
        );
    }

    @Test
    void testCmRequestStatusMessage_retriesWithHigherGranularity() {
        // Given
        var permissionRequest = projection(AllowedGranularity.PT15M);
        when(repository.findByConversationIdOrCMRequestId(any(), any()))
                .thenReturn(List.of(permissionRequest));
        var cmRequestStatus = cmRequestStatus(ResponseCode.CmReqOnl.REQUESTED_DATA_NOT_DELIVERABLE);
        var now = LocalDate.now(ZoneOffset.UTC);
        var timeframe = new Timeframe(now, now);
        when(dataNeedCalculationService.calculate("dnid"))
                .thenReturn(new ValidatedHistoricalDataDataNeedResult(List.of(Granularity.PT15M, Granularity.P1D),
                                                                      timeframe,
                                                                      timeframe));

        // When
        handler.handleCCMOReject(cmRequestStatus);

        // Then
        verify(outbox).commit(validatedEventCaptor.capture());
        assertEquals(AllowedGranularity.P1D, validatedEventCaptor.getValue().granularity());
    }

    private static AtPermissionRequestProjection projection(AllowedGranularity allowedGranularity) {
        return new AtPermissionRequestProjectionTest(
                "pid", "connectionId", "cmRequestId", "conversationId",
                LocalDate.now(ZoneId.systemDefault()), LocalDate.now(ZoneId.systemDefault()), "dnid", "dsoId", "meteringPointId",
                "consentId", "message",
                allowedGranularity.name(), PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR.name(), Instant.now()
        );
    }

    @Test
    void testCmRequestStatusMessage_doesNotRetryOnUnsupportedGranularity() {
        // Given
        var permissionRequest = projection(AllowedGranularity.PT15M);
        when(repository.findByConversationIdOrCMRequestId(any(), any()))
                .thenReturn(List.of(permissionRequest));
        CMRequestStatus cmRequestStatus = cmRequestStatus(ResponseCode.CmReqOnl.REQUESTED_DATA_NOT_DELIVERABLE);
        var now = LocalDate.now(ZoneOffset.UTC);
        var timeframe = new Timeframe(now, now);
        when(dataNeedCalculationService.calculate("dnid"))
                .thenReturn(new ValidatedHistoricalDataDataNeedResult(List.of(Granularity.PT15M),
                                                                      timeframe,
                                                                      timeframe));

        // When
        handler.handleCCMOReject(cmRequestStatus);

        // Then
        verify(outbox).commit(edaAnswerEventCaptor.capture());
        assertEquals(PermissionProcessStatus.INVALID, edaAnswerEventCaptor.getValue().status());
    }

    @Test
    void testCmRequestStatusMessage_doesNotRetryOnHighestGranularity() {
        // Given
        var permissionRequest = projection(AllowedGranularity.P1D);
        when(repository.findByConversationIdOrCMRequestId(any(), any()))
                .thenReturn(List.of(permissionRequest));
        CMRequestStatus cmRequestStatus = cmRequestStatus(ResponseCode.CmReqOnl.REQUESTED_DATA_NOT_DELIVERABLE);

        // When
        handler.handleCCMOReject(cmRequestStatus);

        // Then
        verify(outbox).commit(edaAnswerEventCaptor.capture());
        assertEquals(PermissionProcessStatus.INVALID, edaAnswerEventCaptor.getValue().status());
    }

    @Test
    void testCmRequestStatusMessage_receivingTimeOutEmitsTimeOut() {
        // Given
        var permissionRequest = projection(AllowedGranularity.P1D);
        when(repository.findByConversationIdOrCMRequestId(any(), any()))
                .thenReturn(List.of(permissionRequest));
        CMRequestStatus cmRequestStatus = cmRequestStatus(ResponseCode.CmReqOnl.TIMEOUT);

        // When
        handler.handleCCMOReject(cmRequestStatus);

        // Then
        verify(outbox).commit(edaAnswerEventCaptor.capture());
        assertEquals(PermissionProcessStatus.TIMED_OUT, edaAnswerEventCaptor.getValue().status());
    }

    @Test
    void testCmRequestStatusMessage_receivingRejectedEmitsRejected() {
        // Given
        var permissionRequest = projection(AllowedGranularity.P1D);
        when(repository.findByConversationIdOrCMRequestId(any(), any()))
                .thenReturn(List.of(permissionRequest));
        CMRequestStatus cmRequestStatus = cmRequestStatus(ResponseCode.CmReqOnl.REJECTED);

        // When
        handler.handleCCMOReject(cmRequestStatus);

        // Then
        verify(outbox).commit(edaAnswerEventCaptor.capture());
        assertEquals(PermissionProcessStatus.REJECTED, edaAnswerEventCaptor.getValue().status());
    }
}
