package energy.eddie.regionconnector.dk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import energy.eddie.api.agnostic.RegionConnector;
import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0_82.ConsentMarketDocumentProvider;
import energy.eddie.api.v0_82.cim.config.CommonInformationModelConfiguration;
import energy.eddie.api.v0_82.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.cmd.ConsentMarketDocument;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.regionconnector.dk.energinet.config.EnerginetConfiguration;
import energy.eddie.regionconnector.dk.energinet.config.PlainEnerginetConfiguration;
import energy.eddie.regionconnector.dk.energinet.permission.request.StateBuilderFactory;
import energy.eddie.regionconnector.dk.energinet.permission.request.api.DkEnerginetCustomerPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.permission.request.persistence.DkEnerginetCustomerPermissionRequestRepository;
import energy.eddie.regionconnector.dk.energinet.providers.agnostic.IdentifiableApiResponse;
import energy.eddie.regionconnector.dk.energinet.providers.v0_82.builder.SeriesPeriodBuilderFactory;
import energy.eddie.regionconnector.dk.energinet.providers.v0_82.builder.TimeSeriesBuilderFactory;
import energy.eddie.regionconnector.dk.energinet.providers.v0_82.builder.ValidatedHistoricalDataMarketDocumentBuilderFactory;
import energy.eddie.regionconnector.dk.energinet.services.PollingService;
import energy.eddie.regionconnector.shared.permission.requests.extensions.Extension;
import energy.eddie.regionconnector.shared.permission.requests.extensions.MessagingExtension;
import energy.eddie.regionconnector.shared.permission.requests.extensions.SavingExtension;
import energy.eddie.regionconnector.shared.permission.requests.extensions.v0_82.ConsentMarketDocumentExtension;
import energy.eddie.regionconnector.shared.services.FulfillmentService;
import energy.eddie.regionconnector.shared.services.MeterReadingPermissionUpdateAndFulfillmentService;
import energy.eddie.regionconnector.shared.services.StateFulfillmentService;
import energy.eddie.spring.regionconnector.extensions.cim.v0_82.cmd.CommonConsentMarketDocumentProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.Set;

import static energy.eddie.regionconnector.dk.energinet.EnerginetRegionConnectorMetadata.DK_ZONE_ID;
import static energy.eddie.regionconnector.dk.energinet.EnerginetRegionConnectorMetadata.REGION_CONNECTOR_ID;
import static energy.eddie.regionconnector.dk.energinet.config.EnerginetConfiguration.CUSTOMER_ID_KEY;
import static energy.eddie.regionconnector.dk.energinet.config.EnerginetConfiguration.ENERGINET_CUSTOMER_BASE_PATH_KEY;

@EnableWebMvc
@EnableScheduling
@SpringBootApplication
@RegionConnector(name = REGION_CONNECTOR_ID)
public class DkEnerginetSpringConfig {
    @Bean
    public EnerginetConfiguration energinetConfiguration(
            @Value("${" + ENERGINET_CUSTOMER_BASE_PATH_KEY + "}") String customerBasePath,
            @Value("${" + CUSTOMER_ID_KEY + "}") String customerId
    ) {
        return new PlainEnerginetConfiguration(customerBasePath, customerId);
    }

    @Bean
    public StateBuilderFactory stateBuilderFactory() {
        return new StateBuilderFactory();
    }

    @Bean
    public Sinks.Many<ConnectionStatusMessage> connectionStatusMessageSink() {
        return Sinks.many().multicast().onBackpressureBuffer();
    }

    @Bean
    public Flux<IdentifiableApiResponse> identifiableMeterReadingFlux(PollingService pollingService) {
        return pollingService.identifiableMeterReadings();
    }

    @Bean
    public Sinks.Many<ConsentMarketDocument> consentMarketDocumentSink() {
        return Sinks.many().multicast().onBackpressureBuffer();
    }

    @Bean
    public CommonInformationModelConfiguration cimConfig(
            @Value("${" + CommonInformationModelConfiguration.ELIGIBLE_PARTY_NATIONAL_CODING_SCHEME_KEY + "}")
            String codingScheme
    ) {
        return new PlainCommonInformationModelConfiguration(CodingSchemeTypeList.fromValue(codingScheme));
    }

    @Bean
    public Set<Extension<DkEnerginetCustomerPermissionRequest>> extensions(
            DkEnerginetCustomerPermissionRequestRepository repository,
            Sinks.Many<ConnectionStatusMessage> connectionStatusMessageSink,
            Sinks.Many<ConsentMarketDocument> consentMarketDocumentSink,
            CommonInformationModelConfiguration cimConfig,
            EnerginetConfiguration energinetConfiguration
    ) {
        return Set.of(
                new SavingExtension<>(repository),
                new MessagingExtension<>(connectionStatusMessageSink),
                new ConsentMarketDocumentExtension<>(
                        consentMarketDocumentSink,
                        energinetConfiguration.customerId(),
                        cimConfig.eligiblePartyNationalCodingScheme().value(),
                        DK_ZONE_ID
                )
        );
    }

    @Bean
    public ValidatedHistoricalDataMarketDocumentBuilderFactory validatedHistoricalDataMarketDocumentBuilderFactory(
            EnerginetConfiguration energinetConfiguration,
            CommonInformationModelConfiguration commonInformationModelConfiguration
    ) {
        return new ValidatedHistoricalDataMarketDocumentBuilderFactory(
                energinetConfiguration,
                commonInformationModelConfiguration,
                new TimeSeriesBuilderFactory(new SeriesPeriodBuilderFactory())
        );
    }

    @Bean
    public ConsentMarketDocumentProvider consentMarketDocumentProvider(Sinks.Many<ConsentMarketDocument> sink) {
        return new CommonConsentMarketDocumentProvider(sink);
    }

    @Bean
    public FulfillmentService fulfillmentService() {
        return new StateFulfillmentService();
    }

    @Bean
    public MeterReadingPermissionUpdateAndFulfillmentService meterReadingPermissionUpdateAndFulfillmentService(
            FulfillmentService fulfillmentService
    ) {
        return new MeterReadingPermissionUpdateAndFulfillmentService(fulfillmentService);
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .registerModule(new Jdk8Module());
    }
}
