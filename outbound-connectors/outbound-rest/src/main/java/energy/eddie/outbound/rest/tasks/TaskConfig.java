package energy.eddie.outbound.rest.tasks;

import energy.eddie.api.agnostic.ConnectionStatusMessage;
import energy.eddie.cim.v0_82.pmd.PermissionEnvelope;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataEnvelope;
import energy.eddie.outbound.rest.config.RestOutboundConnectorConfiguration;
import energy.eddie.outbound.rest.connectors.AgnosticConnector;
import energy.eddie.outbound.rest.connectors.cim.v0_82.CimConnector;
import energy.eddie.outbound.rest.model.ConnectionStatusMessageModel;
import energy.eddie.outbound.rest.model.cim.v0_82.PermissionMarketDocumentModel;
import energy.eddie.outbound.rest.model.cim.v0_82.ValidatedHistoricalDataMarketDocumentModel;
import energy.eddie.outbound.rest.persistence.ConnectionStatusMessageRepository;
import energy.eddie.outbound.rest.persistence.cim.v0_82.PermissionMarketDocumentRepository;
import energy.eddie.outbound.rest.persistence.cim.v0_82.ValidatedHistoricalDataMarketDocumentRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class TaskConfig {
    @Bean
    DeletionTask<ValidatedHistoricalDataMarketDocumentModel> vhdDeletionTask(
            ValidatedHistoricalDataMarketDocumentRepository repository,
            RestOutboundConnectorConfiguration config
    ) {
        return new DeletionTask<>(repository, config);
    }

    @Bean
    InsertionTask<ValidatedHistoricalDataEnvelope, ValidatedHistoricalDataMarketDocumentModel> vhdInsertionTask(
            CimConnector cimConnector,
            ValidatedHistoricalDataMarketDocumentRepository repository
    ) {
        return new InsertionTask<>(cimConnector.getHistoricalDataMarketDocumentStream(),
                                   repository,
                                   ValidatedHistoricalDataMarketDocumentModel::new);
    }

    @Bean
    DeletionTask<PermissionMarketDocumentModel> pmdDeletionTask(
            PermissionMarketDocumentRepository repository,
            RestOutboundConnectorConfiguration config
    ) {
        return new DeletionTask<>(repository, config);
    }

    @Bean
    InsertionTask<PermissionEnvelope, PermissionMarketDocumentModel> pmdInsertionTask(
            CimConnector cimConnector,
            PermissionMarketDocumentRepository repository
    ) {
        return new InsertionTask<>(cimConnector.getPermissionMarketDocumentStream(),
                                   repository,
                                   PermissionMarketDocumentModel::new);
    }

    @Bean
    DeletionTask<ConnectionStatusMessageModel> csmDeletionTask(
            ConnectionStatusMessageRepository repository,
            RestOutboundConnectorConfiguration config
    ) {
        return new DeletionTask<>(repository, config);
    }

    @Bean
    InsertionTask<ConnectionStatusMessage, ConnectionStatusMessageModel> csmInsertionTask(
            AgnosticConnector agnosticConnector,
            ConnectionStatusMessageRepository repository
    ) {
        return new InsertionTask<>(agnosticConnector.getConnectionStatusMessageStream(),
                                   repository,
                                   ConnectionStatusMessageModel::new);
    }
}
