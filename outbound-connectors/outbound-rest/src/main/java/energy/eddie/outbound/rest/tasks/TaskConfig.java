// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.rest.tasks;

import energy.eddie.api.agnostic.ConnectionStatusMessage;
import energy.eddie.api.agnostic.RawDataMessage;
import energy.eddie.cim.v0_82.ap.AccountingPointEnvelope;
import energy.eddie.cim.v0_82.pmd.PermissionEnvelope;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataEnvelope;
import energy.eddie.cim.v1_04.rtd.RTDEnvelope;
import energy.eddie.cim.v1_04.vhd.VHDEnvelope;
import energy.eddie.outbound.rest.config.RestOutboundConnectorConfiguration;
import energy.eddie.outbound.rest.connectors.AgnosticConnector;
import energy.eddie.outbound.rest.connectors.cim.v0_82.CimConnector;
import energy.eddie.outbound.rest.connectors.cim.v1_04.CimConnectorV1_04;
import energy.eddie.outbound.rest.model.ConnectionStatusMessageModel;
import energy.eddie.outbound.rest.model.RawDataMessageModel;
import energy.eddie.outbound.rest.model.cim.v0_82.AccountingPointDataMarketDocumentModel;
import energy.eddie.outbound.rest.model.cim.v0_82.PermissionMarketDocumentModel;
import energy.eddie.outbound.rest.model.cim.v0_82.ValidatedHistoricalDataMarketDocumentModel;
import energy.eddie.outbound.rest.model.cim.v1_04.NearRealTimeDataMarketDocumentModel;
import energy.eddie.outbound.rest.model.cim.v1_04.ValidatedHistoricalDataMarketDocumentModelV1_04;
import energy.eddie.outbound.rest.persistence.ConnectionStatusMessageRepository;
import energy.eddie.outbound.rest.persistence.RawDataMessageRepository;
import energy.eddie.outbound.rest.persistence.cim.v0_82.AccountingPointDataMarketDocumentRepository;
import energy.eddie.outbound.rest.persistence.cim.v0_82.PermissionMarketDocumentRepository;
import energy.eddie.outbound.rest.persistence.cim.v0_82.ValidatedHistoricalDataMarketDocumentRepository;
import energy.eddie.outbound.rest.persistence.cim.v1_04.NearRealTImeDataMarketDocumentRepository;
import energy.eddie.outbound.rest.persistence.cim.v1_04.ValidatedHistoricalDataMarketDocumentV1_04Repository;
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
    DeletionTask<NearRealTimeDataMarketDocumentModel> rtdDeletionTask(
            NearRealTImeDataMarketDocumentRepository repository,
            RestOutboundConnectorConfiguration config
    ) {
        return new DeletionTask<>(repository, config);
    }

    @Bean
    InsertionTask<RTDEnvelope, NearRealTimeDataMarketDocumentModel> rtdInsertionTask(
            CimConnectorV1_04 cimConnector,
            NearRealTImeDataMarketDocumentRepository repository
    ) {
        return new InsertionTask<>(cimConnector.getNearRealTimeDataMarketDocumentStream(),
                                   repository,
                                   NearRealTimeDataMarketDocumentModel::new);
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
    DeletionTask<AccountingPointDataMarketDocumentModel> apDeletionTask(
            AccountingPointDataMarketDocumentRepository repository,
            RestOutboundConnectorConfiguration config
    ) {
        return new DeletionTask<>(repository, config);
    }

    @Bean
    InsertionTask<AccountingPointEnvelope, AccountingPointDataMarketDocumentModel> apInsertionTask(
            CimConnector cimConnector,
            AccountingPointDataMarketDocumentRepository repository
    ) {
        return new InsertionTask<>(cimConnector.getAccountingPointDataMarketDocumentStream(),
                                   repository,
                                   AccountingPointDataMarketDocumentModel::new);
    }

    @Bean
    DeletionTask<ValidatedHistoricalDataMarketDocumentModelV1_04> vhdV104DeletionTask(
            ValidatedHistoricalDataMarketDocumentV1_04Repository repository,
            RestOutboundConnectorConfiguration config
    ) {
        return new DeletionTask<>(repository, config);
    }

    @Bean
    InsertionTask<VHDEnvelope, ValidatedHistoricalDataMarketDocumentModelV1_04> vhdV104InsertionTask(
            CimConnectorV1_04 cimConnector,
            ValidatedHistoricalDataMarketDocumentV1_04Repository repository
    ) {
        return new InsertionTask<>(cimConnector.getValidatedHistoricalDataMarketDocumentStream(),
                                   repository,
                                   ValidatedHistoricalDataMarketDocumentModelV1_04::new);
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

    @Bean
    DeletionTask<RawDataMessageModel> rdmDeletionTask(
            RawDataMessageRepository repository,
            RestOutboundConnectorConfiguration config
    ) {
        return new DeletionTask<>(repository, config);
    }

    @Bean
    InsertionTask<RawDataMessage, RawDataMessageModel> rdmInsertionTask(
            AgnosticConnector agnosticConnector,
            RawDataMessageRepository repository
    ) {
        return new InsertionTask<>(agnosticConnector.getRawDataMessageStream(),
                                   repository,
                                   RawDataMessageModel::new);
    }
}
