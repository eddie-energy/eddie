package energy.eddie.regionconnector.de.eta.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import energy.eddie.regionconnector.de.eta.EtaRegionConnectorMetadata;
import energy.eddie.regionconnector.de.eta.config.DeEtaPlusConfiguration;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequest;
import energy.eddie.regionconnector.de.eta.providers.EtaPlusMeteredData;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.SSLException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Collections;

@Component
public class EtaPlusApiClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(EtaPlusApiClient.class);
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final DeEtaPlusConfiguration configuration;

    public EtaPlusApiClient(
            WebClient webClient,
            ObjectMapper objectMapper,
            DeEtaPlusConfiguration configuration
    ) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
        this.configuration = configuration;
    }

    public Flux<EtaPlusMeteredData.MeterReading> streamMeteredData(DePermissionRequest permissionRequest) {
        return streamMeteredData(
                permissionRequest.meteringPointId(),
                permissionRequest.start(),
                permissionRequest.end().plusDays(1)
        );
    }

    public Flux<EtaPlusMeteredData.MeterReading> streamMeteredData(String meteringPointId, LocalDate from, LocalDate to) {
        String fromDate = from.format(DateTimeFormatter.ISO_LOCAL_DATE);
        String toDate = to.format(DateTimeFormatter.ISO_LOCAL_DATE);

        LOGGER.info("Opening stream for {} from {} to {}", meteringPointId, fromDate, toDate);

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/meters/historical")
                        .queryParam("meteringPointId", meteringPointId)
                        .queryParam("from", fromDate)
                        .queryParam("to", toDate)
                        .build())
                .retrieve()
                .bodyToMono(EtaPlusMeteredData.class)
                .flatMapMany(response -> {
                    if (response.readings() == null) {
                        return Flux.fromIterable(Collections.emptyList());
                    }
                    return Flux.fromIterable(response.readings());
                })
                .doOnError(e -> LOGGER.error("Stream broken for " + meteringPointId, e));
    }

    public reactor.core.publisher.Mono<Boolean> checkPermissionValidity(DePermissionRequest permissionRequest) {
        return reactor.core.publisher.Mono.just(true);
    }
}
