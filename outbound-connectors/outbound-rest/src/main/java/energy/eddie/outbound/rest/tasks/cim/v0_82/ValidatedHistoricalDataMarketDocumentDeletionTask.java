package energy.eddie.outbound.rest.tasks.cim.v0_82;

import energy.eddie.outbound.rest.config.RestOutboundConnectorConfiguration;
import energy.eddie.outbound.rest.model.cim.v0_82.ValidatedHistoricalDataMarketDocumentModel;
import energy.eddie.outbound.rest.persistence.cim.v0_82.ValidatedHistoricalDataMarketDocumentRepository;
import energy.eddie.outbound.rest.persistence.specifications.InsertionTimeSpecification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

@Component
public class ValidatedHistoricalDataMarketDocumentDeletionTask {
    private final ValidatedHistoricalDataMarketDocumentRepository repository;
    private final RestOutboundConnectorConfiguration restConfig;

    public ValidatedHistoricalDataMarketDocumentDeletionTask(
            ValidatedHistoricalDataMarketDocumentRepository repository,
            RestOutboundConnectorConfiguration restConfig
    ) {
        this.repository = repository;
        this.restConfig = restConfig;
    }

    @Scheduled(cron = "${outbound-connector.rest.retention-removal:0 0 * * * *}")
    public void delete() {
        var timestamp = ZonedDateTime.now(ZoneOffset.UTC).plus(restConfig.retentionTime());
        var where = InsertionTimeSpecification.<ValidatedHistoricalDataMarketDocumentModel>insertedBeforeEquals(
                timestamp);
        repository.delete(where);
    }
}
