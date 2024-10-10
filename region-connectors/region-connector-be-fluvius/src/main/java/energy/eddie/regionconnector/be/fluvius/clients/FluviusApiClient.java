package energy.eddie.regionconnector.be.fluvius.clients;

import energy.eddie.regionconnector.be.fluvius.client.model.*;
import energy.eddie.regionconnector.be.fluvius.config.FluviusConfiguration;
import energy.eddie.regionconnector.be.fluvius.oauth.OAuthRequestException;
import energy.eddie.regionconnector.be.fluvius.oauth.OAuthTokenService;
import energy.eddie.regionconnector.be.fluvius.permission.request.Flow;
import jakarta.annotation.Priority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Component
@Priority(value = 2)
public class FluviusApiClient implements FluviusApi {
    public static final String QUARTER_HOURLY = "VH_kwartier_uur";
    public static final String OCP_APIM_SUBSCRIPTION_KEY = "Ocp-Apim-Subscription-Key";
    private static final Logger LOGGER = LoggerFactory.getLogger(FluviusApiClient.class);
    private final WebClient webClient;
    private final FluviusConfiguration fluviusConfiguration;
    private final OAuthTokenService oAuthTokenService;

    public FluviusApiClient(
            WebClient webClient,
            FluviusConfiguration fluviusConfiguration,
            OAuthTokenService oAuthTokenService
    ) {
        this.webClient = webClient;
        this.fluviusConfiguration = fluviusConfiguration;
        this.oAuthTokenService = oAuthTokenService;
    }

    @Override
    public Mono<FluviusSessionCreateResultResponseModelApiDataResponse> shortUrlIdentifier(
            String permissionId,
            Flow flow,
            ZonedDateTime from,
            ZonedDateTime to
    ) {
        return fetchAccessToken()
                .flatMap(token -> shortUrlIdentifier(permissionId, flow, from, to, token));
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
            ZonedDateTime to
    ) {
        return fetchAccessToken()
                .flatMap(token -> mockMandate(permissionId, from, to, token));
    }

    private Mono<FluviusSessionCreateResultResponseModelApiDataResponse> shortUrlIdentifier(
            String permissionId,
            Flow flow,
            ZonedDateTime from,
            ZonedDateTime to,
            String token
    ) {
        var start = from.format(DateTimeFormatter.ISO_DATE_TIME);
        var end = to.format(DateTimeFormatter.ISO_DATE_TIME);

        var request = new FluviusSessionRequestModel()
                .dataAccessContractNumber(fluviusConfiguration.contractNumber())
                .referenceNumber(transformPermissionId(permissionId))
                .flow(flow.name())
                .addDataServicesItem(
                        new FluviusSessionRequestDataServiceModel()
                                .dataServiceType(QUARTER_HOURLY)
                                .dataPeriodFrom(start)
                                .dataPeriodTo(end)
                )
                .numberOfEans(1)
                .returnUrlSuccess(null)
                .returnUrlFailed(null)
                .sso(true)
                .enterpriseNumber(null);
        return webClient.post()
                        .uri("/api/v2.0/shortUrlIdentifier")
                        .headers(h -> h.setBearerAuth(token))
                        .header(OCP_APIM_SUBSCRIPTION_KEY,
                                fluviusConfiguration.subscriptionKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(request)
                        .retrieve()
                        .bodyToMono(FluviusSessionCreateResultResponseModelApiDataResponse.class);
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
                        .bodyToMono(GetMandateResponseModelApiDataResponse.class);
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
            ZonedDateTime from,
            ZonedDateTime to,
            String token
    ) {
        var start = from.format(DateTimeFormatter.ISO_DATE_TIME);
        var end = to.format(DateTimeFormatter.ISO_DATE_TIME);
        LOGGER.info("Mocking mandates is enabled, creating a new mock mandate");
        var request = new CreateMandateRequestModel()
                .referenceNumber(transformPermissionId(permissionId))
                .dataServiceType(QUARTER_HOURLY)
                .dataPeriodFrom(start)
                .dataPeriodTo(end)
                .status("Approved")
                .eanNumber("541440110000000001")
                .mandateExpirationDate(end)
                .renewalStatus("ToBeRenewed");
        return webClient.post()
                        .uri("/api/v2.0/Mandate/mock")
                        .headers(h -> h.setBearerAuth(token))
                        .header(OCP_APIM_SUBSCRIPTION_KEY, fluviusConfiguration.subscriptionKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(request)
                        .retrieve()
                        .bodyToMono(CreateMandateResponseModelApiDataResponse.class)
                        .doOnSuccess(res -> LOGGER.info("Created mock mandate for permission request {}",
                                                        permissionId))
                        .doOnError(
                                Throwable.class,
                                t -> LOGGER.warn("Failed to create mock mandate for permission request {}",
                                                 permissionId,
                                                 t)
                        );
    }

    private static String transformPermissionId(String permissionId) {
        return permissionId.replace("-", "");
    }
}
