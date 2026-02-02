package energy.eddie.regionconnector.de.eta.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.regionconnector.de.eta.EtaRegionConnectorMetadata;
import energy.eddie.regionconnector.de.eta.config.PlainDeConfiguration;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequest;
import energy.eddie.regionconnector.de.eta.providers.EtaPlusMeteredData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Client for the ETA Plus API.
 * This client is responsible for fetching validated historical data from the German ETA Plus system.
 */
@Component
public class EtaPlusApiClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(EtaPlusApiClient.class);

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final PlainDeConfiguration configuration;

    public EtaPlusApiClient(
            @Qualifier("etaWebClient") WebClient etaWebClient,
            ObjectMapper objectMapper,
            PlainDeConfiguration configuration
    ) {
        this.webClient = etaWebClient;
        this.objectMapper = objectMapper;
        this.configuration = configuration;
    }

    /**
     * Fetch validated historical metered data for a permission request.
     * @param permissionRequest the permission request containing connection details
     * @return a Mono emitting the metered data or an error
     */
    public Mono<EtaPlusMeteredData> fetchMeteredData(DePermissionRequest permissionRequest) {
        LOGGER.atInfo()
                .addArgument(permissionRequest::permissionId)
                .addArgument(permissionRequest::meteringPointId)
                .addArgument(permissionRequest::start)
                .addArgument(permissionRequest::end)
                .log("Fetching metered data for permission request {} with metering point {} from {} to {}");

        LocalDate today = LocalDate.now(EtaRegionConnectorMetadata.DE_ZONE_ID);
        LocalDate effectiveEnd = permissionRequest.end().isAfter(today) ? today : permissionRequest.end();

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(configuration.meteredDataEndpoint())
                        .queryParam("meteringPointId", permissionRequest.meteringPointId())
                        .queryParam("start", permissionRequest.start())
                        .queryParam("end", effectiveEnd)
                        .build())
                .retrieve()
                .bodyToFlux(EtaPlusReadingDto.class)
                .collectList()
                .map(readings -> mapToDomain(readings, permissionRequest, effectiveEnd));
    }

    /**
     * Maps the API DTOs to the domain model.
     */
    private EtaPlusMeteredData mapToDomain(
            List<EtaPlusReadingDto> readings,
            DePermissionRequest request,
            LocalDate effectiveEnd
    ) {
        List<EtaPlusMeteredData.MeterReading> domainReadings = readings.stream()
                .map(dto -> new EtaPlusMeteredData.MeterReading(
                        dto.timestamp().toString(),
                        dto.value(),
                        dto.unit(),
                        dto.status()
                ))
                .collect(Collectors.toList());

        // Create raw JSON string for RawDataProvider
        String rawJson;
        try {
            rawJson = objectMapper.writeValueAsString(readings);
        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to serialize readings to JSON", e);
            rawJson = "[]";
        }

        return new EtaPlusMeteredData(
                request.meteringPointId(),
                request.start(),
                effectiveEnd,
                domainReadings,
                rawJson
        );
    }

    /**
     * Check if permission is still valid at ETA Plus.
     * Used to verify that the final customer hasn't revoked permission.
     *
     * @param permissionRequest the permission request to check
     * @return a Mono emitting true if permission is valid, false otherwise
     */
    public Mono<Boolean> checkPermissionValidity(DePermissionRequest permissionRequest) {
        LOGGER.atDebug()
                .addArgument(permissionRequest::permissionId)
                .log("Checking permission validity for permission request {}");

        return webClient.head()
                .uri(uriBuilder -> uriBuilder
                        .path(configuration.permissionCheckEndpoint())
                        .build(permissionRequest.permissionId()))
                .retrieve()
                .toBodilessEntity()
                .map(response -> response.getStatusCode().is2xxSuccessful())
                .onErrorResume(e -> {
                    LOGGER.warn("Permission check failed for {}: {}", permissionRequest.permissionId(), e.getMessage());
                    return Mono.just(false);
                });
    }

    /**
     * DTO for mapping the JSON response from ETA Plus API.
     */
    record EtaPlusReadingDto(
            @JsonProperty("timestamp") OffsetDateTime timestamp,
            @JsonProperty("value") double value,
            @JsonProperty("unit") String unit,
            @JsonProperty("status") String status
    ) {}
}