// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.dk.energinet.permission.handler;

import energy.eddie.dataneeds.needs.AccountingPointDataNeed;
import energy.eddie.dataneeds.needs.TimeframedDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.dk.energinet.permission.events.DkAcceptedEvent;
import energy.eddie.regionconnector.dk.energinet.permission.request.EnerginetPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.permission.request.EnerginetPermissionRequestBuilder;
import energy.eddie.regionconnector.dk.energinet.persistence.DkPermissionRequestRepository;
import energy.eddie.regionconnector.dk.energinet.services.AccountingPointDetailsService;
import energy.eddie.regionconnector.dk.energinet.services.PollingService;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.EventBusImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AcceptanceHandlerTest {
    @Spy
    private final EventBus eventBus = new EventBusImpl();
    @Mock
    private PollingService pollingService;
    @Mock
    private AccountingPointDetailsService accountingPointDetailsService;
    @Mock
    private DkPermissionRequestRepository repository;
    @InjectMocks
    @SuppressWarnings("unused")
    private AcceptanceHandler acceptanceHandler;
    @Mock
    private DataNeedsService dataNeedsService;
    @Mock
    private TimeframedDataNeed timeframedDataNeed;
    @Mock
    private AccountingPointDataNeed accountingPointDataNeed;

    @Test
    void testAccept_triggersPolling() {
        // Given
        var pr = permissionRequest();
        when(dataNeedsService.findById("dnid")).thenReturn(Optional.of(timeframedDataNeed));
        when(repository.getByPermissionId("pid")).thenReturn(pr);
        // When
        eventBus.emit(new DkAcceptedEvent("pid", "access"));

        // Then
        verify(pollingService).fetchHistoricalMeterReadings(pr);
    }

    @Test
    void testAccept_triggersAccountingPointService() {
        // Given
        var pr = permissionRequest();
        when(dataNeedsService.findById("dnid")).thenReturn(Optional.of(accountingPointDataNeed));
        when(repository.getByPermissionId("pid")).thenReturn(pr);
        // When
        eventBus.emit(new DkAcceptedEvent("pid", "access"));

        // Then
        verify(accountingPointDetailsService).fetchMeteringPointDetails(pr);
    }

    private EnerginetPermissionRequest permissionRequest() {
        return new EnerginetPermissionRequestBuilder()
                .setDataNeedId("dnid")
                .build();
    }
}
