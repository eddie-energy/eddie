package energy.eddie.regionconnector.dk;

import energy.eddie.api.agnostic.RegionConnector;
import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.ConsumptionRecord;
import energy.eddie.regionconnector.dk.energinet.config.EnerginetConfiguration;
import energy.eddie.regionconnector.dk.energinet.config.PlainEnerginetConfiguration;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.InMemoryPermissionRequestRepository;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.api.DkEnerginetCustomerPermissionRequestRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import reactor.core.publisher.Sinks;

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
        return Sinks.many().unicast().onBackpressureBuffer();
    }

    @Bean
    public DkEnerginetCustomerPermissionRequestRepository permissionRequestRepository() {
        return new InMemoryPermissionRequestRepository();
    }
}
