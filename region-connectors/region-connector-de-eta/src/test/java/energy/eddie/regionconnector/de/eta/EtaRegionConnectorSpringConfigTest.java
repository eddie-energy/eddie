package energy.eddie.regionconnector.de.eta;

import energy.eddie.api.agnostic.data.needs.DataNeedCalculationService;
import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.de.eta.config.PlainDeConfiguration;
import energy.eddie.regionconnector.de.eta.data.needs.EtaDataNeedRuleSet;
import energy.eddie.regionconnector.de.eta.persistence.DePermissionRequestRepository;
import energy.eddie.regionconnector.de.eta.persistence.DePermissionEventRepository;
import energy.eddie.regionconnector.shared.cim.v0_82.TransmissionScheduleProvider;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.reactive.function.client.WebClient;

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
                .withBean(EtaDataNeedRuleSet.class, EtaDataNeedRuleSet::new)
                .withPropertyValues(
                        "region-connector.de.eta.eligible-party-id=test-eligible-party-id",
                        "region-connector.de.eta.api-base-url=https://test-url.de",
                        "region-connector.de.eta.api-client-id=test-client-id",
                        "region-connector.de.eta.api-client-secret=test-client-secret"
                )
                .withBean(CommonInformationModelConfiguration.class, () -> {
                    CommonInformationModelConfiguration cimConfig = mock(CommonInformationModelConfiguration.class);
                    when(cimConfig.eligiblePartyNationalCodingScheme()).thenReturn(CodingSchemeTypeList.EIC);
                    return cimConfig;
                })
                .run(context -> {
                    assertThat(context).hasSingleBean(EventBus.class);
                    assertThat(context).hasSingleBean(Outbox.class);
                    assertThat(context).hasBean("deConnectionStatusMessageHandler");
                    assertThat(context).hasBean("dePermissionMarketDocumentMessageHandler");
                    assertThat(context).hasSingleBean(TransmissionScheduleProvider.class);
                    assertThat(context).hasSingleBean(DataNeedCalculationService.class);
                    assertThat(context).hasSingleBean(WebClient.class);
                });
    }
}
