package energy.eddie.regionconnector.de.eta;

import energy.eddie.api.agnostic.data.needs.DataNeedCalculationService;
import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.dataneeds.rules.DataNeedRuleSet;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.de.eta.data.needs.EtaDataNeedRuleSet;
import energy.eddie.regionconnector.de.eta.persistence.DePermissionRequestRepository;
import energy.eddie.regionconnector.de.eta.persistence.DePermissionEventRepository;
import energy.eddie.regionconnector.shared.cim.v0_82.TransmissionScheduleProvider;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EtaRegionConnectorSpringConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(EtaRegionConnectorSpringConfig.class);

    @Test
    void testBeansAreCreated() {
        contextRunner
                .withBean(DePermissionEventRepository.class, () -> mock(DePermissionEventRepository.class))
                .withBean(DePermissionRequestRepository.class, () -> mock(DePermissionRequestRepository.class))
                .withBean(DataNeedsService.class, () -> mock(DataNeedsService.class))
                .withBean(RegionConnectorMetadata.class, EtaRegionConnectorMetadata::getInstance)
                .withBean(DataNeedRuleSet.class, EtaDataNeedRuleSet::new)
                .withBean(CommonInformationModelConfiguration.class, () -> {
                    CommonInformationModelConfiguration cimConfig = mock(CommonInformationModelConfiguration.class);
                    when(cimConfig.eligiblePartyNationalCodingScheme()).thenReturn(CodingSchemeTypeList.EIC);
                    return cimConfig;
                })
                .withPropertyValues(
                        "region-connector.de.eta.eligible-party-id=party-1",
                        "region-connector.de.eta.api-base-url=https://api.eta-plus.de",
                        "region-connector.de.eta.api-client-id=client-id",
                        "region-connector.de.eta.api-client-secret=client-secret")
                .run(context -> {
                    assertThat(context).hasSingleBean(EventBus.class);
                    assertThat(context).hasSingleBean(Outbox.class);
                    assertThat(context).hasBean("deConnectionStatusMessageHandler");
                    assertThat(context).hasBean("dePermissionMarketDocumentMessageHandler");
                    assertThat(context).hasSingleBean(TransmissionScheduleProvider.class);
                    assertThat(context).hasSingleBean(DataNeedCalculationService.class);
                });
    }
}
