// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.tasks;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.cds.master.data.CdsServerBuilder;
import energy.eddie.regionconnector.cds.permission.requests.CdsPermissionRequestBuilder;
import energy.eddie.regionconnector.cds.persistence.CdsPermissionRequestRepository;
import energy.eddie.regionconnector.cds.persistence.CdsServerRepository;
import energy.eddie.regionconnector.cds.services.oauth.AuthorizationService;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RetrySendingTaskTest {
    @Mock
    private Outbox outbox;
    @Mock
    private CdsPermissionRequestRepository permissionRequestRepository;
    @Mock
    private AuthorizationService authorizationService;
    @Mock
    private CdsServerRepository cdsServerRepository;
    @InjectMocks
    private RetrySendingTask retrySendingTask;

    @Test
    void testRetry_retriesSendingPermissionRequests() {
        // Given
        var pr = new CdsPermissionRequestBuilder()
                .setStatus(PermissionProcessStatus.UNABLE_TO_SEND)
                .setPermissionId("pid")
                .setCdsServer(1)
                .build();
        var server = new CdsServerBuilder().build();
        when(permissionRequestRepository.findByStatus(PermissionProcessStatus.UNABLE_TO_SEND))
                .thenReturn(List.of(pr));
        when(cdsServerRepository.getReferenceById(1L))
                .thenReturn(server);

        // When
        retrySendingTask.retry();

        // Then
        verify(outbox).commit(assertArg(event -> assertEquals(PermissionProcessStatus.VALIDATED, event.status())));
        verify(authorizationService).createOAuthRequest(server, "pid");
    }
}