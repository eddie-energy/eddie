package energy.eddie.regionconnector.cds.client.customer.data;

import energy.eddie.regionconnector.cds.master.data.CdsServer;
import energy.eddie.regionconnector.cds.oauth.OAuthCredentials;
import energy.eddie.regionconnector.cds.openapi.model.UsageSegmentEndpoint200Response;
import energy.eddie.regionconnector.cds.openapi.model.UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner;
import energy.eddie.regionconnector.cds.permission.requests.CdsPermissionRequest;
import energy.eddie.regionconnector.cds.services.oauth.CustomerDataTokenService;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.List;

public class CustomerDataClient {
    private final WebClient webClient;
    private final CdsServer cdsServer;
    private final CustomerDataTokenService tokenService;

    public CustomerDataClient(WebClient webClient, CdsServer cdsServer, CustomerDataTokenService tokenService) {
        this.webClient = webClient;
        this.cdsServer = cdsServer;
        this.tokenService = tokenService;
    }

    /**
     * @param request permission request that the data is queried for
     * @param before  Filters for all usage segments before this datetime {@code before > usagesegment.datetime}
     * @param after   Filters for all usage segments after this datetime {@code after < usagesegment.datetime}
     * @return all usage segments
     */
    public Mono<List<UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner>> usagePoints(
            CdsPermissionRequest request,
            ZonedDateTime before,
            ZonedDateTime after
    ) {
        return tokenService.getOAuthCredentialsAsync(request.permissionId(), cdsServer)
                           .flatMapMany(token -> usagePoints(token, before, after))
                           .flatMapIterable(UsageSegmentEndpoint200Response::getUsageSegments)
                           .collectList();
    }

    @SuppressWarnings("DataFlowIssue")
    private Flux<UsageSegmentEndpoint200Response> usagePoints(
            OAuthCredentials credentials,
            ZonedDateTime before,
            ZonedDateTime after
    ) {
        var page = webClient.get()
                            .uri(cdsServer.endpoints().usagePointEndpoint().toString(),
                                 uriBuilder -> uriBuilder.queryParam("before", before)
                                                         .queryParam("after", after)
                                                         .build())
                            .headers(h -> h.setBearerAuth(credentials.accessToken()))
                            .retrieve()
                            .bodyToMono(UsageSegmentEndpoint200Response.class);
        return expandUsageSegments(page, credentials);
    }

    @SuppressWarnings("DataFlowIssue")
    private Flux<UsageSegmentEndpoint200Response> usagePoints(URI next, OAuthCredentials credentials) {
        var page = webClient.get()
                            .uri(next)
                            .headers(h -> h.setBearerAuth(credentials.accessToken()))
                            .retrieve()
                            .bodyToMono(UsageSegmentEndpoint200Response.class);
        return expandUsageSegments(page, credentials);
    }

    private Flux<UsageSegmentEndpoint200Response> expandUsageSegments(
            Mono<UsageSegmentEndpoint200Response> response,
            OAuthCredentials credentials
    ) {
        return response
                .expand(res -> {
                    if (res.getNext() == null) {
                        return Mono.empty();
                    }
                    return usagePoints(res.getNext(), credentials);
                });
    }
}
