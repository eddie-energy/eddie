// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.tasks;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.at.api.AtPermissionRequestProjection;
import energy.eddie.regionconnector.at.api.AtPermissionRequestRepository;
import energy.eddie.regionconnector.at.eda.ponton.messages.ecmplist.TestEdaECMPList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdentifyECMPListTaskTest {
    @Mock
    private AtPermissionRequestRepository repository;
    @Mock
    private AtPermissionRequestProjection projection;
    @InjectMocks
    private IdentifyECMPListTask task;

    @Test
    void givenNoPermissionRequest_whenIdentifyingECMPList_thenReturnOptionalEmpty() {
        // Given
        when(repository.findByConversationIdOrCMRequestId("conversationId", null))
                .thenReturn(List.of());
        var ecmpList = new TestEdaECMPList("messageId", "conversationId", "ecId");

        // When
        var res = task.identify(ecmpList);

        // Then
        assertThat(res).isEmpty();
    }

    @Test
    void givenPermissionRequest_whenIdentifyingECMPList_thenReturnIdentifiedECMPList() {
        // Given
        when(projection.getStatus()).thenReturn(PermissionProcessStatus.ACCEPTED.name());
        when(projection.getCreated()).thenReturn(Instant.now(Clock.systemUTC()));
        when(repository.findByConversationIdOrCMRequestId("conversationId", null))
                .thenReturn(List.of(projection));
        var ecmpList = new TestEdaECMPList("messageId", "conversationId", "ecId");

        // When
        var res = task.identify(ecmpList);

        // Then
        assertThat(res).isPresent();
    }
}