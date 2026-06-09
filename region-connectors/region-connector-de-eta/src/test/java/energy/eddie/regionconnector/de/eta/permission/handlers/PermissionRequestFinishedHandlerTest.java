// SPDX-FileCopyrightText: 2026 The ETA+ Developers <bilal.sakhawat@etaplus.energy>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.de.eta.permission.handlers;

import energy.eddie.cim.agnostic.PermissionProcessStatus;
import energy.eddie.regionconnector.de.eta.permission.request.events.SimpleEvent;
import energy.eddie.regionconnector.de.eta.persistence.DePermissionCredentialsRepository;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionRequestFinishedHandlerTest {

    private static final String PERMISSION_ID = "perm-1";

    @Mock
    private DePermissionCredentialsRepository repository;

    @Mock
    private EventBus eventBus;

    private PermissionRequestFinishedHandler handler;

    @BeforeEach
    void setUp() {
        when(eventBus.filteredFlux(PermissionProcessStatus.TERMINATED)).thenReturn(Flux.empty());
        when(eventBus.filteredFlux(PermissionProcessStatus.EXTERNALLY_TERMINATED)).thenReturn(Flux.empty());
        when(eventBus.filteredFlux(PermissionProcessStatus.FULFILLED)).thenReturn(Flux.empty());
        when(eventBus.filteredFlux(PermissionProcessStatus.UNFULFILLABLE)).thenReturn(Flux.empty());
        when(eventBus.filteredFlux(PermissionProcessStatus.REVOKED)).thenReturn(Flux.empty());
        handler = new PermissionRequestFinishedHandler(eventBus, repository);
    }

    @Test
    void accept_deletesCredentials_onTerminated() {
        handler.accept(new SimpleEvent(PERMISSION_ID, PermissionProcessStatus.TERMINATED));
        verify(repository).deleteByPermissionId(PERMISSION_ID);
    }

    @Test
    void accept_deletesCredentials_onExternallyTerminated() {
        handler.accept(new SimpleEvent(PERMISSION_ID, PermissionProcessStatus.EXTERNALLY_TERMINATED));
        verify(repository).deleteByPermissionId(PERMISSION_ID);
    }

    @Test
    void accept_deletesCredentials_onFulfilled() {
        handler.accept(new SimpleEvent(PERMISSION_ID, PermissionProcessStatus.FULFILLED));
        verify(repository).deleteByPermissionId(PERMISSION_ID);
    }

    @Test
    void accept_deletesCredentials_onUnfulfillable() {
        handler.accept(new SimpleEvent(PERMISSION_ID, PermissionProcessStatus.UNFULFILLABLE));
        verify(repository).deleteByPermissionId(PERMISSION_ID);
    }

    @Test
    void accept_deletesCredentials_onRevoked() {
        handler.accept(new SimpleEvent(PERMISSION_ID, PermissionProcessStatus.REVOKED));
        verify(repository).deleteByPermissionId(PERMISSION_ID);
    }
}