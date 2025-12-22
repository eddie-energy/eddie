package energy.eddie.regionconnector.be.fluvius.client;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.regionconnector.be.fluvius.client.model.*;
import energy.eddie.regionconnector.be.fluvius.config.FluviusConfiguration;
import energy.eddie.regionconnector.be.fluvius.oauth.OAuthRequestException;
import energy.eddie.regionconnector.be.fluvius.oauth.OAuthTokenService;
import energy.eddie.regionconnector.be.fluvius.permission.request.Flow;
import jakarta.annotation.Priority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@Priority(value = 2)
public class FluviusApiClient implements FluviusApi {
    public static final String OCP_APIM_SUBSCRIPTION_KEY = "Ocp-Apim-Subscription-Key";
    public static final String PERIOD_TYPE_READ_TIME = "readTime";
    private static final Logger LOGGER = LoggerFactory.getLogger(FluviusApiClient.class);
    private final WebClient webClient;
    private final FluviusConfiguration fluviusConfiguration;
    private final OAuthTokenService oAuthTokenService;
    private final RedirectUriHelper uriHelper;
    private Health health = Health.unknown().build();

    public FluviusApiClient(
            WebClient webClient,
            FluviusConfiguration fluviusConfiguration,
            OAuthTokenService oAuthTokenService,
            @Value("${region-connector.be.fluvius.redirect-uri}") String publicUrl
    ) {
        this.webClient = webClient;
        this.fluviusConfiguration = fluviusConfiguration;
        this.oAuthTokenService = oAuthTokenService;
        this.uriHelper = new RedirectUriHelper(publicUrl);
    }

    @Override
    public Mono<FluviusSessionCreateResultResponseModelApiDataResponse> shortUrlIdentifier(
            String permissionId,
            Flow flow,
            ZonedDateTime from,
            ZonedDateTime to,
            Granularity granularity
    ) {
        return fetchAccessToken()
                .flatMap(token -> shortUrlIdentifier(permissionId, flow, from, to, granularity, token));
    }

    @Override
    public Mono<GetMandateResponseModelApiDataResponse> mandateFor(String permissionId) {
        return fetchAccessToken()
                .flatMap(token -> mandateFor(permissionId, token));
    }

    @Override
    public Mono<CreateMandateResponseModelApiDataResponse> mockMandate(
            String permissionId,
            ZonedDateTime from,
            ZonedDateTime to,
            String ean,
            Granularity granularity
    ) {
        return fetchAccessToken()
                .flatMap(token -> mockMandate(permissionId, ean, from, to, granularity, token));
    }

    @Override
    public Mono<GetEnergyResponseModelApiDataResponse> energy(
            String permissionId,
            String eanNumber,
            DataServiceType dataServiceType,
            ZonedDateTime from,
            ZonedDateTime to
    ) {
        return fetchAccessToken().flatMap(
                token -> energy(permissionId, eanNumber, dataServiceType, from, to, token)
        );
    }

    public Health health() {
        return health;
    }

