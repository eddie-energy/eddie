package energy.eddie.regionconnector.de.eta.client;

import energy.eddie.regionconnector.de.eta.EtaRegionConnectorMetadata;
import energy.eddie.regionconnector.de.eta.config.DeEtaPlusConfiguration;
import energy.eddie.regionconnector.de.eta.exceptions.EtaPlusClientExceptions.AuthenticationException;
import energy.eddie.regionconnector.de.eta.exceptions.EtaPlusClientExceptions.DeserializationException;
import energy.eddie.regionconnector.de.eta.exceptions.EtaPlusClientExceptions.EtaPlusServerException;
import energy.eddie.regionconnector.de.eta.exceptions.EtaPlusClientExceptions.EtaPlusTimeoutException;
import energy.eddie.regionconnector.de.eta.exceptions.EtaPlusOperationExceptions.RateLimitException;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequest;
import energy.eddie.regionconnector.de.eta.providers.EtaPlusMeteredData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.codec.DecodingException;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Client for the ETA Plus API.
 * This client is responsible for fetching validated historical data from the German ETA Plus system.
 */
@Component
public class EtaPlusApiClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(EtaPlusApiClient.class);

    private final WebClient webClient;
    private final DeEtaPlusConfiguration configuration;

    public EtaPlusApiClient(
            WebClient webClient,
            DeEtaPlusConfiguration configuration
    ) {
        this.webClient = webClient;
        this.configuration = configuration;
    }

    /**
     * Fetch validated historical metered data for a permission request.
     * @param permissionRequest the permission request containing connection details
     * @return a Mono emitting the metered data or an error
     */
    public Mono<EtaPlusMeteredData> fetchMeteredData(DePermissionRequest permissionRequest, String accessToken) {
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
                        .queryParam("from", permissionRequest.start().atStartOfDay())
                        .queryParam("to", effectiveEnd.atStartOfDay())
                        .build())
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToFlux(EtaPlusReadingDto.class)
                .timeout(Duration.ofSeconds(configuration.responseTimeoutSeconds()))
                .collectList()
                .map(readings -> mapToDomain(readings, permissionRequest, effectiveEnd))
                .retryWhen(retrySpec(permissionRequest.permissionId()))
                .onErrorMap(WebClientResponseException.Unauthorized.class, ex ->
                        new AuthenticationException(
                                "Authentication failed for permission request " + permissionRequest.permissionId(),
                                ex.getStatusCode().value(), ex))
                .onErrorMap(WebClientResponseException.TooManyRequests.class, ex ->
                        new RateLimitException(
                                "Rate limit exceeded for permission request " + permissionRequest.permissionId()))
                .onErrorMap(ex -> ex instanceof WebClientResponseException wce
                                && wce.getStatusCode().is5xxServerError(),
                        ex -> new EtaPlusServerException(
                                "ETA Plus server error for permission request " + permissionRequest.permissionId(),
                                ((WebClientResponseException) ex).getStatusCode().value(), ex))
                .onErrorMap(DecodingException.class, ex ->
                        new DeserializationException(
                                "Failed to deserialize metered data for permission request " + permissionRequest.permissionId(),
                                ex))
                .onErrorMap(java.util.concurrent.TimeoutException.class, ex ->
                        new EtaPlusTimeoutException(
                                "Request timed out for permission request " + permissionRequest.permissionId(),
                                ex));
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
                .filter(dto -> dto.timestamp() != null)
                .map(dto -> new EtaPlusMeteredData.MeterReading(
                        dto.timestamp().toZonedDateTime(),
                        dto.value(),
                        dto.unit(),
                        dto.status(),
                        dto.direction()
                ))
                .toList();

        return new EtaPlusMeteredData(
                request.meteringPointId(),
                request.start(),
                effectiveEnd,
                domainReadings
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
                .retryWhen(retrySpec(permissionRequest.permissionId()))
                .onErrorResume(ex -> {
                    if (ex instanceof WebClientResponseException wce) {
                        int status = wce.getStatusCode().value();
                        if (status == 401) {
                            return Mono.error(new AuthenticationException(
                                    "Authentication failed during permission check for " + permissionRequest.permissionId(),
                                    status, ex));
                        }
                        if (status == 429) {
                            return Mono.error(new RateLimitException(
                                    "Rate limit exceeded during permission check for " + permissionRequest.permissionId()));
                        }
                    }
                    LOGGER.warn("Permission check failed for {}: {}", permissionRequest.permissionId(), ex.getMessage());
                    return Mono.just(false);
                });
    }

    private Retry retrySpec(String permissionId) {
        return Retry.backoff(configuration.retryMaxAttempts(),
                        Duration.ofSeconds(configuration.retryInitialBackoffSeconds()))
                .filter(EtaPlusApiClient::isRetryable)
                .doBeforeRetry(signal -> LOGGER.atWarn()
                        .addArgument(signal.totalRetries() + 1)
                        .addArgument(configuration.retryMaxAttempts())
                        .addArgument(permissionId)
                        .addArgument(signal.failure()::getMessage)
                        .log("Retry attempt {}/{} for permission request {} due to: {}"))
                .onRetryExhaustedThrow((spec, signal) -> signal.failure());
    }

    private static boolean isRetryable(Throwable ex) {
        if (ex instanceof WebClientResponseException wce) {
            return wce.getStatusCode().value() == 429 || wce.getStatusCode().is5xxServerError();
        }
        return false;
    }

    /**
     * DTO for mapping the JSON response from ETA Plus API.
     */
    record EtaPlusReadingDto(
            OffsetDateTime timestamp,
            Double value,
            String unit,
            String status,
            String direction
    ) {}
}