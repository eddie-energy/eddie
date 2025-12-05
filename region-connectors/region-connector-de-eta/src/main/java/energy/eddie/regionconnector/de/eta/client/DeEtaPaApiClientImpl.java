package energy.eddie.regionconnector.de.eta.client;

import energy.eddie.regionconnector.de.eta.oauth.DeEtaOAuthTokenService;
import energy.eddie.regionconnector.de.eta.permission.requests.DeEtaPermissionRequest;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Implementation of the PA API client for DE-ETA.
 */
@Component
public class DeEtaPaApiClientImpl implements DeEtaPaApiClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeEtaPaApiClientImpl.class);
    private static final String METRIC_NAME_PA_REQUESTS_FAILED = "pa.requests.failed";
    private static final String TAG_KEY_REGION = "region";
    private static final String REGION_DE_ETA = "de-eta";
    private static final Tags METRIC_TAGS = Tags.of(TAG_KEY_REGION, REGION_DE_ETA);
    private final WebClient webClient;
    private final DeEtaOAuthTokenService tokenService;
    private final DeEtaPaApiProperties properties;

    public DeEtaPaApiClientImpl(
            @Qualifier("deEtaPaWebClient") WebClient webClient,
            DeEtaOAuthTokenService tokenService,
            DeEtaPaApiProperties properties
    ) {
        this.webClient = webClient;
        this.tokenService = tokenService;
        this.properties = properties;
    }

    @Override
    public Mono<SendPermissionResponse> sendPermissionRequest(DeEtaPermissionRequest request) {
        String connectionId = request.connectionId();
        String token = tokenService.getValidAccessToken(connectionId);

        var payload = new PermissionPayload(
                request.permissionId(),
                request.dataNeedId(),
                formatStart(request),
                formatEnd(request),
                request.granularity()
        );

        return webClient.post()
                .uri(properties.paUrl())
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .bodyValue(payload)
                .exchangeToMono(resp -> {
                    // Logging per response
                    var statusVal = resp.statusCode().value();
                    LOGGER.info("DE-ETA PA response permissionId={} connectionId={} status={}",
                            request.permissionId(), connectionId, statusVal);
                    return mapResponse(resp);
                })
                .doOnSubscribe(sub -> {
                    LOGGER.info("Sending DE-ETA PA request permissionId={} connectionId={} granularity={}",
                            request.permissionId(), connectionId, request.granularity());
                    Metrics.counter("pa.requests.sent", METRIC_TAGS).increment();
                })
                .doOnError(ex -> {
                    // Transport/client error (timeouts, connection issues)
                    LOGGER.warn("DE-ETA PA request transport error permissionId={} connectionId={} reason={}",
                            request.permissionId(), connectionId, ex.getClass().getSimpleName());
                    Metrics.counter(METRIC_NAME_PA_REQUESTS_FAILED, METRIC_TAGS.and("type", "transport")).increment();
                })
                .retryWhen(Retry.backoff(properties.maxRetries() + 1L, Duration.ofMillis(properties.initialBackoffMs()))
                        .maxBackoff(Duration.ofSeconds(5))
                        .filter(TransientPaException.class::isInstance)
                        .doBeforeRetry(sig -> LOGGER.warn("Retrying DE-ETA PA request permissionId={} attempt={} reason={}",
                                request.permissionId(), sig.totalRetriesInARow() + 1,
                                sig.failure() == null ? "unknown" : sig.failure().getClass().getSimpleName()))
                );
    }

    private Mono<SendPermissionResponse> mapResponse(ClientResponse resp) {
        if (resp.statusCode().is2xxSuccessful()) {
            return Mono.just(new SendPermissionResponse(true));
        } else if (resp.statusCode().is4xxClientError()) {
            Metrics.counter(METRIC_NAME_PA_REQUESTS_FAILED, METRIC_TAGS.and("status", String.valueOf(resp.statusCode().value())))
                    .increment();
            return Mono.just(new SendPermissionResponse(false));
        } else if (resp.statusCode().is5xxServerError()) {
            Metrics.counter(METRIC_NAME_PA_REQUESTS_FAILED, METRIC_TAGS.and("status", String.valueOf(resp.statusCode().value())))
                    .increment();
            return resp.createException().flatMap(ex -> Mono.<SendPermissionResponse>error(new TransientPaException("PA responded with " + resp.statusCode(), ex)));
        }
        // Treat other statuses as non-successful but non-transient
        return Mono.just(new SendPermissionResponse(false));
    }

    private static String formatStart(DeEtaPermissionRequest pr) {
        if (pr.start() == null) return null;
        return pr.start().atStartOfDay(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    private static String formatEnd(DeEtaPermissionRequest pr) {
        if (pr.end() == null) return null;
        // End of day at 23:59:59Z to include the day fully
        return pr.end().plusDays(1).atStartOfDay(ZoneOffset.UTC).minusSeconds(1)
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    /**
     * Minimal payload structure expected by the PA.
     */
    record PermissionPayload(
            String permissionId,
            String dataNeedId,
            String dataStart,
            String dataEnd,
            String granularity
    ) {}
}
