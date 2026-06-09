package energy.eddie.regionconnector.de.eta.client;

import energy.eddie.regionconnector.de.eta.EtaRegionConnectorMetadata;
import energy.eddie.regionconnector.de.eta.config.DeEtaPlusConfiguration;
import energy.eddie.regionconnector.de.eta.exceptions.EtaPlusClientExceptions.AuthenticationException;
import energy.eddie.regionconnector.de.eta.exceptions.EtaPlusClientExceptions.DeserializationException;
import energy.eddie.regionconnector.de.eta.exceptions.EtaPlusClientExceptions.EtaPlusBadRequestException;
import energy.eddie.regionconnector.de.eta.exceptions.EtaPlusClientExceptions.EtaPlusForbiddenException;
import energy.eddie.regionconnector.de.eta.exceptions.EtaPlusClientExceptions.EtaPlusNotFoundException;
import energy.eddie.regionconnector.de.eta.exceptions.EtaPlusClientExceptions.EtaPlusServerException;
import energy.eddie.regionconnector.de.eta.exceptions.EtaPlusClientExceptions.EtaPlusTimeoutException;
import energy.eddie.regionconnector.de.eta.exceptions.EtaPlusOperationExceptions.RateLimitException;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequest;
import energy.eddie.regionconnector.de.eta.providers.EtaPlusAccountingPointData;
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
     * Fetch validated historical metered data for a permission request over its full range,
     * capped at today (the API holds no data for future dates).
     *
     * @param permissionRequest the permission request containing connection details
     * @param accessToken       the customer's OAuth bearer token
     * @return a Mono emitting the metered data or an error
     */
    public Mono<EtaPlusMeteredData> fetchMeteredData(DePermissionRequest permissionRequest, String accessToken) {
        LocalDate today = LocalDate.now(EtaRegionConnectorMetadata.DE_ZONE_ID);
        LocalDate effectiveEnd = permissionRequest.end().isAfter(today) ? today : permissionRequest.end();
        return fetchMeteredData(permissionRequest, accessToken, permissionRequest.start(), effectiveEnd);
    }

    /**
     * Fetch validated historical metered data for a permission request over an explicit window.
     * Used for retransmission requests, where the eligible party specifies the timeframe. The
     * caller is responsible for supplying a valid window (within the permission range and in the
     * past); see {@link energy.eddie.regionconnector.shared.retransmission.RetransmissionValidation}.
     *
     * @param permissionRequest the permission request containing connection details
     * @param accessToken       the customer's OAuth bearer token
     * @param from              inclusive lower bound of the request window
     * @param to                inclusive upper bound of the request window
     * @return a Mono emitting the metered data or an error
     */
    public Mono<EtaPlusMeteredData> fetchMeteredData(
            DePermissionRequest permissionRequest,
            String accessToken,
            LocalDate from,
            LocalDate to
    ) {
        LOGGER.atInfo()
                .addArgument(permissionRequest::permissionId)
                .addArgument(permissionRequest::meteringPointId)
                .addArgument(() -> from)
                .addArgument(() -> to)
                .log("Fetching metered data for permission request {} with metering point {} from {} to {}");

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(configuration.meteredDataEndpoint())
                        .queryParam("meteringPointId", permissionRequest.meteringPointId())
                        .queryParam("from", from.atStartOfDay())
                        .queryParam("to", to.atStartOfDay())
                        .build())
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToFlux(EtaPlusReadingDto.class)
                .timeout(Duration.ofSeconds(configuration.responseTimeoutSeconds()))
                .collectList()
                .map(readings -> mapToDomain(readings, permissionRequest, from, to))
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
            LocalDate from,
            LocalDate to
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
                from,
                to,
                domainReadings
        );
    }

    /**
     * Fetch accounting point master data for a permission request.
     * Single-shot fetch — caller is responsible for retry / state transitions
     * on failure. Error envelope bodies (4xx/5xx) are not parsed; status code
     * drives the mapping.
     *
     * @param permissionRequest the permission request whose metering point we want
     * @param accessToken       the customer's OAuth bearer
     * @return a Mono emitting the accounting point data or an error
     */
    public Mono<EtaPlusAccountingPointData> fetchAccountingPointData(
            DePermissionRequest permissionRequest,
            String accessToken
    ) {
        LOGGER.atInfo()
              .addArgument(permissionRequest::permissionId)
              .addArgument(permissionRequest::meteringPointId)
              .log("Fetching accounting point data for permission {} on metering point {}");

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(configuration.accountingPointEndpoint())
                        .queryParam("meteringPointId", permissionRequest.meteringPointId())
                        .build())
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(EtaPlusAccountingPointData.class)
                .timeout(Duration.ofSeconds(configuration.responseTimeoutSeconds()))
                .retryWhen(retrySpec(permissionRequest.permissionId()))
                .onErrorMap(WebClientResponseException.BadRequest.class, ex ->
                        new EtaPlusBadRequestException(
                                "Bad request fetching accounting point data for permission " + permissionRequest.permissionId(),
                                ex.getStatusCode().value(), ex))
                .onErrorMap(WebClientResponseException.Unauthorized.class, ex ->
                        new AuthenticationException(
                                "Authentication failed fetching accounting point data for permission " + permissionRequest.permissionId(),
                                ex.getStatusCode().value(), ex))
                .onErrorMap(WebClientResponseException.Forbidden.class, ex ->
                        new EtaPlusForbiddenException(
                                "Forbidden fetching accounting point data for permission " + permissionRequest.permissionId(),
                                ex.getStatusCode().value(), ex))
                .onErrorMap(WebClientResponseException.NotFound.class, ex ->
                        new EtaPlusNotFoundException(
                                "Metering point not found for permission " + permissionRequest.permissionId(),
                                ex.getStatusCode().value(), ex))
                .onErrorMap(WebClientResponseException.TooManyRequests.class, ex ->
                        new RateLimitException(
                                "Rate limit exceeded fetching accounting point data for permission " + permissionRequest.permissionId()))
                .onErrorMap(ex -> ex instanceof WebClientResponseException wce
                                && wce.getStatusCode().is5xxServerError(),
                        ex -> new EtaPlusServerException(
                                "ETA Plus server error fetching accounting point data for permission " + permissionRequest.permissionId(),
                                ((WebClientResponseException) ex).getStatusCode().value(), ex))
                .onErrorMap(DecodingException.class, ex ->
                        new DeserializationException(
                                "Failed to deserialize accounting point data for permission " + permissionRequest.permissionId(),
                                ex))
                .onErrorMap(java.util.concurrent.TimeoutException.class, ex ->
                        new EtaPlusTimeoutException(
                                "Accounting point request timed out for permission " + permissionRequest.permissionId(),
                                ex));
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