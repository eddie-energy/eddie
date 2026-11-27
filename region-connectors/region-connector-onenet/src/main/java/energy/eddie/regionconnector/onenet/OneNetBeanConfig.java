// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.onenet;

import energy.eddie.api.agnostic.data.needs.DataNeedCalculationService;
import energy.eddie.api.agnostic.process.model.events.PermissionEventRepository;
import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.onenet.data.needs.OneNetDataNeedRuleSet;
import energy.eddie.regionconnector.onenet.permission.request.OneNetPermissionRequest;
import energy.eddie.regionconnector.onenet.persistence.OneNetPermissionEventRepository;
import energy.eddie.regionconnector.onenet.persistence.OneNetPermissionRequestRepository;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.EventBusImpl;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.integration.ConnectionStatusMessageHandler;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.integration.PermissionMarketDocumentMessageHandler;
import energy.eddie.regionconnector.shared.services.data.needs.DataNeedCalculationServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Supplier;

@Configuration
public class OneNetBeanConfig {
    @Bean
    public DataNeedCalculationService dataNeedCalculationService(
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") DataNeedsService dataNeedsService,
            OneNetDataNeedRuleSet dataNeedRuleSet,
            OneNetRegionConnectorMetadata oneNetRegionConnectorMetadata
    ) {
        return new DataNeedCalculationServiceImpl(dataNeedsService, oneNetRegionConnectorMetadata, dataNeedRuleSet);
    }

    @Bean
    public EventBus eventBus() {
        return new EventBusImpl();
    }

    @Bean
    public Outbox outbox(EventBus eventBus, OneNetPermissionEventRepository repository) {
        return new Outbox(eventBus, repository);
    }

    @Bean
    public ConnectionStatusMessageHandler<OneNetPermissionRequest> connectionStatusMessageHandler(
            EventBus eventBus,
            OneNetPermissionRequestRepository oneNetPermissionRequestRepository
    ) {
        return new ConnectionStatusMessageHandler<>(
                eventBus,
                oneNetPermissionRequestRepository,
                pr -> null
        );
    }

    @Bean
    public PermissionMarketDocumentMessageHandler<OneNetPermissionRequest> permissionMarketDocumentMessageHandler(
            EventBus eventBus,
            OneNetPermissionRequestRepository oneNetPermissionRequestRepository,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") DataNeedsService dataNeedsService,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") CommonInformationModelConfiguration cimConfig,
            OneNetRegionConnectorMetadata oneNetRegionConnectorMetadata
    ) {
        return new PermissionMarketDocumentMessageHandler<>(
                eventBus,
                oneNetPermissionRequestRepository,
                dataNeedsService,
                "REPLACE_ME", // TODO: Replace this with the real eligible party ID
                cimConfig,
                pr -> null, // TODO: Replace this with the real transmission schedule provider if one exists
                oneNetRegionConnectorMetadata.timeZone()
        );
    }

    @Bean
    Supplier<PermissionEventRepository> permissionEventSupplier(OneNetPermissionEventRepository repo) {
        return () -> repo;
    }
}
