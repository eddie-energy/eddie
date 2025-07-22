package energy.eddie.outbound.rest.tasks;

import energy.eddie.outbound.rest.config.RestOutboundConnectorConfiguration;
import energy.eddie.outbound.rest.persistence.ConnectionStatusMessageRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static energy.eddie.outbound.rest.persistence.specifications.ConnectionStatusMessageSpecification.insertedBeforeEquals;

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
        var where = insertedBeforeEquals(ZonedDateTime.now(ZoneOffset.UTC).plus(restConfig.retentionTime()));
        repository.delete(where);
    }
}
