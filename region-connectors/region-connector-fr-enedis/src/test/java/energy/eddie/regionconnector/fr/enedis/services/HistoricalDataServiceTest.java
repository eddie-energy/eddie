// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.services;

import energy.eddie.regionconnector.fr.enedis.api.FrEnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.permission.request.EnedisPermissionRequestBuilder;
import energy.eddie.regionconnector.fr.enedis.persistence.FrPermissionRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HistoricalDataServiceTest {
    @Mock
    private FrPermissionRequestRepository permissionRequestRepository;
    @Mock
    private PollingService pollingService;
    @InjectMocks
    private HistoricalDataService historicalDataService;

    @Test
    void fetchHistoricalMeterReadings_forActiveRequest_fetches() {
        // Given
        LocalDate start = LocalDate.now(ZoneOffset.UTC).minusDays(20);
        LocalDate end = LocalDate.now(ZoneOffset.UTC).minusDays(10);
        FrEnedisPermissionRequest request = new EnedisPermissionRequestBuilder()
                .setPermissionId("pid")
                .setStart(start)
                .setEnd(end)
                .create();
        when(permissionRequestRepository.getByPermissionId("pid")).thenReturn(request);
        when(pollingService.isActiveAndNeedsToBeFetched(request)).thenCallRealMethod();

        // When
        historicalDataService.fetchHistoricalMeterReadings("pid");

        // Then
        verify(pollingService).pollTimeSeriesData(request);
    }


    @Test
    void fetchHistoricalMeterReadings_doesNothing_IfPermissionIsNotActive() {
        // Given
        LocalDate start = LocalDate.now(ZoneOffset.UTC).plusDays(20);
        LocalDate end = LocalDate.now(ZoneOffset.UTC).plusDays(10);
        FrEnedisPermissionRequest request = new EnedisPermissionRequestBuilder()
                .setPermissionId("pid")
                .setStart(start)
                .setEnd(end)
                .create();
        when(pollingService.isActiveAndNeedsToBeFetched(request)).thenCallRealMethod();
        when(permissionRequestRepository.getByPermissionId("pid")).thenReturn(request);

        // When
        historicalDataService.fetchHistoricalMeterReadings("pid");

        // Then
        verify(pollingService, never()).pollTimeSeriesData(any());
    }
}
