// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.metric.service;

import energy.eddie.api.agnostic.outbound.PermissionEventRepositories;
import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.api.agnostic.process.model.events.PermissionEventRepository;
import energy.eddie.cim.agnostic.ConnectionStatusMessage;
import energy.eddie.cim.agnostic.DataSourceInformation;
import energy.eddie.cim.agnostic.PermissionProcessStatus;
import energy.eddie.cim.agnostic.SimpleDataSourceInformation;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.outbound.metric.connectors.AgnosticConnector;
import energy.eddie.outbound.metric.model.PermissionRequestStatusDurationModel;
import energy.eddie.outbound.metric.repositories.PermissionRequestMetricsRepository;
import energy.eddie.outbound.metric.repositories.PermissionRequestStatusDurationRepository;
import energy.eddie.outbound.shared.testing.MockPermissionEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.publisher.TestPublisher;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionRequestMetricsServiceTest {

    private final DataSourceInformation dataSourceInformation = new SimpleDataSourceInformation(
            "AT",
            "at-eda",
            "mdaId",
            "paId"
    );
    @Mock
    private AgnosticConnector agnosticConnector;
    @Mock
    private PermissionRequestMetricsRepository metricsRepository;
    @Mock
    private PermissionRequestStatusDurationRepository statusDurationRepository;
    @Mock
    private DataNeedsService dataNeedsService;
    @Mock
    private PermissionEventRepositories repositories;
    @Mock
    private PermissionEventRepository permissionEventRepository;
    @Mock
    private DataNeed dataNeed;

    @Test
    void upsertMetricTest() {
        // Given
        var permissionId = "pid";
        var regionConnectorId = dataSourceInformation.regionConnectorId();
        var now = ZonedDateTime.now(ZoneOffset.UTC);

        var csm = mock(ConnectionStatusMessage.class);
        when(csm.status()).thenReturn(PermissionProcessStatus.VALIDATED);
        when(csm.timestamp()).thenReturn(now);
        when(csm.permissionId()).thenReturn("pid");
        when(csm.dataNeedId()).thenReturn("dnId");
        when(csm.dataSourceInformation()).thenReturn(dataSourceInformation);

        var prevEvent = new MockPermissionEvent(permissionId, PermissionProcessStatus.CREATED, now.minusSeconds(2));
        var currEvent = new MockPermissionEvent(permissionId, PermissionProcessStatus.VALIDATED);
        List<PermissionEvent> permissionEvents = List.of(currEvent, prevEvent);

        when(permissionEventRepository.findTop2ByPermissionIdAndEventCreatedLessThanEqualOrderByEventCreatedDesc(
                permissionId,
                now)).thenReturn(permissionEvents);
        when(repositories.getPermissionEventRepositoryByRegionConnectorId(regionConnectorId))
                .thenReturn(Optional.of(permissionEventRepository));
        when(dataNeed.type()).thenReturn("dnType");
        when(dataNeedsService.getById("dnId")).thenReturn(dataNeed);

        when(metricsRepository.getPermissionRequestMetrics(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(statusDurationRepository.getMedianDurationMilliseconds(any(), any(), any()))
                .thenReturn(100.0);

        TestPublisher<ConnectionStatusMessage> csmPublisher = TestPublisher.create();
        when(agnosticConnector.getConnectionStatusMessageStream()).thenReturn(csmPublisher.flux());
        PermissionRequestMetricsService service = new PermissionRequestMetricsService(
                agnosticConnector,
                metricsRepository,
                statusDurationRepository,
                dataNeedsService,
                repositories
        );

        // When
        service.upsertMetric(csm);

        // Then
        verify(statusDurationRepository).save(any(PermissionRequestStatusDurationModel.class));
        verify(metricsRepository).upsertPermissionRequestMetric(assertArg(pr -> assertAll(
                                                                        () -> assertEquals(100.0, pr.getMedian()),
                                                                        () -> assertEquals(1, pr.getPermissionRequestCount()),
                                                                        () -> assertEquals(PermissionProcessStatus.CREATED, pr.getPermissionRequestStatus()),
                                                                        () -> assertEquals("dnType", pr.getDataNeedType())
                                                                ))
        );
    }

    @Test
    void upsertMetric_return() {
        // Given
        TestPublisher<ConnectionStatusMessage> csmPublisher = TestPublisher.create();
        when(agnosticConnector.getConnectionStatusMessageStream()).thenReturn(csmPublisher.flux());

        @SuppressWarnings("unused")
        PermissionRequestMetricsService permissionRequestMetricsService = new PermissionRequestMetricsService(
                agnosticConnector,
                metricsRepository,
                statusDurationRepository,
                dataNeedsService,
                repositories
        );

        // When
        var csm = new ConnectionStatusMessage(
                "cid",
                "pid",
                "dnId",
                dataSourceInformation,
                PermissionProcessStatus.CREATED
        );
        csmPublisher.emit(csm);

        // Then
        verifyNoInteractions(statusDurationRepository, metricsRepository, repositories);
    }
}
