package energy.eddie.regionconnector.de.eta.client;

import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequest;
import energy.eddie.regionconnector.de.eta.providers.EtaPlusAccountingPointData;
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
import reactor.core.publisher.Flux;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.SSLException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Collections; // Added for safety

@Component
public class EtaPlusApiClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(EtaPlusApiClient.class);
    private final WebClient webClient;

    public EtaPlusApiClient(
            WebClient.Builder webClientBuilder,
            @Value("${REGION_CONNECTOR_DE_ETA_BASEPATH}") String baseUrl,
            @Value("${REGION_CONNECTOR_DE_ETA_USERNAME}") String username,
            @Value("${REGION_CONNECTOR_DE_ETA_PASSWORD}") String password
    ) throws SSLException {

        HttpClient httpClient = HttpClient.create();

        if (baseUrl.startsWith("https")) {
            SslContext sslContext = SslContextBuilder.forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .build();
            httpClient = httpClient.secure(sslSpec -> sslSpec.sslContext(sslContext));
        }

        String credentials = username + ":" + password;
        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader("Authorization", "Basic " + encodedCredentials)
                .build();
    }

    /**
     * Legacy method for full contract fetching.
     */
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

    /**
     * Fetches accounting point data from the ETA Plus MDA API.
     * Accounting point data contains information about the metering point and customer.
     * 
     * Aligns with EDDIE documentation:
     * - https://architecture.eddie.energy/framework/3-extending/region-connector/quickstart.html#accounting-point-data
     * - Requests accounting point data instead of validated historical data when the permission request
     *   contains AccountingPointDataNeed
     * 
     * @param meteringPointId the metering point identifier
     * @return Mono containing the accounting point data
     */
    public reactor.core.publisher.Mono<EtaPlusAccountingPointData> fetchAccountingPointData(String meteringPointId) {
        LOGGER.info("Fetching accounting point data for metering point {}", meteringPointId);

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/meters/accounting-point")
                        .queryParam("meteringPointId", meteringPointId)
                        .build())
                .retrieve()
                .bodyToMono(EtaPlusAccountingPointData.class)
                .doOnError(e -> LOGGER.error("Failed to fetch accounting point data for " + meteringPointId, e))
                .onErrorReturn(createEmptyAccountingPointData(meteringPointId));
    }

    /**
     * Creates an empty accounting point data object as fallback.
     * This should be replaced with proper error handling based on actual API behavior.
     */
    private EtaPlusAccountingPointData createEmptyAccountingPointData(String meteringPointId) {
        return new EtaPlusAccountingPointData(
                meteringPointId,
                null, // customerId
                null, // streetName
                null, // address
                null, // postalCode
                null, // city
                null, // country
                null, // energyType
                null, // voltageLevel
                null, // connectionDate
                null, // status
                "" // rawJson
        );
    }
}