    private Mono<GetEnergyResponseModelApiDataResponse> energy(
            String permissionId,
            String eanNumber,
            DataServiceType dataServiceType,
            ZonedDateTime from,
            ZonedDateTime to,
            String token
    ) {
        var fromIso = from.format(DateTimeFormatter.ISO_DATE_TIME);
        var toIso = to.format(DateTimeFormatter.ISO_DATE_TIME);

        return webClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/api/v2.0/Mandate/energy")
                                .queryParam("referenceNumber", transformPermissionId(permissionId))
                                .queryParam("eanNumber", eanNumber)
                                .queryParam("DataServiceType", dataServiceType.value())
                                .queryParam("PeriodType", PERIOD_TYPE_READ_TIME)
                                .queryParam("from", fromIso)
                                .queryParam("to", toIso)
                                .build()
                        )
                        .headers(h -> h.setBearerAuth(token))
                        .header(OCP_APIM_SUBSCRIPTION_KEY,
                                fluviusConfiguration.subscriptionKey())
                        .retrieve()
                        .bodyToMono(GetEnergyResponseModelApiDataResponse.class)
                        .doOnError(WebClientResponseException.class, e -> {
                            var status = e.getStatusCode();
                            // Narrow down status codes that update health to account for expected error responses
                            if (status.is5xxServerError() || status == HttpStatus.REQUEST_TIMEOUT) {
                                health = Health.down(e).build();
                            }
                        })
                        .doOnSuccess(ignored -> health = Health.up().build());
    }

    private Mono<FluviusSessionCreateResultResponseModelApiDataResponse> shortUrlIdentifier(
            String permissionId,
            Flow flow,
            ZonedDateTime from,
            ZonedDateTime to,
            Granularity granularity,
            String token
    ) {
        var start = from.format(DateTimeFormatter.ISO_DATE_TIME);
        var end = to.format(DateTimeFormatter.ISO_DATE_TIME);

        var request = new FluviusSessionRequestModel(
                fluviusConfiguration.contractNumber(),
                transformPermissionId(permissionId),
                flow.name(),
                List.of(
                        new FluviusSessionRequestDataServiceModel(
                                DataServiceType.from(granularity).value(),
                                start,
                                end
                        )
                ),
                1,
                uriHelper.successUri(permissionId),
                uriHelper.rejectUri(permissionId),
                true,
                null
        );
        return webClient.post()
                        .uri("/api/v2.0/shortUrlIdentifier")
                        .headers(h -> h.setBearerAuth(token))
                        .header(OCP_APIM_SUBSCRIPTION_KEY,
                                fluviusConfiguration.subscriptionKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(request)
                        .retrieve()
                        .bodyToMono(FluviusSessionCreateResultResponseModelApiDataResponse.class)
                        .doOnError(Exception.class, err -> health = Health.down(err).build())
                        .doOnSuccess(ignored -> health = Health.up().build());
    }

    private Mono<GetMandateResponseModelApiDataResponse> mandateFor(
            String permissionId,
            String token
    ) {
        return webClient.get()
                        .uri(u -> u.path("/api/v2.0/Mandate")
                                   .queryParam("ReferenceNumber", transformPermissionId(permissionId))
                                   .build())
                        .headers(h -> h.setBearerAuth(token))
                        .header(OCP_APIM_SUBSCRIPTION_KEY,
                                fluviusConfiguration.subscriptionKey())
                        .retrieve()
                        .bodyToMono(GetMandateResponseModelApiDataResponse.class)
                        .doOnError(Exception.class, err -> health = Health.down(err).build())
                        .doOnSuccess(ignored -> health = Health.up().build());
    }

    private Mono<String> fetchAccessToken() {
        return Mono.create(monoSink -> {
            try {
                monoSink.success(this.oAuthTokenService.accessToken());
            } catch (Exception e) {
                LOGGER.warn("Error while fetching access token", e);
                monoSink.error(new OAuthRequestException(e));
            }
        });
    }

    private Mono<CreateMandateResponseModelApiDataResponse> mockMandate(
            String permissionId,
            String ean,
            ZonedDateTime from,
            ZonedDateTime to,
            Granularity granularity,
            String token
    ) {
        var start = from.format(DateTimeFormatter.ISO_DATE_TIME);
        var end = to.format(DateTimeFormatter.ISO_DATE_TIME);
        LOGGER.info("Mocking mandates is enabled, creating a new mock mandate");
        var request = new CreateMandateRequestModel(
                transformPermissionId(permissionId),
                ean,
                DataServiceType.from(granularity).value(),
                start,
                end,
                "Approved",
                end,
                "ToBeRenewed"
        );
        return webClient.post()
                        .uri("/api/v2.0/Mandate/mock")
                        .headers(h -> h.setBearerAuth(token))
                        .header(OCP_APIM_SUBSCRIPTION_KEY, fluviusConfiguration.subscriptionKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(request)
                        .retrieve()
                        .bodyToMono(CreateMandateResponseModelApiDataResponse.class)
                        .doOnSuccess(ignored -> {
                            LOGGER.info("Created mock mandate for permission request {}", permissionId);
                            health = Health.up().build();
                        })
                        .doOnError(
                                Exception.class,
                                err -> {
                                    LOGGER.warn("Failed to create mock mandate for permission request {}",
                                                permissionId,
                                                err);
                                    health = Health.down(err).build();
                                }
                        );
    }

    private static String transformPermissionId(String permissionId) {
        return permissionId.replace("-", "");
    }
}
