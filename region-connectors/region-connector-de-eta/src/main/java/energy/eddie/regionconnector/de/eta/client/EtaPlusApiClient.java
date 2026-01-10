package energy.eddie.regionconnector.de.eta.client;

import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequest;
import energy.eddie.regionconnector.de.eta.providers.EtaPlusMeteredData;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.SSLException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

@Component
public class EtaPlusApiClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(EtaPlusApiClient.class);
    private final WebClient webClient;

    /**
     * Constructor builds a specific WebClient for ETA Plus with:
     * 1. Basic Authentication (from properties)
     * 2. Self-signed SSL trust (for localhost development)
     */

    public EtaPlusApiClient(
            WebClient.Builder webClientBuilder,
            @Value("${REGION_CONNECTOR_DE_ETA_BASEPATH}") String baseUrl,
            @Value("${REGION_CONNECTOR_DE_ETA_USERNAME}") String username,
            @Value("${REGION_CONNECTOR_DE_ETA_PASSWORD}") String password
    ) throws SSLException {

        SslContext sslContext = SslContextBuilder.forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .build();

        HttpClient httpClient = HttpClient.create()
                .secure(sslSpec -> sslSpec.sslContext(sslContext));

        String credentials = username + ":" + password;
        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader("Authorization", "Basic " + encodedCredentials)
                .build();
    }

    /**
     * Streams validated historical data item-by-item.
     * Returns a Flux immediately (Low Memory Usage).
     */

    public Flux<EtaPlusMeteredData.MeterReading> streamMeteredData(DePermissionRequest permissionRequest) {
        String meteringPointId = permissionRequest.meteringPointId();

        String fromDate = permissionRequest.start().format(DateTimeFormatter.ISO_LOCAL_DATE);
        String toDate = permissionRequest.end().plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE);

        LOGGER.atInfo()
                .addArgument(permissionRequest::permissionId)
                .addArgument(meteringPointId)
                .log("Opening stream for permission request {}");

        URI uri = UriComponentsBuilder.fromUriString("/api/meters/historical")
                .queryParam("meteringPointId", meteringPointId)
                .queryParam("from", fromDate)
                .queryParam("to", toDate)
                .build()
                .toUri();

        return webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToFlux(EtaPlusMeteredData.MeterReading.class)
                .doOnError(e -> LOGGER.error("Stream broken for " + meteringPointId, e));
    }

    public reactor.core.publisher.Mono<Boolean> checkPermissionValidity(DePermissionRequest permissionRequest) {
        return reactor.core.publisher.Mono.just(true);
    }
}