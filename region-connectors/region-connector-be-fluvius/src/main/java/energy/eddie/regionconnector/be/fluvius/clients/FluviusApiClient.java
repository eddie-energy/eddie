package energy.eddie.regionconnector.be.fluvius.clients;

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
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Component
@Priority(value = 2)
public class FluviusApiClient implements FluviusApi {
    public static final String OCP_APIM_SUBSCRIPTION_KEY = "Ocp-Apim-Subscription-Key";
    private static final Logger LOGGER = LoggerFactory.getLogger(FluviusApiClient.class);
    public static final String PERIOD_TYPE_READ_TIME = "readTime";
    private final WebClient webClient;
    private final FluviusConfiguration fluviusConfiguration;
    private final OAuthTokenService oAuthTokenService;
    private final RedirectUriHelper uriHelper;

    public FluviusApiClient(
            WebClient webClient,
            FluviusConfiguration fluviusConfiguration,
            OAuthTokenService oAuthTokenService,
            @Value("${eddie.public.url}") String publicUrl
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
            ZonedDateTime to,
            String ean
    ) {
        return fetchAccessToken()
                .flatMap(token -> mockMandate(permissionId, ean, from, to, token));
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
                .bodyToMono(GetEnergyResponseModelApiDataResponse.class);
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
                                .dataServiceType(DataServiceType.QUARTER_HOURLY.value())
                                .dataPeriodFrom(start)
                                .dataPeriodTo(end)
                )
                .numberOfEans(1)
                .returnUrlSuccess(uriHelper.successUri(permissionId))
                .returnUrlFailed(uriHelper.rejectUri(permissionId))
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
            String ean,
            ZonedDateTime from,
            ZonedDateTime to,
            String token
    ) {
        var start = from.format(DateTimeFormatter.ISO_DATE_TIME);
        var end = to.format(DateTimeFormatter.ISO_DATE_TIME);
        LOGGER.info("Mocking mandates is enabled, creating a new mock mandate");
        var request = new CreateMandateRequestModel()
                .referenceNumber(transformPermissionId(permissionId))
                .dataServiceType(DataServiceType.QUARTER_HOURLY.value())
                .dataPeriodFrom(start)
                .dataPeriodTo(end)
                .status("Approved")
                .eanNumber(ean)
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
