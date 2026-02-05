// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.handlers;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.at.api.AtPermissionRequestRepository;
import energy.eddie.regionconnector.at.eda.EdaAdapter;
import energy.eddie.regionconnector.at.eda.TransmissionException;
import energy.eddie.regionconnector.at.eda.config.AtConfiguration;
import energy.eddie.regionconnector.at.eda.permission.request.EdaPermissionRequest;
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

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;

import static energy.eddie.api.v0.PermissionProcessStatus.FAILED_TO_TERMINATE;
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
    @Captor
    private ArgumentCaptor<SimpleEvent> simpleEventCaptor;

    @Test
    void terminatePermission_edaThrows_emitsFailedToTerminate() throws TransmissionException {
        // given
        var start = LocalDate.now(ZoneOffset.UTC);
        var end = start.plusDays(10);
        doThrow(new TransmissionException(null)).when(edaAdapter).sendCMRevoke(any());
        var permissionRequest = new EdaPermissionRequest("connectionId", "pid", "dnid", "cmRequestId",
                                                         "conversationId", "mid", "dsoId", start, end,
                                                         AllowedGranularity.PT15M,
                                                         PermissionProcessStatus.ACCEPTED, "",
                                                         "consentId", ZonedDateTime.now(ZoneOffset.UTC));
        when(repository.findByPermissionId("pid")).thenReturn(Optional.of(permissionRequest));
        new TerminationHandler(outbox, eventBus, repository, new AtConfiguration("epid", null), edaAdapter);
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
        new TerminationHandler(outbox, eventBus, repository, new AtConfiguration("epid", null), edaAdapter);
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
        var permissionRequest = new EdaPermissionRequest("connectionId", "pid", "dnid", "cmRequestId",
                                                         "conversationId", "mid", "dsoId", start, end,
                                                         AllowedGranularity.PT15M,
                                                         PermissionProcessStatus.ACCEPTED, "",
                                                         "consentId", ZonedDateTime.now(ZoneOffset.UTC));
        when(repository.findByPermissionId("pid")).thenReturn(Optional.of(permissionRequest));
        new TerminationHandler(outbox, eventBus, repository, new AtConfiguration("epid", null), edaAdapter);
        // when
        eventBus.emit(new SimpleEvent("pid", PermissionProcessStatus.REQUIRES_EXTERNAL_TERMINATION));

        // then
        verify(edaAdapter).sendCMRevoke(any());
    }
}
