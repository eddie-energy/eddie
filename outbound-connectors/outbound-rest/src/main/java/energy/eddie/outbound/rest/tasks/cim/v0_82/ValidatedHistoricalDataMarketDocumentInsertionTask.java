package energy.eddie.outbound.rest.tasks.cim.v0_82;

import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataEnvelope;
import energy.eddie.outbound.rest.connectors.cim.v0_82.CimConnector;
import energy.eddie.outbound.rest.model.cim.v0_82.ValidatedHistoricalDataMarketDocumentModel;
import energy.eddie.outbound.rest.persistence.cim.v0_82.ValidatedHistoricalDataMarketDocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ValidatedHistoricalDataMarketDocumentInsertionTask {
    private static final Logger LOGGER = LoggerFactory.getLogger(ValidatedHistoricalDataMarketDocumentInsertionTask.class);
    private final ValidatedHistoricalDataMarketDocumentRepository repository;

    public ValidatedHistoricalDataMarketDocumentInsertionTask(
            ValidatedHistoricalDataMarketDocumentRepository repository,
            CimConnector cimConnector
    ) {
        this.repository = repository;
        cimConnector.getHistoricalDataMarketDocumentStream()
                    .subscribe(this::insert);
    }

    public void insert(ValidatedHistoricalDataEnvelope envelope) {
        LOGGER.info("Inserting validated historical data market document");
        repository.save(new ValidatedHistoricalDataMarketDocumentModel(envelope));
    }
}
