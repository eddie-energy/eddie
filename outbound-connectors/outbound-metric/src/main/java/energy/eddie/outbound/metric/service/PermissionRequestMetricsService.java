package energy.eddie.outbound.metric.service;

import energy.eddie.api.agnostic.ConnectionStatusMessage;
import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.outbound.metric.connectors.AgnosticConnector;
import energy.eddie.outbound.metric.model.PermissionRequestMetricsModel;
import energy.eddie.outbound.metric.model.PermissionRequestStatusDurationModel;
import energy.eddie.outbound.metric.repositories.PermissionRequestMetricsRepository;
import energy.eddie.outbound.metric.repositories.PermissionRequestStatusDurationRepository;
import energy.eddie.outbound.metric.repositories.PermissionRequestTimestampRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
public class PermissionRequestMetricsService {

    private final AgnosticConnector connector;
    private final PermissionRequestMetricsRepository metricsRepository;
    private final PermissionRequestStatusDurationRepository statusDurationRepository;
    private final PermissionRequestTimestampRepository timestampRepository;
    private static final String PERMISSION_EVENT_TABLE = ".permission_event";

    public PermissionRequestMetricsService(AgnosticConnector connector,
                                           PermissionRequestMetricsRepository metricsRepository,
                                           PermissionRequestStatusDurationRepository statusDurationRepository,
                                           PermissionRequestTimestampRepository timestampRepository) {
        this.connector = connector;
        this.metricsRepository = metricsRepository;
        this.statusDurationRepository = statusDurationRepository;
        this.timestampRepository = timestampRepository;
        connector.getConnectionStatusMessageStream().subscribe(this::upsertMetric);
    }

    public void upsertMetric(ConnectionStatusMessage csm) {

        PermissionProcessStatus status = csm.status();
        if(status.equals(PermissionProcessStatus.CREATED)) {
            return;
        }

        String dataNeedId = csm.dataNeedId();
        String permissionAdministratorId = csm.dataSourceInformation().permissionAdministratorId();
        String regionConnectorId = csm.dataSourceInformation().regionConnectorId();
        String countryCode = csm.dataSourceInformation().countryCode();
        String permissionId = csm.permissionId();
        String tableName = regionConnectorId.replace('-', '_') + PERMISSION_EVENT_TABLE;
        PermissionEvent prevPermissionEvent = timestampRepository.getPermissionRequestTimestamp(permissionId, tableName);
        long durationMilliseconds = Duration.between(prevPermissionEvent.eventCreated(), csm.timestamp()).toMillis();

        PermissionProcessStatus prevEventStatus = prevPermissionEvent.status();
        PermissionRequestStatusDurationModel prStatusDuration =  new PermissionRequestStatusDurationModel(permissionId,
            prevEventStatus, durationMilliseconds, dataNeedId, permissionAdministratorId, regionConnectorId, countryCode);
        statusDurationRepository.save(prStatusDuration);

        Optional<PermissionRequestMetricsModel> prMetrics = metricsRepository.getPermissionRequestMetrics(prevEventStatus,
                dataNeedId, permissionAdministratorId, regionConnectorId, countryCode);

        int currentCount = prMetrics.map(PermissionRequestMetricsModel::getPermissionRequestCount).orElse(0);
        int newCount = currentCount + 1;

        double currentMean = prMetrics.map(PermissionRequestMetricsModel::getMean).orElse(0.0);
        double newMean = ((currentMean * currentCount) + (double) durationMilliseconds) / (double) newCount;

        double median = statusDurationRepository.getMedianDurationMilliseconds(prevEventStatus.name(), dataNeedId,
                permissionAdministratorId, regionConnectorId, countryCode);

        metricsRepository.upsertPermissionRequestMetric(newMean, median, newCount, prevEventStatus.name(), dataNeedId,
                permissionAdministratorId, regionConnectorId, countryCode);
    }
}
