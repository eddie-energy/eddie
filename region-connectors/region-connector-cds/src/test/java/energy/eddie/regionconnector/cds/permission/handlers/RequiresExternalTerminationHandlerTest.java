// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.permission.handlers;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.cds.permission.events.SimpleEvent;
import energy.eddie.regionconnector.cds.permission.requests.CdsPermissionRequestBuilder;
import energy.eddie.regionconnector.cds.persistence.CdsPermissionRequestRepository;
import energy.eddie.regionconnector.cds.services.TerminationService;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.EventBusImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequiresExternalTerminationHandlerTest {
    @Spy
    private final EventBus eventBus = new EventBusImpl();
    @Mock
    private CdsPermissionRequestRepository repository;
    @Mock
    private TerminationService terminationService;
    @InjectMocks
    @SuppressWarnings("unused")
    private RequiresExternalTerminationHandler handler;

    @Test
    void testAccept_terminatesPermissionRequest() {
        // Given
        var event = new SimpleEvent("pid", PermissionProcessStatus.REQUIRES_EXTERNAL_TERMINATION);
        var pr = new CdsPermissionRequestBuilder()
                .build();
        when(repository.getByPermissionId("pid")).thenReturn(pr);

        // When
        eventBus.emit(event);

        // Then
        verify(terminationService).terminate(pr);
    }
}