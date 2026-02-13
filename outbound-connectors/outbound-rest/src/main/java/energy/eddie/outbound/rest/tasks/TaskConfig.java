// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.rest.tasks;

import energy.eddie.api.agnostic.ConnectionStatusMessage;
import energy.eddie.api.agnostic.RawDataMessage;
import energy.eddie.cim.v0_82.ap.AccountingPointEnvelope;
import energy.eddie.cim.v0_82.pmd.PermissionEnvelope;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataEnvelope;
import energy.eddie.cim.v1_04.vhd.VHDEnvelope;
import energy.eddie.outbound.rest.config.RestOutboundConnectorConfiguration;
import energy.eddie.outbound.rest.connectors.AgnosticConnector;
import energy.eddie.outbound.rest.model.ConnectionStatusMessageModel;
import energy.eddie.outbound.rest.model.RawDataMessageModel;
import energy.eddie.outbound.rest.model.cim.v0_82.AccountingPointDataMarketDocumentModel;
import energy.eddie.outbound.rest.model.cim.v0_82.PermissionMarketDocumentModel;
import energy.eddie.outbound.rest.model.cim.v0_82.ValidatedHistoricalDataMarketDocumentModel;
import energy.eddie.outbound.rest.model.cim.v1_04.ValidatedHistoricalDataMarketDocumentModelV1_04;
import energy.eddie.outbound.rest.persistence.ConnectionStatusMessageRepository;
import energy.eddie.outbound.rest.persistence.RawDataMessageRepository;
import energy.eddie.outbound.rest.persistence.cim.v0_82.AccountingPointDataMarketDocumentRepository;
import energy.eddie.outbound.rest.persistence.cim.v0_82.PermissionMarketDocumentRepository;
import energy.eddie.outbound.rest.persistence.cim.v0_82.ValidatedHistoricalDataMarketDocumentRepository;
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
            energy.eddie.outbound.rest.connectors.cim.v0_82.CimConnector cimConnector,
            ValidatedHistoricalDataMarketDocumentRepository repository
    ) {
        return new InsertionTask<>(cimConnector.getHistoricalDataMarketDocumentStream(),
                                   repository,
                                   ValidatedHistoricalDataMarketDocumentModel::new);
    }

    @Bean
    DeletionTask<energy.eddie.outbound.rest.model.cim.v1_04.NearRealTimeDataMarketDocumentModel> rtdV104DeletionTask(
            energy.eddie.outbound.rest.persistence.cim.v1_04.NearRealTimeDataMarketDocumentRepository repository,
            RestOutboundConnectorConfiguration config
    ) {
        return new DeletionTask<>(repository, config);
    }

    @Bean
    InsertionTask<energy.eddie.cim.v1_04.rtd.RTDEnvelope, energy.eddie.outbound.rest.model.cim.v1_04.NearRealTimeDataMarketDocumentModel> rtdV104InsertionTask(
            energy.eddie.outbound.rest.connectors.cim.v1_04.CimConnector cimConnector,
            energy.eddie.outbound.rest.persistence.cim.v1_04.NearRealTimeDataMarketDocumentRepository repository
    ) {
        return new InsertionTask<>(cimConnector.getNearRealTimeDataMarketDocumentStream(),
                                   repository,
                                   energy.eddie.outbound.rest.model.cim.v1_04.NearRealTimeDataMarketDocumentModel::new);
    }

    @Bean
    DeletionTask<energy.eddie.outbound.rest.model.cim.v1_12.NearRealTimeDataMarketDocumentModel> rtdV112DeletionTask(
            energy.eddie.outbound.rest.persistence.cim.v1_12.NearRealTimeDataMarketDocumentRepository repository,
            RestOutboundConnectorConfiguration config
    ) {
        return new DeletionTask<>(repository, config);
    }

    @Bean
    InsertionTask<energy.eddie.cim.v1_12.rtd.RTDEnvelope, energy.eddie.outbound.rest.model.cim.v1_12.NearRealTimeDataMarketDocumentModel> rtdV112InsertionTask(
            energy.eddie.outbound.rest.connectors.cim.v1_12.CimConnector cimConnector,
            energy.eddie.outbound.rest.persistence.cim.v1_12.NearRealTimeDataMarketDocumentRepository repository
    ) {
        return new InsertionTask<>(cimConnector.getNearRealTimeDataMarketDocumentStream(),
                                   repository,
                                   energy.eddie.outbound.rest.model.cim.v1_12.NearRealTimeDataMarketDocumentModel::new);
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
            energy.eddie.outbound.rest.connectors.cim.v0_82.CimConnector cimConnector,
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
            energy.eddie.outbound.rest.connectors.cim.v0_82.CimConnector cimConnector,
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
            energy.eddie.outbound.rest.connectors.cim.v1_04.CimConnector cimConnector,
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
