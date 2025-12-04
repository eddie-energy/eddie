package energy.eddie.regionconnector.de.eta.client;

import energy.eddie.regionconnector.de.eta.oauth.DeEtaOAuthTokenService;
import energy.eddie.regionconnector.de.eta.permission.requests.DeEtaPermissionRequest;
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
                .exchangeToMono(this::mapResponse)
                .retryWhen(Retry.backoff(properties.maxRetries() + 1, Duration.ofMillis(properties.initialBackoffMs()))
                        .maxBackoff(Duration.ofSeconds(5))
                        .filter(TransientPaException.class::isInstance)
                );
    }

    private Mono<SendPermissionResponse> mapResponse(ClientResponse resp) {
        if (resp.statusCode().is2xxSuccessful()) {
            return Mono.just(new SendPermissionResponse(true));
        } else if (resp.statusCode().is4xxClientError()) {
            return Mono.just(new SendPermissionResponse(false));
        } else if (resp.statusCode().is5xxServerError()) {
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
