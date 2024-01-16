package energy.eddie.regionconnector.dk;

import energy.eddie.api.agnostic.RegionConnector;
import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.ConsumptionRecord;
import energy.eddie.regionconnector.dk.energinet.config.EnerginetConfiguration;
import energy.eddie.regionconnector.dk.energinet.config.PlainEnerginetConfiguration;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.InMemoryPermissionRequestRepository;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.api.DkEnerginetCustomerPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.api.DkEnerginetCustomerPermissionRequestRepository;
import energy.eddie.regionconnector.shared.permission.requests.extensions.Extension;
import energy.eddie.regionconnector.shared.permission.requests.extensions.MessagingExtension;
import energy.eddie.regionconnector.shared.permission.requests.extensions.SavingExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import reactor.core.publisher.Sinks;

import java.util.Set;

import static energy.eddie.regionconnector.dk.energinet.EnerginetRegionConnectorMetadata.REGION_CONNECTOR_ID;
import static energy.eddie.regionconnector.dk.energinet.config.EnerginetConfiguration.ENERGINET_CUSTOMER_BASE_PATH_KEY;

@EnableWebMvc
@SpringBootApplication
@RegionConnector(name = REGION_CONNECTOR_ID)
public class DkEnerginetSpringConfig {
    @Bean
    public EnerginetConfiguration energinetConfiguration(
            @Value("${" + ENERGINET_CUSTOMER_BASE_PATH_KEY + "}") String customerBasePath) {
        return new PlainEnerginetConfiguration(customerBasePath);
    }

    @Bean
    public Sinks.Many<ConnectionStatusMessage> connectionStatusMessageSink() {
        return Sinks.many().multicast().onBackpressureBuffer();
    }

    @Bean
    public Sinks.Many<ConsumptionRecord> consumptionRecordSink() {
        return Sinks.many().multicast().onBackpressureBuffer();
    }

    @Bean
    public Set<Extension<DkEnerginetCustomerPermissionRequest>> extensions(
            DkEnerginetCustomerPermissionRequestRepository repository,
            Sinks.Many<ConnectionStatusMessage> sink
    ) {
        return Set.of(
                new SavingExtension<>(repository),
                new MessagingExtension<>(sink)
        );
    }

    @Bean
    public DkEnerginetCustomerPermissionRequestRepository permissionRequestRepository() {
        return new InMemoryPermissionRequestRepository();
    }
}
