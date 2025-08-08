package energy.eddie.outbound.metric.service;

import energy.eddie.api.agnostic.ConnectionStatusMessage;
import energy.eddie.api.agnostic.outbound.PermissionEventRepositories;
import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.api.agnostic.process.model.events.PermissionEventRepository;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.outbound.metric.connectors.AgnosticConnector;
import energy.eddie.outbound.metric.model.PermissionRequestMetricsModel;
import energy.eddie.outbound.metric.model.PermissionRequestStatusDurationModel;
import energy.eddie.outbound.metric.repositories.PermissionRequestMetricsRepository;
import energy.eddie.outbound.metric.repositories.PermissionRequestStatusDurationRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class PermissionRequestMetricsService {

    private final PermissionRequestMetricsRepository metricsRepository;
    private final PermissionRequestStatusDurationRepository statusDurationRepository;
    private final DataNeedsService dataNeedsService;
    private final PermissionEventRepositories repositories;

    public PermissionRequestMetricsService(AgnosticConnector connector,
                                           PermissionRequestMetricsRepository metricsRepository,
                                           PermissionRequestStatusDurationRepository statusDurationRepository,
                                           @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
                                           DataNeedsService dataNeedsService,
                                           @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
                                           PermissionEventRepositories repositories) {
        this.metricsRepository = metricsRepository;
        this.statusDurationRepository = statusDurationRepository;
        this.dataNeedsService = dataNeedsService;
        this.repositories = repositories;
        connector.getConnectionStatusMessageStream().subscribe(this::upsertMetric);
    }

    public void upsertMetric(ConnectionStatusMessage csm) {

        PermissionProcessStatus status = csm.status();
        if(status.equals(PermissionProcessStatus.CREATED)) {
            return;
        }

        String countryCode = csm.dataSourceInformation().countryCode();
        String permissionId = csm.permissionId();
        ZonedDateTime eventCreated = csm.timestamp();
        List<PermissionEvent> permissionEvents = getCurrentAndPreviousPermissionEvents(permissionId, eventCreated,
                countryCode);
        ZonedDateTime currentPermissionEventCreated = permissionEvents.getFirst().eventCreated();
        PermissionEvent prevPermissionEvent = permissionEvents.get(1);
        long durationMilliseconds = Duration.between(prevPermissionEvent.eventCreated(), currentPermissionEventCreated)
                .toMillis();
        PermissionProcessStatus prevEventStatus = prevPermissionEvent.status();
        String dataNeedType = Objects.requireNonNull(dataNeedsService.findById(csm.dataNeedId())
                .map(DataNeed::type).orElse(null));
        String permissionAdministratorId = csm.dataSourceInformation().permissionAdministratorId();
        String regionConnectorId = csm.dataSourceInformation().regionConnectorId();
        PermissionRequestStatusDurationModel prStatusDuration =  new PermissionRequestStatusDurationModel(permissionId,
            prevEventStatus, durationMilliseconds, dataNeedType, permissionAdministratorId, regionConnectorId, countryCode);
        statusDurationRepository.save(prStatusDuration);
        Optional<PermissionRequestMetricsModel> prMetrics = metricsRepository.getPermissionRequestMetrics(prevEventStatus,
                dataNeedType, permissionAdministratorId, regionConnectorId, countryCode);

        int currentCount = prMetrics.map(PermissionRequestMetricsModel::getPermissionRequestCount).orElse(0);
        int newCount = currentCount + 1;
        double currentMean = prMetrics.map(PermissionRequestMetricsModel::getMean).orElse(0.0);
        double newMean = ((currentMean * currentCount) + (double) durationMilliseconds) / (double) newCount;
        double median = statusDurationRepository.getMedianDurationMilliseconds(prevEventStatus.name(), dataNeedType,
                permissionAdministratorId, regionConnectorId, countryCode);

        metricsRepository.upsertPermissionRequestMetric(newMean, median, newCount, prevEventStatus.name(), dataNeedType,
                permissionAdministratorId, regionConnectorId, countryCode);
    }

    public List<PermissionEvent> getCurrentAndPreviousPermissionEvents(String permissionId, ZonedDateTime eventCreated,
                                                                       String countryCode) {
        PermissionEventRepository repository = repositories.getPermissionEventRepositoryByCountryCode(
                countryCode);
        return repository.findTop2ByPermissionIdAndEventCreatedLessThanEqualOrderByEventCreatedDesc(permissionId,
                eventCreated);
    }
}
