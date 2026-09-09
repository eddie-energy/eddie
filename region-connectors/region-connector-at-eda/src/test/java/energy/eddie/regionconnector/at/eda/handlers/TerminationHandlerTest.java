// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.handlers;

import energy.eddie.api.agnostic.data.needs.EnergyDirection;
import energy.eddie.cim.agnostic.PermissionProcessStatus;
import energy.eddie.regionconnector.at.api.AtPermissionRequestRepository;
import energy.eddie.regionconnector.at.eda.EdaAdapter;
import energy.eddie.regionconnector.at.eda.TransmissionException;
import energy.eddie.regionconnector.at.eda.config.AtConfiguration;
import energy.eddie.regionconnector.at.eda.permission.request.EdaPermissionRequestBuilder;
import energy.eddie.regionconnector.at.eda.permission.request.events.SimpleEvent;
import energy.eddie.regionconnector.at.eda.requests.CCMORevoke;
import energy.eddie.regionconnector.at.eda.requests.EdaGroupingIdFactory;
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

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;

import static energy.eddie.cim.agnostic.PermissionProcessStatus.FAILED_TO_TERMINATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TerminationHandlerTest {
    private final EventBus eventBus = new EventBusImpl();
    private final AtConfiguration configuration = new AtConfiguration("epid", null, null, "DEV");
    private final EdaGroupingIdFactory groupingIdFactory = new EdaGroupingIdFactory(configuration);
    @Mock
    private AtPermissionRequestRepository repository;
    @Mock
    private Outbox outbox;
    @Mock
    private EdaAdapter edaAdapter;
    @Captor
    private ArgumentCaptor<SimpleEvent> simpleEventCaptor;

    @Test
    void terminatePermission_edaThrows_emitsFailedToTerminate() throws TransmissionException {
        // given
        var start = LocalDate.now(ZoneOffset.UTC);
        var end = start.plusDays(10);
        doThrow(new TransmissionException(null)).when(edaAdapter).sendCMRevoke(any());
        var permissionRequest = new EdaPermissionRequestBuilder().setConnectionId("connectionId")
                                                                 .setPermissionId("pid")
                                                                 .setDataNeedId("dnid")
                                                                 .setCmRequestId("cmRequestId")
                                                                 .setConversationId("conversationId")
                                                                 .setMeteringPointId("mid")
                                                                 .setDsoId("dsoId")
                                                                 .setStart(start)
                                                                 .setEnd(end)
                                                                 .setGranularity(AllowedGranularity.PT15M)
                                                                 .setStatus(PermissionProcessStatus.ACCEPTED)
                                                                 .setMessage("")
                                                                 .setConsentId("consentId")
                                                                 .setCreated(ZonedDateTime.now(ZoneOffset.UTC))
                                                                 .setEnergyDirection(EnergyDirection.CONSUMPTION)
                                                                 .setParticipationFactor(1)
                                                                 .build();
        when(repository.findByPermissionId("pid")).thenReturn(Optional.of(permissionRequest));
        new TerminationHandler(outbox, eventBus, repository, configuration, groupingIdFactory, edaAdapter);
        // when
        eventBus.emit(new SimpleEvent("pid", PermissionProcessStatus.REQUIRES_EXTERNAL_TERMINATION));

        // then
        verify(outbox).commit(simpleEventCaptor.capture());
        var res = simpleEventCaptor.getValue();
        assertEquals(FAILED_TO_TERMINATE, res.status());
    }

    @Test
    void terminatePermission_unknownPermissionRequest_emitsNothing() {
        // given
        when(repository.findByPermissionId("pid")).thenReturn(Optional.empty());
        new TerminationHandler(outbox, eventBus, repository, configuration, groupingIdFactory, edaAdapter);
        // when
        eventBus.emit(new SimpleEvent("pid", PermissionProcessStatus.REQUIRES_EXTERNAL_TERMINATION));

        // then
        verify(outbox, never()).commit(any());
    }

    @Test
    void terminatePermission_revokeIsSent() throws TransmissionException {
        // given
        var start = LocalDate.now(ZoneOffset.UTC);
        var end = start.plusDays(10);
        var permissionRequest = new EdaPermissionRequestBuilder().setConnectionId("connectionId")
                                                                 .setPermissionId("pid")
                                                                 .setDataNeedId("dnid")
                                                                 .setCmRequestId("cmRequestId")
                                                                 .setConversationId("conversationId")
                                                                 .setMeteringPointId("mid")
                                                                 .setDsoId("dsoId")
                                                                 .setStart(start)
                                                                 .setEnd(end)
                                                                 .setGranularity(AllowedGranularity.PT15M)
                                                                 .setStatus(PermissionProcessStatus.ACCEPTED)
                                                                 .setMessage("")
                                                                 .setConsentId("consentId")
                                                                 .setCreated(ZonedDateTime.now(ZoneOffset.UTC))
                                                                 .setEnergyDirection(EnergyDirection.CONSUMPTION)
                                                                 .setParticipationFactor(1)
                                                                 .build();
        when(repository.findByPermissionId("pid")).thenReturn(Optional.of(permissionRequest));
        new TerminationHandler(outbox, eventBus, repository, configuration, groupingIdFactory, edaAdapter);
        // when
        eventBus.emit(new SimpleEvent("pid", PermissionProcessStatus.REQUIRES_EXTERNAL_TERMINATION));

        // then
        var revokeCaptor = ArgumentCaptor.forClass(CCMORevoke.class);
        verify(edaAdapter).sendCMRevoke(revokeCaptor.capture());
        assertTrue(revokeCaptor.getValue().messageId().startsWith("DEVepidT"));
    }
}
