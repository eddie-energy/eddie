package energy.eddie.outbound.metric.service;

import energy.eddie.api.agnostic.ConnectionStatusMessage;
import energy.eddie.api.agnostic.outbound.PermissionEventRepositories;
import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.api.agnostic.process.model.events.PermissionEventRepository;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.outbound.metric.connectors.AgnosticConnector;
import energy.eddie.outbound.metric.model.PermissionRequestStatusDurationModel;
import energy.eddie.outbound.metric.repositories.PermissionRequestMetricsRepository;
import energy.eddie.outbound.metric.repositories.PermissionRequestStatusDurationRepository;
import energy.eddie.outbound.shared.testing.MockDataSourceInformation;
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

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PermissionRequestMetricsServiceTest {

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
    private ConnectionStatusMessage csm;

    @Mock
    private DataNeed dataNeed;

    private final MockDataSourceInformation dataSourceInformation = new MockDataSourceInformation(
            "AT",
            "at-eda",
            "paId",
            "mdaId"
    );

    @Test
    void upsertMetricTest() {
        // Given
        var permissionId = "pid";
        var regionConnectorId = dataSourceInformation.regionConnectorId();
        var now = ZonedDateTime.now(ZoneOffset.UTC);

        when(csm.status()).thenReturn(PermissionProcessStatus.VALIDATED);
        when(csm.timestamp()).thenReturn(now);
        when(csm.permissionId()).thenReturn("pid");
        when(csm.dataNeedId()).thenReturn("dnId");
        when(csm.dataSourceInformation()).thenReturn(dataSourceInformation);

        var prevEvent = new MockPermissionEvent(permissionId, PermissionProcessStatus.CREATED, now.minusSeconds(2));
        var currEvent = new MockPermissionEvent(permissionId, PermissionProcessStatus.VALIDATED);
        List<PermissionEvent> permissionEvents = List.of(currEvent, prevEvent);

        when(permissionEventRepository.findTop2ByPermissionIdAndEventCreatedLessThanEqualOrderByEventCreatedDesc(permissionId,
                now)).thenReturn(permissionEvents);
        when(repositories.getPermissionEventRepositoryByRegionConnectorId(regionConnectorId))
                .thenReturn(permissionEventRepository);
        when(dataNeed.type()).thenReturn("dnType");
        when(dataNeedsService.getById("dnId")).thenReturn(dataNeed);

        when(metricsRepository.getPermissionRequestMetrics(any(), any(), any(), any(), any()))
                .thenReturn(Optional.empty());
        when(statusDurationRepository.getMedianDurationMilliseconds(any(), any(), any(), any(), any()))
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
        verify(metricsRepository).upsertPermissionRequestMetric(
                anyDouble(),
                eq(100.0),
                eq(1),
                eq(PermissionProcessStatus.CREATED.name()),
                eq("dnType"),
                eq("paId"),
                eq("at-eda"),
                eq("AT")
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
