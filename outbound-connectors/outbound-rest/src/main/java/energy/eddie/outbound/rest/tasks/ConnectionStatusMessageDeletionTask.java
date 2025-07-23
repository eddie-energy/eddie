package energy.eddie.outbound.rest.tasks;

import energy.eddie.outbound.rest.config.RestOutboundConnectorConfiguration;
import energy.eddie.outbound.rest.model.ConnectionStatusMessageModel;
import energy.eddie.outbound.rest.persistence.ConnectionStatusMessageRepository;
import energy.eddie.outbound.rest.persistence.specifications.InsertionTimeSpecification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

@Component
public class ConnectionStatusMessageDeletionTask {
    private final ConnectionStatusMessageRepository repository;
    private final RestOutboundConnectorConfiguration restConfig;

    public ConnectionStatusMessageDeletionTask(
            ConnectionStatusMessageRepository repository,
            RestOutboundConnectorConfiguration restConfig
    ) {
        this.repository = repository;
        this.restConfig = restConfig;
    }

    @Scheduled(cron = "${outbound-connector.rest.retention-removal:0 0 * * * *}")
    public void deleteConnectionStatusMessages() {
        var timestamp = ZonedDateTime.now(ZoneOffset.UTC).plus(restConfig.retentionTime());
        var where = InsertionTimeSpecification.<ConnectionStatusMessageModel>insertedBeforeEquals(timestamp);
        repository.delete(where);
    }
}
