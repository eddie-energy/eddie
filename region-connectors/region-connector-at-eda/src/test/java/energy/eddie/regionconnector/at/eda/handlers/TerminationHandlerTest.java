// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.handlers;

import energy.eddie.cim.agnostic.PermissionProcessStatus;
import energy.eddie.dataneeds.needs.CESUJoinRequestDataNeed;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.at.api.AtPermissionRequestRepository;
import energy.eddie.regionconnector.at.eda.EdaAdapter;
import energy.eddie.regionconnector.at.eda.TransmissionException;
import energy.eddie.regionconnector.at.eda.config.AtConfiguration;
import energy.eddie.regionconnector.at.eda.permission.request.EdaPermissionRequestBuilder;
import energy.eddie.regionconnector.at.eda.permission.request.events.SimpleEvent;
import energy.eddie.regionconnector.at.eda.requests.restricted.enums.AllowedGranularity;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.EventBusImpl;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static energy.eddie.cim.agnostic.PermissionProcessStatus.FAILED_TO_TERMINATE;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TerminationHandlerTest {
    private final EventBus eventBus = new EventBusImpl();
    @Mock
    private AtPermissionRequestRepository repository;
    @Mock
    private Outbox outbox;
    @Mock
    private EdaAdapter edaAdapter;
    @Mock
    private DataNeedsService dataNeedsService;
    @Captor
    private ArgumentCaptor<SimpleEvent> simpleEventCaptor;

    @Test
    void terminatePermission_edaThrows_emitsFailedToTerminate() throws TransmissionException {
        // given
        doThrow(new TransmissionException(null)).when(edaAdapter).sendCMRevoke(any());
        var permissionRequest = new EdaPermissionRequestBuilder()
                .setPermissionId("pid")
                .setDataNeedId("dnid")
                .setCmRequestId("cmRequestId")
                .setConversationId("conversationId")
                .setMeteringPointId("mid")
                .setDsoId("dsoId")
                .setGranularity(AllowedGranularity.PT15M)
                .setStatus(PermissionProcessStatus.ACCEPTED)
                .setConsentId("consentId")
                .build();
        when(repository.getByPermissionId("pid")).thenReturn(permissionRequest);
        when(dataNeedsService.getById("dnid")).thenReturn(new ValidatedHistoricalDataDataNeed());
        var config = new AtConfiguration("epid", null, null);
        new TerminationHandler(outbox, eventBus, repository, config, edaAdapter, dataNeedsService);

        // when
        eventBus.emit(new SimpleEvent("pid", PermissionProcessStatus.REQUIRES_EXTERNAL_TERMINATION));

        // then
        verify(outbox).commit(simpleEventCaptor.capture());
        var res = simpleEventCaptor.getValue();
        assertEquals(FAILED_TO_TERMINATE, res.status());
    }

    @Test
    void terminatePermission_revokeIsSent() throws TransmissionException {
        // given
        var permissionRequest = new EdaPermissionRequestBuilder()
                .setPermissionId("pid")
                .setDataNeedId("dnid")
                .setCmRequestId("cmRequestId")
                .setConversationId("conversationId")
                .setMeteringPointId("mid")
                .setDsoId("dsoId")
                .setGranularity(AllowedGranularity.PT15M)
                .setStatus(PermissionProcessStatus.ACCEPTED)
                .setConsentId("consentId")
                .build();
        when(repository.getByPermissionId("pid")).thenReturn(permissionRequest);
        when(dataNeedsService.getById("dnid")).thenReturn(new ValidatedHistoricalDataDataNeed());
        var config = new AtConfiguration("epid", null, null);
        new TerminationHandler(outbox, eventBus, repository, config, edaAdapter, dataNeedsService);
        // when
        eventBus.emit(new SimpleEvent("pid", PermissionProcessStatus.REQUIRES_EXTERNAL_TERMINATION));

        // then
        verify(edaAdapter).sendCMRevoke(assertArg(revoke -> assertAll(
                () -> assertEquals("epid", revoke.eligiblePartyId()),
                () -> assertEquals("Terminated by the Eligible Party", revoke.reason())
        )));
    }


    @Test
    void terminatePermissionForCesu_revokeIsSent() throws TransmissionException {
        // given
        var permissionRequest = new EdaPermissionRequestBuilder()
                .setPermissionId("pid")
                .setDataNeedId("dnid")
                .setCmRequestId("cmRequestId")
                .setConversationId("conversationId")
                .setMeteringPointId("mid")
                .setDsoId("dsoId")
                .setGranularity(AllowedGranularity.PT15M)
                .setStatus(PermissionProcessStatus.ACCEPTED)
                .setConsentId("consentId")
                .build();
        when(repository.getByPermissionId("pid")).thenReturn(permissionRequest);
        when(dataNeedsService.getById("dnid")).thenReturn(new CESUJoinRequestDataNeed());
        var config = new AtConfiguration("epid", "ec-id", "ec-id");
        new TerminationHandler(outbox, eventBus, repository, config, edaAdapter, dataNeedsService);
        // when
        eventBus.emit(new SimpleEvent("pid", PermissionProcessStatus.REQUIRES_EXTERNAL_TERMINATION));

        // then
        verify(edaAdapter).sendCMRevoke(assertArg(revoke -> assertAll(
                () -> assertEquals("ec-id", revoke.eligiblePartyId()),
                () -> assertEquals("Terminated by the Energy Community Operator", revoke.reason())
        )));
    }


    @Test
    void terminatePermissionForCesuWithoutConfiguredECId_emitsFailedToTerminated() {
        // given
        var permissionRequest = new EdaPermissionRequestBuilder()
                .setPermissionId("pid")
                .setDataNeedId("dnid")
                .setCmRequestId("cmRequestId")
                .setConversationId("conversationId")
                .setMeteringPointId("mid")
                .setDsoId("dsoId")
                .setGranularity(AllowedGranularity.PT15M)
                .setStatus(PermissionProcessStatus.ACCEPTED)
                .setConsentId("consentId")
                .build();
        when(repository.getByPermissionId("pid")).thenReturn(permissionRequest);
        when(dataNeedsService.getById("dnid")).thenReturn(new CESUJoinRequestDataNeed());
        var config = new AtConfiguration("epid", null, null);
        new TerminationHandler(outbox, eventBus, repository, config, edaAdapter, dataNeedsService);
        // when
        eventBus.emit(new SimpleEvent("pid", PermissionProcessStatus.REQUIRES_EXTERNAL_TERMINATION));

        // then
        verifyNoInteractions(edaAdapter);
        verify(outbox).commit(simpleEventCaptor.capture());
        var res = simpleEventCaptor.getValue();
        assertEquals(FAILED_TO_TERMINATE, res.status());
    }
}
