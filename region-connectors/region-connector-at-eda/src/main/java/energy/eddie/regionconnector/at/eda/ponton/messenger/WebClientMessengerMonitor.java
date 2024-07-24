package energy.eddie.regionconnector.at.eda.ponton.messenger;

import com.fasterxml.jackson.annotation.JsonFormat;
import energy.eddie.regionconnector.at.eda.ponton.PontonXPAdapterConfiguration;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import reactor.util.retry.RetrySpec;

import java.time.ZonedDateTime;
import java.util.List;

@Component
public class WebClientMessengerMonitor implements MessengerMonitor {
    public static final RetrySpec UNAUTHORIZED_FILTER = Retry.max(5)
                                                             .filter(e -> e instanceof WebClientResponseException.Unauthorized);
    public static final String PONTON_DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    private static final Logger LOGGER = LoggerFactory.getLogger(WebClientMessengerMonitor.class);
    private final WebClient webClient;
    private final PontonXPAdapterConfiguration config;
    private final PontonTokenProvider tokenProvider;

    public WebClientMessengerMonitor(
            PontonXPAdapterConfiguration config,
            WebClient webClient,
            PontonTokenProvider tokenProvider
    ) {
        this.webClient = webClient;
        this.config = config;
        this.tokenProvider = tokenProvider;
    }

    @Override
    public void resendFailedMessages(ZonedDateTime date) {
        var request = new ResendRequest(
                List.of("FAILED"),
                List.of(config.adapterId()),
                date
        );
        Mono.defer(() -> tokenProvider
                    .getToken()
                    .flatMap(token -> postResend(token, request))
            )
            .retryWhen(UNAUTHORIZED_FILTER)
            .subscribe(
                    response -> LOGGER.info("Triggered resend of failed messages from PontonXPAdapter for 'fromDate' {}",
                                            date),
                    error -> LOGGER.error("Failed to trigger resend of failed messages from PontonXPAdapter", error)
            );
    }

    private @NotNull Mono<ResponseEntity<Void>> postResend(String token, ResendRequest request) {
        return webClient
                .post()
                .uri(config.apiEndpoint() + "/messagemonitor/resendFailedMessages")
                .headers(headers -> headers.setBearerAuth(token))
                .headers(httpHeaders -> httpHeaders.setAccept(List.of(MediaType.APPLICATION_JSON)))
                .headers(httpHeaders -> httpHeaders.setContentType(MediaType.APPLICATION_JSON))
                .bodyValue(request)
                .retrieve()
                .toBodilessEntity();
    }


    private record ResendRequest(
            List<String> inboundStates,
            List<String> adapterIds,
            @JsonFormat(pattern = PONTON_DATE_PATTERN)
            ZonedDateTime fromDate
    ) {
    }
}
