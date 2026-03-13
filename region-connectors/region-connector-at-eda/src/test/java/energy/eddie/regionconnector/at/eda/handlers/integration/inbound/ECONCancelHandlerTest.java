// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.handlers.integration.inbound;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.at.api.AtPermissionRequestProjection;
import energy.eddie.regionconnector.at.api.AtPermissionRequestRepository;
import energy.eddie.regionconnector.at.eda.models.CMRequestStatus;
import energy.eddie.regionconnector.at.eda.permission.request.events.EdaAnswerEvent;
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

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ECONCancelHandlerTest {
    @Mock
    private AtPermissionRequestRepository repository;
    @Mock
    private Outbox outbox;
    @Captor
    private ArgumentCaptor<EdaAnswerEvent> eventCaptor;
    @InjectMocks
    private ECONCancelHandler handler;

    @Test
    void givenECONCancel_whenHandleECONCancel_thenRevokesPermissionRequest() {
        // Given
        var pr = projection();
        when(repository.findByConversationIdOrCMRequestId("conversationId", null)).thenReturn(List.of(pr));
        var status = new CMRequestStatus(NotificationMessageType.ECON_CANCEL,
                                         "conversationId",
                                         "Zustimmung wurde entzogen");

        // When
        handler.handleECONCancel(status);

        // Then
        verify(outbox).commit(eventCaptor.capture());
        var res = eventCaptor.getValue();
        assertEquals(PermissionProcessStatus.REVOKED, res.status());
    }

    private static AtPermissionRequestProjection projection() {
        return new AtPermissionRequestProjectionTest(
                "pid",
                "connectionId",
                "cmRequestId",
                "conversationId",
                LocalDate.now(ZoneId.systemDefault()),
                LocalDate.now(ZoneId.systemDefault()),
                "dnid",
                "dsoId",
                "meteringPointId",
                "consentId",
                "message",
                AllowedGranularity.PT15M.name(),
                PermissionProcessStatus.ACCEPTED.name(),
                Instant.now(Clock.systemUTC())
        );
    }
}