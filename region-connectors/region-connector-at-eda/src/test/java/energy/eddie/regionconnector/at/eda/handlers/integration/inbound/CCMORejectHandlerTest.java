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
import energy.eddie.regionconnector.at.eda.config.PlainAtConfiguration;
import energy.eddie.regionconnector.at.eda.dto.SimpleResponseData;
import energy.eddie.regionconnector.at.eda.models.CMRequestStatus;
import energy.eddie.regionconnector.at.eda.models.ConsentData;
import energy.eddie.regionconnector.at.eda.models.ResponseCode;
import energy.eddie.regionconnector.at.eda.permission.request.EdaPermissionRequest;
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

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

import static energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata.AT_ZONE_ID;
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
    @Mock
    private DataNeedsService dataNeedsService;
    @Spy
    @SuppressWarnings("unused") // injected
    private ValidatedEventFactory validatedEventFactory = new ValidatedEventFactory(new PlainAtConfiguration("test"));
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
        LocalDate today = LocalDate.now(AT_ZONE_ID);
        var permissionRequest = new EdaPermissionRequest("connectionId", "pid", "dnid", "cmRequestId",
                                                         "conversationId", null, "dsoId", today, today.plusDays(1),
                                                         AllowedGranularity.PT15M,
                                                         PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR,
                                                         "", null, null);
        when(repository.findByConversationIdOrCMRequestId(any(), any()))
                .thenReturn(List.of(permissionRequest));
        CMRequestStatus cmRequestStatus = cmRequestStatus(ResponseCode.CmReqOnl.CONSENT_REQUEST_ID_ALREADY_EXISTS);

        // When
        handler.handleCCMOReject(cmRequestStatus);

        // Then
        verify(outbox).commit(validatedEventCaptor.capture());
        assertAll(
                () -> assertEquals(permissionRequest.start(), validatedEventCaptor.getValue().start()),
                () -> assertEquals(permissionRequest.end(), validatedEventCaptor.getValue().end()),
                () -> assertEquals(permissionRequest.granularity(), validatedEventCaptor.getValue().granularity())
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
        var permissionRequest = permissionRequest(AllowedGranularity.PT15M);
        when(repository.findByConversationIdOrCMRequestId(any(), any()))
                .thenReturn(List.of(permissionRequest));
        CMRequestStatus cmRequestStatus = cmRequestStatus(ResponseCode.CmReqOnl.REQUESTED_DATA_NOT_DELIVERABLE);
        when(dataNeedsService.getById("dnid"))
                .thenReturn(new ValidatedHistoricalDataDataNeed(
                        new RelativeDuration(Period.ofDays(-10), Period.ofDays(-1), null),
                        EnergyType.ELECTRICITY,
                        Granularity.PT15M,
                        Granularity.P1Y
                ));
        when(dataNeedCalculationService.calculate(any()))
                .thenReturn(new DataNeedCalculation(true, List.of(Granularity.PT15M, Granularity.P1D), null, null));

        // When
        handler.handleCCMOReject(cmRequestStatus);

        // Then
        verify(outbox).commit(validatedEventCaptor.capture());
        assertEquals(AllowedGranularity.P1D, validatedEventCaptor.getValue().granularity());
    }

    private static EdaPermissionRequest permissionRequest(AllowedGranularity allowedGranularity) {
        return new EdaPermissionRequest("connectionId", "pid", "dnid", "cmRequestId",
                                        "conversationId", null, "dsoId", null, null,
                                        allowedGranularity,
                                        PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR,
                                        "", null, null);
    }

    @Test
    void testCmRequestStatusMessage_doesNotRetryOnUnsupportedGranularity() {
        // Given
        var permissionRequest = permissionRequest(AllowedGranularity.PT15M);
        when(repository.findByConversationIdOrCMRequestId(any(), any()))
                .thenReturn(List.of(permissionRequest));
        CMRequestStatus cmRequestStatus = cmRequestStatus(ResponseCode.CmReqOnl.REQUESTED_DATA_NOT_DELIVERABLE);
        when(dataNeedsService.getById("dnid"))
                .thenReturn(new ValidatedHistoricalDataDataNeed(
                        new RelativeDuration(Period.ofDays(-10), Period.ofDays(-1), null),
                        EnergyType.ELECTRICITY,
                        Granularity.PT15M,
                        Granularity.PT1H
                ));
        when(dataNeedCalculationService.calculate(any()))
                .thenReturn(new DataNeedCalculation(true, List.of(Granularity.PT15M), null, null));

        // When
        handler.handleCCMOReject(cmRequestStatus);

        // Then
        verify(outbox).commit(edaAnswerEventCaptor.capture());
        assertEquals(PermissionProcessStatus.INVALID, edaAnswerEventCaptor.getValue().status());
    }

    @Test
    void testCmRequestStatusMessage_doesNotRetryOnHighestGranularity() {
        // Given
        var permissionRequest = permissionRequest(AllowedGranularity.P1D);
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
        var permissionRequest = permissionRequest(AllowedGranularity.P1D);
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
        var permissionRequest = permissionRequest(AllowedGranularity.P1D);
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
