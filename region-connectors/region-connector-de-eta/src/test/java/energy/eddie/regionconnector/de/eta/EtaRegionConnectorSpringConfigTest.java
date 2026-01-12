package energy.eddie.regionconnector.de.eta;

import energy.eddie.api.agnostic.data.needs.DataNeedCalculationService;
import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.de.eta.config.PlainDeConfiguration;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequestRepository;
import energy.eddie.regionconnector.de.eta.persistence.DePermissionEventRepository;
import energy.eddie.regionconnector.de.eta.service.PollingService;
import energy.eddie.regionconnector.shared.cim.v0_82.TransmissionScheduleProvider;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.services.CommonFutureDataService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.SimpleAsyncTaskScheduler;
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
                .withBean(TaskScheduler.class, SimpleAsyncTaskScheduler::new)
                .withBean(PollingService.class, () -> mock(PollingService.class))
                .withBean(PlainDeConfiguration.class, () -> new PlainDeConfiguration(
                        "party-1",
                        "https://api.eta-plus.de",
                        "client-id",
                        "client-secret",
                        "0 0 17 * * *"
                ))
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
                    assertThat(context).hasSingleBean(CommonFutureDataService.class);
                });
    }
}
