package energy.eddie.regionconnector.fr.enedis;

import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.ConsumptionRecord;
import energy.eddie.api.v0.RegionConnector;
import energy.eddie.api.v0.process.model.PermissionRequestRepository;
import energy.eddie.api.v0.process.model.TimeframedPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.api.EnedisApi;
import energy.eddie.regionconnector.fr.enedis.client.EnedisApiClient;
import energy.eddie.regionconnector.fr.enedis.client.EnedisApiClientDecorator;
import energy.eddie.regionconnector.fr.enedis.client.HealthCheckedEnedisApi;
import energy.eddie.regionconnector.fr.enedis.config.EnedisConfiguration;
import energy.eddie.regionconnector.fr.enedis.config.PlainEnedisConfiguration;
import energy.eddie.regionconnector.fr.enedis.permission.request.InMemoryPermissionRequestRepository;
import energy.eddie.regionconnector.fr.enedis.permission.request.PermissionRequestFactory;
import energy.eddie.regionconnector.fr.enedis.services.PermissionRequestService;
import energy.eddie.regionconnector.shared.permission.requests.extensions.Extension;
import energy.eddie.regionconnector.shared.permission.requests.extensions.MessagingExtension;
import energy.eddie.regionconnector.shared.permission.requests.extensions.SavingExtension;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import reactor.core.publisher.Sinks;

import java.util.Set;
import java.util.function.Supplier;

import static energy.eddie.regionconnector.fr.enedis.config.EnedisConfiguration.*;

@SpringBootApplication
@EnableAsync
@EnableRetry
public class SpringConfig {
    @Nullable
    private static ConfigurableApplicationContext ctx;

    public static void main(String[] args) {
        SpringApplication.run(SpringConfig.class, args);
    }

    public static synchronized RegionConnector start() {
        if (ctx == null) {
            var app = new SpringApplicationBuilder(SpringConfig.class)
                    .build();
            // These arguments are needed, since this spring instance tries to load the data needs configs of the core configuration.
            // Random port for this spring application, subject to change in GH-109
            ctx = app.run("--spring.config.import=", "--import.config.file=", "--server.port=0");
        }
        var factory = ctx.getBeanFactory();
        return factory.getBean(RegionConnector.class);
    }

    @Bean
    public EnedisConfiguration enedisConfiguration(
            @Value("${" + ENEDIS_CLIENT_ID_KEY + "}") String clientId,
            @Value("${" + ENEDIS_CLIENT_SECRET_KEY + "}") String clientSecret,
            @Value("${" + ENEDIS_BASE_PATH_KEY + "}") String basePath
    ) {
        return new PlainEnedisConfiguration(clientId, clientSecret, basePath);
    }

    @Bean
    public EnedisApi enedisApi(EnedisConfiguration enedisConfiguration) {
        return new HealthCheckedEnedisApi(
                new EnedisApiClientDecorator(
                        new EnedisApiClient(enedisConfiguration)
                )
        );
    }

    @Bean
    public PermissionRequestRepository<TimeframedPermissionRequest> permissionRequestRepository() {
        return new InMemoryPermissionRequestRepository();
    }

    @Bean
    public Sinks.Many<ConnectionStatusMessage> messages() {
        return Sinks.many().multicast().onBackpressureBuffer();
    }

    @Bean
    public Sinks.Many<ConsumptionRecord> consumptionRecords() {
        return Sinks.many().multicast().onBackpressureBuffer();
    }


    @Bean
    public Set<Extension<TimeframedPermissionRequest>> extensions(PermissionRequestRepository<TimeframedPermissionRequest> repository,
                                                                  Sinks.Many<ConnectionStatusMessage> messages) {
        return Set.of(
                new SavingExtension<>(repository),
                new MessagingExtension<>(messages)
        );
    }

    @Bean
    public PermissionRequestFactory factory(Set<Extension<TimeframedPermissionRequest>> extensions) {
        return new PermissionRequestFactory(extensions);
    }

    @Bean
    public Supplier<Integer> portSupplier(ServletWebServerApplicationContext server) {
        return () -> server.getWebServer().getPort();
    }

    @Bean
    public RegionConnector regionConnector(
            EnedisApi enedisApi,
            PermissionRequestService permissionRequestService,
            Sinks.Many<ConnectionStatusMessage> messages,
            Sinks.Many<ConsumptionRecord> consumptionRecordSink,
            Supplier<Integer> portSupplier
    ) {
        return new EnedisRegionConnector(enedisApi, permissionRequestService, messages, consumptionRecordSink, portSupplier);
    }
}
