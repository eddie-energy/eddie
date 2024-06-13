package energy.eddie.regionconnector.us;

import energy.eddie.api.agnostic.RegionConnector;
import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.EventBusImpl;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.integration.ConnectionStatusMessageHandler;
import energy.eddie.regionconnector.us.green.button.api.GreenButtonApi;
import energy.eddie.regionconnector.us.green.button.client.GreenButtonClient;
import energy.eddie.regionconnector.us.green.button.config.GreenButtonConfiguration;
import energy.eddie.regionconnector.us.green.button.config.PlainGreenButtonConfiguration;
import energy.eddie.regionconnector.us.green.button.permission.request.api.UsGreenButtonPermissionRequest;
import energy.eddie.regionconnector.us.green.button.persistence.UsPermissionEventRepository;
import energy.eddie.regionconnector.us.green.button.persistence.UsPermissionRequestRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.xml.Jaxb2XmlDecoder;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import reactor.core.publisher.Sinks;

import java.util.Map;

import static energy.eddie.regionconnector.us.green.button.GreenButtonRegionConnectorMetadata.REGION_CONNECTOR_ID;
import static energy.eddie.regionconnector.us.green.button.config.GreenButtonConfiguration.*;

@EnableWebMvc
@EnableScheduling
@SpringBootApplication
@RegionConnector(name = REGION_CONNECTOR_ID)
public class GreenButtonSpringConfig {

    @Bean
    public GreenButtonConfiguration greenButtonConfiguration(
            @Value("${" + GREEN_BUTTON_CLIENT_API_TOKEN_KEY + "}") String apiToken,
            @Value("${" + GREEN_BUTTON_BASE_PATH_KEY + "}") String basePath,
            @Value("#{${" + GREEN_BUTTON_CLIENT_IDS_KEY + "}}") Map<String, String> clientIds,
            @Value("#{${" + GREEN_BUTTON_CLIENT_SECRETS_KEY + "}}") Map<String, String> clientSecrets,
            @Value("${" + GREEN_BUTTON_REDIRECT_URL_KEY + "}") String redirectUrl
    ) {
        return new PlainGreenButtonConfiguration(apiToken, basePath, clientIds, clientSecrets, redirectUrl);
    }

    @Bean
    public Sinks.Many<ConnectionStatusMessage> connectionStatusMessageSink() {
        return Sinks.many().multicast().onBackpressureBuffer();
    }

    @Bean
    public ConnectionStatusMessageHandler<UsGreenButtonPermissionRequest> connectionStatusMessageHandler(
            EventBus eventBus,
            Sinks.Many<ConnectionStatusMessage> messages,
            UsPermissionRequestRepository repository
    ) {
        return new ConnectionStatusMessageHandler<>(eventBus, messages, repository, pr -> "");
    }

    @Bean
    public WebClient webClient(GreenButtonConfiguration greenButtonConfiguration) {
        var exchangeStrategies = ExchangeStrategies
                .builder()
                .codecs(clientCodecConfigurer -> clientCodecConfigurer.defaultCodecs().jaxb2Decoder(
                        new Jaxb2XmlDecoder(MediaType.APPLICATION_XML, MediaType.TEXT_XML, MediaType.TEXT_PLAIN)
                ))
                .build();

        return WebClient.builder()
                        .baseUrl(greenButtonConfiguration.basePath())
                        .exchangeStrategies(exchangeStrategies)
                        .defaultHeader(HttpHeaders.ACCEPT, "application/atom+xml")
                        .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + greenButtonConfiguration.apiToken())
                        .build();
    }

    @Bean
    public GreenButtonApi greenButtonApi(WebClient webClient, GreenButtonConfiguration configuration) {
        return new GreenButtonClient(webClient, configuration);
    }

    @Bean
    public EventBus eventBus() {
        return new EventBusImpl();
    }

    @Bean
    public Outbox outbox(EventBus eventBus, UsPermissionEventRepository permissionEventRepository) {
        return new Outbox(eventBus, permissionEventRepository);
    }
}
