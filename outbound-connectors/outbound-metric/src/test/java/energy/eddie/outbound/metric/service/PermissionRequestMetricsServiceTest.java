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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.publisher.TestPublisher;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

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

    private final MockDataSourceInformation dataSourceInformation = new MockDataSourceInformation("AT",
            "at-eda", "paId", "mdaId");

    @Test
    void getCurrentAndPreviousPermissionEvents_returnsTwoEvents() {
        // Given
        var permissionId = "pid";
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        var countryCode = dataSourceInformation.countryCode();

        PermissionEvent prevEvent = mock(PermissionEvent.class);
        PermissionEvent currentEvent = mock(PermissionEvent.class);

        PermissionEventRepository repo = mock(PermissionEventRepository.class);
        when(repo.findTop2ByPermissionIdAndEventCreatedLessThanEqualOrderByEventCreatedDesc(permissionId, now))
                .thenReturn(List.of(prevEvent, currentEvent));
        when(repositories.getPermissionEventRepositoryByCountryCode(countryCode)).thenReturn(repo);

        TestPublisher<ConnectionStatusMessage> csmPublisher = TestPublisher.create();
        when(agnosticConnector.getConnectionStatusMessageStream()).thenReturn(csmPublisher.flux());
        PermissionRequestMetricsService service = new PermissionRequestMetricsService(agnosticConnector,
                metricsRepository, statusDurationRepository, dataNeedsService, repositories);

        // When
        List<PermissionEvent> result = service.getCurrentAndPreviousPermissionEvents(permissionId, now, countryCode);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(prevEvent, currentEvent);
    }

    @Test
    void upsertMetricTest() {
        // Given
        var csm = mock(ConnectionStatusMessage.class);
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        var countryCode = dataSourceInformation.countryCode();

        when(csm.status()).thenReturn(PermissionProcessStatus.VALIDATED);
        when(csm.timestamp()).thenReturn(now);
        when(csm.permissionId()).thenReturn("pid");
        when(csm.dataNeedId()).thenReturn("dnId");
        when(csm.dataSourceInformation()).thenReturn(dataSourceInformation);

        var prevEvent = mock(PermissionEvent.class);
        var currEvent = mock(PermissionEvent.class);
        when(prevEvent.eventCreated()).thenReturn(now.minusSeconds(2));
        when(currEvent.eventCreated()).thenReturn(now);
        when(prevEvent.status()).thenReturn(PermissionProcessStatus.CREATED);

        var permissionEvents = List.of(currEvent, prevEvent);
        when(metricsRepository.getPermissionRequestMetrics(any(), any(), any(), any(), any()))
                .thenReturn(Optional.empty());
        when(statusDurationRepository.getMedianDurationMilliseconds(any(), any(), any(), any(), any()))
                .thenReturn(100.0);

        DataNeed dataNeed = mock(DataNeed.class);
        when(dataNeed.type()).thenReturn("dnType");
        when(dataNeedsService.findById("dnId")).thenReturn(Optional.of(dataNeed));

        TestPublisher<ConnectionStatusMessage> csmPublisher = TestPublisher.create();
        when(agnosticConnector.getConnectionStatusMessageStream()).thenReturn(csmPublisher.flux());
        PermissionRequestMetricsService service = Mockito.spy(new PermissionRequestMetricsService(agnosticConnector,
                metricsRepository, statusDurationRepository, dataNeedsService, repositories));
        doReturn(permissionEvents).when(service).getCurrentAndPreviousPermissionEvents("pid", now, countryCode);

        // When
        service.upsertMetric(csm);

        // Then
        verify(statusDurationRepository).save(any(PermissionRequestStatusDurationModel.class));
        verify(metricsRepository).upsertPermissionRequestMetric(anyDouble(),eq(100.0), eq(1),
                eq(PermissionProcessStatus.CREATED.name()), eq("dnType"), eq("paId"),
                eq("at-eda"), eq(countryCode));
    }

    @Test
    void upsertMetric_return() {
        // Given
        TestPublisher<ConnectionStatusMessage> csmPublisher = TestPublisher.create();
        when(agnosticConnector.getConnectionStatusMessageStream()).thenReturn(csmPublisher.flux());

        @SuppressWarnings("unused")
        PermissionRequestMetricsService permissionRequestMetricsService = new PermissionRequestMetricsService(
                agnosticConnector, metricsRepository, statusDurationRepository, dataNeedsService, repositories);

        // When
        var csm = new ConnectionStatusMessage("cid", "pid", "dnId",
                dataSourceInformation, PermissionProcessStatus.CREATED);
        csmPublisher.emit(csm);

        // Then
        verifyNoInteractions(statusDurationRepository, metricsRepository, repositories);
    }
}
